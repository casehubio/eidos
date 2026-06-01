# eidos#23 — Real-World Agent Profile Library

**Date:** 2026-06-01  
**Issue:** casehubio/eidos#23  
**Status:** Approved (v5 — reviewer follow-up fixes applied)

---

## Overview

The current `EvalDataset` uses synthetic `AgentDescriptor` instances that exercise the renderer's structural coverage but are not grounded in real-world role definitions. This issue builds a curated library of real-world agent profiles — derived from published system prompts and occupational frameworks — and integrates them into the eval harness with two new quality signals:

1. **Semantic proximity** — does the rendered prompt convey the same role identity as the original prose?
2. **Personality preservation** — does the pipeline preserve personality differentiation between agents with distinct dispositions, measured empirically through three attribution stages that localise pipeline failures?

**Phase 1 note:** Phase 1 is a human research task — find 8 published system prompts from the sources listed in the Target Profile Set section. The YAML files in `eval/src/test/resources/profiles/` are the Phase 1 output; their content is filled in during implementation after research is complete.

**Renderer LLM budget:** `ClaudeMarkdownRenderer` may itself call a `ChatModel` when one is configured. For the eval harness, use structural-only rendering (no LLM-path) to keep the budget bounded. Configure this via a `@TestProfile` that sets `casehub.eidos.renderer.llm-enabled=false` or equivalent, or ensure no `ChatModel` bean is registered in the eval test context beyond the judge model.

---

## Architectural Decisions

### Decision 1 — Separate ProximityJudge (not a new EvalDimension)

`EvalDimension.applicableFor(format)` has a clean invariant: format alone determines applicable dimensions. All existing dimensions measure the rendered output against the descriptor. Proximity measures against external ground-truth prose — a different reference that exists only for profile-backed cases. Folding it into `EvalDimension` would:

1. Corrupt `applicableFor(format, boolean hasSourceProfile)` — unrelated concerns in one method
2. Make `EvalResult.overall` incomparable across synthetic and real-world cases
3. Make A2A_CARD proximity incoherent (discovery artifact ≠ system prompt)
4. Risk GE-20260531-686150 (adding a dimension breaks evaluators iterating `values()`)

Proximity is a separate quality signal with its own judge, result type, and pass/fail threshold.

### Decision 2 — Sealed EvalCase (SyntheticEvalCase + ProfiledEvalCase)

`Optional<AgentProfile>` in a record is a design smell: it signals two kinds of `EvalCase` that a sealed interface expresses cleanly and type-safely. With three new judges all requiring an `AgentProfile`, `ProfiledEvalCase` eliminates all `isPresent()` guards. Adding a third case type later is additive.

```java
public sealed interface EvalCase permits SyntheticEvalCase, ProfiledEvalCase {
    String name();
    AgentDescriptor descriptor();
    AgentPromptContext context();
}
public record SyntheticEvalCase(
    String name, AgentDescriptor descriptor, AgentPromptContext context
) implements EvalCase {}
public record ProfiledEvalCase(
    String name, AgentDescriptor descriptor, AgentPromptContext context, AgentProfile profile
) implements EvalCase {}
```

**JSON serialisation note:** `EvalReportWriter.writeJson()` serialises `EvalResult.evalCase()` polymorphically. Without `@JsonTypeInfo` on the `EvalCase` interface, Jackson serialises the concrete runtime type without a type discriminator. The JSON structure changes silently after this refactor. As eval reports are write-only diagnostic output (never re-parsed as Java objects), this is acceptable — but document it in the output format.

### Decision 3 — Three-Stage Personality Preservation with Attribution Diagnosis

Proximity scoring verifies holistic fidelity but does not attribute failure. The personality preservation system adds attribution with a computed `AttributionDiagnosis` per profile × axis:

| Stage | Input | Measures | Failure mode identified |
|-------|-------|----------|------------------------|
| Stage 0 | Profile data (deterministic) | Variant pair axis isolation | Profile design error — pairs not isolated |
| Stage 1 | Prose only | Disposition axis expressiveness | Vocabulary gap — axis cannot express this concept |
| Stage 2 | Rendered text only (blind) | Trait expression in render | Renderer flattening — axis encoded but render loses it |
| Stage 3 | Two rendered texts + axis | Pairwise discriminability + effect size | Profile design too similar, or Stage 1/2 masking |

**Attribution logic** (computed per profile × axis in `PersonalityPreservationReport.build()`):

| Stage 1 | Stage 2 direction match | Stage 3 effectSize | Diagnosis |
|---------|------------------------|-------------------|-----------|
| ≤ 2 | (any) | (any) | `VOCABULARY_GAP` → fix vocabulary; feed eidos#26 |
| ≥ 4 | false | (any) | `RENDERER_FLATTENING` → renderer doesn't amplify disposition |
| ≥ 4 | true | ≤ 2 | `PROFILE_DESIGN_GAP` → profiles too similar; differentiate further |
| ≥ 4 | true | ≥ 3 | `WORKING` → pipeline preserving this axis correctly |

Stage 3 covers only variant pair axes (`riskAppetite`, `ruleFollowing`). For `socialOrient` and `autonomy` (no variant pairs), `effectSize` is `-1` and only `VOCABULARY_GAP` vs `RENDERER_FLATTENING` can be diagnosed. This is a known coverage limitation — extend with two more profile pairs in a follow-on if needed.

### Decision 4 — Stage 1 Purpose: Axis Expressiveness, Not Formal Vocabulary

