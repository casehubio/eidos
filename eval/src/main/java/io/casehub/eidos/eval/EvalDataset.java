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
        final var descriptor = AgentDescriptor.builder()
            .agentId("planner-1")
            .name("Devtown Planner")
            .version("1.0")
            .provider("anthropic")
            .modelFamily("claude")
            .modelVersion("claude-3-5-sonnet")
            .domainVocabulary("https://vocab.casehub.io/devtown")
            .slotVocabulary("https://vocab.casehub.io/devtown")
            .slot("planner")
            .capabilities(List.of(
                new AgentCapability("sprint-planning", 0.9, 200L, "medium",
                    List.of("backlog", "team-capacity"), List.of("sprint-plan"), List.of(),
                    Map.of("agile", 0.9, "kanban", 0.7)),
                new AgentCapability("estimation", 0.8, 100L, "low",
                    List.of("user-story"), List.of("story-points"), List.of(),
                    Map.of("agile", 0.85))
            ))
            .disposition(AgentDisposition.builder()
                .socialOrient("collaborative")
                .ruleFollowing("adaptive")
                .riskAppetite("moderate")
                .autonomy("assisted")
                .delegation(true)
                .build())
            .tenancyId("devtown-1")
            .build();
        final var context = AgentPromptContext.forFormat(RenderFormat.MARKDOWN)
            .withGoal(new GoalContext("Plan sprint 42",
                List.of("Prioritise backlog", "Assign capacity"), "case-sprint-42"));
        return new SyntheticEvalCase("devtown-planner", descriptor, context);
    }

    private static SyntheticEvalCase crossVocab() {
        final var descriptor = AgentDescriptor.builder()
            .agentId("reviewer-1")
            .name("Code Reviewer")
            .version("2.0")
            .provider("anthropic")
            .modelFamily("claude")
            .modelVersion("claude-3-7-sonnet")
            .domainVocabulary("https://vocab.casehub.io/svo")
            .slotVocabulary("https://vocab.casehub.io/devtown")
            .slot("reviewer")
            .capabilities(List.of(new AgentCapability("code-review", 0.95, 150L, "low",
                List.of("pull-request"), List.of("review-comment"), List.of(),
                Map.of("java", 0.95, "rust", 0.4, "python", 0.7))))
            .disposition(AgentDisposition.builder()
                .socialOrient("independent")
                .ruleFollowing("strict")
                .riskAppetite("conservative")
                .autonomy("directed")
                .build())
            .jurisdiction("EU")
            .dataHandlingPolicy("gdpr-compliant")
            .tenancyId("devtown-1")
            .build();
        return new SyntheticEvalCase("cross-vocab", descriptor, AgentPromptContext.forFormat(RenderFormat.MARKDOWN));
    }

    private static SyntheticEvalCase epistemicWeak() {
        final var descriptor = AgentDescriptor.builder()
            .agentId("ml-agent-1")
            .name("ML Researcher")
            .version("1.0")
            .provider("openai")
            .modelFamily("gpt")
            .modelVersion("gpt-4o")
            .slot("researcher")
            .capabilities(List.of(new AgentCapability("literature-review", 0.6, 500L, "high",
                List.of("papers"), List.of("summary"), List.of(),
                Map.of("reinforcement-learning", 0.25, "supervised-learning", 0.8))))
            .tenancyId("research-1")
            .build();
        final var context = AgentPromptContext.forFormat(RenderFormat.MARKDOWN)
            .withSituationalContext("Reviewing recent RL papers for quarterly report");
        return new SyntheticEvalCase("epistemic-weak", descriptor, context);
    }

    private static SyntheticEvalCase minimal() {
        final var descriptor = AgentDescriptor.builder()
            .agentId("min-1")
            .name("Minimal Agent")
            .slot("worker")
            .capabilities(List.of())
            .tenancyId("tenant-1")
            .build();
        return new SyntheticEvalCase("minimal", descriptor, AgentPromptContext.forFormat(RenderFormat.MARKDOWN));
    }

    private static SyntheticEvalCase maximal() {
        final var descriptor = AgentDescriptor.builder()
            .agentId("max-agent-001")
            .name("Maximal Agent")
            .version("3.1.4")
            .provider("anthropic")
            .modelFamily("claude")
            .modelVersion("claude-opus-4-7")
            .weightsFingerprint("fp-abc123def456")
            .domainVocabulary("https://vocab.casehub.io/svo")
            .slotVocabulary("https://vocab.casehub.io/casehub-slot")
            .dispositionVocabulary("https://vocab.casehub.io/conscientiousness")
            .slot("orchestrator")
            .capabilities(List.of(
                new AgentCapability("planning", 0.95, 200L, "medium",
                    List.of("goal", "constraints"), List.of("plan", "timeline"), List.of("urgent"),
                    Map.of("strategy", 0.9, "operations", 0.8)),
                new AgentCapability("delegation", 0.85, 50L, "low",
                    List.of("task-spec"), List.of("assignment"), List.of(),
                    Map.of("team-management", 0.75))
            ))
            .disposition(AgentDisposition.builder()
                .socialOrient("collaborative")
                .ruleFollowing("adaptive")
                .riskAppetite("moderate")
                .autonomy("autonomous")
                .delegation(true)
                .build())
            .jurisdiction("US")
            .dataHandlingPolicy("hipaa-compliant")
            .tenancyId("enterprise-1")
            .build();
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
        final var descriptor = AgentDescriptor.builder()
            .agentId("planner-1")
            .name("Devtown Planner")
            .version("1.0")
            .provider("anthropic")
            .modelFamily("claude")
            .modelVersion("claude-3-5-sonnet")
            .domainVocabulary("https://vocab.casehub.io/devtown")
            .slotVocabulary("https://vocab.casehub.io/devtown")
            .slot("planner")
            .capabilities(List.of(
                new AgentCapability("sprint-planning", 0.9, 200L, "medium",
                    List.of("backlog", "team-capacity"), List.of("sprint-plan"), List.of(),
                    Map.of("agile", 0.9, "kanban", 0.7)),
                new AgentCapability("estimation", 0.8, 100L, "low",
                    List.of("user-story"), List.of("story-points"), List.of(),
                    Map.of("agile", 0.85))
            ))
            .disposition(AgentDisposition.builder()
                .socialOrient("collaborative")
                .ruleFollowing("adaptive")
                .riskAppetite("moderate")
                .autonomy("assisted")
                .delegation(true)
                .build())
            .tenancyId("devtown-1")
            .build();
        final var context = AgentPromptContext.forFormat(RenderFormat.PROSE)
            .withGoal(new GoalContext("Plan sprint 42",
                List.of("Prioritise backlog", "Assign capacity"), "case-sprint-42"));
        return new SyntheticEvalCase("devtown-planner-prose", descriptor, context);
    }

    private static SyntheticEvalCase maximalProse() {
        final var descriptor = AgentDescriptor.builder()
            .agentId("max-agent-001")
            .name("Maximal Agent")
            .version("3.1.4")
            .provider("anthropic")
            .modelFamily("claude")
            .modelVersion("claude-opus-4-7")
            .weightsFingerprint("fp-abc123def456")
            .domainVocabulary("https://vocab.casehub.io/svo")
            .slotVocabulary("https://vocab.casehub.io/casehub-slot")
            .dispositionVocabulary("https://vocab.casehub.io/conscientiousness")
            .slot("orchestrator")
            .capabilities(List.of(
                new AgentCapability("planning", 0.95, 200L, "medium",
                    List.of("goal", "constraints"), List.of("plan", "timeline"), List.of("urgent"),
                    Map.of("strategy", 0.9, "operations", 0.8)),
                new AgentCapability("delegation", 0.85, 50L, "low",
                    List.of("task-spec"), List.of("assignment"), List.of(),
                    Map.of("team-management", 0.75))
            ))
            .disposition(AgentDisposition.builder()
                .socialOrient("collaborative")
                .ruleFollowing("adaptive")
                .riskAppetite("moderate")
                .autonomy("autonomous")
                .delegation(true)
                .build())
            .jurisdiction("US")
            .dataHandlingPolicy("hipaa-compliant")
            .tenancyId("enterprise-1")
            .build();
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
        final var descriptor = AgentDescriptor.builder()
            .agentId("planner-1")
            .name("Devtown Planner")
            .version("1.0")
            .provider("anthropic")
            .modelFamily("claude")
            .modelVersion("claude-3-5-sonnet")
            .domainVocabulary("https://vocab.casehub.io/devtown")
            .slotVocabulary("https://vocab.casehub.io/devtown")
            .slot("planner")
            .capabilities(List.of(
                new AgentCapability("sprint-planning", 0.9, 200L, "medium",
                    List.of("backlog", "team-capacity"), List.of("sprint-plan"), List.of(),
                    Map.of("agile", 0.9, "kanban", 0.7)),
                new AgentCapability("estimation", 0.8, 100L, "low",
                    List.of("user-story"), List.of("story-points"), List.of(),
                    Map.of("agile", 0.85))
            ))
            .disposition(AgentDisposition.builder()
                .socialOrient("collaborative")
                .ruleFollowing("adaptive")
                .riskAppetite("moderate")
                .autonomy("assisted")
                .delegation(true)
                .build())
            .tenancyId("devtown-1")
            .build();
        return new SyntheticEvalCase("devtown-planner-a2a", descriptor,
            AgentPromptContext.forFormat(RenderFormat.A2A_CARD));
    }

    private static SyntheticEvalCase minimalA2a() {
        final var descriptor = AgentDescriptor.builder()
            .agentId("min-1")
            .name("Minimal Agent")
            .slot("worker")
            .capabilities(List.of())
            .tenancyId("tenant-1")
            .build();
        return new SyntheticEvalCase("minimal-a2a", descriptor,
            AgentPromptContext.forFormat(RenderFormat.A2A_CARD));
    }
}
