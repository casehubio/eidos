package io.casehub.eidos.runtime.health;

import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentDisposition;
import io.casehub.eidos.api.CapabilityHealth.ProbeContext;
import io.casehub.eidos.api.DispositionHealth;
import io.casehub.eidos.api.DispositionHealth.DispositionStatus;
import io.casehub.eidos.api.DispositionSignalStore;
import io.casehub.eidos.api.DispositionValue;
import io.casehub.eidos.api.VocabularyMetadata;
import io.casehub.eidos.api.VocabularyRegistry;
import io.casehub.eidos.api.VocabularyTerm;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@QuarkusTest
class DefaultDispositionHealthTest {

    @Inject
    DispositionHealth health;

    @Inject
    DispositionSignalStore signalStore;

    @Inject
    VocabularyRegistry vocabRegistry;

    @VocabularyMetadata(uri = "urn:test:disposition-functions", name = "Test Functions", version = "1.0")
    enum TestFunction implements VocabularyTerm {
        F1("f1", "Function 1") {
            @Override public Optional<VocabularyTerm> opposite() { return Optional.of(F3); }
        },
        F2("f2", "Function 2") {
            @Override public Optional<VocabularyTerm> opposite() { return Optional.of(F4); }
        },
        F3("f3", "Function 3") {
            @Override public Optional<VocabularyTerm> opposite() { return Optional.of(F1); }
        },
        F4("f4", "Function 4") {
            @Override public Optional<VocabularyTerm> opposite() { return Optional.of(F2); }
        },
        F5("f5", "Function 5"),
        F6("f6", "Function 6");

        final String value, label;
        TestFunction(String v, String l) { value = v; label = l; }
        @Override public String value() { return value; }
        @Override public String label() { return label; }
    }

    static final String VOCAB_URI = "urn:test:disposition-functions";

    static List<DispositionValue> testProfile() {
        return List.of(
                new DispositionValue("f1", 0.35),
                new DispositionValue("f2", 0.25),
                new DispositionValue("f3", 0.15),
                new DispositionValue("f4", 0.10),
                new DispositionValue("f5", 0.08),
                new DispositionValue("f6", 0.07));
    }

    AgentDescriptor agentWithProfile(String agentId, List<DispositionValue> profile) {
        return AgentDescriptor.builder()
                              .agentId(agentId).name("Test").slot("test").tenancyId("t1")
                              .dispositionVocabulary(VOCAB_URI)
                              .disposition(AgentDisposition.builder()
                                                           .dispositionProfile(profile)
                                                           .build())
                              .build();}

    @BeforeEach
    void setUp() {
        if (!vocabRegistry.isRegistered(VOCAB_URI)) {
            vocabRegistry.register(TestFunction.class);
        }
        signalStore.clear("a1", "t1");
    }

    @Test
    void aligned_when_no_profile() {
        var descriptor = AgentDescriptor.builder()
                .agentId("a1").name("Test").slot("test").tenancyId("t1")
                .disposition(AgentDisposition.builder().build())
                .build();
        var status = health.probe(descriptor, ProbeContext.of(null));
        assertThat(status).isInstanceOf(DispositionStatus.Aligned.class);
        assertThat(((DispositionStatus.Aligned) status).effectiveWeights()).isEmpty();
    }

    @Test
    void aligned_when_no_signals() {
        var descriptor = agentWithProfile("a1", testProfile());
        var status = health.probe(descriptor, ProbeContext.of(null));
        assertThat(status).isInstanceOf(DispositionStatus.Aligned.class);
        var aligned = (DispositionStatus.Aligned) status;
        assertThat(aligned.effectiveWeights()).containsEntry("f1", 0.35);
        assertThat(aligned.effectiveWeights()).containsEntry("f2", 0.25);
    }

    @Test
    void drifted_when_small_activation() {
        var descriptor = agentWithProfile("a1", testProfile());
        signalStore.recordActivation("a1", "t1", "f2");
        var status = health.probe(descriptor, ProbeContext.of(null));
        assertThat(status).isInstanceOf(DispositionStatus.Drifted.class);
        var drifted = (DispositionStatus.Drifted) status;
        assertThat(drifted.mostActivated()).isEqualTo("f2");
        assertThat(drifted.driftMagnitude()).isGreaterThan(0.0);
    }

    @Test
    void dominant_auxiliary_swap_when_auxiliary_exceeds_dominant_base() {
        var descriptor = agentWithProfile("a1", testProfile());
        // F2 needs 3 activations at delta=0.06 to reach F1's base weight of 0.35
        for (int i = 0; i < 3; i++) signalStore.recordActivation("a1", "t1", "f2");
        var status = health.probe(descriptor, ProbeContext.of(null));
        assertThat(status).isInstanceOf(DispositionStatus.EvolutionPending.class);
        var pending = (DispositionStatus.EvolutionPending) status;
        assertThat(pending.type().name()).isEqualTo("DOMINANT_AUXILIARY_SWAP");
        assertThat(pending.candidateFunction()).isEqualTo("f2");
    }

    @Test
    void dominant_replacement_when_shadow_exceeds_dominant_base() {
        var descriptor = agentWithProfile("a1", testProfile());
        // F3 (shadow of F1) needs 6 activations to reach F1's base of 0.35
        for (int i = 0; i < 6; i++) signalStore.recordActivation("a1", "t1", "f3");
        var status = health.probe(descriptor, ProbeContext.of(null));
        assertThat(status).isInstanceOf(DispositionStatus.EvolutionPending.class);
        var pending = (DispositionStatus.EvolutionPending) status;
        assertThat(pending.type().name()).isEqualTo("DOMINANT_REPLACEMENT");
        assertThat(pending.candidateFunction()).isEqualTo("f3");
    }