Stage 1 asks "how precisely can a short open-string phrase capture what the original prose says about this personality axis?" — not "how well does the formal vocabulary definition match?" This distinction matters: the Conscientiousness and SVO vocabularies are themselves open strings without machine-readable formal definitions. Stage 1 measures whether the **axis concept** has sufficient expressiveness, regardless of which specific vocabulary terms are chosen. This feeds eidos#26 by identifying which axes need richer vocabulary (more terms, formal ontology, or sub-axis decomposition).

**Deferred:**
- eidos#26 — Belbin/DISC/Big Five vocabulary module (awaits eidos#29)
- eidos#27 — Theoretical framework grounding in AgentDescriptor and renderer
- eidos#28 — Belbin-based agent composition for project phases
- eidos#29 — Docs: Mapping Personality and Role Frameworks to AgentDescriptor

---

## New Types

All in `eval/src/main/java/io/casehub/eidos/eval/` unless noted.

### SourceType (enum)

```java
public enum SourceType {
    PRACTITIONER,       // practitioner library or community repo
    ACADEMIC,           // published academic paper
    OPENAI_COOKBOOK,
    ANTHROPIC_LIBRARY,
    ONET_SYNTHESISED    // prose synthesised from O*NET structured data, not a published human prompt
                        // proximity scores for ONET_SYNTHESISED profiles have lower confidence:
                        // the "original prose" is itself synthetic, not a real system prompt
}
```

### CoverageLoss (enum)

```java
public enum CoverageLoss {
    PARTIAL,  // concept approximated; some nuance lost
    FULL      // no vocabulary equivalent; completely unexpressed
}
```

### TraitPolarity (enum)

```java
public enum TraitPolarity {
    HIGH,    // prose expresses this trait strongly
    LOW,     // prose expresses the opposite strongly
    NEUTRAL  // prose does not clearly express this trait in either direction
}
```

### Attribution (enum)

```java
public enum Attribution {
    VOCABULARY_GAP,      // Stage 1 ≤ 2: axis cannot express this concept
    RENDERER_FLATTENING, // Stage 1 ≥ 4, Stage 2 mismatch: encoded but render loses it
    PROFILE_DESIGN_GAP,  // Stage 1 ≥ 4, Stage 2 match, Stage 3 ≤ 2: profiles too similar
    WORKING,             // All stages pass
    INSUFFICIENT_DATA    // Stage 3 not available for this axis (no variant pair covers it)
                         // combined with Stage 1/2 to give partial attribution
}
```

### VocabularyGap

```java
public record VocabularyGap(
    String concept,      // e.g. "correctness-over-velocity tradeoff"
    String description,
    CoverageLoss loss
) {}
```

### AgentProfile

```java
public record AgentProfile(
    String name,
    String role,
    String domain,
    String sourceUrl,
    String sourceCitation,
    SourceType sourceType,
    String originalProse,
    GoalContext evalGoal,                   // goal context for AgentPromptContext in eval; null → empty
                                            // context (systematic downward bias on proximity scores).
                                            // Nullable, not Optional<GoalContext> — Optional in record
                                            // components is a design smell (see Decision 2 rationale).
                                            // A null evalGoal means "intentionally absent": YAML authors
                                            // omit the block for context-insensitive roles (e.g. technical
                                            // writer); presence distinguishes intent from omission.
    String notes,
    Map<String, String> theoreticalFramework,
    Map<String, TraitPolarity> expectedTraits,
    AgentDescriptor descriptor,
    List<VocabularyGap> vocabularyGaps
) {}
```

**`evalGoal`** addresses the systematic proximity bias from empty contexts: original prose implies operational context that rendered prompts with empty `AgentPromptContext` will lack. Where the original prose implies a specific operational situation, author a `GoalContext` capturing the description, any relevant sub-goals, and leave `caseRef` null. Context-insensitive roles (e.g. technical writer, whose prompt applies regardless of situation) legitimately omit this field. Null and absent key are equivalent — both mean "no goal context"; the downward proximity bias applies and is acknowledged in the report.

### VariantPair

```java
public record VariantPair(
    String primaryAxis,  // exact AgentDisposition field name
    String higher,       // profile slug with higher trait polarity on this axis
    String lower
) {}
```

### VariantIndex

```java
public record VariantIndex(
    List<String> profiles,
    List<VariantPair> variants
) {}
```

### AttributionDiagnosis

```java
public record AttributionDiagnosis(
    String profileName,
    String axis,
    int stage1Score,          // 1–5; vocabulary expressiveness (or -1 if not evaluated)
    int stage2ExpressionScore, // 1–5; blind expression score averaged across formats (or -1)
    int stage3EffectSize,     // 1–5; pairwise effect size (or -1 if no variant pair covers axis)
    Attribution attribution
) {}
```

### ProximityResult

```java
public record ProximityResult(
    EvalCase evalCase,
    int score,   // 0–5
    String reasoning,
    List<String> gaps
) {
    public ProximityResult {
        if (score < 0 || score > 5)
            throw new IllegalArgumentException("ProximityResult score out of range: " + score);
    }
}
```

### ProximityReport

```java
public record ProximityReport(
    List<ProximityResult> results,
    double floor,
    double meanScore,
    double minScore,
    int belowFloor
) {
    public static ProximityReport build(List<ProximityResult> results, double floor) { ... }
}
```

### VocabularyExpressivenessResult (Stage 1)

```java
public record VocabularyExpressivenessResult(
    String profileName,
    Map<String, Integer> expressivenessScores,  // axis → 1–5
    List<String> weakAxes                       // axes scoring ≤ 2
) {}
```

### TraitExpressionResult (Stage 2)

```java
public record TraitExpressionResult(
    ProfiledEvalCase evalCase,
    RenderFormat format,
    Map<String, Integer> expressionScores,   // axis → 1–5 (blind, 4 numeric axes)
    Map<String, Boolean> directionMatches,   // axis → declared direction matched?
    String delegationAssessment              // "YES" | "NO" | "UNCERTAIN"
) {}
```

