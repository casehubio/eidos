package io.casehub.eidos.runtime.vocabulary;

import io.casehub.eidos.api.DataDrivenVocabulary;
import io.casehub.eidos.api.DispositionAxis;
import io.casehub.eidos.api.MatchDegree;
import io.casehub.eidos.api.VocabularyInfo;
import io.casehub.eidos.api.VocabularyMetadata;
import io.casehub.eidos.api.VocabularyRegistry;
import io.casehub.eidos.api.VocabularyTerm;
import io.casehub.eidos.api.spi.DataDrivenVocabularyRegistrar;
import io.casehub.eidos.api.spi.VocabularyRegistrar;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CDI-discovered vocabulary registry.
 *
 * <p>Thread safety: {@code register()} is single-threaded — designed for {@code @PostConstruct}
 * initialization. The internal maps are {@code ConcurrentHashMap} for safe concurrent reads
 * after {@code @PostConstruct} completes.
 */
@ApplicationScoped
public class CdiVocabularyRegistry implements VocabularyRegistry {

    private static final Logger LOG = Logger.getLogger(CdiVocabularyRegistry.class);

    @Inject @Any Instance<VocabularyRegistrar> registrars;
    @Inject @Any Instance<DataDrivenVocabularyRegistrar> dataDrivenRegistrars;

    // --- URI-keyed primary maps (ALL vocabs: enum + data-driven) ---
    // uri → (value + aliases) → term
    private final ConcurrentHashMap<String, Map<String, VocabularyTerm>> termsByUri =
        new ConcurrentHashMap<>();
    // uri → terms in declaration/insertion order
    private final ConcurrentHashMap<String, List<? extends VocabularyTerm>> orderedByUri =
        new ConcurrentHashMap<>();
    // uri → vocabulary metadata
    private final ConcurrentHashMap<String, VocabularyInfo> metadataByUri =
        new ConcurrentHashMap<>();

    // --- Class-keyed secondary maps (enum vocabs only) ---
    // vocabUri → enum class
    private final ConcurrentHashMap<String, Class<? extends Enum<?>>> byUri =
        new ConcurrentHashMap<>();
    // class → (value + aliases) → constant  (lookup index)
    private final ConcurrentHashMap<Class<?>, Map<String, VocabularyTerm>> byClass =
        new ConcurrentHashMap<>();
    // class → constants in declaration order  (for allTerms — stored immutably)
    private final ConcurrentHashMap<Class<?>, List<? extends VocabularyTerm>> byClassOrdered =
        new ConcurrentHashMap<>();

    // DAG index — hierarchy / subsumption
    private final ConcurrentHashMap<String, Set<String>> valueToVocabs = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Map<String, List<AncestorEntry>>> ancestorIndex = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Map<String, List<DescendantEntry>>> descendantIndex = new ConcurrentHashMap<>();

    private record AncestorEntry(VocabularyTerm term, int depth, String vocabUri) {}
    private record DescendantEntry(VocabularyTerm term, int depth, String vocabUri) {}

    @PostConstruct
    void init() {
        // Phase 1a: enum registrars
        for (VocabularyRegistrar r : registrars) {
            registerTermsRaw(r.vocabulary());
        }
        // Phase 1b: data-driven registrars
        if (dataDrivenRegistrars != null) {
            for (DataDrivenVocabularyRegistrar r : dataDrivenRegistrars) {
                for (DataDrivenVocabulary v : r.vocabularies()) {
                    registerDataDriven(v);
                }
            }
        }
        // Phase 2: hierarchy
        buildAllHierarchyIndexes();
    }

    /**
     * Bridges the wildcard return type for term-only registration. The cast is safe because any
     * concrete enum returned by the registrar satisfies {@code T extends Enum<T> & VocabularyTerm}
     * at its declaration site — the wildcard just loses that information at the use site.
     */
    @SuppressWarnings("unchecked")
    private <T extends Enum<T> & VocabularyTerm>
    void registerTermsRaw(Class<? extends Enum<? extends VocabularyTerm>> vocab) {
        registerTerms((Class<T>) vocab);
    }

