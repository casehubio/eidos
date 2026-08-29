package io.casehub.eidos.runtime.yaml;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.casehub.eidos.api.AgentDisposition;
import io.casehub.eidos.api.DispositionAxis;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DispositionDeserializerTest {

    private final ObjectMapper mapper = createMapper(null);

    private static ObjectMapper createMapper(io.casehub.eidos.api.VocabularyRegistry registry) {
        var module = new SimpleModule();
        module.addDeserializer(AgentDisposition.class, new DispositionDeserializer(registry));
        return new ObjectMapper(new YAMLFactory()).registerModule(module);
    }

    @Test
    void stringAxis_convertsToDispositionValueList() throws Exception {
        var yaml = """
            socialOrient: collaborative
            ruleFollowing: strict
            riskAppetite: bold
            autonomy: autonomous
            conflictMode: competing
            delegation: true
            """;
        var disp = mapper.readValue(yaml, AgentDisposition.class);
        assertThat(disp.primaryTerm(DispositionAxis.SOCIAL_ORIENTATION)).isEqualTo("collaborative");
        assertThat(disp.primaryTerm(DispositionAxis.RULE_FOLLOWING)).isEqualTo("strict");
        assertThat(disp.primaryTerm(DispositionAxis.RISK_APPETITE)).isEqualTo("bold");
        assertThat(disp.primaryTerm(DispositionAxis.AUTONOMY)).isEqualTo("autonomous");
        assertThat(disp.primaryTerm(DispositionAxis.CONFLICT_MODE)).isEqualTo("competing");
        assertThat(disp.delegation()).isTrue();
    }

    @Test
    void dispositionProfile_parsesWeightedValues() throws Exception {
        var yaml = """
            dispositionProfile:
              - term: te
                weight: 0.35
              - term: ni
                weight: 0.20
            """;
        var disp = mapper.readValue(yaml, AgentDisposition.class);
        assertThat(disp.dispositionProfile()).hasSize(2);
        assertThat(disp.dispositionProfile().get(0).term()).isEqualTo("te");
        assertThat(disp.dispositionProfile().get(0).weight()).isEqualTo(0.35);
    }

    @Test
    void styleProfile_parsesWeightedValues() throws Exception {
        var yaml = """
            styleProfile:
              - term: concise
                weight: 0.60
              - term: formal
                weight: 0.40
            """;
        var disp = mapper.readValue(yaml, AgentDisposition.class);
        assertThat(disp.styleProfile()).hasSize(2);
        assertThat(disp.styleProfile().get(0).term()).isEqualTo("concise");
    }

    @Test
    void emptyDisposition_returnsDefaults() throws Exception {
        var yaml = "delegation: false";
        var disp = mapper.readValue(yaml, AgentDisposition.class);
        assertThat(disp.socialOrient()).isEmpty();
        assertThat(disp.delegation()).isFalse();
    }

    @Test
    void mbtiType_resolvesProfile_whenRegistryAvailable() throws Exception {
        var registry = testVocabRegistry();
        var mapperWithVocab = createMapper(registry);
        var yaml = "mbtiType: ENTJ";
        var disp = mapperWithVocab.readValue(yaml, AgentDisposition.class);
        assertThat(disp.dispositionProfile()).hasSize(8);
        assertThat(disp.dispositionProfile().get(0).term()).isEqualTo("te");
    }

    @Test
    void mbtiType_caseInsensitive() throws Exception {
        var registry = testVocabRegistry();
        var mapperWithVocab = createMapper(registry);
        var yaml = "mbtiType: entj";
        var disp = mapperWithVocab.readValue(yaml, AgentDisposition.class);
        assertThat(disp.dispositionProfile()).hasSize(8);
    }

    @Test
    void explicitProfile_winsOverMbtiType() throws Exception {
        var registry = testVocabRegistry();
        var mapperWithVocab = createMapper(registry);
        var yaml = """
            mbtiType: ENTJ
            dispositionProfile:
              - term: ti
                weight: 0.50
            """;
        var disp = mapperWithVocab.readValue(yaml, AgentDisposition.class);
        assertThat(disp.dispositionProfile()).hasSize(1);
        assertThat(disp.dispositionProfile().get(0).term()).isEqualTo("ti");
    }

    @Test
    void enneagramType_projectsAxes() throws Exception {
        var registry = testVocabRegistry();
        var mapperWithVocab = createMapper(registry);
        var yaml = "enneagramType: challenger";
        var disp = mapperWithVocab.readValue(yaml, AgentDisposition.class);
        assertThat(disp.primaryTerm(DispositionAxis.RULE_FOLLOWING)).isEqualTo("flexible");
        assertThat(disp.primaryTerm(DispositionAxis.RISK_APPETITE)).isEqualTo("bold");
        assertThat(disp.primaryTerm(DispositionAxis.CONFLICT_MODE)).isEqualTo("competing");
    }

    @Test
    void enneagramType_doesNotOverwriteExplicitAxes() throws Exception {
        var registry = testVocabRegistry();
        var mapperWithVocab = createMapper(registry);
        var yaml = """
            enneagramType: challenger
            socialOrient: collaborative
            """;
        var disp = mapperWithVocab.readValue(yaml, AgentDisposition.class);
        assertThat(disp.primaryTerm(DispositionAxis.SOCIAL_ORIENTATION)).isEqualTo("collaborative");
    }

    @Test
    void mbtiType_ignoredWithoutRegistry() throws Exception {
        var yaml = "mbtiType: ENTJ";
        var disp = mapper.readValue(yaml, AgentDisposition.class);
        assertThat(disp.dispositionProfile()).isEmpty();
    }

    private static io.casehub.eidos.api.VocabularyRegistry testVocabRegistry() {
        var registry = new io.casehub.eidos.runtime.vocabulary.CdiVocabularyRegistry();
        registry.register(io.casehub.eidos.vocab.JungianFunctionTerm.class);
        registry.register(io.casehub.eidos.vocab.MbtiTypeTerm.class);
        registry.register(io.casehub.eidos.vocab.EnneagramTerm.class);
        registry.register(io.casehub.eidos.vocab.ConscientiousnessTerm.class);
        registry.register(io.casehub.eidos.vocab.ThomasKilmannTerm.class);
        return registry;
    }
}
