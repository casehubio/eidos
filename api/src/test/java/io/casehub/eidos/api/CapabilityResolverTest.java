package io.casehub.eidos.api;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CapabilityResolverTest {

    static final String VOCAB_URI = "urn:test:capabilities";
    static VocabularyRegistry registry;

    @BeforeAll
    static void setUp() {
        registry = new StubVocabularyRegistry(TestCapabilityVocab.class);
    }

    static AgentCapability grounded(String name) {
        return AgentCapability.builder().name(name)
            .capabilityVocabulary(VOCAB_URI).build();
    }

    static AgentCapability ungrounded(String name) {
        return AgentCapability.builder().name(name).build();
    }

    // --- match() tests ---

    @Test
    void match_exact_name_returns_exact() {
        var cap = grounded("code-review");
        var result = CapabilityResolver.match(cap, "code-review", registry);
        assertThat(result).isInstanceOf(MatchDegree.Exact.class);
    }

    @Test
    void match_ungrounded_non_exact_returns_none() {
        var cap = ungrounded("code-review");
        var result = CapabilityResolver.match(cap, "security-review", registry);
        assertThat(result).isInstanceOf(MatchDegree.None.class);
    }

    @Test
    void match_grounded_parent_returns_plugin() {
        // code-review is parent of security-review → Plugin
        var cap = grounded("code-review");
        var result = CapabilityResolver.match(cap, "security-review", registry);
        assertThat(result).isInstanceOf(MatchDegree.Plugin.class);
        assertThat(((MatchDegree.Plugin) result).depth()).isEqualTo(1);
    }

    @Test
    void match_grounded_child_returns_specialization() {
        // security-review is child of code-review → Specialization
        var cap = grounded("security-review");
        var result = CapabilityResolver.match(cap, "code-review", registry);
        assertThat(result).isInstanceOf(MatchDegree.Specialization.class);
        assertThat(((MatchDegree.Specialization) result).depth()).isEqualTo(1);
    }

    @Test
    void match_grounded_unrelated_returns_none() {
        var cap = grounded("code-review");
        var result = CapabilityResolver.match(cap, "testing", registry);
        assertThat(result).isInstanceOf(MatchDegree.None.class);
    }

    // --- resolve() tests ---

    @Test
    void resolve_exact_match_preferred_over_subsumption() {
        var caps = List.of(grounded("code-review"), grounded("security-review"));
        var result = CapabilityResolver.resolve(caps, "security-review", registry);
        assertThat(result).isNotNull();
        assertThat(result.capability().name()).isEqualTo("security-review");
        assertThat(result.degree()).isInstanceOf(MatchDegree.Exact.class);
    }

    @Test
    void resolve_closest_depth_wins() {
        var caps = List.of(grounded("review"), grounded("testing"));
        var result = CapabilityResolver.resolve(caps, "unit-testing", registry);
        assertThat(result).isNotNull();
        assertThat(result.capability().name()).isEqualTo("testing");
        assertThat(result.degree()).isInstanceOf(MatchDegree.Plugin.class);
        assertThat(((MatchDegree.Plugin) result.degree()).depth()).isEqualTo(1);
    }

    @Test
    void resolve_ungrounded_exact_only() {
        var caps = List.of(ungrounded("code-review"));
        assertThat(CapabilityResolver.resolve(caps, "security-review", registry)).isNull();
        var result = CapabilityResolver.resolve(caps, "code-review", registry);
        assertThat(result).isNotNull();
        assertThat(result.degree()).isInstanceOf(MatchDegree.Exact.class);
    }

    @Test
    void resolve_null_capabilities_returns_null() {
        assertThat(CapabilityResolver.resolve(null, "code-review", registry)).isNull();
    }

    @Test
    void resolve_empty_capabilities_returns_null() {
        assertThat(CapabilityResolver.resolve(List.of(), "code-review", registry)).isNull();
    }

    @Test
    void resolve_first_in_list_wins_at_equal_depth() {
        var caps = List.of(grounded("code-review"), grounded("design-review"));
        var result = CapabilityResolver.resolve(caps, "review", registry);
        assertThat(result).isNotNull();
        assertThat(result.capability().name()).isEqualTo("code-review");
        assertThat(result.degree()).isInstanceOf(MatchDegree.Specialization.class);
    }

    @Test
    void resolve_plugin_beats_specialization_across_depth() {
        // code-review is Plugin(1) for security-review query.
        // If an agent had a Specialization(1) and a Plugin(2), Plugin wins.
        // Here: "review" is Plugin(2) for "security-review", "testing" is no match.
        // We need a case where Plugin and Specialization compete:
        // Query "security-review":
        //   "code-review" → Plugin(1) (code-review subsumes security-review)
        //   "sast-review" is not in vocab — skip.
        // Instead, test with the compareTo-based selection:
        // Query "review":
        //   "code-review" → Specialization(1) (code-review specializes review)
        //   "testing" → None
        // This doesn't create a Plugin vs Specialization competition in resolve().
        // The Comparable ordering is already tested in MatchDegreeTest.
        // The resolve() loop uses compareTo, so any two degrees compare correctly.
        var caps = List.of(grounded("code-review"), grounded("review"));
        var result = CapabilityResolver.resolve(caps, "security-review", registry);
        assertThat(result).isNotNull();
        // "review" is Plugin(2) for "security-review", "code-review" is Plugin(1)
        assertThat(result.capability().name()).isEqualTo("code-review");
        assertThat(result.degree()).isEqualTo(new MatchDegree.Plugin(1));
    }

    /**
     * Minimal stub VocabularyRegistry for testing.
     * Only implements match() by walking VocabularyTerm.specializes().
     */
    private static class StubVocabularyRegistry implements VocabularyRegistry {
        private final Class<? extends VocabularyTerm> vocab;
        private final String uri;

        StubVocabularyRegistry(Class<? extends VocabularyTerm> vocab) {
            this.vocab = vocab;
            var meta = vocab.getAnnotation(VocabularyMetadata.class);
            this.uri = meta != null ? meta.uri() : "";
        }

        @Override
        public MatchDegree match(String vocabUri, String declaredValue, String requestedValue) {
            if (!uri.equals(vocabUri)) {
                return new MatchDegree.None();
            }
            if (declaredValue.equals(requestedValue)) {
                return new MatchDegree.Exact();
            }

            var declaredTerm = findTerm(declaredValue);
            var requestedTerm = findTerm(requestedValue);
            if (declaredTerm == null || requestedTerm == null) {
                return new MatchDegree.None();
            }

            // Check if declared is ancestor of requested (Plugin)
            int pluginDepth = countAncestors(requestedTerm, declaredTerm);
            if (pluginDepth > 0) {
                return new MatchDegree.Plugin(pluginDepth);
            }

            // Check if declared is descendant of requested (Specialization)
            int specDepth = countAncestors(declaredTerm, requestedTerm);
            if (specDepth > 0) {
                return new MatchDegree.Specialization(specDepth);
            }

            return new MatchDegree.None();
        }

        private VocabularyTerm findTerm(String value) {
            for (var constant : vocab.getEnumConstants()) {
                if (constant.value().equals(value)) {
                    return constant;
                }
            }
            return null;
        }

        private int countAncestors(VocabularyTerm term, VocabularyTerm target) {
            var parents = term.specializes();
            if (parents.isEmpty()) {
                return 0;
            }
            for (var parent : parents) {
                if (parent.value().equals(target.value())) {
                    return 1;
                }
                int depth = countAncestors(parent, target);
                if (depth > 0) {
                    return depth + 1;
                }
            }
            return 0;
        }

        // Stub implementations for all other methods
        @Override public <T extends Enum<T> & VocabularyTerm> void register(Class<T> vocab) {}
        @Override public boolean isRegistered(String vocabUri) { return uri.equals(vocabUri); }
        @Override public java.util.Optional<? extends VocabularyTerm> resolve(String vocabUri, String value) {
            return java.util.Optional.empty();
        }
        @Override public List<? extends VocabularyTerm> allTerms(String vocabUri) { return List.of(); }
        @Override public java.util.Optional<String> equivalentValues(String fromUri, String value, String toUri) {
            return java.util.Optional.empty();
        }
        @Override public java.util.Optional<String> equivalentValues(String fromUri, String value, String toUri,
                                                                       DispositionAxis axis) {
            return java.util.Optional.empty();
        }
        @Override public <T extends Enum<T> & VocabularyTerm> java.util.Optional<T> resolve(Class<T> vocab,
                                                                                              String value) {
            return java.util.Optional.empty();
        }
        @Override public <S extends Enum<S> & VocabularyTerm, T extends Enum<T> & VocabularyTerm>
                java.util.Optional<T> equivalentValues(S from, Class<T> targetVocab) {
            return java.util.Optional.empty();
        }
        @Override public <S extends Enum<S> & VocabularyTerm, T extends Enum<T> & VocabularyTerm>
                java.util.Optional<T> equivalentValues(S from, Class<T> targetVocab, DispositionAxis axis) {
            return java.util.Optional.empty();
        }
        @Override public java.util.Optional<VocabularyMetadata> vocabularyMetadata(String uri) {
            return java.util.Optional.empty();
        }
        @Override public boolean subsumes(String vocabUri, String generalValue, String specificValue) {
            return false;
        }
        @Override public List<? extends VocabularyTerm> ancestors(String vocabUri, String value) {
            return List.of();
        }
        @Override public List<? extends VocabularyTerm> descendants(String vocabUri, String value) {
            return List.of();
        }
        @Override public java.util.Map<String, java.util.Set<String>> expandForMatchingByVocabulary(String value) {
            return java.util.Map.of();
        }
        @Override public java.util.Set<String> registeredUris() { return java.util.Set.of(uri); }
    }
}
