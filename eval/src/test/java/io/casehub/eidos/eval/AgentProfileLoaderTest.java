package io.casehub.eidos.eval;

import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentDisposition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentProfileLoaderTest {

    @Test
    void load_returns_all_profiles_from_index() {
        final List<AgentProfile> profiles = new AgentProfileLoader().load();
        assertThat(profiles).hasSize(8);
        assertThat(profiles).extracting(AgentProfile::name)
            .containsExactlyInAnyOrder(
                "sw-engineer-careful",
                "sw-engineer-bold",
                "security-analyst-defensive",
                "security-analyst-proactive",
                "product-manager",
                "clinical-researcher",
                "customer-support-agent",
                "technical-writer");
    }

    @Test
    void load_deserializes_descriptor_fields_correctly() {
        final var profile = new AgentProfileLoader().load().stream()
            .filter(p -> p.name().equals("sw-engineer-careful")).findFirst().orElseThrow();
        assertThat(profile.descriptor().agentId()).isEqualTo("sw-engineer-careful-01");
        assertThat(profile.descriptor().slot()).isEqualTo("reviewer");
        assertThat(profile.descriptor().tenancyId()).isEqualTo("profiles-1");
    }

    @Test
    void load_deserializes_delegation_boolean() {
        final var profile = new AgentProfileLoader().load().stream()
            .filter(p -> p.name().equals("sw-engineer-careful")).findFirst().orElseThrow();
        assertThat(profile.descriptor().disposition()).isNotNull();
        assertThat(profile.descriptor().disposition().delegation()).isFalse();
    }

    @Test
    void load_deserializes_expectedTraits() {
        final var profile = new AgentProfileLoader().load().stream()
            .filter(p -> p.name().equals("sw-engineer-careful")).findFirst().orElseThrow();
        assertThat(profile.expectedTraits()).containsEntry("riskAppetite", TraitPolarity.LOW);
    }

    @Test
    void load_deserializes_sourceType_enum() {
        final var profile = new AgentProfileLoader().load().stream()
            .filter(p -> p.name().equals("sw-engineer-careful")).findFirst().orElseThrow();
        assertThat(profile.sourceType()).isEqualTo(SourceType.ANTHROPIC_LIBRARY);
    }

    @Test
    void loadIndex_returns_variant_pairs() {
        final var index = new AgentProfileLoader().loadIndex();
        assertThat(index.variants()).hasSize(2);
        assertThat(index.variants().get(0).primaryAxis()).isEqualTo("riskAppetite");
        assertThat(index.variants().get(0).higher()).isEqualTo("sw-engineer-bold");
    }

    @Test
    void stage0_fails_when_primaryAxis_same_value_in_both_profiles() {
        // Both profiles have riskAppetite=conservative (identical) — Stage 0 must reject
        final var disp = new AgentDisposition("independent", "strict", "conservative", "directed", null, false);
        final var desc1 = new AgentDescriptor(
            "p1", "P1", null, null, null, null, null,
            null, null, null, null, "worker", List.of(), disp, null, null, "t");
        final var desc2 = new AgentDescriptor(
            "p2", "P2", null, null, null, null, null,
            null, null, null, null, "worker", List.of(), disp, null, null, "t");
        final var p1 = new AgentProfile(
            "p1", "Role", "domain", null, null, SourceType.PRACTITIONER,
            "prose", null, null, Map.of(), Map.of(), desc1, List.of());
        final var p2 = new AgentProfile(
            "p2", "Role", "domain", null, null, SourceType.PRACTITIONER,
            "prose", null, null, Map.of(), Map.of(), desc2, List.of());

        final var index = new VariantIndex(
            List.of("p1.yaml", "p2.yaml"),
            List.of(new VariantPair("riskAppetite", "p1", "p2")));
        final var profiles = Map.of("p1", p1, "p2", p2);

        assertThatThrownBy(() -> new AgentProfileLoader().validatePairs(index, profiles))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("same value");
    }
}
