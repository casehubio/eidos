package io.casehub.eidos.runtime.registrar;

import io.casehub.eidos.api.*;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class DispositionProfileDerivationTest {

    @Inject
    VocabularyRegistry vocabRegistry;

    private static final String JUNGIAN_URI = "urn:casehub:vocab:jungian";

    static List<DispositionValue> intpProfile() {
        return List.of(
                new DispositionValue("ti", 0.35),
                new DispositionValue("ne", 0.20),
                new DispositionValue("fi", 0.08),
                new DispositionValue("ni", 0.07),
                new DispositionValue("si", 0.06),
                new DispositionValue("te", 0.05),
                new DispositionValue("se", 0.05),
                new DispositionValue("fe", 0.04));
    }

    AgentDescriptor descriptorWithProfile(List<DispositionValue> profile) {
        return AgentDescriptor.builder()
                .agentId("test").name("Test").slot("analyst").tenancyId("t1")
                .dispositionVocabulary(JUNGIAN_URI)
                .disposition(AgentDisposition.builder()
                        .dispositionProfile(profile)
                        .build())
                .build();
    }

    @Test
    void derives_social_orient_from_profile() {
        var descriptor = descriptorWithProfile(intpProfile());
        var derived = DescriptorCollector.deriveDispositionAxes(descriptor, vocabRegistry);
        assertThat(derived.disposition().socialOrient())
                .extracting(DispositionValue::term)
                .contains("independent");
    }

    @Test
    void derives_rule_following_from_profile() {
        var descriptor = descriptorWithProfile(intpProfile());
        var derived = DescriptorCollector.deriveDispositionAxes(descriptor, vocabRegistry);
        assertThat(derived.disposition().ruleFollowing()).isNotEmpty();
        assertThat(derived.disposition().ruleFollowing())
                .extracting(DispositionValue::term)
                .contains("principled");
    }

    @Test
    void derives_conflict_mode_from_profile() {
        var descriptor = descriptorWithProfile(intpProfile());
        var derived = DescriptorCollector.deriveDispositionAxes(descriptor, vocabRegistry);
        assertThat(derived.disposition().conflictMode()).isNotEmpty();
    }

    @Test
    void derived_axis_weights_sum_to_one() {
        var descriptor = descriptorWithProfile(intpProfile());
        var derived = DescriptorCollector.deriveDispositionAxes(descriptor, vocabRegistry);
        double sum = derived.disposition().socialOrient().stream()
                .mapToDouble(DispositionValue::weight).sum();
        assertThat(sum).isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.01));
    }

    @Test
    void explicit_axis_values_take_precedence() {
        var descriptor = AgentDescriptor.builder()
                .agentId("test").name("Test").slot("analyst").tenancyId("t1")
                .dispositionVocabulary(JUNGIAN_URI)
                .disposition(AgentDisposition.builder()
                        .socialOrient("collaborative")
                        .dispositionProfile(intpProfile())
                        .build())
                .build();
        var derived = DescriptorCollector.deriveDispositionAxes(descriptor, vocabRegistry);
        assertThat(derived.disposition().socialOrient()).hasSize(1);
        assertThat(derived.disposition().socialOrient().getFirst().term())
                .isEqualTo("collaborative");
    }

    @Test
    void axis_vocabularies_populated_for_derived_axes() {
        var descriptor = descriptorWithProfile(intpProfile());
        var derived = DescriptorCollector.deriveDispositionAxes(descriptor, vocabRegistry);
        var axisVocabs = derived.axisVocabularies();
        assertThat(axisVocabs).isNotNull();
        assertThat(axisVocabs.get(DispositionAxis.SOCIAL_ORIENTATION))
                .isEqualTo("urn:casehub:vocab:conscientiousness");
        assertThat(axisVocabs.get(DispositionAxis.CONFLICT_MODE))
                .isEqualTo("urn:casehub:vocab:thomas-kilmann");
    }

    @Test
    void no_derivation_when_no_profile() {
        var descriptor = AgentDescriptor.builder()
                .agentId("test").name("Test").slot("analyst").tenancyId("t1")
                .disposition(AgentDisposition.builder().build())
                .build();
        var derived = DescriptorCollector.deriveDispositionAxes(descriptor, vocabRegistry);
        assertThat(derived.disposition().socialOrient()).isEmpty();
    }

    @Test
    void no_derivation_when_no_vocabulary() {
        var descriptor = AgentDescriptor.builder()
                .agentId("test").name("Test").slot("analyst").tenancyId("t1")
                .disposition(AgentDisposition.builder()
                        .dispositionProfile(intpProfile())
                        .build())
                .build();
        var derived = DescriptorCollector.deriveDispositionAxes(descriptor, vocabRegistry);
        assertThat(derived.disposition().socialOrient()).isEmpty();
    }
}
