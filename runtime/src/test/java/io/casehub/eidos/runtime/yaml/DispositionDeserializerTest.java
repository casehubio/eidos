package io.casehub.eidos.runtime.yaml;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.casehub.eidos.api.AgentDisposition;
import io.casehub.eidos.api.DispositionAxis;
import io.casehub.eidos.core.yaml.DispositionDeserializer;
import io.casehub.yaml.jackson.YamlMappers;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DispositionDeserializerTest {

    private final ObjectMapper mapper = createMapper(null);

    private static ObjectMapper createMapper(io.casehub.eidos.api.VocabularyRegistry registry) {
        var module = new SimpleModule();
        module.addDeserializer(AgentDisposition.class, new DispositionDeserializer(registry));
        return YamlMappers.create().registerModule(module);
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
    void weightedAxis_parsesTermAndWeight() throws Exception {
        var yaml = """
                   socialOrient:
                     - term: collaborative
                       weight: 0.7
                     - term: independent
                       weight: 0.3
                   ruleFollowing: strict
                   """;
        var disp = mapper.readValue(yaml, AgentDisposition.class);
        assertThat(disp.get(DispositionAxis.SOCIAL_ORIENTATION)).hasSize(2);
        assertThat(disp.get(DispositionAxis.SOCIAL_ORIENTATION).get(0).term()).isEqualTo("collaborative");
        assertThat(disp.get(DispositionAxis.SOCIAL_ORIENTATION).get(0).weight()).isEqualTo(0.7);
        assertThat(disp.get(DispositionAxis.SOCIAL_ORIENTATION).get(1).term()).isEqualTo("independent");
        assertThat(disp.get(DispositionAxis.SOCIAL_ORIENTATION).get(1).weight()).isEqualTo(0.3);
        // bare string form still works alongside
        assertThat(disp.primaryTerm(DispositionAxis.RULE_FOLLOWING)).isEqualTo("strict");
    }

    @Test
    void weightedAxis_allFiveAxes() throws Exception {
        var yaml = """
                   socialOrient:
                     - term: collaborative
                       weight: 0.7
                     - term: independent
                       weight: 0.3
                   ruleFollowing:
                     - term: strict
                       weight: 0.6
                     - term: flexible
                       weight: 0.4
                   riskAppetite:
                     - term: bold
                       weight: 0.8
                     - term: cautious
                       weight: 0.2
                   autonomy:
                     - term: autonomous
                       weight: 0.9
                     - term: guided
                       weight: 0.1
                   conflictMode:
                     - term: competing
                       weight: 0.5
                     - term: collaborating
                       weight: 0.5
                   """;
        var disp = mapper.readValue(yaml, AgentDisposition.class);
        assertThat(disp.get(DispositionAxis.SOCIAL_ORIENTATION)).hasSize(2);
        assertThat(disp.get(DispositionAxis.RULE_FOLLOWING)).hasSize(2);
        assertThat(disp.get(DispositionAxis.RISK_APPETITE)).hasSize(2);
        assertThat(disp.get(DispositionAxis.AUTONOMY)).hasSize(2);
        assertThat(disp.get(DispositionAxis.CONFLICT_MODE)).hasSize(2);
    }

    @Test
    void weightedAxis_preventsEnneagramOverride() throws Exception {
        var registry        = testVocabRegistry();
        var mapperWithVocab = createMapper(registry);
        var yaml = """
                   enneagramType: challenger
                   socialOrient:
                     - term: collaborative
                       weight: 0.7
                     - term: independent
                       weight: 0.3
                   """;
        var disp = mapperWithVocab.readValue(yaml, AgentDisposition.class);
        // explicit weighted axis should NOT be overwritten by enneagram derivation
        assertThat(disp.get(DispositionAxis.SOCIAL_ORIENTATION)).hasSize(2);
        assertThat(disp.get(DispositionAxis.SOCIAL_ORIENTATION).get(0).term()).isEqualTo("collaborative");
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
