package io.casehub.eidos.runtime.vocabulary;

import io.casehub.eidos.api.DispositionAxis;
import io.casehub.eidos.api.MatchDegree;
import io.casehub.eidos.api.VocabularyMetadata;
import io.casehub.eidos.api.VocabularyRegistry;
import io.casehub.eidos.api.VocabularyTerm;
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
        for (VocabularyRegistrar r : registrars) {
            registerTermsRaw(r.vocabulary());
        }
        buildAllHierarchyIndexes(Map.copyOf(byUri));
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

        // Write all three maps
        byClassOrdered.put(vocab, orderedList);
        byClass.put(vocab, Map.copyOf(lookupMap));
        byUri.put(uri, (Class<? extends Enum<?>>) vocab);
    }

    /**
     * Phase 2 of two-pass registration: builds a GLOBAL DAG across all vocabularies in the
     * snapshot, computes ancestors/descendants with global BFS, then populates per-vocabulary
     * indexes with cross-vocabulary injection.
     *
     * <p>Pure function of its input — does not read from class-level {@link #byUri}/{@link #byClass}
     * during computation. Only writes to class-level maps after ALL validation passes
     * (compute-validate-write atomicity).
     *
     * @param vocabSnapshot immutable snapshot of all registered vocabularies (uri → enum class)
     */
    private void buildAllHierarchyIndexes(Map<String, Class<? extends Enum<?>>> vocabSnapshot) {
        // Build reverse lookup: enum class → vocab URI
        Map<Class<?>, String> classToUri = new HashMap<>();
        for (var entry : vocabSnapshot.entrySet()) {
            classToUri.put(entry.getValue(), entry.getKey());
        }

        // 1. Collect ALL terms from ALL vocabularies into global structures
        //    termToUri: term → native vocab URI (the vocabulary that declares it)
        Map<VocabularyTerm, String> termToUri = new HashMap<>();
        Map<VocabularyTerm, List<VocabularyTerm>> globalEdges = new HashMap<>(); // parent → children
        Map<VocabularyTerm, Integer> inDegree = new HashMap<>();
        List<VocabularyTerm> allTerms = new ArrayList<>();

        for (var entry : vocabSnapshot.entrySet()) {
            String uri = entry.getKey();
            var vocab = entry.getValue();
            for (var constant : vocab.getEnumConstants()) {
                var term = (VocabularyTerm) constant;
                termToUri.put(term, uri);
                inDegree.put(term, 0);
                globalEdges.put(term, new ArrayList<>());
                allTerms.add(term);
            }
        }

        // 2. Build global edge map and validate cross-vocabulary parent references
        for (var term : allTerms) {
            String childVocabUri = termToUri.get(term);
            for (var parent : term.specializes()) {
                // Validate: parent's vocabulary must be in the snapshot
                @SuppressWarnings("unchecked")
                Class<? extends Enum<?>> parentDeclClass =
                    (Class<? extends Enum<?>>) ((Enum<?>) parent).getDeclaringClass();
                String parentVocabUri = classToUri.get(parentDeclClass);
                if (parentVocabUri == null) {
                    throw new IllegalArgumentException(
                        "Cross-vocabulary specializes() references unregistered vocabulary: "
                            + term.value() + " in " + childVocabUri
                            + " specializes " + parent.value()
                            + " from " + parentDeclClass.getName()
                            + ", but " + parentDeclClass.getName() + " is not registered");
                }
                // Validate: parent term exists in the global term set
                if (!termToUri.containsKey(parent)) {
                    throw new IllegalArgumentException(
                        "specializes() references unknown term: " + term.value()
                            + " in " + childVocabUri + " specializes " + parent.value()
                            + " from " + parentVocabUri + ", but that term is not registered");
                }
                // Log cross-vocabulary edges for observability
                if (!childVocabUri.equals(parentVocabUri)) {
                    LOG.infof("Cross-vocabulary specialization: %s in %s specializes %s in %s",
                        term.value(), childVocabUri, parent.value(), parentVocabUri);
                }
                globalEdges.get(parent).add(term);
                inDegree.merge(term, 1, Integer::sum);
            }
        }

        // 3. Global cycle detection via Kahn's algorithm (topological sort)
        Queue<VocabularyTerm> queue = new ArrayDeque<>();
        for (var term : allTerms) {
            if (inDegree.get(term) == 0) queue.add(term);
        }
        int processed = 0;
        while (!queue.isEmpty()) {
            var node = queue.poll();
            processed++;
            for (var child : globalEdges.get(node)) {
                int newDegree = inDegree.get(child) - 1;
                inDegree.put(child, newDegree);
                if (newDegree == 0) queue.add(child);
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

        // 4. Global BFS from each term to compute ancestors with min depth and source vocabUri
        Map<VocabularyTerm, List<AncestorEntry>> globalAncestors = new HashMap<>();
        for (var term : allTerms) {
            List<AncestorEntry> ancestors = new ArrayList<>();
            Map<VocabularyTerm, Integer> visited = new HashMap<>();
            Queue<AncestorEntry> bfsQueue = new ArrayDeque<>();
            for (var parent : term.specializes()) {
                bfsQueue.add(new AncestorEntry(parent, 1, termToUri.get(parent)));
                visited.put(parent, 1);
            }
            while (!bfsQueue.isEmpty()) {
                var ae = bfsQueue.poll();
                if (visited.getOrDefault(ae.term, Integer.MAX_VALUE) < ae.depth) continue;
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
        // Sort each descendant list by depth
        for (var entry : globalDescendants.entrySet()) {
            entry.getValue().sort(Comparator.comparingInt(DescendantEntry::depth));
        }

        // 6. Build per-vocabulary indexes with cross-vocabulary injection
        //    Process vocabularies in sorted URI order for deterministic collision messages
        var sortedUris = vocabSnapshot.keySet().stream().sorted().toList();

        Map<String, Map<String, List<AncestorEntry>>> newAncestorIndex = new HashMap<>();
        Map<String, Map<String, List<DescendantEntry>>> newDescendantIndex = new HashMap<>();
        Map<String, Set<String>> newValueToVocabs = new HashMap<>();

        for (var uri : sortedUris) {
            var vocab = vocabSnapshot.get(uri);
            Map<String, List<AncestorEntry>> vocabAncestorMap = new HashMap<>();
            Map<String, List<DescendantEntry>> vocabDescendantMap = new HashMap<>();
            Map<String, String> injectedValueToSource = new HashMap<>(); // Track injection sources

            // 6a. Native entries: terms declared in this vocabulary
            for (var constant : vocab.getEnumConstants()) {
                var term = (VocabularyTerm) constant;
                vocabAncestorMap.put(term.value(), globalAncestors.get(term));
                vocabDescendantMap.put(term.value(), List.copyOf(globalDescendants.get(term)));
                newValueToVocabs.computeIfAbsent(term.value(), k -> new HashSet<>()).add(uri);
            }

            // 6b. Injection: terms from OTHER vocabularies that have a transitive ancestor
            //     in THIS vocabulary. These terms are visible within V's index for matching.
            for (var term : allTerms) {
                String nativeUri = termToUri.get(term);
                if (nativeUri.equals(uri)) continue; // skip native terms, already added

                // Check if this term has any ancestor that is native to vocab V
                boolean hasAncestorInV = false;
                for (var ae : globalAncestors.get(term)) {
                    if (termToUri.get(ae.term).equals(uri)) {
                        hasAncestorInV = true;
                        break;
                    }
                }
                if (!hasAncestorInV) continue;

                // Inline collision detection
                String value = term.value();
                if (vocabAncestorMap.containsKey(value)) {
                    // Determine if the existing entry is native or injected
                    boolean existingIsNative = false;
                    for (var constant : vocab.getEnumConstants()) {
                        if (((VocabularyTerm) constant).value().equals(value)) {
                            existingIsNative = true;
                            break;
                        }
                    }
                    String existingSource = existingIsNative ? uri : injectedValueToSource.get(value);
                    throw new IllegalArgumentException(
                        "Value collision in index for '" + uri + "': '" + value
                            + "' from '" + existingSource + "' collides with '"
                            + value + "' from '" + nativeUri + "'");
                }

                // Inject: use the term's global ancestors/descendants
                vocabAncestorMap.put(value, globalAncestors.get(term));
                vocabDescendantMap.put(value, List.copyOf(globalDescendants.get(term)));
                injectedValueToSource.put(value, nativeUri);
                // Do NOT add to newValueToVocabs — valueToVocabs should only contain declaring vocabularies
            }

            newAncestorIndex.put(uri, Map.copyOf(vocabAncestorMap));
            newDescendantIndex.put(uri, Map.copyOf(vocabDescendantMap));
        }

        // 7. Atomic write: only update class-level maps after ALL validation passes
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
            if (existing == vocab) return;  // same class — idempotent
            // different class with same URI — registerTerms will throw
        }

        // Late register() atomicity: build term maps locally, merge with snapshot,
        // run buildAllHierarchyIndexes(), only write all maps on success.
        // Save current state for rollback
        var savedByUri = new HashMap<>(byUri);
        var savedByClass = new HashMap<>(byClass);
        var savedByClassOrdered = new HashMap<>(byClassOrdered);
        var savedAncestorIndex = new HashMap<>(ancestorIndex);
        var savedDescendantIndex = new HashMap<>(descendantIndex);
        // Deep-copy valueToVocabs: copy each Set to avoid mutation corruption
        var savedValueToVocabs = new HashMap<String, Set<String>>();
        valueToVocabs.forEach((k, v) -> savedValueToVocabs.put(k, new HashSet<>(v)));

        try {
            // Register terms (writes to byUri, byClass, byClassOrdered)
            registerTerms(vocab);

            // Rebuild ALL hierarchy indexes with the full snapshot (including new vocab)
            // buildAllHierarchyIndexes() computes locally then writes to class-level maps atomically
            buildAllHierarchyIndexes(Map.copyOf(byUri));
        } catch (Exception e) {
            // Rollback: restore all maps to pre-registration state
            byUri.clear(); byUri.putAll(savedByUri);
            byClass.clear(); byClass.putAll(savedByClass);
            byClassOrdered.clear(); byClassOrdered.putAll(savedByClassOrdered);
            ancestorIndex.clear(); ancestorIndex.putAll(savedAncestorIndex);
            descendantIndex.clear(); descendantIndex.putAll(savedDescendantIndex);
            valueToVocabs.clear(); valueToVocabs.putAll(savedValueToVocabs);
            throw e;
        }
    }

    @Override
    public boolean isRegistered(String vocabUri) {
        return byUri.containsKey(vocabUri);
    }

    @Override
    public Set<String> registeredUris() {
        return Set.copyOf(byUri.keySet());
    }


    @Override
    public Optional<? extends VocabularyTerm> resolve(String vocabUri, String value) {
        var clazz = byUri.get(vocabUri);
        if (clazz == null) return Optional.empty();
        return Optional.ofNullable(byClass.get(clazz).get(value));
    }

    @Override
    public List<? extends VocabularyTerm> allTerms(String vocabUri) {
        var clazz = byUri.get(vocabUri);
        return clazz == null ? List.of() : byClassOrdered.get(clazz);
    }

    @Override
    public Optional<String> equivalentValues(String fromUri, String value, String toUri) {
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
    public Optional<VocabularyMetadata> vocabularyMetadata(String uri) {
        var clazz = byUri.get(uri);
        if (clazz == null) return Optional.empty();
        // register() guarantees @VocabularyMetadata is present for anything in byUri
        return Optional.of(clazz.getAnnotation(VocabularyMetadata.class));
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
