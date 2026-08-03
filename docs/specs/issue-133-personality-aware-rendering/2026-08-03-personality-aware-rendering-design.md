# Personality-Aware Rendering — Design Spec

> **Issue:** eidos#133 (epic)
> **Covers:** #128, #130, #120
> **Date:** 2026-08-03
> **Status:** Approved

## Summary

Three improvements to how personality reaches agent behavior through the
rendering pipeline and descriptor API. Closes the calibration gap identified
in the structured-personality-composition paper (Section 9, items 1 and 5)
and adds goal priority evolution from engine#800 Sub-epic C.

**Research references:**
- structured-personality-composition-in-llm-agents.md §9 items 1, 5
- JPAF paper (arXiv:2601.10025)

---

## §1 Briefing-Framework Coherence Validation (#128)

Pre-flight conflict detection between briefing text and disposition profile.
Two-tier validation: structural checks always run (vocabulary-grounded term
scanning), LLM-based semantic analysis runs additionally when ChatModel is
available.

### §1.1 API Types (casehub-eidos-api, Tier 1)

```java
public enum CoherenceLevel { ALIGNED, TENSION, CONFLICT }
```

Severity ordering: ALIGNED < TENSION < CONFLICT.

```java
public record CoherenceViolation(
    CoherenceLevel level,
    String description,
    String briefingExcerpt,
    DispositionAxis axis,
    String declaredValue,
    String impliedValue
) {}
```

`axis` is nullable — not all violations map to a disposition axis (e.g.,
orientation contradictions map to cognitive profile attitude, not an axis).

```java
public record CoherenceReport(
    CoherenceLevel overall,
    List<CoherenceViolation> violations
) {
    public static final CoherenceReport ALIGNED =
        new CoherenceReport(CoherenceLevel.ALIGNED, List.of());
}
```

`overall` is the worst level across all violations. Empty violations list
with ALIGNED level is the clean state.

### §1.2 BriefingCoherenceValidator (casehub-eidos runtime)

`@ApplicationScoped` CDI bean. Injects `VocabularyRegistry` and
`Instance<ChatModel>`.

**Public API:**

```java
CoherenceReport validate(AgentDescriptor descriptor);
CoherenceReport validateStructural(AgentDescriptor descriptor);
```

`validate()` runs both tiers based on ChatModel availability.
`validateStructural()` runs tier 1 only (for hot-path callers).

**Tier 1 — Structural (always runs, no LLM):**

1. If descriptor has no briefing or no disposition, return ALIGNED.
2. Scan briefing text for word-boundary matches against all registered
   `VocabularyTerm` values across all vocabularies.
3. For each match, resolve its axis mapping via `axisExactMatch()`.
4. Compare against the descriptor's actual axis values:
   - If the briefing term maps to an axis value that directly opposes
     the descriptor's primary (highest-weight) value on that axis →
     CONFLICT.
   - If it maps to a non-primary value that is present but
     low-weighted → TENSION.
5. Orientation check: scan for keywords ("outgoing", "gregarious",
   "thinks out loud" → extraverted; "reserved", "introspective",
   "works alone" → introverted). Compare against the cognitive
   profile's dominant function attitude (if Jungian vocab). Contradiction
   → TENSION.

**Tier 2 — Semantic (when ChatModel available):**

1. Send briefing text to ChatModel with structured prompt: extract
   behavioral signals and map to the five disposition axes.
2. Response parsed via JSON schema (reuses existing RESPONSE_FORMAT
   pattern from eval).
3. Compare extracted signals against the full disposition profile.
4. Report contradictions as TENSION or CONFLICT based on severity.
5. On ChatModel failure (timeout, error), fall back to structural-only
   results. Semantic validation failure is never propagated — the
   structural tier always provides a baseline.

### §1.3 Integration Points

**DescriptorCollector:** Receives `BriefingCoherenceValidator` as parameter
in `collectAndValidate()`. Runs structural validation at registration.
TENSION → log warning. CONFLICT → log warning. Not a registration gate —
advisory only.

**EidosSystemPromptRenderer:** Attaches `CoherenceReport` to
`RenderedPrompt` as metadata via new optional field
`CoherenceReport coherenceReport()`. Consumers inspect coherence without
re-running validation. Runs structural validation only (render path is
latency-sensitive).

**Eval harness:** Runs full validation (structural + semantic). Reports
coherence violations as eval output alongside judge scores.

### §1.4 Boundaries

