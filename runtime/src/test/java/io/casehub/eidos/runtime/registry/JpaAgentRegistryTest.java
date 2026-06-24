package io.casehub.eidos.runtime.registry;

import io.casehub.eidos.api.*;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@QuarkusTest
class JpaAgentRegistryTest {

    @Inject
    AgentRegistry registry;

    static AgentDescriptor descriptor(String agentId, String slot, String tenancyId,
                                      String... capabilityNames) {
        var caps = Arrays.stream(capabilityNames)
            .map(n -> AgentCapability.builder().name(n).qualityHint(0.9)
                .epistemicDomains(Map.of()).build())
            .toList();
        return AgentDescriptor.builder()
            .agentId(agentId)
            .name("Agent " + agentId)
            .version("1.0")
            .provider("anthropic")
            .modelFamily("claude")
            .modelVersion("claude-3-7")
            .slot(slot)
            .capabilities(caps)
            .disposition(AgentDisposition.builder()
                .socialOrient("collaborative")
                .ruleFollowing("principled")
                .riskAppetite("measured")
                .autonomy("semi-autonomous")
                .build())
            .tenancyId(tenancyId)
            .build();
    }

    @Test
    @TestTransaction
    void register_and_find_by_id() {
        registry.register(descriptor("agent-1", "reviewer", "default", "code-review"));

        var found = registry.findById("agent-1", "default");

        assertThat(found).isPresent();
        assertThat(found.get().agentId()).isEqualTo("agent-1");
        assertThat(found.get().slot()).isEqualTo("reviewer");
        assertThat(found.get().tenancyId()).isEqualTo("default");
        assertThat(found.get().capabilities()).hasSize(1);
        assertThat(found.get().capabilities().get(0).name()).isEqualTo("code-review");
    }

    @Test
    @TestTransaction
    void find_by_slot_returns_matching_agents_only() {
        registry.register(descriptor("agent-2a", "reviewer", "default", "code-review"));
        registry.register(descriptor("agent-2b", "planner", "default", "planning"));

        var reviewers = registry.find(AgentQuery.bySlot("reviewer", "default"));

        assertThat(reviewers).hasSize(1);
        assertThat(reviewers.get(0).agentId()).isEqualTo("agent-2a");
    }

    @Test
    @TestTransaction
    void find_by_capability_returns_agents_with_that_capability() {
        registry.register(descriptor("agent-3a", "reviewer", "default", "code-review", "test-writing"));
        registry.register(descriptor("agent-3b", "executor", "default", "test-writing"));

        var codeReviewers = registry.find(AgentQuery.byCapability("code-review", "default"));

        assertThat(codeReviewers).hasSize(1);
        assertThat(codeReviewers.get(0).agentId()).isEqualTo("agent-3a");
    }

    @Test
    @TestTransaction
    void find_by_slot_and_capability_applies_both_filters() {
        registry.register(descriptor("agent-4a", "reviewer", "default", "code-review"));
        registry.register(descriptor("agent-4b", "executor", "default", "code-review"));

        var result = registry.find(AgentQuery.bySlotAndCapability("reviewer", "code-review", "default"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).agentId()).isEqualTo("agent-4a");
    }

    @Test
    @TestTransaction
    void register_upserts_existing_agent() {
        registry.register(descriptor("agent-5", "reviewer", "default", "code-review"));
        registry.register(descriptor("agent-5", "planner", "default", "planning"));

        var found = registry.findById("agent-5", "default");

        assertThat(found).isPresent();
        assertThat(found.get().slot()).isEqualTo("planner");
        assertThat(found.get().capabilities()).hasSize(1);
        assertThat(found.get().capabilities().get(0).name()).isEqualTo("planning");
    }

    @Test
    @TestTransaction
    void tenancy_isolation_excludes_other_tenant_agents() {
        registry.register(descriptor("agent-6", "reviewer", "tenant-a", "code-review"));

        var result = registry.find(AgentQuery.bySlot("reviewer", "tenant-b"));

        assertThat(result).isEmpty();
    }

    @Test
    @TestTransaction
    void find_all_returns_only_own_tenant() {
        registry.register(descriptor("agent-7a", "reviewer", "tenant-a", "code-review"));
        registry.register(descriptor("agent-7b", "planner", "tenant-b", "planning"));

        var tenantA = registry.find(AgentQuery.all("tenant-a"));

        assertThat(tenantA).hasSize(1);
        assertThat(tenantA.get(0).agentId()).isEqualTo("agent-7a");
    }

