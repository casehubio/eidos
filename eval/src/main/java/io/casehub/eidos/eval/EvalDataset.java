package io.casehub.eidos.eval;

import io.casehub.eidos.api.*;
import io.casehub.eidos.api.SystemPromptRenderer.RenderFormat;

import java.util.List;
import java.util.Map;

public class EvalDataset {

    public static List<EvalCase> all() {
        return List.of(
            // MARKDOWN (5 existing)
            devtownPlanner(), crossVocab(), epistemicWeak(), minimal(), maximal(),
            // PROSE (2 new)
            devtownPlannerProse(), maximalProse(),
            // A2A_CARD (2 new)
            devtownPlannerA2a(), minimalA2a()
        );
    }

    private static SyntheticEvalCase devtownPlanner() {
        final var descriptor = new AgentDescriptor(
            "planner-1", "Devtown Planner", "1.0", "anthropic",
            "claude", "claude-3-5-sonnet", null,
            "https://vocab.casehub.io/devtown",
            "https://vocab.casehub.io/devtown", null,
            "planner",
            List.of(
                new AgentCapability("sprint-planning", 0.9, 200L, "medium",
                    List.of("backlog", "team-capacity"), List.of("sprint-plan"), List.of(),
                    Map.of("agile", 0.9, "kanban", 0.7)),
                new AgentCapability("estimation", 0.8, 100L, "low",
                    List.of("user-story"), List.of("story-points"), List.of(),
                    Map.of("agile", 0.85))
            ),
            new AgentDisposition("collaborative", "adaptive", "moderate", "assisted", true),
            null, null, "devtown-1"
        );
        final var context = AgentPromptContext.forFormat(RenderFormat.MARKDOWN)
            .withGoal(new GoalContext("Plan sprint 42",
                List.of("Prioritise backlog", "Assign capacity"), "case-sprint-42"));
        return new SyntheticEvalCase("devtown-planner", descriptor, context);
    }

    private static SyntheticEvalCase crossVocab() {
        final var descriptor = new AgentDescriptor(
            "reviewer-1", "Code Reviewer", "2.0", "anthropic",
            "claude", "claude-3-7-sonnet", null,
            "https://vocab.casehub.io/svo",
            "https://vocab.casehub.io/devtown", null,
            "reviewer",
            List.of(new AgentCapability("code-review", 0.95, 150L, "low",
                List.of("pull-request"), List.of("review-comment"), List.of(),
                Map.of("java", 0.95, "rust", 0.4, "python", 0.7))),
            new AgentDisposition("independent", "strict", "conservative", "directed", false),
            "EU", "gdpr-compliant", "devtown-1"
        );
        return new SyntheticEvalCase("cross-vocab", descriptor, AgentPromptContext.forFormat(RenderFormat.MARKDOWN));
    }

    private static SyntheticEvalCase epistemicWeak() {
        final var descriptor = new AgentDescriptor(
            "ml-agent-1", "ML Researcher", "1.0", "openai",
            "gpt", "gpt-4o", null, null, null, null,
            "researcher",
            List.of(new AgentCapability("literature-review", 0.6, 500L, "high",
                List.of("papers"), List.of("summary"), List.of(),
                Map.of("reinforcement-learning", 0.25, "supervised-learning", 0.8))),
            null, null, null, "research-1"
        );
        final var context = AgentPromptContext.forFormat(RenderFormat.MARKDOWN)
            .withSituationalContext("Reviewing recent RL papers for quarterly report");
        return new SyntheticEvalCase("epistemic-weak", descriptor, context);
    }

    private static SyntheticEvalCase minimal() {
        final var descriptor = new AgentDescriptor(
            "min-1", "Minimal Agent", null, null, null, null, null,
            null, null, null, "worker", List.of(), null, null, null, "tenant-1"
        );
        return new SyntheticEvalCase("minimal", descriptor, AgentPromptContext.forFormat(RenderFormat.MARKDOWN));
    }

    private static SyntheticEvalCase maximal() {
        final var descriptor = new AgentDescriptor(
            "max-agent-001", "Maximal Agent", "3.1.4", "anthropic",
            "claude", "claude-opus-4-7", "fp-abc123def456",
            "https://vocab.casehub.io/svo",
            "https://vocab.casehub.io/casehub-slot",
            "https://vocab.casehub.io/conscientiousness",
            "orchestrator",
            List.of(
                new AgentCapability("planning", 0.95, 200L, "medium",
                    List.of("goal", "constraints"), List.of("plan", "timeline"), List.of("urgent"),
                    Map.of("strategy", 0.9, "operations", 0.8)),
                new AgentCapability("delegation", 0.85, 50L, "low",
                    List.of("task-spec"), List.of("assignment"), List.of(),
                    Map.of("team-management", 0.75))
            ),
            new AgentDisposition("collaborative", "adaptive", "moderate", "autonomous", true),
            "US", "hipaa-compliant", "enterprise-1"
        );
        final var context = AgentPromptContext.forFormat(RenderFormat.MARKDOWN)
            .withGoal(new GoalContext("Coordinate quarterly planning cycle",
                List.of("Gather input from all teams", "Synthesise priorities", "Produce roadmap"),
                "case-q3-planning"))
            .withResources(List.of(
                new Resource("https://internal.company.io/roadmap", "Current Roadmap", "document"),
                new Resource("https://internal.company.io/okrs", "OKRs", "spreadsheet")))
            .withSituationalContext("End of Q2 — all teams must submit priorities by Friday.");
        return new SyntheticEvalCase("maximal", descriptor, context);
    }