- Does NOT block descriptor registration — advisory, not a gate.
- Does NOT modify briefing or disposition — reports only.
- Does NOT validate briefing quality (eval protocol domain).

---

## §2 Function-Specific Prompt Constraints (#130)

Dominant cognitive function shapes response format expectations. Function-
specific behavioral constraints are vocabulary-level knowledge, rendered
into the cognitive profile section of the system prompt.

### §2.1 Vocabulary Extension (casehub-eidos-vocab)

Two new methods on `JungianFunctionTerm`:

**`responseStyleGuidance()`** — what the dominant function means for response
style:

| Function | Guidance |
|----------|----------|
| Te | Produce structured plans with explicit criteria and measurable outcomes. Organize information systematically. Prioritize efficiency and logical execution. |
| Ti | Build internal logical frameworks. Analyze from first principles. Seek precision and internal consistency over external validation. |
| Fe | Frame responses around group impact and relational dynamics. Seek consensus. Consider how decisions affect team harmony and stakeholder relationships. |
| Fi | Evaluate through deeply held principles. Make authentic value-aligned choices. Prioritize ethical consistency over external approval. |
| Ne | Explore multiple possibilities and connections. Brainstorm divergent options. Generate creative alternatives before converging. |
| Ni | Synthesize patterns into singular strategic insights. Focus on foresight and long-term implications. Converge on the most likely outcome. |
| Se | Focus on immediate, concrete, actionable data. Address present-moment realities. Deliver practical, hands-on solutions. |
| Si | Draw on established procedures and past precedent. Follow proven methodologies. Provide detailed, step-by-step approaches based on what has worked before. |

**`antiPatternWarning()`** — what the dominant function should avoid:

| Function | Anti-pattern |
|----------|-------------|
| Te | Avoid unstructured brainstorming or open-ended exploration without clear criteria. |
| Ti | Avoid accepting conclusions without verifying internal logical consistency. |
| Fe | Avoid decisions that prioritize individual preference over group impact. |
| Fi | Avoid compromising core principles for expedience or social pressure. |
| Ne | Avoid premature convergence on a single solution before exploring alternatives. |
| Ni | Avoid getting lost in divergent possibilities when convergent insight is needed. |
| Se | Avoid abstract theorizing when concrete action is available. |
| Si | Avoid novel approaches when proven procedures exist and apply. |

Both are `String`-returning methods on the enum, same pattern as
`description()` and `label()`.

### §2.2 Renderer Changes (EidosRenderPipeline)

**MARKDOWN:** In `assembleMarkdownCognitiveProfile`, after the compensation
instruction, append:

```markdown
**Your Response Style:** [dominant.responseStyleGuidance()]

**Avoid:** [dominant.antiPatternWarning()]
```

Only renders when `dispositionVocabulary` is Jungian and the dominant
function resolves to a `JungianFunctionTerm` via `VocabularyRegistry.resolve()`.
The resolver returns `VocabularyTerm`; use `instanceof JungianFunctionTerm jft`
pattern match before accessing the new methods. Non-Jungian terms skip
constraint rendering silently.

**PROSE:** Semantic enrichment step receives `responseStyleGuidance` and
`antiPatternWarning` as structured fields in the enrichment payload. The
LLM weaves them into natural prose.

**A2A_CARD:** Added to the cognitive profile JSON block:

```json
"cognitiveProfile": {
  "dominant": {
    "term": "te", "weight": 0.45,
    "responseStyle": "Produce structured plans...",
    "antiPattern": "Avoid unstructured brainstorming..."
  },
  "auxiliary": { "term": "ni", "weight": 0.20 }
}
```

Only the dominant function gets response style and anti-pattern. The
auxiliary complements but does not constrain.

### §2.3 Boundaries

- **Observation instructions** (mechanism 2 from issue) — engine/consumer
  concern. The rendered prompt already gives the agent its cognitive style
  via response style guidance. Engine can read the A2A_CARD `responseStyle`
  field if it needs observation hints separately.
- **Structured generation enforcement** (mechanism 3 from issue) —
  explicitly a "research direction." Out of scope.

---

## §3 Goal Priority Evolution (#120)

Goal priorities shift based on accumulated behavioral signals. Follows the
established pattern: `DispositionSignalStore` for personality signals,
`BehavioralSignalStore` for capability signals, now `GoalSignalStore` for
goal signals.

### §3.1 API Types (casehub-eidos-api, Tier 1)

