package io.casehub.eidos.memory;

import io.casehub.eidos.api.AgentCapability;
import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentDisposition;
import io.casehub.eidos.api.AgentGoal;
import io.casehub.eidos.api.AgentQuery;
import io.casehub.eidos.api.AgentRegistry;
import io.casehub.eidos.api.AgentValidationException;
import io.casehub.eidos.api.GoalPriority;
import io.casehub.eidos.api.MatchDegree;
import io.casehub.eidos.api.TestCapabilityVocab;
import io.casehub.eidos.api.Visibility;
import io.casehub.eidos.api.VocabularyRegistry;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@QuarkusTest
class InMemoryAgentRegistryTest {

    @Inject AgentRegistry registry;
    @Inject InMemoryAgentRegistry store;
    @Inject VocabularyRegistry vocabularyRegistry;

    @BeforeEach
    void clearStore() {
        store.clear();
    }

    @BeforeEach
    void registerTestVocabulary() {
        // Register the test capability vocabulary before each test
        vocabularyRegistry.register(TestCapabilityVocab.class);
    }

    static AgentDescriptor descriptor(String agentId, String slot, String tenancyId, String... caps) {
        var capabilities = Arrays.stream(caps)
            .map(n -> AgentCapability.builder().name(n).qualityHint(0.9)
                .epistemicDomains(Map.of()).build())
            .toList();
        return AgentDescriptor.builder()
            .agentId(agentId)
            .name("Agent")
            .version("1.0")
            .provider("anthropic")
            .modelFamily("claude")
            .modelVersion("claude-3-7")
            .slot(slot)
            .capabilities(capabilities)
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
    void register_and_find_by_id() {
        registry.register(descriptor("m-1", "reviewer", "default", "code-review"));
        var found = registry.findById("m-1", "default");
        assertThat(found).isPresent();
        assertThat(found.get().slot()).isEqualTo("reviewer");
        assertThat(found.get().tenancyId()).isEqualTo("default");
    }

    @Test
    void find_by_slot() {
        registry.register(descriptor("m-2a", "reviewer", "default", "code-review"));
        registry.register(descriptor("m-2b", "planner", "default", "planning"));
        var result = registry.find(AgentQuery.bySlot("reviewer", "default"));
        assertThat(result).extracting(m -> m.descriptor().agentId())
            .contains("m-2a").doesNotContain("m-2b");
        assertThat(result).allSatisfy(m -> assertThat(m.resolvedCapability()).isNull());
    }

    @Test
    void find_by_capability() {
        registry.register(descriptor("m-3a", "reviewer", "default", "code-review"));
        registry.register(descriptor("m-3b", "executor", "default", "testing"));
        var result = registry.find(AgentQuery.byCapability("code-review", "default"));
        assertThat(result).extracting(m -> m.descriptor().agentId())
            .contains("m-3a").doesNotContain("m-3b");
        assertThat(result).allSatisfy(m -> {
            assertThat(m.resolvedCapability()).isNotNull();
            assertThat(m.resolvedCapability().capability().name()).isEqualTo("code-review");
            assertThat(m.resolvedCapability().degree()).isInstanceOf(MatchDegree.Exact.class);
        });
    }

    @Test
    void find_by_slot_and_capability() {
        registry.register(descriptor("m-4a", "reviewer", "default", "code-review"));
        registry.register(descriptor("m-4b", "executor", "default", "code-review"));
        var result = registry.find(AgentQuery.bySlotAndCapability("reviewer", "code-review", "default"));
        assertThat(result).extracting(m -> m.descriptor().agentId())
            .contains("m-4a").doesNotContain("m-4b");
        assertThat(result).allSatisfy(m -> assertThat(m.resolvedCapability()).isNotNull());
    }

    @Test
    void tenancy_isolation() {
        registry.register(descriptor("m-5", "reviewer", "tenant-a", "code-review"));
        var result = registry.find(AgentQuery.bySlot("reviewer", "tenant-b"));
        assertThat(result).isEmpty();
    }

    @Test
    void upsert_replaces_existing() {
        registry.register(descriptor("m-6", "reviewer", "default", "code-review"));
        registry.register(descriptor("m-6", "planner", "default", "planning"));
        assertThat(registry.findById("m-6", "default").get().slot()).isEqualTo("planner");
    }

    @Test
    void findById_with_null_tenancyId_throws() {
        registry.register(descriptor("m-10", "reviewer", "default", "code-review"));
        assertThatNullPointerException()
            .isThrownBy(() -> registry.findById("m-10", null))
            .withMessageContaining("tenancyId");
    }

    @Test
    void findById_with_null_agentId_throws() {
        assertThatNullPointerException()
            .isThrownBy(() -> registry.findById(null, "default"))
            .withMessageContaining("agentId");
    }

    @Test
    void capability_with_empty_types_lists_is_valid() {
        // Empty inputTypes/outputTypes/tags are allowed; name is required.
        var cap = AgentCapability.builder().name("empty-types").qualityHint(0.9)
            .inputTypes(List.of()).outputTypes(List.of()).tags(List.of())
            .epistemicDomains(Map.of()).build();
        var descriptor = AgentDescriptor.builder()
            .agentId("m-8")
            .name("Agent")
            .version("1.0")
            .provider("anthropic")
            .modelFamily("claude")
            .modelVersion("claude-3-7")
            .slot("reviewer")
            .capabilities(List.of(cap))
            .disposition(AgentDisposition.builder()
                .socialOrient("collaborative")
                .ruleFollowing("principled")
                .riskAppetite("measured")
                .autonomy("semi-autonomous")
                .build())
            .tenancyId("default")
            .build();
        registry.register(descriptor);
        // Empty lists should not cause NPE in find()
        var result = registry.find(AgentQuery.byCapability("empty-types", "default"));
        assertThat(result).extracting(m -> m.descriptor().agentId()).contains("m-8");
        assertThat(result).allSatisfy(m -> assertThat(m.resolvedCapability()).isNotNull());
    }

    // --- Subsumption tests ---

    @Test
    void find_by_capability_matches_via_subsumption() {
        // Register an agent with a general "review" capability grounded in TestCapabilityVocab
        var generalCap = AgentCapability.builder()
            .name("review")
            .capabilityVocabulary("urn:test:capabilities")
            .qualityHint(0.9)
            .epistemicDomains(Map.of())
            .build();
        var desc = AgentDescriptor.builder()
            .agentId("m-sub-1")
            .name("Agent")
            .version("1.0")
            .provider("anthropic")
            .modelFamily("claude")
            .modelVersion("claude-3-7")
            .slot("reviewer")
            .capabilities(List.of(generalCap))
            .disposition(AgentDisposition.builder()
                .socialOrient("collaborative")
                .ruleFollowing("principled")
                .riskAppetite("measured")
                .autonomy("semi-autonomous")
                .build())
            .tenancyId("default")
            .build();
        registry.register(desc);

        // Query for "security-review" — which is a specialization of "review"
        var result = registry.find(AgentQuery.byCapability("security-review", "default"));

        // The agent should be found via subsumption
        assertThat(result).extracting(m -> m.descriptor().agentId()).contains("m-sub-1");
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().resolvedCapability()).isNotNull();
        assertThat(result.getFirst().resolvedCapability().degree())
            .isInstanceOf(MatchDegree.Specialization.class);
        assertThat(((MatchDegree.Specialization) result.getFirst().resolvedCapability().degree()).depth())
            .isEqualTo(2);
    }

