package io.casehub.eidos.eval;

import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentDisposition;
import io.casehub.eidos.api.DispositionSignalStore;
import io.casehub.eidos.api.DispositionValue;
import io.casehub.eidos.api.VocabularyRegistry;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class PersonalityEvolutionJudgeTest {

    @Inject
    PersonalityEvolutionJudge judge;

    @Inject
    DispositionSignalStore signalStore;

    @Inject
    VocabularyRegistry vocabRegistry;

    static final String JUNGIAN_URI = "urn:casehub:vocab:jungian";

    static List<DispositionValue> intpProfile() {
        return List.of(
                new DispositionValue("ti", 0.35),
                new DispositionValue("ne", 0.20),
                new DispositionValue("si", 0.075),
                new DispositionValue("fe", 0.075),
                new DispositionValue("fi", 0.075),
                new DispositionValue("ni", 0.075),
                new DispositionValue("te", 0.075),
                new DispositionValue("se", 0.075));
    }

    AgentDescriptor intpAgent() {
        return AgentDescriptor.builder()
                .agentId("evo-test").name("Test INTP").slot("test").tenancyId("evo-t1")
                .dispositionVocabulary(JUNGIAN_URI)
                .disposition(AgentDisposition.builder()
                        .dispositionProfile(intpProfile())
                        .build())
                .build();
    }

    @BeforeEach
    void setUp() {
        signalStore.clear("evo-test", "evo-t1");
    }

    @Test
    void no_evolution_with_zero_activations() {
        var result = judge.evaluate(intpAgent(), "fe", 0);
        assertThat(result.psa()).isEqualTo(0.0);
        assertThat(result.evolutionType()).isNull();
    }

    @Test
    void dominant_auxiliary_swap_with_sufficient_activations() {
        // ne needs effective weight >= ti base (0.35)
        // ne_raw = 0.20 + n*0.06; sum = 1.0 + n*0.06
        // ne_eff = (0.20 + 0.06n) / (1.0 + 0.06n) >= 0.35
        // 0.20 + 0.06n >= 0.35 + 0.021n → 0.039n >= 0.15 → n >= 4
        var result = judge.evaluate(intpAgent(), "ne", 4);
        assertThat(result.evolutionType()).isNotNull();
        assertThat(result.resultingType()).isNotNull();
        assertThat(result.psa()).isGreaterThanOrEqualTo(0.0);
    }

    @Test
    void evolution_produces_valid_weight_tiers() {
        var result = judge.evaluate(intpAgent(), "ne", 4);
        if (result.psa() > 0.0) {
            assertThat(result.weightTiersValid()).isTrue();
        }
    }

    @Test
    void evolution_produces_structurally_valid_profile() {
        var result = judge.evaluate(intpAgent(), "ne", 4);
        if (result.psa() > 0.0) {
            assertThat(result.structurallyValid()).isTrue();
        }
    }

    @Test
    void heavy_shadow_activation_triggers_replacement() {
        // te (shadow of ti) needs effective weight >= ti base (0.35)
        // te_raw = 0.075 + n*0.06; sum = 1.0 + n*0.06
        // te_eff = (0.075 + 0.06n) / (1.0 + 0.06n) >= 0.35
        // 0.075 + 0.06n >= 0.35 + 0.021n → 0.039n >= 0.275 → n >= 8
        var result = judge.evaluate(intpAgent(), "te", 8);
        assertThat(result.evolutionType()).isNotNull();
        assertThat(result.detail()).isNotEqualTo("Aligned");
    }

    @Test
    void initial_type_reflects_dominant_auxiliary() {
        var result = judge.evaluate(intpAgent(), "fe", 0);
        assertThat(result.initialType()).isEqualTo("TI-NE");
    }
}