```java
public enum GoalOutcome { SUCCESS, FAILURE }

public record GoalOutcomeCounts(int successCount, int failureCount) {
    public double successRate() {
        int total = successCount + failureCount;
        return total == 0 ? 0.0 : (double) successCount / total;
    }
}
```

### §3.2 GoalSignalStore SPI (casehub-eidos-api, Tier 1)

```java
public interface GoalSignalStore {
    void recordOutcome(String agentId, String tenancyId,
                       String goalName, GoalOutcome outcome);
    Map<String, GoalOutcomeCounts> outcomeCounts(String agentId,
                                                 String tenancyId);
    void decay(String agentId, String tenancyId, double decayFactor);
    void clear(String agentId, String tenancyId);
}
```

Cumulative counts, no TTL — same lifecycle semantics as
`DispositionSignalStore`. Counts accumulate until explicitly decayed by
evolution or cleared post-transition.

- `recordOutcome` — increments the appropriate counter for the goal.
- `outcomeCounts` — returns goal name → counts mapping.
- `decay` — multiplies all counts by `(1 - decayFactor)`, truncated
  to integer. Same formula as `DispositionSignalStore.decay()`.
- `clear` — resets all signal data.

### §3.3 GoalEvolution SPI (casehub-eidos-api, Tier 1)

```java
public interface GoalEvolution {
    GoalEvolutionResult evaluate(AgentDescriptor descriptor,
                                 Map<String, GoalOutcomeCounts> counts);
}
```

Takes counts directly rather than injecting the store — keeps the SPI
pure and testable. The engine fetches counts then calls evaluate.

```java
public sealed interface GoalEvolutionResult
    permits GoalEvolutionResult.Unchanged,
            GoalEvolutionResult.Evolved,
            GoalEvolutionResult.Dampened {

    record Unchanged() implements GoalEvolutionResult {}

    record Evolved(
        List<AgentGoal> newGoals,
        List<String> promotedGoals,
        List<String> demotedGoals
    ) implements GoalEvolutionResult {}

    record Dampened(double decayFactor)
        implements GoalEvolutionResult {}
}
```

- `Evolved.promotedGoals` — goals promoted from SECONDARY to PRIMARY
  (empty list when no promotions occurred).
- `Evolved.demotedGoals` — goals demoted from PRIMARY to SECONDARY
  (empty list when no demotions occurred).
- `Evolved.newGoals` — the complete updated goal list with new priorities.
- `Dampened` — returned when demotion would leave zero PRIMARY goals
  and no SECONDARY exists to promote. Consumer should call
  `GoalSignalStore.decay()` with the returned factor.

### §3.4 DefaultGoalEvolution (casehub-eidos runtime, @ApplicationScoped)

Evolution rules, evaluated in order:

1. **Promotion check:** For each SECONDARY goal, if
   `successRate() >= promotionThreshold` AND
   `successCount >= promotionMinCount` → candidate for promotion.
2. **Demotion check:** For each PRIMARY goal, if failure rate
   `(1.0 - successRate()) >= demotionThreshold` AND
   `failureCount >= demotionMinCount` → candidate for demotion.
3. **Swap guard:** If demotion would leave zero PRIMARY goals, atomically
   promote the highest-success-rate SECONDARY before demoting. If no
   SECONDARY exists to promote, return `Dampened` instead of demoting.
4. **No thresholds crossed:** Return `Unchanged`.

Multiple promotions and demotions can occur in a single evaluation. The
result contains the complete new goal list.

### §3.5 Preference Keys

| Key | Default | Purpose |
|-----|---------|---------|
| `eidos.goal.promotion.threshold` | 0.8 | Success rate to promote SECONDARY → PRIMARY |
| `eidos.goal.promotion.min-count` | 10 | Minimum successes before promotion eligible |
| `eidos.goal.demotion.threshold` | 0.7 | Failure rate to demote PRIMARY → SECONDARY |
| `eidos.goal.demotion.min-count` | 10 | Minimum failures before demotion eligible |
| `eidos.goal.decay.factor` | 0.20 | Decay multiplier (matches disposition) |

Resolved via `PreferenceProvider` (per-tenancy overrides), following the
existing pattern from `DispositionPreferenceKeys`.

### §3.6 CDI Ladder

| Bean | Module | Scope |
|------|--------|-------|
| `NoOpGoalSignalStore` @DefaultBean | runtime | Deployments without goal tracking |
| `NoOpGoalEvolution` @DefaultBean | runtime | Returns Unchanged always |
| `DefaultGoalEvolution` @IfBuildProperty | runtime | Rule-based evaluation (gated like JpaGoalSignalStore) |
| `InMemoryGoalSignalStore` @Alternative @Priority(1) | persistence-memory | Tests, in-memory deployments |
| `JpaGoalSignalStore` @IfBuildProperty | runtime | Persistent, Flyway-managed |