`directionMatches` covers only the 4 numeric axes (`socialOrient`, `ruleFollowing`, `riskAppetite`, `autonomy`). `delegation` is reported separately.

### PairContrastResult (Stage 3)

```java
public record PairContrastResult(
    String profileHigh,
    String profileLow,
    String primaryAxis,
    RenderFormat format,
    boolean correctlyIdentified,
    int effectSize,   // 1–5
    String reasoning
) {
    public PairContrastResult {
        if (effectSize < 1 || effectSize > 5)
            throw new IllegalArgumentException("effectSize out of range: " + effectSize);
    }
}
```

### PersonalityPreservationReport

```java
public record PersonalityPreservationReport(
    List<VocabularyExpressivenessResult> expressivenessResults,
    List<TraitExpressionResult> traitExpressionResults,
    List<PairContrastResult> pairContrastResults,
    List<AttributionDiagnosis> diagnoses,      // primary output — one per (profile × axis)
    double meanExpressivenessScore,
    double meanTraitMatchRate,
    double meanEffectSize,
    double discriminationAccuracy,
    List<String> annotations
) {
    /**
     * @param expressivenessResults Stage 1 results
     * @param traitExpressionResults Stage 2 results
     * @param pairContrastResults Stage 3 results
     */
    public static PersonalityPreservationReport build(
        List<VocabularyExpressivenessResult> expressivenessResults,
        List<TraitExpressionResult> traitExpressionResults,
        List<PairContrastResult> pairContrastResults
    ) {
        // meanExpressivenessScore:
        //   sum of all (profile × axis) scores / (num_profiles × 4)
        //   flat mean across all cells, not per-profile average

        // meanTraitMatchRate:
        //   count of matching (profile × format × axis) cells
        //   / (num_profiles × num_formats × 4 numeric axes)
        //   flat mean — not per-profile averaged

        // discriminationAccuracy:
        //   count of correctly identified pairs
        //   / (num_variant_pairs × num_formats)
        //   flat mean — not per-pair averaged

        // meanEffectSize:
        //   sum of all effectSize values / (num_variant_pairs × num_formats)

        // diagnoses: one per (profile × axis) for profiles with expectedTraits
        //   Apply attribution table from Decision 3.
        //   Stage 3 effectSize: look up by matching axis + format; -1 if no pair covers this axis.
        //   Stage 2 direction match: mean of MARKDOWN + PROSE direction match booleans for this axis.

        // annotations: new ArrayList<>() — mutable, reliability warnings appended post-build
        ...
    }
}
```

---

## Profile YAML Format

**Location:** `eval/src/test/resources/profiles/<slug>.yaml`

**YAML key naming:** all keys must exactly match Java record field names (camelCase). Jackson silently drops unrecognised keys — a typo like `primary_axis` instead of `primaryAxis` will not error; the field will deserialise as `null`, and Stage 0 will fail with `"primaryAxis is null"` rather than a helpful key-not-found message.

**Index:** `eval/src/test/resources/profiles/index.yaml`

```yaml
# index.yaml
profiles:
  - sw-engineer-careful.yaml
  - sw-engineer-bold.yaml
  - security-analyst-defensive.yaml
  - security-analyst-proactive.yaml
  - product-manager.yaml
  - clinical-researcher.yaml
  - customer-support-agent.yaml
  - technical-writer.yaml

variants:
  - primaryAxis: riskAppetite
    higher: sw-engineer-bold
    lower: sw-engineer-careful
  - primaryAxis: ruleFollowing
    higher: security-analyst-defensive
    lower: security-analyst-proactive
```

**Variant pair constraint:** pairs must differ on exactly the declared `primaryAxis`; all other `AgentDisposition` fields must be equal between the two profiles. `AgentProfileLoader` validates this at Stage 0.

**Stage 3 coverage:** only `riskAppetite` and `ruleFollowing` have variant pairs. `socialOrient` and `autonomy` can only receive `VOCABULARY_GAP` or `RENDERER_FLATTENING` diagnoses (no effect size data). This is a known limitation; two additional pairs can be added in a follow-on without changes to the type system.

### Required vs Optional YAML Fields

| Field path | Required? | Notes |
|---|---|---|
| `descriptor.agentId` | **Required** | validated by compact constructor |
| `descriptor.name` | **Required** | validated by compact constructor |
| `descriptor.slot` | **Required** | validated by compact constructor |
| `descriptor.tenancyId` | **Required** | validated by compact constructor |
| `descriptor.disposition.delegation` | **Required** | primitive `boolean`; Jackson cannot default a primitive to `false` when the key is absent — must be explicit in YAML |
| `name` | **Required** | profile slug used for matching |
| `sourceType` | **Required** | enum value; must match exactly |
| `originalProse` | **Required** | ground truth for proximity evaluation |
| All other descriptor fields | Optional | absent key → `null` |
| `disposition` block | Optional | defaults to `null` AgentDisposition |
| `capabilities` | Optional | defaults to empty list |
| `vocabularyGaps` | Optional | defaults to empty list |
| `evalGoal` | Optional | `null` / absent → empty context (proximity bias applies); omit for context-insensitive roles |
| `theoreticalFramework` | Optional | human annotation for eidos#29 |
| `expectedTraits` | Optional | required only for profiles included in Stage 2/3 evaluation |

### Profile Schema