    // ── PROSE cases (new) ──────────────────────────────────────────────────────

    private static SyntheticEvalCase devtownPlannerProse() {
        final var descriptor = new AgentDescriptor(
            "planner-1", "Devtown Planner", "1.0", "anthropic",
            "claude", "claude-3-5-sonnet", null,
            "https://vocab.casehub.io/devtown",
            "https://vocab.casehub.io/devtown", null,
            "planner",
            List.of(
                new AgentCapability("sprint-planning", 0.9, 200L, "medium",
                    List.of("backlog", "team-capacity"), List.of("sprint-plan"), List.of(),
                    Map.of("agile", 0.9, "kanban", 0.7)),
                new AgentCapability("estimation", 0.8, 100L, "low",
                    List.of("user-story"), List.of("story-points"), List.of(),
                    Map.of("agile", 0.85))
            ),
            new AgentDisposition("collaborative", "adaptive", "moderate", "assisted", true),
            null, null, "devtown-1"
        );
        final var context = AgentPromptContext.forFormat(RenderFormat.PROSE)
            .withGoal(new GoalContext("Plan sprint 42",
                List.of("Prioritise backlog", "Assign capacity"), "case-sprint-42"));
        return new SyntheticEvalCase("devtown-planner-prose", descriptor, context);
    }

    private static SyntheticEvalCase maximalProse() {
        final var descriptor = new AgentDescriptor(
            "max-agent-001", "Maximal Agent", "3.1.4", "anthropic",
            "claude", "claude-opus-4-7", "fp-abc123def456",
            "https://vocab.casehub.io/svo",
            "https://vocab.casehub.io/casehub-slot",
            "https://vocab.casehub.io/conscientiousness",
            "orchestrator",
            List.of(
                new AgentCapability("planning", 0.95, 200L, "medium",
                    List.of("goal", "constraints"), List.of("plan", "timeline"), List.of("urgent"),
                    Map.of("strategy", 0.9, "operations", 0.8)),
                new AgentCapability("delegation", 0.85, 50L, "low",
                    List.of("task-spec"), List.of("assignment"), List.of(),
                    Map.of("team-management", 0.75))
            ),
            new AgentDisposition("collaborative", "adaptive", "moderate", "autonomous", true),
            "US", "hipaa-compliant", "enterprise-1"
        );
        final var context = AgentPromptContext.forFormat(RenderFormat.PROSE)
            .withGoal(new GoalContext("Coordinate quarterly planning cycle",
                List.of("Gather input from all teams", "Synthesise priorities", "Produce roadmap"),
                "case-q3-planning"))
            .withResources(List.of(
                new Resource("https://internal.company.io/roadmap", "Current Roadmap", "document"),
                new Resource("https://internal.company.io/okrs", "OKRs", "spreadsheet")))
            .withSituationalContext("End of Q2 — all teams must submit priorities by Friday.");
        return new SyntheticEvalCase("maximal-prose", descriptor, context);
    }

    // ── A2A_CARD cases (new) ───────────────────────────────────────────────────

    private static SyntheticEvalCase devtownPlannerA2a() {
        final var descriptor = new AgentDescriptor(
            "planner-1", "Devtown Planner", "1.0", "anthropic",
            "claude", "claude-3-5-sonnet", null,
            "https://vocab.casehub.io/devtown",
            "https://vocab.casehub.io/devtown", null,
            "planner",
            List.of(
                new AgentCapability("sprint-planning", 0.9, 200L, "medium",
                    List.of("backlog", "team-capacity"), List.of("sprint-plan"), List.of(),
                    Map.of("agile", 0.9, "kanban", 0.7)),
                new AgentCapability("estimation", 0.8, 100L, "low",
                    List.of("user-story"), List.of("story-points"), List.of(),
                    Map.of("agile", 0.85))
            ),
            new AgentDisposition("collaborative", "adaptive", "moderate", "assisted", true),
            null, null, "devtown-1"
        );
        return new SyntheticEvalCase("devtown-planner-a2a", descriptor,
            AgentPromptContext.forFormat(RenderFormat.A2A_CARD));
    }

    private static SyntheticEvalCase minimalA2a() {
        final var descriptor = new AgentDescriptor(
            "min-1", "Minimal Agent", null, null, null, null, null,
            null, null, null, "worker", List.of(), null, null, null, "tenant-1"
        );
        return new SyntheticEvalCase("minimal-a2a", descriptor,
            AgentPromptContext.forFormat(RenderFormat.A2A_CARD));
    }
}