    @Test
    @TestTransaction
    void find_by_id_returns_empty_for_missing_agent() {
        assertThat(registry.findById("nonexistent", "default")).isEmpty();
    }

    @Test
    @TestTransaction
    void findById_with_null_agentId_throws() {
        assertThatNullPointerException()
            .isThrownBy(() -> registry.findById(null, "default"))
            .withMessageContaining("agentId");
    }

    @Test
    @TestTransaction
    void findById_with_null_tenancyId_throws() {
        assertThatThrownBy(() -> registry.findById("nonexistent", null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    @TestTransaction
    void axis_vocabularies_round_trips_through_jpa() {
        var d = AgentDescriptor.builder()
            .agentId("agent-axis")
            .name("Axis Agent")
            .version("1.0")
            .provider("anthropic")
            .modelFamily("claude")
            .modelVersion("claude-3-7")
            .slotVocabulary("urn:casehub:vocab:belbin")
            .dispositionVocabulary("urn:casehub:vocab:disc")
            .axisVocabularies(Map.of(DispositionAxis.CONFLICT_MODE, "urn:casehub:vocab:thomas-kilmann"))
            .slot("co-ordinator")
            .capabilities(List.of())
            .disposition(AgentDisposition.builder()
                .socialOrient("facilitative")
                .ruleFollowing("principled")
                .riskAppetite("measured")
                .autonomy("semi-autonomous")
                .conflictMode("collaborating")
                .delegation(true)
                .build())
            .tenancyId("default")
            .build();
        registry.register(d);
        var found = registry.findById("agent-axis", "default").orElseThrow();
        assertThat(found.axisVocabularies())
            .containsEntry(DispositionAxis.CONFLICT_MODE, "urn:casehub:vocab:thomas-kilmann");
        assertThat(found.vocabUriForAxis(DispositionAxis.CONFLICT_MODE))
            .contains("urn:casehub:vocab:thomas-kilmann");
        assertThat(found.vocabUriForAxis(DispositionAxis.SOCIAL_ORIENTATION))
            .contains("urn:casehub:vocab:disc");
    }

    @Test
    @TestTransaction
    void null_axis_vocabularies_round_trips_as_null() {
        registry.register(descriptor("agent-noaxis", "reviewer", "default"));
        var found = registry.findById("agent-noaxis", "default").orElseThrow();
        assertThat(found.axisVocabularies()).isNull();
    }

    @Test
    @TestTransaction
    void domain_filter_excludes_agents_with_excluded_domain() {
        var caps = List.of(AgentCapability.builder()
            .name("code-review").qualityHint(0.9)
            .excludedDomains(Set.of("rust", "c++"))
            .build());
        registry.register(AgentDescriptor.builder()
            .agentId("agent-excl").name("Excluded").slot("reviewer")
            .capabilities(caps).tenancyId("default").build());
        registry.register(descriptor("agent-incl", "reviewer", "default", "code-review"));

        var result = registry.find(AgentQuery.byCapabilityAndDomain("code-review", "rust", "default"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).agentId()).isEqualTo("agent-incl");
    }

    @Test
    @TestTransaction
    void domain_filter_includes_agents_without_exclusions() {
        registry.register(descriptor("agent-open", "reviewer", "default", "code-review"));

        var result = registry.find(AgentQuery.byCapabilityAndDomain("code-review", "java", "default"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).agentId()).isEqualTo("agent-open");
    }

    @Test
    @TestTransaction
    void excluded_domains_round_trips_through_jpa() {
        var caps = List.of(AgentCapability.builder()
            .name("code-review").qualityHint(0.9)
            .excludedDomains(Set.of("rust", "go"))
            .build());
        registry.register(AgentDescriptor.builder()
            .agentId("agent-rt").name("RT").slot("reviewer")
            .capabilities(caps).tenancyId("default").build());

        var found = registry.findById("agent-rt", "default").orElseThrow();
        assertThat(found.capabilities().get(0).excludedDomains())
            .containsExactlyInAnyOrder("rust", "go");
    }
}