    @Test
    void auxiliary_replacement_when_auxiliary_shadow_exceeds_auxiliary_base() {
        var descriptor = agentWithProfile("a1", testProfile());
        // F4 (shadow of F2) needs 4 activations to reach F2's base of 0.25
        for (int i = 0; i < 4; i++) signalStore.recordActivation("a1", "t1", "f4");
        var status = health.probe(descriptor, ProbeContext.of(null));
        assertThat(status).isInstanceOf(DispositionStatus.EvolutionPending.class);
        var pending = (DispositionStatus.EvolutionPending) status;
        assertThat(pending.type().name()).isEqualTo("AUXILIARY_REPLACEMENT");
        assertThat(pending.candidateFunction()).isEqualTo("f4");
    }

    @Test
    void structural_reorganization_when_unrelated_exceeds_dominant_base() {
        var descriptor = agentWithProfile("a1", testProfile());
        // F5 (no shadow relationship) needs 7 activations to reach F1's base of 0.35
        for (int i = 0; i < 7; i++) signalStore.recordActivation("a1", "t1", "f5");
        var status = health.probe(descriptor, ProbeContext.of(null));
        assertThat(status).isInstanceOf(DispositionStatus.EvolutionPending.class);
        var pending = (DispositionStatus.EvolutionPending) status;
        assertThat(pending.type().name()).isEqualTo("STRUCTURAL_REORGANIZATION");
        assertThat(pending.candidateFunction()).isEqualTo("f5");
    }

    @Test
    void over_reinforcement_returns_drifted_when_dominant_exceeds_ceiling() {
        var descriptor = agentWithProfile("a1", testProfile());
        // Heavily reinforce the dominant function (f1 base=0.35, ceiling=0.50)
        // With enough activations of f1 only, its effective weight rises above 0.50
        // f1_raw = 0.35 + n*0.06; sum = 1.0 + n*0.06
        // f1_eff = (0.35 + 0.06n) / (1.0 + 0.06n) ≥ 0.50
        // 0.35 + 0.06n ≥ 0.50 + 0.03n → 0.03n ≥ 0.15 → n ≥ 5
        for (int i = 0; i < 5; i++) {signalStore.recordActivation("a1", "t1", "f1");}
        var status = health.probe(descriptor, ProbeContext.of(null));
        assertThat(status).isInstanceOf(DispositionStatus.Drifted.class);
        var drifted = (DispositionStatus.Drifted) status;
        assertThat(drifted.driftMagnitude()).isGreaterThan(0.0);
    }


    @Test
    void effective_weights_normalized_to_one() {
        var descriptor = agentWithProfile("a1", testProfile());
        for (int i = 0; i < 2; i++) signalStore.recordActivation("a1", "t1", "f2");
        var status = health.probe(descriptor, ProbeContext.of(null));
        Map<String, Double> weights = switch (status) {
            case DispositionStatus.Aligned a -> a.effectiveWeights();
            case DispositionStatus.Drifted d -> d.effectiveWeights();
            case DispositionStatus.EvolutionPending e -> e.effectiveWeights();
        };
        double sum = weights.values().stream().mapToDouble(Double::doubleValue).sum();
        assertThat(sum).isCloseTo(1.0, within(0.001));
    }

    @Test
    void drift_magnitude_is_l2_distance() {
        var descriptor = agentWithProfile("a1", testProfile());
        signalStore.recordActivation("a1", "t1", "f2");
        var status = health.probe(descriptor, ProbeContext.of(null));
        assertThat(status).isInstanceOf(DispositionStatus.Drifted.class);
        var drifted = (DispositionStatus.Drifted) status;
        assertThat(drifted.driftMagnitude()).isCloseTo(0.048, within(0.005));
    }

    @Test
    void aligned_when_no_vocabulary_uri() {
        var descriptor = AgentDescriptor.builder()
                .agentId("a1").name("Test").slot("test").tenancyId("t1")
                .disposition(AgentDisposition.builder()
                        .dispositionProfile(testProfile())
                        .build())
                .build();
        signalStore.recordActivation("a1", "t1", "f3");
        signalStore.recordActivation("a1", "t1", "f3");
        signalStore.recordActivation("a1", "t1", "f3");
        signalStore.recordActivation("a1", "t1", "f3");
        signalStore.recordActivation("a1", "t1", "f3");
        signalStore.recordActivation("a1", "t1", "f3");
        var status = health.probe(descriptor, ProbeContext.of(null));
        // Without vocabulary, can't resolve shadows → only weight-based conditions
        // F3 gains 6 activations but can't be identified as shadow of F1 without vocab
        // F3 is the 3rd function by base weight, not auxiliary → not a swap
        // Without shadow knowledge, no DOMINANT_REPLACEMENT detected
        // But drift IS detected
        assertThat(status).isInstanceOf(DispositionStatus.Drifted.class);
    }

    @Test
    void tenancy_isolation_between_probes() {
        var descriptor1 = agentWithProfile("a1", testProfile());
        var descriptor2 = AgentDescriptor.builder()
                .agentId("a1").name("Test").slot("test").tenancyId("t2")
                .dispositionVocabulary(VOCAB_URI)
                .disposition(AgentDisposition.builder()
                        .dispositionProfile(testProfile())
                        .build())
                .build();
        for (int i = 0; i < 3; i++) signalStore.recordActivation("a1", "t1", "f2");
        var status1 = health.probe(descriptor1, ProbeContext.of(null));
        var status2 = health.probe(descriptor2, ProbeContext.of(null));
        assertThat(status1).isInstanceOf(DispositionStatus.EvolutionPending.class);
        assertThat(status2).isInstanceOf(DispositionStatus.Aligned.class);
    }
}
