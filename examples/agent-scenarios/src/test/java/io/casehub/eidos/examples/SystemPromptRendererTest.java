package io.casehub.eidos.examples;

import io.casehub.eidos.api.*;
import io.casehub.eidos.api.SystemPromptRenderer.RenderFormat;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@QuarkusTest
class SystemPromptRendererTest {

    @Inject AgentRegistry registry;
    @Inject SystemPromptRenderer renderer;

    @BeforeEach
    void registerAgent() {
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
                List.of("requirements"), List.of("plan"), List.of(),
                Map.of("software", 0.95, "logistics", 0.4))))
            .disposition(AgentDisposition.builder()
                .socialOrient("facilitative")
                .ruleFollowing("principled")
                .riskAppetite("measured")
                .autonomy("semi-autonomous")
                .delegation(true)
                .build())
            .jurisdiction("EU")
            .dataHandlingPolicy("gdpr-compliant")
            .tenancyId("default")
            .build());
    }

    @Test
    void renders_agent_without_goal() {
        final var descriptor = registry.findById("planner-1", "default").orElseThrow();
        final var context = AgentPromptContext.forFormat(RenderFormat.MARKDOWN);

        final var result = renderer.render(descriptor, context);

        assertThat(result.content()).contains("Strategic Planner");
        assertThat(result.content()).contains("planning");
        assertThat(result.format()).isEqualTo(RenderFormat.MARKDOWN);
        assertThat(result.descriptorHash()).isNotBlank();
        assertThat(result.contextHash()).isNotBlank();
    }

    @Test
    void renders_agent_with_goal_and_resources() {
        final var descriptor = registry.findById("planner-1", "default").orElseThrow();
        final var context = AgentPromptContext.forFormat(RenderFormat.MARKDOWN)
                .withGoal(new GoalContext("Plan the Q3 release", List.of("Define milestones", "Assign owners"), "case-q3"))
                .withResources(List.of(new Resource("https://jira.example.com/q3", "Q3 board", "web")))
                .withSituationalContext("EOQ sprint planning session");

        final var result = renderer.render(descriptor, context);

        assertThat(result.content()).contains("Strategic Planner");
        assertThat(result.content()).contains("Plan the Q3 release");
        assertThat(result.content()).contains("jira.example.com");
        assertThat(result.content()).contains("EOQ sprint planning session");
    }

    @Test
    void hash_changes_when_context_changes() {
        final var descriptor = registry.findById("planner-1", "default").orElseThrow();
        final var ctx1 = AgentPromptContext.forFormat(RenderFormat.MARKDOWN);
        final var ctx2 = AgentPromptContext.forFormat(RenderFormat.MARKDOWN)
                .withSituationalContext("different context");

        final var r1 = renderer.render(descriptor, ctx1);
        final var r2 = renderer.render(descriptor, ctx2);

        assertThat(r1.descriptorHash()).isEqualTo(r2.descriptorHash());
        assertThat(r1.contextHash()).isNotEqualTo(r2.contextHash());
    }
}
