package io.casehub.eidos.runtime.registrar;

import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentDisposition;
import io.casehub.eidos.api.DispositionAxis;
import io.casehub.eidos.api.DispositionValue;
import io.casehub.eidos.api.MatchDegree;
import io.casehub.eidos.api.VocabularyInfo;
import io.casehub.eidos.api.VocabularyMetadata;
import io.casehub.eidos.api.VocabularyRegistry;
import io.casehub.eidos.api.VocabularyTerm;
import io.casehub.eidos.vocab.ArchetypeFamily;
import io.casehub.eidos.vocab.ArchetypeTerm;
import io.casehub.eidos.vocab.MbtiTypeTerm;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

class ArchetypeDeriverTest {

    private static AgentDescriptor.Builder base() {
        return AgentDescriptor.builder()
            .agentId("test").name("Test").slot("analyst").tenancyId("t1");
    }

    @Test
    void explicitArchetypeNotOverridden() {
        var d = base()
            .archetype("mentor")
            .dispositionVocabulary(MbtiTypeTerm.URI)
            .disposition(AgentDisposition.builder()
                .dispositionProfile(List.of(new DispositionValue("intj", 1.0)))
                .build())
            .build();
        var result = ArchetypeDeriver.deriveArchetype(d);
        assertThat(result.archetype()).isEqualTo("mentor");
    }

    @Test
    void noDispositionReturnsNull() {
        var d = base().build();
        var result = ArchetypeDeriver.deriveArchetype(d);
        assertThat(result.archetype()).isNull();
    }

    @Test
    void mbtiIntjDerivesToSageFamily() {
        var d = base()
            .dispositionVocabulary(MbtiTypeTerm.URI)
            .disposition(AgentDisposition.builder()
                .dispositionProfile(List.of(new DispositionValue("intj", 1.0)))
                .build())
            .build();
        var result = ArchetypeDeriver.deriveArchetype(d);
        assertThat(result.archetype()).isNotNull();
        var term = findTerm(result.archetype());
        assertThat(term).isNotNull();
        assertThat(term.family()).isIn(ArchetypeFamily.SAGE, ArchetypeFamily.MAGICIAN, ArchetypeFamily.SOVEREIGN);
    }

    @Test
    void noVocabularyUriSkipsDerivation() {
        var d = base()
            .disposition(AgentDisposition.builder()
                .dispositionProfile(List.of(new DispositionValue("intj", 1.0)))
                .build())
            .build();
        var result = ArchetypeDeriver.deriveArchetype(d);
        assertThat(result.archetype()).isNull();
    }

    @Test
    void emptyProfileSkipsDerivation() {
        var d = base()
            .dispositionVocabulary(MbtiTypeTerm.URI)
            .disposition(AgentDisposition.builder().build())
            .build();
        var result = ArchetypeDeriver.deriveArchetype(d);
        assertThat(result.archetype()).isNull();
    }

    @Test
    void domainVocabularyUsedAsFallback() {
        var d = base()
            .domainVocabulary(MbtiTypeTerm.URI)
            .disposition(AgentDisposition.builder()
                .dispositionProfile(List.of(new DispositionValue("intj", 1.0)))
                .build())
            .build();
        var result = ArchetypeDeriver.deriveArchetype(d);
        assertThat(result.archetype()).isNotNull();
    }

    private static ArchetypeTerm findTerm(String value) {
        for (var t : ArchetypeTerm.values()) {
            if (t.value().equals(value)) return t;
        }
        return null;
    }

    static class TestVocabRegistryFactory {
        static VocabularyRegistry create() {
            var reg = new MinimalVocabRegistry();
            reg.register(MbtiTypeTerm.class);
            return reg;
        }
    }

    static class MinimalVocabRegistry implements VocabularyRegistry {
        private final Map<String, List<? extends VocabularyTerm>> byUri = new ConcurrentHashMap<>();

        @Override
        public <T extends Enum<T> & VocabularyTerm> void register(Class<T> vocab) {
            var metadata = vocab.getAnnotation(VocabularyMetadata.class);
            byUri.put(metadata.uri(), Arrays.asList(vocab.getEnumConstants()));
        }

        @Override public boolean isRegistered(String uri) { return byUri.containsKey(uri); }
        @Override public Set<String> registeredUris() { return Set.copyOf(byUri.keySet()); }

        @Override
        public Optional<? extends VocabularyTerm> resolve(String uri, String value) {
            var terms = byUri.get(uri);
            if (terms == null) return Optional.empty();
            return terms.stream().filter(t -> t.value().equals(value)).findFirst();
        }

        @Override public List<? extends VocabularyTerm> allTerms(String uri) { return byUri.getOrDefault(uri, List.of()); }
        @Override public Optional<String> equivalentValues(String f, String v, String t) { return Optional.empty(); }
        @Override public Optional<String> equivalentValues(String f, String v, String t, DispositionAxis a) { return Optional.empty(); }
        @Override public <T extends Enum<T> & VocabularyTerm> Optional<T> resolve(Class<T> v, String val) { return Optional.empty(); }
        @Override public <S extends Enum<S> & VocabularyTerm, T extends Enum<T> & VocabularyTerm> Optional<T> equivalentValues(S f, Class<T> t) { return Optional.empty(); }
        @Override public <S extends Enum<S> & VocabularyTerm, T extends Enum<T> & VocabularyTerm> Optional<T> equivalentValues(S f, Class<T> t, DispositionAxis a) { return Optional.empty(); }
        @Override public Optional<VocabularyInfo> vocabularyMetadata(String uri) { return Optional.empty(); }
        @Override public void register(String uri, String name, String version, String description, List<? extends VocabularyTerm> terms) {}
        @Override public boolean subsumes(String uri, String gen, String spec) { return false; }
        @Override public MatchDegree match(String uri, String decl, String req) { return new MatchDegree.None(); }
        @Override public List<? extends VocabularyTerm> ancestors(String uri, String val) { return List.of(); }
        @Override public List<? extends VocabularyTerm> descendants(String uri, String val) { return List.of(); }
        @Override public Map<String, Set<String>> expandForMatchingByVocabulary(String val) { return Map.of(); }
    }

// --- Disposition derivation from archetype ---