    /**
     * Phase 1 of two-pass registration: validates and stores vocabulary terms without building
     * hierarchy indexes. Populates {@link #byUri}, {@link #byClass}, and {@link #byClassOrdered}.
     * Hierarchy computation happens later in {@link #buildAllHierarchyIndexes(Map)}.
     */
    private <T extends Enum<T> & VocabularyTerm> void registerTerms(Class<T> vocab) {
        var meta = vocab.getAnnotation(VocabularyMetadata.class);
        if (meta == null) {
            throw new IllegalArgumentException(
                "Vocabulary enum " + vocab.getName() + " is missing @VocabularyMetadata");
        }
        var constants = vocab.getEnumConstants();
        if (constants == null || constants.length == 0) {
            throw new IllegalArgumentException(
                "Vocabulary enum " + vocab.getName() + " has no constants");
        }
        var uri = meta.uri();
        if (uri.isBlank()) {
            throw new IllegalArgumentException(
                "Vocabulary URI must not be blank in @VocabularyMetadata on " + vocab.getName());
        }
        // Validate URI and state before any map operations (fast-fail)
        var existing = byUri.get(uri);
        if (existing != null) {
            if (existing != vocab) {
                throw new IllegalArgumentException("URI " + uri + " already registered by "
                    + existing.getName() + "; cannot register " + vocab.getName());
            }
            return;  // same class already registered — idempotent, nothing to do
        }
        // Cross-type collision: data-driven vocab already owns this URI
        if (metadataByUri.containsKey(uri)) {
            throw new IllegalArgumentException("URI " + uri + " already registered as data-driven vocabulary"
                + "; cannot register enum " + vocab.getName());
        }

        // Build ordered list locally — List.copyOf is immutable so allTerms() returns directly
        var orderedList = List.copyOf(Arrays.asList(constants));

        // Build lookup map locally (validates duplicates before writing)
        var lookupMap = new LinkedHashMap<String, VocabularyTerm>();
        for (var constant : constants) {
            if (lookupMap.containsKey(constant.value())) {
                throw new IllegalArgumentException(
                    "Duplicate primary value '" + constant.value() + "' in " + vocab.getName());
            }
            lookupMap.put(constant.value(), constant);
            for (var alias : constant.aliases()) {
                if (lookupMap.containsKey(alias)) {
                    throw new IllegalArgumentException(
                        "Alias '" + alias + "' conflicts with an existing value or alias in "
                            + vocab.getName());
                }
                lookupMap.put(alias, constant);
            }
        }

        // Write class-keyed maps
        byClassOrdered.put(vocab, orderedList);
        byClass.put(vocab, Map.copyOf(lookupMap));
        byUri.put(uri, (Class<? extends Enum<?>>) vocab);

        // Write URI-keyed maps
        termsByUri.put(uri, Map.copyOf(lookupMap));
        orderedByUri.put(uri, orderedList);
        metadataByUri.put(uri, new VocabularyInfo(meta.uri(), meta.name(), meta.version(), meta.description()));
    }

    private void registerDataDriven(DataDrivenVocabulary vocab) {
        var uri = vocab.uri();
        if (uri == null || uri.isBlank()) {
            throw new IllegalArgumentException("DataDrivenVocabulary URI must not be blank");
        }
        // Cross-type collision: enum vocab already owns this URI
        if (byUri.containsKey(uri)) {
            throw new IllegalArgumentException("URI " + uri + " already registered as enum vocabulary"
                                               + "; cannot register data-driven vocabulary");
        }
        // Same-type collision: another data-driven vocab already owns this URI
        if (metadataByUri.containsKey(uri)) {
            throw new IllegalArgumentException("URI " + uri + " already registered"
                                               + "; cannot register duplicate data-driven vocabulary");
        }

        var orderedList = List.<VocabularyTerm>copyOf(vocab.terms());

        // Build lookup map with alias collision detection
        var lookupMap = new LinkedHashMap<String, VocabularyTerm>();
        for (var term : vocab.terms()) {
            if (lookupMap.containsKey(term.value())) {
                throw new IllegalArgumentException(
                        "Duplicate primary value '" + term.value() + "' in data-driven vocabulary " + uri);
            }
            lookupMap.put(term.value(), term);
            for (var alias : term.aliases()) {
                if (lookupMap.containsKey(alias)) {
                    throw new IllegalArgumentException(
                            "Alias '" + alias + "' conflicts with an existing value or alias in "
                            + "data-driven vocabulary " + uri);
                }
                lookupMap.put(alias, term);
            }
        }

        // Write URI-keyed maps only (no class-keyed maps for data-driven)
        termsByUri.put(uri, Map.copyOf(lookupMap));
        orderedByUri.put(uri, orderedList);
        metadataByUri.put(uri, new VocabularyInfo(uri, vocab.name(), vocab.version(), vocab.description()));
    }