### §3.7 Schema — New Table `goal_signal` (Flyway V10)

| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK | Generated |
| agent_id | VARCHAR(255) | NOT NULL |
| tenancy_id | VARCHAR(255) | NOT NULL |
| goal_name | VARCHAR(200) | NOT NULL |
| success_count | INT | NOT NULL DEFAULT 0 |
| failure_count | INT | NOT NULL DEFAULT 0 |

**UNIQUE:** `(agent_id, tenancy_id, goal_name)`

### §3.8 Orchestration Boundary

Same pattern as disposition evolution — eidos provides detection and
decision; the engine orchestrates:

1. Engine records outcomes via `GoalSignalStore.recordOutcome()` after
   task completion.
2. Engine periodically calls `GoalEvolution.evaluate()` (debounced,
   not on every signal).
3. `Evolved` → engine creates a new descriptor version via registry,
   then calls `GoalSignalStore.clear()` to reset accumulated counts.
   The new descriptor carries the updated priorities; old signal data
   is no longer relevant.
4. `Dampened` → engine calls `GoalSignalStore.decay()`.
5. Concurrency handled by engine's existing descriptor versioning
   (optimistic locking on version number).

---

## §4 Cross-Cutting Concerns

### §4.1 Module Placement

| Component | Module | Tier |
|-----------|--------|------|
| `CoherenceLevel`, `CoherenceViolation`, `CoherenceReport` | casehub-eidos-api | 1 |
| `GoalOutcome`, `GoalOutcomeCounts` | casehub-eidos-api | 1 |
| `GoalSignalStore` SPI | casehub-eidos-api | 1 |
| `GoalEvolution` SPI, `GoalEvolutionResult` | casehub-eidos-api | 1 |
| `JungianFunctionTerm.responseStyleGuidance()`, `antiPatternWarning()` | casehub-eidos-vocab | — |
| `BriefingCoherenceValidator` | casehub-eidos (runtime) | 3 |
| `DefaultGoalEvolution` | casehub-eidos (runtime) | 3 |
| `NoOpGoalSignalStore` @DefaultBean | casehub-eidos (runtime) | 3 |
| `NoOpGoalEvolution` @DefaultBean | casehub-eidos (runtime) | 3 |
| `JpaGoalSignalStore` @IfBuildProperty | casehub-eidos (runtime) | 3 |
| Cognitive profile rendering changes | casehub-eidos (runtime) | 3 |
| `InMemoryGoalSignalStore` @Alternative | casehub-eidos-memory | — |
| Goal evolution preference keys | casehub-eidos (runtime) | 3 |
| Schema migration V10 | casehub-eidos (runtime) | 3 |

### §4.2 RenderedPrompt Extension

New optional field on `RenderedPrompt`:

```java
CoherenceReport coherenceReport()  // nullable, populated when validation runs
```

### §4.3 Implementation Order

1. **#128 — Coherence validation** (independent, highest value)
   - API types → validator → DescriptorCollector integration → renderer
     metadata → tests
2. **#130 — Function-specific constraints** (benefits from #128)
   - Vocabulary methods → renderer changes → eval profile updates → tests
3. **#120 — Goal priority evolution** (independent, follows naturally)
   - API types → SPI → CDI ladder → schema → DefaultGoalEvolution → tests

### §4.4 Downstream Impact

- **casehub-engine:** Will call `GoalSignalStore.recordOutcome()` and
  `GoalEvolution.evaluate()` — engine-side work tracked under engine#800
  Sub-epic C. No eidos changes needed; SPIs are the contract.
- **casehub-eidos-eval:** Coherence validation runs in eval harness.
  Function-specific constraints affect rendered prompts — existing judges
  should see improved function activation scores.
- **Examples/consumers:** Pre-release — breaking changes are acceptable.
  `RenderedPrompt` record gains a new `coherenceReport` field (constructor
  change). New SPIs with @DefaultBean no-ops, new vocabulary methods.

### §4.5 Issue Mapping

| Issue | Scope | Section |
|-------|-------|---------|
| #128 | Briefing-framework coherence validation | §1 |
| #130 | Function-specific prompt constraints | §2 |
| #120 | Goal priority evolution | §3 |
