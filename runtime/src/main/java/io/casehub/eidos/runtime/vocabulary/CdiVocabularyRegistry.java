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

    private record AncestorEntry(VocabularyTerm term, int depth) {}
    private record DescendantEntry(VocabularyTerm term, int depth) {}

    @PostConstruct
    void init() {
        for (VocabularyRegistrar r : registrars) {
            registerRaw(r.vocabulary());
        }
    }

    /**
     * Bridges the wildcard return type of {@link VocabularyRegistrar#vocabulary()} to the
     * recursive bound required by {@link #register(Class)}. The cast is safe because any
     * concrete enum returned by the registrar satisfies {@code T extends Enum<T> & VocabularyTerm}
     * at its declaration site — the wildcard just loses that information at the use site.
     */
    @SuppressWarnings("unchecked")
    private <T extends Enum<T> & VocabularyTerm>
    void registerRaw(Class<? extends Enum<? extends VocabularyTerm>> vocab) {
        register((Class<T>) vocab);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Enum<T> & VocabularyTerm> void register(Class<T> vocab) {
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

        // Build DAG index — hierarchy / subsumption
        buildHierarchyIndex(vocab, uri, constants);
    }

    private <T extends Enum<T> & VocabularyTerm> void buildHierarchyIndex(
            Class<T> vocab, String uri, T[] constants) {
        // 1. Validate specializes() references (same enum class check)
        for (var constant : constants) {
            for (var parent : constant.specializes()) {
                if (!vocab.isInstance(parent)) {
                    throw new IllegalArgumentException(
                        "Cross-vocabulary specializes() not allowed: " + constant.value()
                            + " in " + vocab.getName() + " specializes " + parent.value()
                            + " from different vocabulary");
                }
            }
        }

        // 2. Cycle detection via Kahn's algorithm (topological sort)
        Map<VocabularyTerm, Integer> inDegree = new HashMap<>();
        Map<VocabularyTerm, List<VocabularyTerm>> edges = new HashMap<>();
        for (var c : constants) {
            inDegree.put(c, 0);
            edges.put(c, new ArrayList<>());
        }
        for (var c : constants) {
            for (var parent : c.specializes()) {
                edges.get(parent).add(c);
                inDegree.merge(c, 1, Integer::sum);
            }
        }
        Queue<VocabularyTerm> queue = new ArrayDeque<>();
        for (var c : constants) {
            if (inDegree.get(c) == 0) queue.add(c);
        }
        int processed = 0;
        while (!queue.isEmpty()) {
            var node = queue.poll();
            processed++;
            for (var child : edges.get(node)) {
                int newDegree = inDegree.get(child) - 1;
                inDegree.put(child, newDegree);
                if (newDegree == 0) queue.add(child);
            }
        }
        if (processed != constants.length) {
            throw new IllegalArgumentException(
                "Cycle detected in specializes() hierarchy for vocabulary " + vocab.getName());
        }

        // 3. BFS from each term to compute ancestors with min depth
        Map<String, List<AncestorEntry>> ancestorMap = new HashMap<>();
        for (var c : constants) {
            List<AncestorEntry> ancestors = new ArrayList<>();
            Map<VocabularyTerm, Integer> visited = new HashMap<>();
            Queue<AncestorEntry> bfsQueue = new ArrayDeque<>();
            for (var parent : c.specializes()) {
                bfsQueue.add(new AncestorEntry(parent, 1));
                visited.put(parent, 1);
            }
            while (!bfsQueue.isEmpty()) {
                var entry = bfsQueue.poll();
                if (visited.get(entry.term) < entry.depth) continue;  // Skip if we found shorter path
                ancestors.add(entry);
                for (var grandparent : entry.term.specializes()) {
                    int newDepth = entry.depth + 1;
                    if (!visited.containsKey(grandparent) || visited.get(grandparent) > newDepth) {
                        visited.put(grandparent, newDepth);
                        bfsQueue.add(new AncestorEntry(grandparent, newDepth));
                    }
                }
            }
            ancestors.sort(Comparator.comparingInt(AncestorEntry::depth));
            ancestorMap.put(c.value(), ancestors);
        }

        // 4. Invert edges, BFS for descendants with min depth
        Map<VocabularyTerm, List<VocabularyTerm>> reverseEdges = new HashMap<>();
        for (var c : constants) {
            reverseEdges.put(c, new ArrayList<>());
        }
        for (var c : constants) {
            for (var parent : c.specializes()) {
                reverseEdges.get(parent).add(c);
            }
        }
        Map<String, List<DescendantEntry>> descendantMap = new HashMap<>();
        for (var c : constants) {
            List<DescendantEntry> descendants = new ArrayList<>();
            Map<VocabularyTerm, Integer> visited = new HashMap<>();
            Queue<DescendantEntry> bfsQueue = new ArrayDeque<>();
            for (var child : reverseEdges.get(c)) {
                bfsQueue.add(new DescendantEntry(child, 1));
                visited.put(child, 1);
            }
            while (!bfsQueue.isEmpty()) {
                var entry = bfsQueue.poll();
                if (visited.get(entry.term) < entry.depth) continue;
                descendants.add(entry);
                for (var grandchild : reverseEdges.get(entry.term)) {
                    int newDepth = entry.depth + 1;
                    if (!visited.containsKey(grandchild) || visited.get(grandchild) > newDepth) {
                        visited.put(grandchild, newDepth);
                        bfsQueue.add(new DescendantEntry(grandchild, newDepth));
                    }
                }
            }
            descendants.sort(Comparator.comparingInt(DescendantEntry::depth));
            descendantMap.put(c.value(), descendants);
        }

        // 5. Populate valueToVocabs
        for (var c : constants) {
            valueToVocabs.computeIfAbsent(c.value(), k -> ConcurrentHashMap.newKeySet()).add(uri);
        }

        // Write indexes
        ancestorIndex.put(uri, ancestorMap);
        descendantIndex.put(uri, descendantMap);
    }

    @Override
    public boolean isRegistered(String vocabUri) {
        return byUri.containsKey(vocabUri);
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
        for (var vocabUri : vocabs) {
            Set<String> expanded = new HashSet<>();
            expanded.add(value);
            // Add ancestors
            var ancestorMap = ancestorIndex.get(vocabUri);
            if (ancestorMap != null) {
                var ancestors = ancestorMap.get(value);
                if (ancestors != null) {
                    for (var entry : ancestors) {
                        expanded.add(entry.term.value());
                    }
                }
            }
            // Add descendants
            var descendantMap = descendantIndex.get(vocabUri);
            if (descendantMap != null) {
                var descendants = descendantMap.get(value);
                if (descendants != null) {
                    for (var entry : descendants) {
                        expanded.add(entry.term.value());
                    }
                }
            }
            result.put(vocabUri, expanded);
        }
        return result;
    }
}
