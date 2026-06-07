package io.casehub.eidos.examples;

import io.casehub.eidos.api.*;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@QuarkusTest
class MultiAgentTeamTest {

    @Inject AgentRegistry registry;

    @BeforeEach
    void registerTeam() {
        registry.register(AgentDescriptor.builder()
            .agentId("planner-1")
            .name("Strategic Planner")
            .version("1.0")
            .provider("anthropic")
            .modelFamily("claude")
            .modelVersion("claude-3-7-sonnet")
            .slotVocabulary("urn:casehub:vocab:casehub-slot")
            .slot("planner")
            .capabilities(List.of(new AgentCapability("planning", 0.9, 200L, "medium",
                List.of("requirements"), List.of("plan"), List.of("orchestration"),
                Map.of("software", 0.95, "logistics", 0.4))))
            .disposition(AgentDisposition.builder()
                .socialOrient("facilitative")
                .ruleFollowing("principled")
                .riskAppetite("measured")
                .autonomy("semi-autonomous")
                .delegation(true)
                .build())
            .tenancyId("default")
            .build());

        registry.register(AgentDescriptor.builder()
            .agentId("reviewer-1")
            .name("Code Reviewer")
            .version("1.0")
            .provider("anthropic")
            .modelFamily("claude")
            .modelVersion("claude-3-7-sonnet")
            .slotVocabulary("urn:casehub:vocab:casehub-slot")
            .slot("reviewer")
            .capabilities(List.of(
                new AgentCapability("code-review", 0.95, 150L, "low",
                    List.of("code"), List.of("review"), List.of("quality"),
                    Map.of("java", 0.95, "python", 0.8, "rust", 0.3)),
                new AgentCapability("test-writing", 0.8, 300L, "medium",
                    List.of("code"), List.of("tests"), List.of("testing"),
                    Map.of("java", 0.9))))
            .disposition(AgentDisposition.builder()
                .socialOrient("independent")
                .ruleFollowing("strict")
                .riskAppetite("conservative")
                .autonomy("directed")
                .build())
            .tenancyId("default")
            .build());

        registry.register(AgentDescriptor.builder()
            .agentId("executor-1")
            .name("Task Executor")
            .version("1.0")
            .provider("anthropic")
            .modelFamily("claude")
            .modelVersion("claude-3-7-sonnet")
            .slotVocabulary("urn:casehub:vocab:casehub-slot")
            .slot("executor")
            .capabilities(List.of(new AgentCapability("code-generation", 0.85, 500L, "high",
                List.of("spec"), List.of("code"), List.of("implementation"),
                Map.of("java", 0.9, "python", 0.85, "rust", 0.6))))
            .disposition(AgentDisposition.builder()
                .socialOrient("collaborative")
                .ruleFollowing("flexible")
                .riskAppetite("bold")
                .autonomy("autonomous")
                .build())
            .tenancyId("default")
            .build());
    }

    @Test
    void find_by_id_returns_complete_descriptor() {
        var planner = registry.findById("planner-1", "default");
        assertThat(planner).isPresent();
        assertThat(planner.get().name()).isEqualTo("Strategic Planner");
        assertThat(planner.get().slot()).isEqualTo("planner");
        assertThat(planner.get().disposition().delegation()).isTrue();
        assertThat(planner.get().capabilities()).hasSize(1);
        assertThat(planner.get().capabilities().get(0).epistemicDomains())
            .containsEntry("software", 0.95);
    }

    @Test
    void find_reviewers_by_slot() {
        var reviewers = registry.find(AgentQuery.bySlot("reviewer", "default"));
        assertThat(reviewers).hasSize(1);
        assertThat(reviewers.get(0).agentId()).isEqualTo("reviewer-1");
    }

    @Test
    void find_agents_with_code_review_capability() {
        var agents = registry.find(AgentQuery.byCapability("code-review", "default"));
        assertThat(agents).hasSize(1);
        assertThat(agents.get(0).agentId()).isEqualTo("reviewer-1");
    }

    @Test
    void find_executor_by_slot_and_capability() {
        var executors = registry.find(
            AgentQuery.bySlotAndCapability("executor", "code-generation", "default"));
        assertThat(executors).hasSize(1);
        assertThat(executors.get(0).agentId()).isEqualTo("executor-1");
    }

    @Test
    void find_all_returns_entire_team() {
        var all = registry.find(AgentQuery.all("default"));
        assertThat(all).hasSizeGreaterThanOrEqualTo(3);
        assertThat(all.stream().map(AgentDescriptor::agentId).toList())
            .contains("planner-1", "reviewer-1", "executor-1");
    }

    @Test
    void agent_with_multiple_capabilities_found_by_either() {
        var codeReviewers = registry.find(AgentQuery.byCapability("code-review", "default"));
        var testWriters = registry.find(AgentQuery.byCapability("test-writing", "default"));
        assertThat(codeReviewers.stream().map(AgentDescriptor::agentId).toList())
            .contains("reviewer-1");
        assertThat(testWriters.stream().map(AgentDescriptor::agentId).toList())
            .contains("reviewer-1");
    }
}