    /**
     * Phase 2 of two-pass registration: builds a GLOBAL DAG across all vocabularies,
     * computes ancestors/descendants with global BFS, then populates per-vocabulary
     * indexes with cross-vocabulary injection.
     *
     * <p>Iterates {@link #orderedByUri} which contains both enum and data-driven terms
     * after Phase 1a and 1b complete. Uses {@code termToUri} map for parent vocabulary
     * resolution instead of {@code getDeclaringClass()} casts.
     */
    private void buildAllHierarchyIndexes() {
        // 1. Collect ALL terms from ALL vocabularies into global structures
        Map<VocabularyTerm, String>               termToUri   = new HashMap<>();
        Map<VocabularyTerm, List<VocabularyTerm>> globalEdges = new HashMap<>();
        Map<VocabularyTerm, Integer>              inDegree    = new HashMap<>();
        List<VocabularyTerm>                      allTerms    = new ArrayList<>();

        for (var entry : orderedByUri.entrySet()) {
            String uri = entry.getKey();
            for (var term : entry.getValue()) {
                termToUri.put(term, uri);
                inDegree.put(term, 0);
                globalEdges.put(term, new ArrayList<>());
                allTerms.add(term);
            }
        }

        // 2. Build global edge map and validate parent references
        for (var term : allTerms) {
            String childVocabUri = termToUri.get(term);
            for (var parent : term.specializes()) {
                String parentVocabUri = termToUri.get(parent);
                if (parentVocabUri == null) {
                    throw new IllegalArgumentException(
                            "Cross-vocabulary specializes() references unregistered vocabulary: "
                            + term.value() + " in " + childVocabUri
                            + " specializes " + parent.value()
                            + ", but that term is not in any registered vocabulary");
                }
                if (!childVocabUri.equals(parentVocabUri)) {
                    LOG.infof("Cross-vocabulary specialization: %s in %s specializes %s in %s",
                              term.value(), childVocabUri, parent.value(), parentVocabUri);
                }
                globalEdges.get(parent).add(term);
                inDegree.merge(term, 1, Integer::sum);
            }
        }

        // 3. Global cycle detection via Kahn's algorithm
        Queue<VocabularyTerm> queue = new ArrayDeque<>();
        for (var term : allTerms) {
            if (inDegree.get(term) == 0) {queue.add(term);}
        }
        int processed = 0;
        while (!queue.isEmpty()) {
            var node = queue.poll();
            processed++;
            for (var child : globalEdges.get(node)) {
                int newDegree = inDegree.get(child) - 1;
                inDegree.put(child, newDegree);
                if (newDegree == 0) {queue.add(child);}
            }
        }
        if (processed != allTerms.size()) {
            var cycleTerms = inDegree.entrySet().stream()
                                     .filter(e -> e.getValue() > 0)
                                     .map(e -> e.getKey().value() + " (" + termToUri.get(e.getKey()) + ")")
                                     .sorted()
                                     .toList();
            throw new IllegalArgumentException(
                    "Cycle detected in specializes() hierarchy; terms involved: " + cycleTerms);
        }

        // 4. Global BFS to compute ancestors
        Map<VocabularyTerm, List<AncestorEntry>> globalAncestors = new HashMap<>();
        for (var term : allTerms) {
            List<AncestorEntry>          ancestors = new ArrayList<>();
            Map<VocabularyTerm, Integer> visited   = new HashMap<>();
            Queue<AncestorEntry>         bfsQueue  = new ArrayDeque<>();
            for (var parent : term.specializes()) {
                bfsQueue.add(new AncestorEntry(parent, 1, termToUri.get(parent)));
                visited.put(parent, 1);
            }
            while (!bfsQueue.isEmpty()) {
                var ae = bfsQueue.poll();
                if (visited.getOrDefault(ae.term, Integer.MAX_VALUE) < ae.depth) {continue;}
                ancestors.add(ae);
                for (var grandparent : ae.term.specializes()) {
                    int newDepth = ae.depth + 1;
                    if (visited.getOrDefault(grandparent, Integer.MAX_VALUE) > newDepth) {
                        visited.put(grandparent, newDepth);
                        bfsQueue.add(new AncestorEntry(grandparent, newDepth,
                                                       termToUri.get(grandparent)));
                    }
                }
            }
            ancestors.sort(Comparator.comparingInt(AncestorEntry::depth));
            globalAncestors.put(term, List.copyOf(ancestors));
        }

        // 5. Compute global descendants by inverting ancestor relationships
        Map<VocabularyTerm, List<DescendantEntry>> globalDescendants = new HashMap<>();
        for (var term : allTerms) {
            globalDescendants.put(term, new ArrayList<>());
        }
        for (var term : allTerms) {
            for (var ae : globalAncestors.get(term)) {
                globalDescendants.get(ae.term).add(
                        new DescendantEntry(term, ae.depth, termToUri.get(term)));
            }
        }
        for (var entry : globalDescendants.entrySet()) {
            entry.getValue().sort(Comparator.comparingInt(DescendantEntry::depth));
        }

        // 6. Build per-vocabulary indexes with cross-vocabulary injection
        var sortedUris = orderedByUri.keySet().stream().sorted().toList();

        Map<String, Map<String, List<AncestorEntry>>>   newAncestorIndex   = new HashMap<>();
        Map<String, Map<String, List<DescendantEntry>>> newDescendantIndex = new HashMap<>();
        Map<String, Set<String>>                        newValueToVocabs   = new HashMap<>();

        for (var uri : sortedUris) {
            var                                vocabTerms            = orderedByUri.get(uri);
            Map<String, List<AncestorEntry>>   vocabAncestorMap      = new HashMap<>();
            Map<String, List<DescendantEntry>> vocabDescendantMap    = new HashMap<>();
            Map<String, String>                injectedValueToSource = new HashMap<>();

            // 6a. Native entries
            for (var term : vocabTerms) {
                vocabAncestorMap.put(term.value(), globalAncestors.get(term));
                vocabDescendantMap.put(term.value(), List.copyOf(globalDescendants.get(term)));
                newValueToVocabs.computeIfAbsent(term.value(), k -> new HashSet<>()).add(uri);
            }

            // 6b. Injection: terms from OTHER vocabularies with an ancestor in THIS vocabulary
            Set<String> nativeValues = new HashSet<>();
            for (var term : vocabTerms) {
                nativeValues.add(term.value());
            }

            for (var term : allTerms) {
                String nativeUri = termToUri.get(term);
                if (nativeUri.equals(uri)) {continue;}

                boolean hasAncestorInV = false;
                for (var ae : globalAncestors.get(term)) {
                    if (termToUri.get(ae.term).equals(uri)) {
                        hasAncestorInV = true;
                        break;
                    }
                }
                if (!hasAncestorInV) {continue;}

                String value = term.value();
                if (vocabAncestorMap.containsKey(value)) {
                    boolean existingIsNative = nativeValues.contains(value);
                    String  existingSource   = existingIsNative ? uri : injectedValueToSource.get(value);
                    throw new IllegalArgumentException(
                            "Value collision in index for '" + uri + "': '" + value
                            + "' from '" + existingSource + "' collides with '"
                            + value + "' from '" + nativeUri + "'");
                }

                vocabAncestorMap.put(value, globalAncestors.get(term));
                vocabDescendantMap.put(value, List.copyOf(globalDescendants.get(term)));
                injectedValueToSource.put(value, nativeUri);
            }

            newAncestorIndex.put(uri, Map.copyOf(vocabAncestorMap));
            newDescendantIndex.put(uri, Map.copyOf(vocabDescendantMap));
        }

        // 7. Atomic write
        for (var entry : newAncestorIndex.entrySet()) {
            ancestorIndex.put(entry.getKey(), entry.getValue());
        }
        for (var entry : newDescendantIndex.entrySet()) {
            descendantIndex.put(entry.getKey(), entry.getValue());
        }
        for (var entry : newValueToVocabs.entrySet()) {
            valueToVocabs.computeIfAbsent(entry.getKey(), k -> ConcurrentHashMap.newKeySet())
                         .addAll(entry.getValue());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Enum<T> & VocabularyTerm> void register(Class<T> vocab) {
        var meta = vocab.getAnnotation(VocabularyMetadata.class);
        if (meta == null) {
            throw new IllegalArgumentException(
                    "Vocabulary enum " + vocab.getName() + " is missing @VocabularyMetadata");
        }
        var uri = meta.uri();

        // Check if already registered (idempotent)
        var existing = byUri.get(uri);
        if (existing != null) {
            if (existing == vocab) {
                return;  // same class — idempotent
            }
            // different class with same URI — registerTerms will throw
        }

        // Save current state for rollback
        var savedByUri           = new HashMap<>(byUri);
        var savedByClass         = new HashMap<>(byClass);
        var savedByClassOrdered  = new HashMap<>(byClassOrdered);
        var savedTermsByUri      = new HashMap<>(termsByUri);
        var savedOrderedByUri    = new HashMap<>(orderedByUri);
        var savedMetadataByUri   = new HashMap<>(metadataByUri);
        var savedAncestorIndex   = new HashMap<>(ancestorIndex);
        var savedDescendantIndex = new HashMap<>(descendantIndex);
        var savedValueToVocabs   = new HashMap<String, Set<String>>();
        valueToVocabs.forEach((k, v) -> savedValueToVocabs.put(k, new HashSet<>(v)));

        try {
            registerTerms(vocab);
            buildAllHierarchyIndexes();
        } catch (Exception e) {
            byUri.clear();
            byUri.putAll(savedByUri);
            byClass.clear();
            byClass.putAll(savedByClass);
            byClassOrdered.clear();
            byClassOrdered.putAll(savedByClassOrdered);
            termsByUri.clear();
            termsByUri.putAll(savedTermsByUri);
            orderedByUri.clear();
            orderedByUri.putAll(savedOrderedByUri);
            metadataByUri.clear();
            metadataByUri.putAll(savedMetadataByUri);
            ancestorIndex.clear();
            ancestorIndex.putAll(savedAncestorIndex);
            descendantIndex.clear();
            descendantIndex.putAll(savedDescendantIndex);
            valueToVocabs.clear();
            valueToVocabs.putAll(savedValueToVocabs);
            throw e;
        }
    }

    @Override
    public boolean isRegistered(String vocabUri) {
        return metadataByUri.containsKey(vocabUri);
    }

    @Override
    public Set<String> registeredUris() {
        return Set.copyOf(metadataByUri.keySet());
    }


    @Override
    public Optional<? extends VocabularyTerm> resolve(String vocabUri, String value) {
        var lookup = termsByUri.get(vocabUri);
        if (lookup == null) return Optional.empty();
        return Optional.ofNullable(lookup.get(value));
    }

    @Override
    public List<? extends VocabularyTerm> allTerms(String vocabUri) {
        var ordered = orderedByUri.get(vocabUri);
        return ordered == null ? List.of() : ordered;
    }

    @Override
    public Optional<String> equivalentValues(String fromUri, String value, String toUri) {
        // exactMatch requires a backing Class — only works for enum vocabs
        var sourceClass = byUri.get(fromUri);
        if (sourceClass == null) return Optional.empty();
        var term = byClass.get(sourceClass).get(value);
        if (term == null) return Optional.empty();
        var targetClass = byUri.get(toUri);
        if (targetClass == null) return Optional.empty();
        return term.exactMatch(targetClass).map(VocabularyTerm::value);
    }

    @Override
    public Optional<String> equivalentValues(String fromUri, String value, String toUri,
                                              DispositionAxis axis) {
        // axisExactMatch requires a backing Class — only works for enum vocabs
        var sourceClass = byUri.get(fromUri);
        if (sourceClass == null) return Optional.empty();
        var term = byClass.get(sourceClass).get(value);
        if (term == null) return Optional.empty();
        var targetClass = byUri.get(toUri);
        if (targetClass == null) return Optional.empty();
        return term.axisExactMatch(targetClass, axis).map(VocabularyTerm::value);
    }

    @Override
    public <T extends Enum<T> & VocabularyTerm> Optional<T> resolve(Class<T> vocab, String value) {
        var lookup = byClass.get(vocab);
        return lookup == null
            ? Optional.empty()
            : Optional.ofNullable(vocab.cast(lookup.get(value)));
    }

    @Override
    public <S extends Enum<S> & VocabularyTerm, T extends Enum<T> & VocabularyTerm>
    Optional<T> equivalentValues(S from, Class<T> targetVocab) {
        return from.exactMatch(targetVocab).map(targetVocab::cast);
    }

    @Override
    public <S extends Enum<S> & VocabularyTerm, T extends Enum<T> & VocabularyTerm>
    Optional<T> equivalentValues(S from, Class<T> targetVocab, DispositionAxis axis) {
        return from.axisExactMatch(targetVocab, axis).map(targetVocab::cast);
    }

    @Override
    public Optional<VocabularyInfo> vocabularyMetadata(String uri) {
        return Optional.ofNullable(metadataByUri.get(uri));
    }

    @Override
    public void register(String uri, String name, String version, String description,
                         List<? extends VocabularyTerm> terms) {
        // Check if already registered
        if (metadataByUri.containsKey(uri)) {
            throw new IllegalArgumentException("URI " + uri + " already registered"
                + "; cannot register duplicate vocabulary");
        }

        // Save state for rollback
        var savedTermsByUri = new HashMap<>(termsByUri);
        var savedOrderedByUri = new HashMap<>(orderedByUri);
        var savedMetadataByUri = new HashMap<>(metadataByUri);
        var savedAncestorIndex = new HashMap<>(ancestorIndex);
        var savedDescendantIndex = new HashMap<>(descendantIndex);
        var savedValueToVocabs = new HashMap<String, Set<String>>();
        valueToVocabs.forEach((k, v) -> savedValueToVocabs.put(k, new HashSet<>(v)));

        try {
            // Build lookup map
            var lookupMap = new LinkedHashMap<String, VocabularyTerm>();
            for (var term : terms) {
                if (lookupMap.containsKey(term.value())) {
                    throw new IllegalArgumentException(
                        "Duplicate primary value '" + term.value() + "' in vocabulary " + uri);
                }
                lookupMap.put(term.value(), term);
                for (var alias : term.aliases()) {
                    if (lookupMap.containsKey(alias)) {
                        throw new IllegalArgumentException(
                            "Alias '" + alias + "' conflicts in vocabulary " + uri);
                    }
                    lookupMap.put(alias, term);
                }
            }

            termsByUri.put(uri, Map.copyOf(lookupMap));
            orderedByUri.put(uri, List.copyOf(terms));
            metadataByUri.put(uri, new VocabularyInfo(uri, name != null ? name : "",
                version != null ? version : "", description != null ? description : ""));
            buildAllHierarchyIndexes();
        } catch (Exception e) {
            termsByUri.clear(); termsByUri.putAll(savedTermsByUri);
            orderedByUri.clear(); orderedByUri.putAll(savedOrderedByUri);
            metadataByUri.clear(); metadataByUri.putAll(savedMetadataByUri);
            ancestorIndex.clear(); ancestorIndex.putAll(savedAncestorIndex);
            descendantIndex.clear(); descendantIndex.putAll(savedDescendantIndex);
            valueToVocabs.clear(); valueToVocabs.putAll(savedValueToVocabs);
            throw e;
        }
    }


    @Override
    public boolean subsumes(String vocabUri, String generalValue, String specificValue) {
        var ancestorMap = ancestorIndex.get(vocabUri);
        if (ancestorMap == null) return false;
        if (generalValue.equals(specificValue)) return true;
        var ancestors = ancestorMap.get(specificValue);
        if (ancestors == null) return false;
        return ancestors.stream().anyMatch(e -> e.term.value().equals(generalValue));
    }

    @Override
    public MatchDegree match(String vocabUri, String declaredValue, String requestedValue) {
        if (declaredValue.equals(requestedValue)) return new MatchDegree.Exact();
        var ancestorMap = ancestorIndex.get(vocabUri);
        if (ancestorMap == null) return new MatchDegree.None();
        // Plugin: declared is ancestor of requested (requested specializes declared)
        var requestedAncestors = ancestorMap.get(requestedValue);
        if (requestedAncestors != null) {
            for (var entry : requestedAncestors) {
                if (entry.term.value().equals(declaredValue)) {
                    return new MatchDegree.Plugin(entry.depth);
                }
            }
        }
        // Specialization: declared is descendant of requested (declared specializes requested)
        var declaredAncestors = ancestorMap.get(declaredValue);
        if (declaredAncestors != null) {
            for (var entry : declaredAncestors) {
                if (entry.term.value().equals(requestedValue)) {
                    return new MatchDegree.Specialization(entry.depth);
                }
            }
        }
        return new MatchDegree.None();
    }

    @Override
    public List<? extends VocabularyTerm> ancestors(String vocabUri, String value) {
        var ancestorMap = ancestorIndex.get(vocabUri);
        if (ancestorMap == null) return List.of();
        var ancestors = ancestorMap.get(value);
        if (ancestors == null) return List.of();
        return ancestors.stream().map(AncestorEntry::term).toList();
    }

    @Override
    public List<? extends VocabularyTerm> descendants(String vocabUri, String value) {
        var descendantMap = descendantIndex.get(vocabUri);
        if (descendantMap == null) return List.of();
        var descendants = descendantMap.get(value);
        if (descendants == null) return List.of();
        return descendants.stream().map(DescendantEntry::term).toList();
    }

    @Override
    public Map<String, Set<String>> expandForMatchingByVocabulary(String value) {
        var vocabs = valueToVocabs.get(value);
        if (vocabs == null || vocabs.isEmpty()) return Map.of();
        Map<String, Set<String>> result = new HashMap<>();

        // Add self-value under each declaring vocabulary
        for (var vocabUri : vocabs) {
            result.computeIfAbsent(vocabUri, k -> new HashSet<>()).add(value);
        }

        // Process each declaring vocabulary's indexes for ancestors/descendants
        for (var vocabUri : vocabs) {
            // Add ancestors — grouped by their declaring vocabulary (entry.vocabUri)
            var ancestorMap = ancestorIndex.get(vocabUri);
            if (ancestorMap != null) {
                var ancestors = ancestorMap.get(value);
                if (ancestors != null) {
                    for (var entry : ancestors) {
                        result.computeIfAbsent(entry.vocabUri(), k -> new HashSet<>())
                            .add(entry.term().value());
                    }
                }
            }
            // Add descendants — grouped by their declaring vocabulary (entry.vocabUri)
            var descendantMap = descendantIndex.get(vocabUri);
            if (descendantMap != null) {
                var descendants = descendantMap.get(value);
                if (descendants != null) {
                    for (var entry : descendants) {
                        result.computeIfAbsent(entry.vocabUri(), k -> new HashSet<>())
                            .add(entry.term().value());
                    }
                }
            }
        }
        return result;
    }
}