    @Test
    void archetypeAutoDerivesDispositionWhenAbsent() {
        var d = base().archetype("sage").build();
        var vocabRegistry = TestVocabRegistryFactory.create();
        var result = ArchetypeDeriver.deriveDispositionFromArchetype(d, vocabRegistry);
        assertThat(result.disposition()).isNotNull();
        assertThat(result.disposition().dispositionProfile()).isNotEmpty();
    }

    @Test
    void explicitDispositionWinsOverArchetypeDerived() {
        var explicitDisposition = AgentDisposition.builder()
                .socialOrient("collaborative")
                .build();
        var d = base().archetype("sage").disposition(explicitDisposition).build();
        var vocabRegistry = TestVocabRegistryFactory.create();
        var result = ArchetypeDeriver.deriveDispositionFromArchetype(d, vocabRegistry);
        assertThat(result.disposition().get(io.casehub.eidos.api.DispositionAxis.SOCIAL_ORIENTATION))
                .extracting(DispositionValue::term)
                .contains("collaborative");
    }

    @Test
    void unknownArchetypeSkipsDispositionDerivation() {
        var d = base().archetype("custom-domain-role").build();
        var vocabRegistry = TestVocabRegistryFactory.create();
        var result = ArchetypeDeriver.deriveDispositionFromArchetype(d, vocabRegistry);
        assertThat(result.disposition()).isNull();
    }

    @Test
    void explicitMbtiTypeWinsOverArchetypeDerived() {
        var disposition = AgentDisposition.builder()
                .mbtiType("ENTJ")
                .build();
        var d = base().archetype("sage").disposition(disposition).build();
        var vocabRegistry = TestVocabRegistryFactory.create();
        var result = ArchetypeDeriver.deriveDispositionFromArchetype(d, vocabRegistry);
        assertThat(result.disposition().mbtiType()).isEqualTo("ENTJ");
    }

// --- Avatar derivation ---

    @Test
    void avatarDerivedWhenArchetypeSetAndAvatarNull() {
        var d      = base().archetype("detective").build();
        var result = ArchetypeDeriver.deriveArchetype(d);
        assertThat(result.avatar()).isNotNull();
        assertThat(result.avatar()).startsWith("mythic:P");
    }

    @Test
    void avatarNotOverriddenWhenExplicitlySet() {
        var d = base()
                        .archetype("detective")
                        .avatar("https://example.com/custom.png")
                        .build();
        var result = ArchetypeDeriver.deriveArchetype(d);
        assertThat(result.avatar()).isEqualTo("https://example.com/custom.png");
    }

    @Test
    void avatarNullWhenNoArchetype() {
        var d      = base().build();
        var result = ArchetypeDeriver.deriveArchetype(d);
        assertThat(result.avatar()).isNull();
    }

    @Test
    void avatarCodeMatchesCanonicalIndex() {
        var    d            = base().archetype("detective").build();
        var    result       = ArchetypeDeriver.deriveArchetype(d);
        String expectedCode = io.casehub.eidos.vocab.AvatarCodec.defaultCode("mythic", "detective");
        assertThat(result.avatar()).isEqualTo(expectedCode);
    }

// --- End-to-end minimal identity ---

    @Test
    void minimalIdentityProducesCompletePrompt() {
        var descriptor = base()
                .name("Code Reviewer")
                .slot("reviewer")
                .domain("software-engineering")
                .archetype("sage")
                .archetypeAdjectives(List.of("cautious", "methodical"))
                .build();

        var vocabRegistry = TestVocabRegistryFactory.create();

        var withDisposition = ArchetypeDeriver.deriveDispositionFromArchetype(descriptor, vocabRegistry);
        assertThat(withDisposition.disposition()).as("auto-derivation should produce disposition").isNotNull();
        assertThat(withDisposition.disposition().dispositionProfile()).as("should have disposition profile").isNotEmpty();

        var withAxes = io.casehub.eidos.core.registrar.DescriptorCollector.deriveDispositionAxes(withDisposition, vocabRegistry);

        var pipeline = new io.casehub.eidos.core.renderer.EidosRenderPipeline(vocabRegistry, new NoOpTemplateRegistry(), new com.fasterxml.jackson.databind.ObjectMapper());
        var context = io.casehub.eidos.api.AgentPromptContext.forFormat(io.casehub.eidos.api.SystemPromptRenderer.RenderFormat.MARKDOWN);
        var s1 = pipeline.buildStage1(withAxes, context);
        var result = pipeline.assemble(s1, Optional.empty(), Optional.empty(), withAxes, context);

        assertThat(result.content())
                .contains("Code Reviewer")
                .contains("reviewer")
                .contains("software-engineering")
                .contains("Operating Principles")
                .contains("sage")
                .contains("cautious");
    }

    static class NoOpTemplateRegistry implements io.casehub.eidos.api.TemplateRegistry {
        @Override public void register(io.casehub.eidos.api.DescriptorTemplate t) {}
        @Override public Optional<io.casehub.eidos.api.DescriptorTemplate> resolve(String id) { return Optional.empty(); }
        @Override public List<io.casehub.eidos.api.DescriptorTemplate> all() { return List.of(); }
    }
}