    @Test
    void find_ungrounded_capability_uses_exact_match_only() {
        // Register an agent with capability "code-review" (no vocabulary)
        registry.register(descriptor("m-exact-1", "reviewer", "default", "code-review"));

        // Query for "security-code-review"
        var result = registry.find(AgentQuery.byCapability("security-code-review", "default"));

        // Should NOT be found (no subsumption without vocabulary grounding)
        assertThat(result).extracting(m -> m.descriptor().agentId()).doesNotContain("m-exact-1");
    }

    @Test
    void register_rejects_unknown_capability_vocabulary() {
        var cap = AgentCapability.builder()
            .name("review")
            .capabilityVocabulary("urn:nonexistent:vocab")
            .qualityHint(0.9)
            .epistemicDomains(Map.of())
            .build();
        var desc = AgentDescriptor.builder()
            .agentId("m-invalid-vocab")
            .name("Agent")
            .version("1.0")
            .provider("anthropic")
            .modelFamily("claude")
            .modelVersion("claude-3-7")
            .slot("reviewer")
            .capabilities(List.of(cap))
            .disposition(AgentDisposition.builder()
                .socialOrient("collaborative")
                .ruleFollowing("principled")
                .riskAppetite("measured")
                .autonomy("semi-autonomous")
                .build())
            .tenancyId("default")
            .build();

        assertThatThrownBy(() -> registry.register(desc))
            .isInstanceOf(AgentValidationException.class)
            .hasMessageContaining("capabilityVocabulary")
            .hasMessageContaining("urn:nonexistent:vocab");
    }

    @Test
    void register_rejects_unknown_term_in_known_vocabulary() {
        var cap = AgentCapability.builder()
            .name("nonexistent-capability")
            .capabilityVocabulary("urn:test:capabilities")
            .qualityHint(0.9)
            .epistemicDomains(Map.of())
            .build();
        var desc = AgentDescriptor.builder()
            .agentId("m-invalid-term")
            .name("Agent")
            .version("1.0")
            .provider("anthropic")
            .modelFamily("claude")
            .modelVersion("claude-3-7")
            .slot("reviewer")
            .capabilities(List.of(cap))
            .disposition(AgentDisposition.builder()
                .socialOrient("collaborative")
                .ruleFollowing("principled")
                .riskAppetite("measured")
                .autonomy("semi-autonomous")
                .build())
            .tenancyId("default")
            .build();

        assertThatThrownBy(() -> registry.register(desc))
            .isInstanceOf(AgentValidationException.class)
            .hasMessageContaining("capability.name")
            .hasMessageContaining("nonexistent-capability");
    }


    @Test
    void find_by_goal_returns_matching_agents() {
        var desc1 = AgentDescriptor.builder()
                                   .agentId("g1").name("G1").slot("s").tenancyId("t")
                                   .goals(List.of(new AgentGoal("quality-review", "Ensure quality", GoalPriority.PRIMARY, Visibility.PUBLIC, List.of())))
                                   .build();
        var desc2 = AgentDescriptor.builder()
                                   .agentId("g2").name("G2").slot("s").tenancyId("t")
                                   .build();
        registry.register(desc1);
        registry.register(desc2);

        var results = registry.find(AgentQuery.byGoal("quality-review", "t"));
        assertThat(results).hasSize(1);
        assertThat(results.get(0).descriptor().agentId()).isEqualTo("g1");
        assertThat(results.get(0).resolvedCapability()).isNull();
    }

    @Test
    void find_by_goal_returns_empty_when_no_match() {
        var desc = AgentDescriptor.builder()
                                  .agentId("g3").name("G3").slot("s").tenancyId("t")
                                  .goals(List.of(new AgentGoal("quality", "Q", GoalPriority.PRIMARY, Visibility.PUBLIC, List.of())))
                                  .build();
        registry.register(desc);

        var results = registry.find(AgentQuery.byGoal("nonexistent", "t"));
        assertThat(results).isEmpty();
    }
}