```yaml
name: sw-engineer-careful
role: Software Engineer — Code Review (Careful)
domain: software-engineering
sourceUrl: "https://docs.anthropic.com/en/prompt-library/..."
sourceCitation: "Anthropic Prompt Library, 2025"
sourceType: ANTHROPIC_LIBRARY

originalProse: |
  You are a senior software engineer with deep expertise in Java and distributed
  systems. Your primary responsibility is code review...

evalGoal:
  description: "Review a Java pull request for correctness, performance, and security"
  subGoals:
    - Flag off-by-one errors and null dereferences
    - Identify security antipatterns (SQL injection, unescaped output)
  caseRef: ~            # null — no active case reference needed for eval

notes: |
  Strong correctness-over-velocity emphasis. Thomas-Kilmann Collaborating mode
  would add precision to socialOrient — flagged for eidos#29.

theoreticalFramework:
  belbin: monitor-evaluator
  disc: C

expectedTraits:
  riskAppetite: LOW
  socialOrient: LOW
  ruleFollowing: HIGH
  autonomy: LOW

descriptor:
  agentId: sw-engineer-careful-01
  name: Software Engineer — Careful
  version: "1.0"
  provider: anthropic
  modelFamily: claude
  modelVersion: claude-opus-4-7
  domainVocabulary: "https://vocab.casehub.io/svo"
  dispositionVocabulary: "https://vocab.casehub.io/conscientiousness"
  slot: reviewer
  capabilities:
    - name: code-review
      qualityHint: 0.95
      latencyHintP50Ms: 45000
      costHint: medium
      inputTypes: [pull-request, diff]
      outputTypes: [review-comment, approval]
      tags: []
      epistemicDomains:
        java: 0.95
        distributed-systems: 0.85
        rust: 0.3
  disposition:
    socialOrient: independent
    ruleFollowing: strict
    riskAppetite: conservative
    autonomy: directed
    delegation: false       # required — primitive boolean cannot be omitted
  tenancyId: profiles-1

vocabularyGaps:
  - concept: correctness-over-velocity
    description: "Engineering philosophy; riskAppetite=conservative is an approximation"
    loss: PARTIAL
```

---

## Target Profile Set

8 profiles across 6 roles. Variant pairs differ on exactly one disposition axis.

| Slug | Belbin | DISC | Domain | Primary disposition contrast |
|---|---|---|---|---|
| sw-engineer-careful | Monitor Evaluator | C | code review | `riskAppetite: LOW` |
| sw-engineer-bold | Shaper | D | code review | `riskAppetite: HIGH` (pair axis) |
| security-analyst-defensive | Implementer | S | security | `ruleFollowing: HIGH` |
| security-analyst-proactive | Shaper | D | security | `ruleFollowing: LOW` (pair axis) |
| product-manager | Co-ordinator | I | product vision / stakeholder comms / roadmap | — |
| clinical-researcher | Specialist | C | clinical trials | jurisdiction-heavy |
| customer-support-agent | Teamworker | S | customer support | `socialOrient: HIGH`, `delegation: false` |
| technical-writer | Completer Finisher | S | documentation | `ruleFollowing: HIGH`, detail-oriented |

