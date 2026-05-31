package io.casehub.eidos.api;

import org.junit.jupiter.api.Test;

import java.util.List;

import static io.casehub.eidos.api.SystemPromptRenderer.RenderFormat.MARKDOWN;
import static org.assertj.core.api.Assertions.*;

class AgentPromptContextTest {

    @Test
    void forFormat_creates_empty_context() {
        final var ctx = AgentPromptContext.forFormat(MARKDOWN);
        assertThat(ctx.format()).isEqualTo(MARKDOWN);
        assertThat(ctx.goal()).isEmpty();
        assertThat(ctx.resources()).isEmpty();
        assertThat(ctx.situationalContext()).isNull();
    }

    @Test
    void withGoal_adds_goal_and_preserves_other_fields() {
        final var goal = GoalContext.of("review PR");
        final var ctx = AgentPromptContext.forFormat(MARKDOWN)
                .withResources(List.of(new Resource("/src", "Source", "filesystem")))
                .withSituationalContext("critical release")
                .withGoal(goal);

        assertThat(ctx.goal()).contains(goal);
        assertThat(ctx.resources()).hasSize(1);
        assertThat(ctx.situationalContext()).isEqualTo("critical release");
        assertThat(ctx.format()).isEqualTo(MARKDOWN);
    }

    @Test
    void withResources_adds_resources_and_preserves_other_fields() {
        final var resources = List.of(new Resource("/src", "Source", "filesystem"));
        final var ctx = AgentPromptContext.forFormat(MARKDOWN)
                .withGoal(GoalContext.of("plan"))
                .withResources(resources);

        assertThat(ctx.resources()).isEqualTo(resources);
        assertThat(ctx.goal()).isPresent();
    }

    @Test
    void withSituationalContext_adds_context_and_preserves_other_fields() {
        final var ctx = AgentPromptContext.forFormat(MARKDOWN)
                .withGoal(GoalContext.of("plan"))
                .withSituationalContext("production deploy");

        assertThat(ctx.situationalContext()).isEqualTo("production deploy");
        assertThat(ctx.goal()).isPresent();
    }

    @Test
    void builder_chain_sets_all_fields() {
        final var goal = GoalContext.of("review");
        final var resource = new Resource("/src", "Source", "filesystem");
        final var ctx = AgentPromptContext.forFormat(MARKDOWN)
                .withGoal(goal)
                .withResources(List.of(resource))
                .withSituationalContext("context");

        assertThat(ctx.goal()).contains(goal);
        assertThat(ctx.resources()).containsExactly(resource);
        assertThat(ctx.situationalContext()).isEqualTo("context");
        assertThat(ctx.format()).isEqualTo(MARKDOWN);
    }
}
