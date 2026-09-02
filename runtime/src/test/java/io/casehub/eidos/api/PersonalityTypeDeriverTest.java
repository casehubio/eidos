package io.casehub.eidos.api;

import io.casehub.eidos.runtime.vocabulary.CdiVocabularyRegistry;
import io.casehub.eidos.vocab.ConscientiousnessTerm;
import io.casehub.eidos.vocab.EnneagramTerm;
import io.casehub.eidos.vocab.JungianFunctionTerm;
import io.casehub.eidos.vocab.MbtiTypeTerm;
import io.casehub.eidos.vocab.ThomasKilmannTerm;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PersonalityTypeDeriverTest {

    @Test
    void mbtiType_derivesProfile_whenNoExplicitProfile() {
        var builder = AgentDisposition.builder();
        var input = new PersonalityInput("ENTJ", "", false, Map.of());
        PersonalityTypeDeriver.derive(input, testVocabRegistry(), builder);
        var disp = builder.build();
        assertThat(disp.dispositionProfile()).hasSize(8);
        assertThat(disp.dispositionProfile().get(0).term()).isEqualTo("te");
    }

    @Test
    void mbtiType_skipped_whenExplicitProfilePresent() {
        var builder = AgentDisposition.builder()
            .dispositionProfile(new DispositionValue("ti", 0.5));
        var input = new PersonalityInput("ENTJ", "", true, Map.of());
        PersonalityTypeDeriver.derive(input, testVocabRegistry(), builder);
        var disp = builder.build();
        assertThat(disp.dispositionProfile()).hasSize(1);
        assertThat(disp.dispositionProfile().get(0).term()).isEqualTo("ti");
    }

    @Test
    void mbtiType_caseInsensitive() {
        var builder = AgentDisposition.builder();
        var input = new PersonalityInput("entj", "", false, Map.of());
        PersonalityTypeDeriver.derive(input, testVocabRegistry(), builder);
        assertThat(builder.build().dispositionProfile()).hasSize(8);
    }

    @Test
    void enneagramType_derivesAxes_whenNoExplicitValues() {
        var builder = AgentDisposition.builder();
        var input = new PersonalityInput("", "challenger", false, Map.of());
        PersonalityTypeDeriver.derive(input, testVocabRegistry(), builder);
        var disp = builder.build();
        assertThat(disp.primaryTerm(DispositionAxis.RULE_FOLLOWING)).isEqualTo("flexible");
        assertThat(disp.primaryTerm(DispositionAxis.RISK_APPETITE)).isEqualTo("bold");
        assertThat(disp.primaryTerm(DispositionAxis.CONFLICT_MODE)).isEqualTo("competing");
    }

    @Test
    void enneagramType_doesNotOverrideExplicitAxes() {
        var explicitAxes = new EnumMap<DispositionAxis, String>(DispositionAxis.class);
        explicitAxes.put(DispositionAxis.SOCIAL_ORIENTATION, "collaborative");
        var builder = AgentDisposition.builder().socialOrient("collaborative");
        var input = new PersonalityInput("", "challenger", false, explicitAxes);
        PersonalityTypeDeriver.derive(input, testVocabRegistry(), builder);
        var disp = builder.build();
        assertThat(disp.primaryTerm(DispositionAxis.SOCIAL_ORIENTATION)).isEqualTo("collaborative");
    }

    @Test
    void nullRegistry_noOp() {
        var builder = AgentDisposition.builder();
        var input = new PersonalityInput("ENTJ", "challenger", false, Map.of());
        PersonalityTypeDeriver.derive(input, null, builder);
        var disp = builder.build();
        assertThat(disp.dispositionProfile()).isEmpty();
    }

    @Test
    void emptyTypes_noOp() {
        var builder = AgentDisposition.builder();
        var input = new PersonalityInput("", "", false, Map.of());
        PersonalityTypeDeriver.derive(input, testVocabRegistry(), builder);
        var disp = builder.build();
        assertThat(disp.dispositionProfile()).isEmpty();
    }

    private static VocabularyRegistry testVocabRegistry() {
        var registry = new CdiVocabularyRegistry();
        registry.register(JungianFunctionTerm.class);
        registry.register(MbtiTypeTerm.class);
        registry.register(EnneagramTerm.class);
        registry.register(ConscientiousnessTerm.class);
        registry.register(ThomasKilmannTerm.class);
        return registry;
    }
}