**Source priority revised:** the goal of Phase 1 is both profile quality AND vocabulary gap discovery. Anthropic/OpenAI Cookbook sources offer highest prose quality (good for proximity validity); O*NET and academic sources stress vocabulary harder (more likely to surface gaps for eidos#26). Use Anthropic/OpenAI sources where prose quality is paramount; use O*NET/academic sources where roles are sparsely covered by practitioner prompts or where vocabulary stress is the goal.

- For profiles with variant pairs (sw-engineer, security-analyst): favour practitioner sources — nuanced disposition differentiation is more likely from human-authored role prompts
- For single-profile roles: O*NET cross-reference for capability derivation is valuable; original prose still from published sources
- O*NET-backed profiles: set `sourceType: ONET_SYNTHESISED` and note that proximity scores are lower-confidence

---

## AgentProfileLoader

**Scope: `eval/src/test/java/io/casehub/eidos/eval/`** — serves only the eval test harness. No non-test context should access this class. The prior "graceful empty list if index absent in non-test context" logic is eliminated by placing the class in test scope.

```java
// eval/src/test/java/io/casehub/eidos/eval/AgentProfileLoader.java
class AgentProfileLoader {
    // ObjectMapper: new ObjectMapper(new YAMLFactory()).findAndRegisterModules()
    // findAndRegisterModules() activates jackson-module-parameter-names,
    // required for Java record deserialization on Java 21+.
    // Without it, all record fields deserialize as null.
    //
    // Classpath access: Thread.currentThread().getContextClassLoader()
    //   .getResourceAsStream("profiles/index.yaml")
    // This is the correct mechanism for Quarkus @QuarkusTest classpath access.
    //
    // load(): reads index.yaml → each profile YAML → deserialises to AgentProfile.
    //   AgentDescriptor compact constructor fires → validates agentId, name, slot, tenancyId.
    //   Throws IllegalArgumentException if delegation key is absent from disposition YAML
    //   (Jackson cannot default a primitive boolean to false when the key is absent).
    //   Runs Stage 0 validation after all profiles are loaded.
    //   Throws IllegalStateException with profile slugs and axis name on Stage 0 violation.
    //
    // loadIndex(): reads index.yaml only → returns VariantIndex.
    //
    // Stage 0 validation:
    //   For each VariantPair in VariantIndex.variants():
    //     Verify primaryAxis is one of {socialOrient, ruleFollowing, riskAppetite, autonomy}.
    //     Load the two AgentProfile instances for pair.higher() and pair.lower().
    //     Assert AgentDisposition.{primaryAxis} values differ between the two profiles.
    //     Assert all other AgentDisposition fields are equal (including delegation).
    List<AgentProfile> load() { ... }
    VariantIndex loadIndex() { ... }
}
```

`jackson-dataformat-yaml`: verify whether it is already a transitive dependency in `eval/pom.xml`; if absent, add an explicit `provided`-scope entry. Do not hardcode the version.

---

## EvalDataset

`all()` unchanged — returns `List<SyntheticEvalCase>`; existing test behavior preserved.

```java
// Return type is List<ProfiledEvalCase> — no cast needed at call sites
public static List<ProfiledEvalCase> realWorld() {
    return new AgentProfileLoader().load().stream()
        .flatMap(profile -> Stream.of(
            profileCase(profile, RenderFormat.MARKDOWN),
            profileCase(profile, RenderFormat.PROSE)
        ))
        .toList();
}

private static ProfiledEvalCase profileCase(AgentProfile profile, RenderFormat format) {
    final AgentPromptContext ctx = profile.evalGoal() != null
        ? AgentPromptContext.forFormat(format).withGoal(profile.evalGoal())
        : AgentPromptContext.forFormat(format);
    return new ProfiledEvalCase(
        profile.name() + "-" + format.name().toLowerCase(),
        profile.descriptor(),
        ctx,
        profile
    );
}
```

---

## ProximityJudge

`@ApplicationScoped`. Injects `ChatModel`. Contract: called for MARKDOWN and PROSE `ProfiledEvalCase` instances only.

**Method signature:**
```java
/**
 * @throws MalformedJudgeResponseException if the LLM response cannot be parsed
 * @throws IllegalStateException if the LLM call fails
 */
public ProximityResult evaluate(ProfiledEvalCase evalCase, RenderedPrompt rendered) { ... }
```

**System prompt:**

```
You are evaluating how faithfully a machine-rendered system prompt captures
the identity expressed in a human-authored system prompt.

The human-authored prompt is the ground truth. The machine-rendered prompt was
derived by structuring the human prompt into an AgentDescriptor, then rendering
that descriptor back into a system prompt.

Score 0–5:
- 5: Conveys the same role, constraints, and operational style.
- 4: Minor gaps — one or two concepts softened or absent.
- 3: Core role present but significant style, constraints, or domain context missing.
- 2: Role recognisable but rendering loses enough to change agent behaviour.
- 1: Superficial match — same domain, fundamentally different character.
- 0: Identity mismatch.

Scoring guidance on additions: The rendered prompt may contain information present
in the descriptor but not in the original prose. Treat such additions as neutral if
they are consistent with the role (they come from the structured representation).
Treat as a gap only if the rendered output contains claims absent from BOTH the
original prose AND the descriptor — those are hallucinations.

Return JSON: { "score": int, "reasoning": string, "gaps": string[] }
```

**User payload:** `{ "originalProse": "...", "rendered": "..." }`

---

## Personality Preservation System

### Stage 0 — Profile Design Validation (in AgentProfileLoader)

See AgentProfileLoader section. Deterministic, zero LLM cost. Runs at `load()` time.

### Stage 1 — VocabularyExpressivenessJudge

`@ApplicationScoped`. Injects `ChatModel`.

**Method signature:**
```java
/**
 * Evaluates disposition axis expressiveness for all 4 scoreable axes.
 * Makes 4 sequential internal LLM calls (one per axis). Format-independent.
 * Sequential — no parallelism. Estimated runtime: ~4 × 3s × 8 profiles = ~96s for Stage 1.
 * @throws MalformedJudgeResponseException if any axis response cannot be parsed
 * @throws IllegalStateException if any LLM call fails
 */
public VocabularyExpressivenessResult evaluate(AgentProfile profile) { ... }
```

**Purpose:** measures how well a short open-string disposition phrase can capture each personality axis expressed in the prose. Does not test formal vocabulary definitions (none exist in machine-readable form). Identifies which axes need richer vocabulary — feeds eidos#26.

**Scope boundary:** Stage 1 is valid for vocabularies with common-language semantics — Conscientiousness and SVO are deliberately designed to use everyday terms ("conservative", "strict", "collaborative") whose general-language meanings closely match their intended use. The LLM's prior understanding of those terms is a valid proxy for vocabulary expressiveness. Stage 1 is NOT valid for domain-specific technical vocabularies where terms carry precise non-standard meanings (e.g. a client-specific vocabulary where "conservative" has a technical definition diverging from common usage). If a profile uses such a vocabulary, Stage 1 will silently measure the wrong thing — general word semantics rather than vocabulary-specific semantics. Flag this in the profile's `notes:` field.

**System prompt (parameterised per axis; one call per axis):**
```
You are evaluating how precisely an open-string label (1–5 words) can express a
personality concept found in the following system prompt.

The concept: [{axis description}]

Score 1–5:
- 5: A short label captures the nuance precisely (e.g. "strictly rule-following",
     "boldly risk-tolerant")
- 3: A label approximates it but loses meaningful nuance
- 1: The concept cannot be meaningfully captured in a short label; it requires
     prose explanation

If the score is ≤ 3, identify the specific nuance that is lost.

Return JSON: { "score": int, "reasoning": string, "gap": string | null }
```

**Axis descriptions passed to the judge:**

| Axis | Description |
|------|-------------|
| `socialOrient` | "how collaborative or independent the agent is — whether it seeks input and coordinates with others or acts independently" |
| `ruleFollowing` | "how strictly the agent follows rules and conventions versus adapting its approach to context" |
| `riskAppetite` | "how risk-tolerant or risk-averse the agent is — whether it favours bold decisions under uncertainty or prioritises caution" |
| `autonomy` | "how self-directed versus directed-by-others the agent is — whether it takes initiative or waits for instruction" |

### Stage 2 — TraitExpressionJudge

`@ApplicationScoped`. Injects `ChatModel`. Receives **rendered text only** — no descriptor, no prose.

**Method signature:**
```java
/**
 * Blind-scores rendered text for personality traits. The judge payload contains
 * only the rendered text — never the descriptor or original prose.
 * @throws MalformedJudgeResponseException if the LLM response cannot be parsed
 * @throws IllegalStateException if the LLM call fails
 */
public TraitExpressionResult evaluate(ProfiledEvalCase evalCase, RenderedPrompt rendered) { ... }
```

**Direction match logic:** HIGH declaration matches if blind score ≥ 4; LOW declaration matches if blind score ≤ 2; NEUTRAL always matches. Score 3 (neutral rubric anchor) matches neither directional declaration — a neutral render of a strongly-characterised profile is a renderer failure to amplify.

**Note on score range:** rubric anchors at 1, 3, and 5. Score 2 = between neutral and low; score 4 = between neutral and high. Score 0 has no rubric meaning and LLMs anchoring to the rubric are unlikely to emit it. The Java type uses `Map<String, Integer>` with no per-value validation (complex to validate map entries in a compact constructor); the judge system prompt specifies "1–5" to constrain LLM output.

**System prompt:**
```
You are characterising an AI agent's personality based solely on the following
system prompt. Do not infer from context or domain — read only what is
explicitly or strongly implied.

Score each axis 1–5:

riskAppetite:
  5 = explicitly endorses bold decisions, accepts uncertainty, prioritises outcomes
      over safety margins
  3 = neutral; neither risk-seeking nor risk-averse language
  1 = explicitly emphasises correctness, caution, and risk avoidance as primary values

socialOrient:
  5 = explicitly collaborative — seeks input, coordinates before acting,
      values team consensus
  3 = neutral
  1 = explicitly independent — self-directed, minimal consultation

ruleFollowing:
  5 = explicitly strict — follows established processes, does not deviate,
      rejects shortcuts
  3 = neutral
  1 = explicitly adaptive — adjusts to context, comfortable bending conventions

autonomy:
  5 = explicitly autonomous — takes initiative, decides without seeking approval
  3 = neutral
  1 = explicitly directed — waits for instruction, seeks explicit approval before acting

delegation:
  Does this prompt explicitly grant or restrict sub-agent delegation authority?
  Answer: "YES" (grants authority) | "NO" (restricts or silent) | "UNCERTAIN"

Return JSON:
{
  "riskAppetite": int, "socialOrient": int, "ruleFollowing": int, "autonomy": int,
  "delegation": "YES"|"NO"|"UNCERTAIN",
  "reasoning": string
}
```

**User payload:** `{ "rendered": "..." }` — no other fields.

### Stage 3 — PairContrastJudge

`@ApplicationScoped`. Injects `ChatModel`.

**Method signature:**
```java
/**
 * Evaluates pairwise discriminability on the declared primary axis.
 * Renders are looked up by matching pair.higher()/pair.lower() against
 * ProfiledEvalCase.profile().name() (not evalCase.name(), which includes format suffix).
 * @throws IllegalArgumentException if a profile slug is absent from the renders map
 * @throws MalformedJudgeResponseException if the LLM response cannot be parsed
 * @throws IllegalStateException if the LLM call fails
 */
public PairContrastResult evaluate(
    VariantPair pair,
    RenderFormat format,
    Map<ProfiledEvalCase, RenderedPrompt> renders
) { ... }
```

The method resolves rendered texts by matching `pair.higher()` and `pair.lower()` against `evalCase.profile().name()` — not against `evalCase.name()` (which has a format suffix). This avoids fragile string-suffix manipulation.

**System prompt:**
```
You are comparing two AI agent system prompts on a specific personality axis.

Axis: [{primaryAxis description}]
Prompt A: {rendered text of higher profile}
Prompt B: {rendered text of lower profile}

Identify which prompt expresses the axis more strongly, and score how starkly
different they are.

Effect size rubric (1–5):
- 5 = unmistakably different; a naive reader could identify which is which without context
- 3 = distinguishable if you are looking for it; would not immediately stand out
- 1 = practically indistinguishable on this axis

Return JSON: { "higher": "A" | "B", "effectSize": int, "reasoning": string }
```

**Axis descriptions passed to the judge** (same table as Stage 1 — same field names, same descriptions).

---

## Judge Reliability Check

```java
// In PromptEvalTest — private helper, called before preservation assertions
private List<String> runReliabilityCheck(
    List<ProfiledEvalCase> sample,          // 2 representative MARKDOWN cases
    Map<ProfiledEvalCase, RenderedPrompt> renders,
    VariantIndex index
) throws Exception {
    // Run TraitExpressionJudge twice on each sample case; compare per-axis scores.
    // Run PairContrastJudge twice on each variant pair; compare effectSize.
    // Write target/judge-reliability.json:
    //   { "stage2Variance": {"riskAppetite": double, ...},
    //     "stage3Variance": {"riskAppetite": int, "ruleFollowing": int},
    //     "passed": boolean, "warnings": string[] }
    // Threshold: stage2 per-axis variance ≤ 0.5; stage3 effectSize variance ≤ 1.
    // Returns warnings list for appending to PersonalityPreservationReport.annotations.
    // Does NOT fail the test — informational only until baseline is established.
    Files.createDirectories(Path.of("target"));
    ...
}
```

---

## PromptEvalTest Extension

**Shared load:** declare profiles and index as `@BeforeAll` class-level fields to avoid two full YAML parse cycles for the same data across test methods:

```java
static List<ProfiledEvalCase> realWorldCases;
static VariantIndex variantIndex;

@BeforeAll
static void loadProfiles() {
    final AgentProfileLoader loader = new AgentProfileLoader();
    // load() internally calls loadIndex() and validates Stage 0 pairs
    realWorldCases = EvalDataset.realWorld();  // uses internally-loaded profiles
    variantIndex = loader.loadIndex();
}
```

**New test method:**

```java
private static final double PROXIMITY_FLOOR = 3.0;

@Inject ProximityJudge proximityJudge;
@Inject VocabularyExpressivenessJudge expressivenessJudge;
@Inject TraitExpressionJudge traitExpressionJudge;
@Inject PairContrastJudge pairContrastJudge;

@Test
void evaluateRealWorldScenarios() throws Exception {
    // Render once per case — shared across quality, proximity, and Stage 2
    final Map<ProfiledEvalCase, RenderedPrompt> renders = realWorldCases.stream()
        .collect(toMap(identity(), c -> renderer.render(c.descriptor(), c.context())));

    // Quality eval
    final List<EvalResult> qualityResults = realWorldCases.stream()
        .map(c -> judge.evaluate(c, renders.get(c))).toList();

    // Proximity eval
    final List<ProximityResult> proximityResults = realWorldCases.stream()
        .map(c -> proximityJudge.evaluate(c, renders.get(c))).toList();

    // Stage 1 — 4 sequential internal calls per profile, format-independent
    final List<VocabularyExpressivenessResult> expressivenessResults =
        realWorldCases.stream().map(c -> c.profile()).distinct()
            .map(p -> expressivenessJudge.evaluate(p)).toList();

    // Stage 2 — blind scoring per case
    final List<TraitExpressionResult> traitResults = realWorldCases.stream()
        .map(c -> traitExpressionJudge.evaluate(c, renders.get(c))).toList();

    // Stage 3 — one call per variant pair per format
    final List<PairContrastResult> contrastResults =
        variantIndex.variants().stream()
            .flatMap(pair -> Stream.of(RenderFormat.MARKDOWN, RenderFormat.PROSE)
                .map(format -> pairContrastJudge.evaluate(pair, format, renders)))
            .toList();

    // Reliability check (2 sample cases × 2 judges × 2 runs = 8 calls)
    final List<ProfiledEvalCase> sample = realWorldCases.stream()
        .filter(c -> c.context().format() == RenderFormat.MARKDOWN).limit(2).toList();
    final List<String> reliabilityWarnings =
        runReliabilityCheck(sample, renders, variantIndex);

    // Build reports
    Files.createDirectories(Path.of("target"));

    final EvalReport qualityReport = EvalReport.build(qualityResults, "judge");
    EvalReportWriter.writeJson(qualityReport,
        Path.of("target/real-world-eval-report.json"));
    System.out.println(EvalReportWriter.summaryTable(qualityReport));

    final ProximityReport proximityReport =
        ProximityReport.build(proximityResults, PROXIMITY_FLOOR);
    EvalReportWriter.writeProximityJson(proximityReport,
        Path.of("target/proximity-report.json"));
    System.out.println(EvalReportWriter.proximitySummaryTable(proximityReport));

    final PersonalityPreservationReport preservationReport =
        PersonalityPreservationReport.build(
            expressivenessResults, traitResults, contrastResults);
    reliabilityWarnings.forEach(w -> preservationReport.annotations().add(w));
    EvalReportWriter.writePreservationJson(preservationReport,
        Path.of("target/personality-preservation-report.json"));
    System.out.println(EvalReportWriter.preservationSummaryTable(preservationReport));

    // Quality assertion — same floor as synthetic cases.
    // Note: real-world profiles may score lower on CONCISENESS or TONE due to
    // representation compression. Tune this floor independently from the synthetic
    // floor after the first run if legitimately lower.
    qualityReport.summaryByFormat().forEach((format, summary) -> {
        assertThat(summary.allCasesComplete())
            .as("All %s real-world cases must include every declared capability", format)
            .isTrue();
        assertThat(summary.meanOverall())
            .as("Mean judge score for %s", format)
            .isGreaterThanOrEqualTo(SCORE_FLOORS.getOrDefault(format, 3.5));
    });

    // Proximity assertion
    assertThat(proximityReport.meanScore())
        .as("Mean proximity score across real-world profiles")
        .isGreaterThanOrEqualTo(PROXIMITY_FLOOR);

    // Personality preservation thresholds: set AFTER first run based on observed values.
    // Consequence guidance:
    //   meanExpressivenessScore low for an axis → vocabulary gap; file eidos#26 with axis data
    //   meanTraitMatchRate low → renderer flattening (check attribution diagnoses)
    //   meanEffectSize low → profile pairs too similar (redesign) or stages 1/2 masking
    //   discriminationAccuracy < 0.6 → profiles may be indistinguishable; check profile design
    // assertThat(preservationReport.meanEffectSize()).isGreaterThanOrEqualTo(TBD);
    // assertThat(preservationReport.meanTraitMatchRate()).isGreaterThanOrEqualTo(TBD);
}
```

**Scale note:** three 0–5 scales are used across this test (EvalScore quality, ProximityResult.score, TraitExpressionResult expressionScores). The same 0–5 range means different things in each. `EvalReportWriter.preservationSummaryTable()` should clearly label each metric with its scale and signal type to prevent misinterpretation.

**`EvalReportWriter` new methods:**
- `writeProximityJson(ProximityReport, Path)`
- `proximitySummaryTable(ProximityReport)` — labels scale as "proximity fidelity 0–5"
- `writePreservationJson(PersonalityPreservationReport, Path)`
- `preservationSummaryTable(PersonalityPreservationReport)` — surfaces `diagnoses` as the primary section; aggregate floats secondary

**VocabularyGap / ProximityResult.gaps cross-reference note:** when a `ProximityResult.gap` string matches a `VocabularyGap.concept` for the same profile, the root cause is vocabulary (descriptor cannot encode it → renderer cannot output it → proximity judge sees it missing). When `ProximityResult.gaps` contains a concept not in `VocabularyGap`, the root cause is renderer quality. Cross-reference is currently manual; automated join requires NLP concept matching and is deferred beyond eidos#23.

---

## Vocabulary Gap Tracking (Phase 4)

During descriptor derivation for each profile:
1. Record gaps in `vocabularyGaps:` with `concept`, `description`, `loss`
2. Note in `notes:` where other frameworks would add precision — feeds eidos#29

After all profiles are drafted: aggregate `VocabularyExpressivenessResult` Stage 1 weak-axis data by axis and vocabulary. File 1–3 GitHub issues against `casehubio/eidos` (vocab module) with specific missing terms and Stage 1 score evidence.

---

## Protocol Compliance

| Protocol | Status |
|---|---|
| `agent-descriptor-compact-constructor-validation` | ✅ YAML deserialization triggers compact constructor; invalid profiles fail at load time |
| `eidos-enrichment-constants-in-pipeline` | ✅ Each judge defines its own constants; no sharing |
| `llm-pass-structural-fallback` | ✅ Not applicable — eval judges are not Foundation rendering SPIs |
| `format-specific-enrichment-schema-isolation` | ✅ Each judge has its own named schema |

---

## LLM Call Budget

Per full eval run (8 profiles, 2 formats, 2 variant pairs). Renderer runs in structural-only mode — no additional LLM calls.

| Stage | Calls | Notes |
|---|---|---|
| Quality eval (PromptJudge) | 16 | 8 profiles × 2 formats |
| Proximity eval (ProximityJudge) | 16 | 8 profiles × 2 formats |
| Stage 1 (VocabularyExpressivenessJudge) | 32 | 8 profiles × 4 sequential internal axis calls |
| Stage 2 (TraitExpressionJudge) | 16 | 8 profiles × 2 formats |
| Stage 3 (PairContrastJudge) | 4 | 2 pairs × 2 formats |
| **Total (steady state)** | **84** | ~8–12 min at 3–5s per call |
| Reliability check (one-time) | 8 | 2 sample cases × 2 judges × 2 runs |

**Partial result risk:** 84 sequential LLM calls in a single test run — any call failure throws and aborts with no partial results saved. This is acceptable for an offline diagnostic tool but limits debuggability. Recovery path for mid-run failures: reduce the profile set in `index.yaml` to the subset that failed, re-run, and merge results manually. An `EvalPipeline` abstraction that evaluates one profile at a time and writes incremental results would automate this; deferred to a follow-on issue.

---

## Files Changed

| File | Change |
|---|---|
| `eval/src/main/java/.../SourceType.java` | New enum |
| `eval/src/main/java/.../CoverageLoss.java` | New enum |
| `eval/src/main/java/.../TraitPolarity.java` | New enum |
| `eval/src/main/java/.../Attribution.java` | New enum |
| `eval/src/main/java/.../VocabularyGap.java` | New record |
| `eval/src/main/java/.../AgentProfile.java` | New record |
| `eval/src/main/java/.../VariantPair.java` | New record |
| `eval/src/main/java/.../VariantIndex.java` | New record |
| `eval/src/main/java/.../AttributionDiagnosis.java` | New record |
| `eval/src/main/java/.../ProximityResult.java` | New record |
| `eval/src/main/java/.../ProximityReport.java` | New record |
| `eval/src/main/java/.../VocabularyExpressivenessResult.java` | New record |
| `eval/src/main/java/.../TraitExpressionResult.java` | New record |
| `eval/src/main/java/.../PairContrastResult.java` | New record |
| `eval/src/main/java/.../PersonalityPreservationReport.java` | New record + static factory |
| `eval/src/main/java/.../EvalCase.java` | Replace with sealed interface |
| `eval/src/main/java/.../SyntheticEvalCase.java` | New record |
| `eval/src/main/java/.../ProfiledEvalCase.java` | New record |
| `eval/src/main/java/.../ProximityJudge.java` | New @ApplicationScoped |
| `eval/src/main/java/.../VocabularyExpressivenessJudge.java` | New @ApplicationScoped |
| `eval/src/main/java/.../TraitExpressionJudge.java` | New @ApplicationScoped |
| `eval/src/main/java/.../PairContrastJudge.java` | New @ApplicationScoped |
| `eval/src/main/java/.../EvalDataset.java` | Add `realWorld()`; update 9 factory methods → SyntheticEvalCase |
| `eval/src/main/java/.../EvalReportWriter.java` | Add proximity + preservation methods |
| `eval/src/test/java/.../AgentProfileLoader.java` | **New — test scope** |
| `eval/src/test/java/.../PromptEvalTest.java` | Add `evaluateRealWorldScenarios()`, `runReliabilityCheck()`, `@BeforeAll` |
| `eval/src/test/java/.../PromptJudgeTest.java` | Replace 8× `new EvalCase(...)` → `new SyntheticEvalCase(...)` |
| `eval/src/test/java/.../EvalReportTest.java` | Replace 1× `new EvalCase(...)` → `new SyntheticEvalCase(...)` |
| `eval/src/test/java/.../EvalReportWriterTest.java` | Replace 2× `new EvalCase(...)` → `new SyntheticEvalCase(...)` |
| `eval/src/test/resources/profiles/index.yaml` | New |
| `eval/src/test/resources/profiles/*.yaml` | New (8 profile files) |
| `eval/pom.xml` | Add `jackson-dataformat-yaml` provided-scope if not already transitive |
