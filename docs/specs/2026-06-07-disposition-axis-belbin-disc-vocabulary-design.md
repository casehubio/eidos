# Design Spec: Disposition Axis Extension + Belbin/DISC/TK Vocabulary Module

**Date:** 2026-06-07  
**Issues:** eidos#26 (Belbin/DISC vocab), eidos#38 (conflictMode axis), eidos#39 (AgentDisposition design)  
**Branch:** `issue-26-belbin-disc-vocabulary`

---

## 1. Issue Resolutions

### eidos#39 — AgentDisposition: open Map vs. fixed fields

**Decision: keep fixed fields.**

`DispositionAxis` enum is the extensibility point for axis *names*; String field values are open strings interpreted through vocabulary. The term "open-String axes" in CLAUDE.md refers to the VALUES (vocabulary-defined strings), not the axis keys.

Switching to `Map<String, String>` would eliminate the compile-time guarantee that `axisExactMatch` implementations cover all axes — the exhaustive switch over `DispositionAxis` is the forcing function that keeps vocabulary authors complete. Axis extensibility works by updating the enum + record together (breaking change, mechanically safe).

**ADR:** The rationale for this decision will be captured as a new ADR alongside implementation. The spec is an ephemeral artifact; the decision is architectural and belongs in the durable record.

### eidos#38 — conflictMode as 5th axis

**Decision: add `CONFLICT_MODE` to `DispositionAxis` and `conflictMode` to `AgentDisposition`.**

Thomas-Kilmann's five conflict modes are genuinely orthogonal to all four existing axes. Adding the axis is compile-time safe: the exhaustive switch in `AgentDisposition.get(DispositionAxis)` fails to compile when `CONFLICT_MODE` is added, forcing an immediate update; any future `axisExactMatch` using an exhaustive switch is likewise forced to cover it.

**Reconciling the "situational vs. stable" tension:** The existing `personality-frameworks.md` §3.1 classifies TK modes as "situational, not a stable trait" and lists them as "Reference only." This spec supersedes that classification. `conflictMode` captures an agent's *default* conflict approach — a stable prior, the same epistemological status as the other four axes (`socialOrient` is also situational in any given interaction; the disposition fields model typical/default behavior, not absolute constraints). The `personality-frameworks.md` §3.1 "Reference only" classification must be corrected to reflect the new vocabulary status.

Zero schema migration cost — `AgentDisposition` is stored as JSON TEXT in a single `disposition` column. Jackson ignores missing fields by default (`FAIL_ON_UNKNOWN_PROPERTIES = false` in Quarkus); existing records without `conflictMode` deserialize with `null` (correct — the field is optional).

---

## 2. API Changes (casehub-eidos-api)

### 2.1 DispositionAxis

Add `CONFLICT_MODE` as the fifth constant:

```java
public enum DispositionAxis {
    SOCIAL_ORIENTATION,
    RULE_FOLLOWING,
    RISK_APPETITE,
    AUTONOMY,
    CONFLICT_MODE;
}
```

### 2.2 AgentDisposition

Add `conflictMode` field, validation, `get()` branch, and a static Builder. `conflictMode` is optional (null = not specified); it reuses `MAX_DISPOSITION_AXIS` — same semantic bound as all other axis fields.

```java
public record AgentDisposition(
        String socialOrient,
        String ruleFollowing,
        String riskAppetite,
        String autonomy,
        String conflictMode,       // Thomas-Kilmann or compatible vocabulary; null = not specified
        boolean delegation
) {
    public AgentDisposition {
        AgentDescriptorValidator.validateOptional("socialOrient",  socialOrient,  MAX_DISPOSITION_AXIS);
        AgentDescriptorValidator.validateOptional("ruleFollowing", ruleFollowing, MAX_DISPOSITION_AXIS);
        AgentDescriptorValidator.validateOptional("riskAppetite",  riskAppetite,  MAX_DISPOSITION_AXIS);
        AgentDescriptorValidator.validateOptional("autonomy",      autonomy,      MAX_DISPOSITION_AXIS);
        AgentDescriptorValidator.validateOptional("conflictMode",  conflictMode,  MAX_DISPOSITION_AXIS);
    }

    public Optional<String> get(DispositionAxis axis) {
        return switch (axis) {
            case SOCIAL_ORIENTATION -> Optional.ofNullable(socialOrient);
            case RULE_FOLLOWING     -> Optional.ofNullable(ruleFollowing);
            case RISK_APPETITE      -> Optional.ofNullable(riskAppetite);
            case AUTONOMY           -> Optional.ofNullable(autonomy);
            case CONFLICT_MODE      -> Optional.ofNullable(conflictMode);
        };
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String socialOrient, ruleFollowing, riskAppetite, autonomy, conflictMode;
        private boolean delegation;

        public Builder socialOrient(String v)  { this.socialOrient  = v; return this; }
        public Builder ruleFollowing(String v) { this.ruleFollowing = v; return this; }
        public Builder riskAppetite(String v)  { this.riskAppetite  = v; return this; }
        public Builder autonomy(String v)      { this.autonomy      = v; return this; }
        public Builder conflictMode(String v)  { this.conflictMode  = v; return this; }
        public Builder delegation(boolean v)   { this.delegation    = v; return this; }

        public AgentDisposition build() {
            return new AgentDisposition(
                    socialOrient, ruleFollowing, riskAppetite, autonomy, conflictMode, delegation);
        }
    }
}
```

### 2.3 AgentDescriptor — per-axis vocabulary override

**Problem:** `dispositionVocabulary` is a single URI. With `CONFLICT_MODE` added, an agent will often want axes 1–4 described in `urn:casehub:vocab:disc` and `conflictMode` in `urn:casehub:vocab:thomas-kilmann`. A single URI cannot express both.

**Solution:** add `Map<DispositionAxis, String> axisVocabularies` and the resolution method `vocabUriForAxis(DispositionAxis)`.

**Resolution precedence (most specific wins):**
1. `axisVocabularies.get(axis)` — per-axis override (if map is non-null and contains the key)
2. `dispositionVocabulary` — applies to all axes
3. `domainVocabulary` — applies to all fields

If `axisVocabularies` is null, or is non-null but does not contain a key for the requested axis, fall through to `dispositionVocabulary`. Absent-key and null-map cases are equivalent.

**`vocabUriForAxis(DispositionAxis)` — the consumption API:**

```java
public Optional<String> vocabUriForAxis(DispositionAxis axis) {
    if (axisVocabularies != null) {
        String uri = axisVocabularies.get(axis);
        if (uri != null) return Optional.of(uri);
    }
    if (dispositionVocabulary != null) return Optional.of(dispositionVocabulary);
    if (domainVocabulary != null)      return Optional.of(domainVocabulary);
    return Optional.empty();
}
```

Callers that need to resolve a disposition field value through the correct vocabulary for a given axis call `vocabUriForAxis(axis)` to get the URI, then pass it to `VocabularyRegistry.resolve(uri, value)`. Without this method, callers implement the three-step precedence themselves — differently, or wrong.

**Compact constructor changes for `axisVocabularies`:**

Null values in a non-null map are caller bugs (a present key with null URI is invalid). Use `validateRequired` — not `validateOptional` — for map values to produce a diagnostic exception before `Map.copyOf` is called:

```java
if (axisVocabularies != null) {
    axisVocabularies.forEach((axis, uri) ->
        AgentDescriptorValidator.validateRequired(
            "axisVocabularies[" + axis.name() + "]", uri, MAX_VOCABULARY_URI));
    axisVocabularies = Map.copyOf(axisVocabularies);
}
```

`validateRequired(String, String, int)` already exists in `AgentDescriptorValidator` (line 34); it delegates to `validateField`, which throws `AgentValidationException` on null — unlike `validateOptional`, which returns early. No new method needed. The previous `validateOptional` would silently pass a null value then NPE in `Map.copyOf` with no field context.

**Record shape** (position: after `dispositionVocabulary`, before `slot`):

```java
public record AgentDescriptor(
        String agentId,
        String name,
        String version,
        String provider,
        String modelFamily,
        String modelVersion,
        String weightsFingerprint,
        String domainVocabulary,
        String slotVocabulary,
        String dispositionVocabulary,
        Map<DispositionAxis, String> axisVocabularies,   // NEW
        String slot,
        List<AgentCapability> capabilities,
        AgentDisposition disposition,
        String jurisdiction,
        String dataHandlingPolicy,
        String tenancyId
) { ... }
```

**Builder for AgentDescriptor:** Add `AgentDescriptor.builder()` — same pattern as `AgentDisposition.Builder`. Warranted for a 17-field record. New code in this implementation uses the builder. Existing test call sites are updated positionally (mechanical migration only).

### 2.4 JPA impact (AgentDescriptorEntity + mapper)

**Entity:** new nullable TEXT column:

```java
@Column(name = "axis_vocabularies", columnDefinition = "TEXT")
String axisVocabularies;   // JSON: {"CONFLICT_MODE":"urn:casehub:vocab:thomas-kilmann", ...}
```

**Mapper:**

```java
// toRecord:
readJson(e.axisVocabularies, new TypeReference<Map<DispositionAxis, String>>() {})
// toEntity:
e.axisVocabularies = writeJson(d.axisVocabularies());
```

`readJson(null, …)` returns `null` (existing mapper guard: `if (json == null) return null`). All existing rows have `axis_vocabularies = NULL`; they deserialize with `axisVocabularies = null` — no per-axis overrides. This is the expected common case.

**Enum key evolution constraint:** Jackson deserializes `Map<DispositionAxis, String>` keys via `DispositionAxis.valueOf(keyString)`. If a `DispositionAxis` constant is renamed or removed, persisted rows with the old key name become unloadable with `InvalidFormatException`. Since eidos#39 establishes axis-name extension (adding to the enum) as the safe extensibility path, axis *renaming* requires a DB migration step — not just a code change. Document this in `docs/operations.md` under an "Axis Evolution" section (create the file if it does not exist).

**Base migration:** add `axis_vocabularies TEXT` to the initial schema SQL. No existing installations — update in place.

---

## 3. New Vocabulary Enums (casehub-eidos-vocab)

### 3.1 BelbinTerm — slot vocabulary

**URI:** `urn:casehub:vocab:belbin`  
**Type:** slot vocabulary — no `axisExactMatch` or `exactMatch` overrides  
**No exactMatch:** Belbin Associates has not released canonical semantic web URIs

| Enum constant | value | label | description | aliases |
|--------------|-------|-------|-------------|---------|
| PLANT | `plant` | Plant | Creative, unorthodox problem-solver; generates novel ideas independently | `["pl"]` |
| RESOURCE_INVESTIGATOR | `resource-investigator` | Resource Investigator | Extrovert who explores external opportunities and develops contacts | `["ri"]` |
| CO_ORDINATOR | `co-ordinator` | Co-ordinator | Clarifies goals, promotes team decision-making, delegates effectively | `["co"]` |
| SHAPER | `shaper` | Shaper | Challenges the team to improve; driven, dynamic, thrives under pressure | `["sh"]` |
| MONITOR_EVALUATOR | `monitor-evaluator` | Monitor Evaluator | Sober, strategic, discerning; sees all options and judges accurately | `["me"]` |
| TEAMWORKER | `teamworker` | Teamworker | Cooperative, perceptive, diplomatic; averts friction and builds cohesion | `["tw"]` |
| IMPLEMENTER | `implementer` | Implementer | Disciplined, reliable, efficient; turns ideas into practical actions | `["imp"]` |
| COMPLETER_FINISHER | `completer-finisher` | Completer Finisher | Painstaking, conscientious, anxious; ensures delivery to standard | `["cf"]` |
| SPECIALIST | `specialist` | Specialist | Dedicated, self-starting, single-minded; provides rare knowledge | `["sp"]` |

**Registrar:** `BelbinVocabRegistrar` (`@ApplicationScoped VocabularyRegistrar`).

**Belbin conflictMode — design decision:** Belbin roles describe team contribution, not conflict approach. Reverse Belbin→TK mappings would conflate role semantics with conflict-mode semantics. No `axisExactMatch` for any axis in `BelbinTerm`; the `personality-frameworks.md` Belbin reference table will show `—` in the conflictMode column with a note.

---

### 3.2 DiscTerm — disposition vocabulary

**URI:** `urn:casehub:vocab:disc`  
**Type:** disposition vocabulary — uses `axisExactMatch(Class<?> targetVocab, DispositionAxis axis)`  
**Two target vocabularies:** `ConscientiousnessTerm` (axes 1–4) and `ThomasKilmannTerm` (CONFLICT_MODE)

**Naming:** DISC is an acronym (like SVO), so `DiscVocabRegistrar` follows the same convention as `SvoVocabRegistrar` — no abbreviation inconsistency.

**Per-constant implementation:** each of the four constants overrides `axisExactMatch` via the **anonymous subclass pattern** — the same pattern used in `SvoTerm` and `CasehubSlotTerm`. The method body shown below is the template for *each constant's* override, not a shared class-level dispatch method:

```java
// Template — repeated inside each constant's anonymous class body:
@Override
public Optional<VocabularyTerm> axisExactMatch(Class<?> targetVocab, DispositionAxis axis) {
    if (targetVocab == ConscientiousnessTerm.class) {
        return switch (axis) {          // exhaustive — new DispositionAxis breaks compile
            case SOCIAL_ORIENTATION -> Optional.of(/* constant-specific */);
            case RULE_FOLLOWING     -> Optional.of(/* constant-specific */);
            case RISK_APPETITE      -> Optional.of(/* constant-specific */);
            case AUTONOMY           -> Optional.of(/* constant-specific */);
            case CONFLICT_MODE      -> Optional.empty();  // DISC has no Conscientiousness conflict mapping
        };
    }
    if (targetVocab == ThomasKilmannTerm.class) {
        return switch (axis) {          // exhaustive
            case CONFLICT_MODE      -> Optional.of(/* constant-specific TK mode */);
            case SOCIAL_ORIENTATION,
                 RULE_FOLLOWING,
                 RISK_APPETITE,
                 AUTONOMY           -> Optional.empty();
        };
    }
    return Optional.empty();   // any other targetVocab: no mapping defined
}
```

**DISC→TK mapping rationale:** DISC is defined by assertiveness (D/i high; S/C low) and cooperativeness (i/S high; D/C low). TK's conflict modes occupy the same two-dimensional space. The mapping follows directly:

**COMPROMISING has no DISC equivalent:** COMPROMISING occupies the center of TK's assertiveness×cooperativeness space (moderate/moderate). No DISC quadrant corresponds to that center — DISC describes four corner quadrants. This is a property of TK's geometry, not a gap in the mapping.

| DiscTerm | CONFLICT_MODE → ThomasKilmannTerm | Evidence |
|----------|-----------------------------------|---------|
| DOMINANCE | COMPETING | High assertiveness, low cooperativeness |
| INFLUENCE | COLLABORATING | High assertiveness, high cooperativeness |
| STEADINESS | ACCOMMODATING | Low assertiveness, high cooperativeness |
| CONSCIENTIOUSNESS_DISC | AVOIDING | Low assertiveness, low cooperativeness |

| Enum constant | value | label | description | aliases |
|--------------|-------|-------|-------------|---------|
| DOMINANCE | `dominance` | Dominance | Results-driven, direct, decisive; prioritises outcomes over relationships | `["D"]` |
| INFLUENCE | `influence` | Influence | Enthusiastic, optimistic, collaborative; motivates and involves others | `["i"]` |
| STEADINESS | `steadiness` | Steadiness | Patient, reliable, supportive; values stability and consistency | `["S"]` |
| CONSCIENTIOUSNESS_DISC | `conscientiousness-disc` | Analytical (DISC-C) | Analytical, systematic, quality-focused; emphasises accuracy | `["C"]` |

Label **"Analytical (DISC-C)"** avoids confusion with the Conscientiousness vocabulary name in rendered output.

Full axisExactMatch mapping:

| DiscTerm | SOCIAL_ORIENTATION | RULE_FOLLOWING | RISK_APPETITE | AUTONOMY | CONFLICT_MODE |
|----------|-------------------|----------------|---------------|----------|---------------|
| DOMINANCE | `INDEPENDENT` | `FLEXIBLE` | `BOLD` | `AUTONOMOUS` | `COMPETING` |
| INFLUENCE | `COLLABORATIVE` | `FLEXIBLE` | `MEASURED` | `SEMI_AUTONOMOUS` | `COLLABORATING` |
| STEADINESS | `FACILITATIVE` | `PRINCIPLED` | `CONSERVATIVE` | `DIRECTED` | `ACCOMMODATING` |
| CONSCIENTIOUSNESS_DISC | `INDEPENDENT` | `STRICT` | `CONSERVATIVE` | `SEMI_AUTONOMOUS` | `AVOIDING` |

Columns 1–4: `ConscientiousnessTerm` constants. CONFLICT_MODE: `ThomasKilmannTerm` constants. Any other `targetVocab`: `Optional.empty()`.

**Registrar:** `DiscVocabRegistrar`.

---

### 3.3 ThomasKilmannTerm — conflict mode vocabulary

**URI:** `urn:casehub:vocab:thomas-kilmann`  
**Type:** disposition vocabulary providing values for the `CONFLICT_MODE` axis  
**No `axisExactMatch` overrides:** Reverse TK→Conscientiousness mappings exist for only 2 of 5 modes (COLLABORATING→COLLABORATIVE, ACCOMMODATING→FACILITATIVE are strong; AVOIDING, COMPROMISING, COMPETING have no clean mapping). Implementing `axisExactMatch` with partial coverage would require `Optional.empty()` for the unmapped modes — which compiles but means the method is misleadingly present for a vocabulary that is primarily a resolution *target*. TK modes serve as resolution targets accessed via `vocabUriForAxis(CONFLICT_MODE)`, not as cross-vocab sources.

Kept separate from `ConscientiousnessTerm` because ConscientiousnessTerm is Big Five-grounded; TK is conflict-mode theory. Mixing them under `urn:casehub:vocab:conscientiousness` would make the URI semantically misleading.

| Enum constant | value | label | description | aliases |
|--------------|-------|-------|-------------|---------|
| COMPETING | `competing` | Competing | High assertiveness, low cooperativeness; pursues own position in conflict | `["competitive"]` |
| COLLABORATING | `collaborating` | Collaborating | High assertiveness, high cooperativeness; seeks joint problem-solving | `["cooperative"]` |
| COMPROMISING | `compromising` | Compromising | Moderate assertiveness and cooperativeness; neither fully assertive nor yielding | `["compromise"]` |
| AVOIDING | `avoiding` | Avoiding | Low assertiveness, low cooperativeness; sidesteps conflict | `["avoidant"]` |
| ACCOMMODATING | `accommodating` | Accommodating | Low assertiveness, high cooperativeness; yields to others' concerns | `["deferring"]` |

**Near-collision note:** `COLLABORATING.value = "collaborating"` vs `ConscientiousnessTerm.COLLABORATIVE.value = "collaborative"` — different strings (suffix -ing vs -ive), different registry namespaces. No collision; both are safe.

**Registrar:** `ThomasKilmannVocabRegistrar` (long form, consistent with `ThomasKilmannTerm`).

**Naming correction in existing code:** `ConsciousnessVocabRegistrar` has a spelling error — rename to `ConscientiousnessVocabRegistrar` as part of this work via IntelliJ rename refactor.

---

## 4. File Changes

**casehub-eidos-api:**
```
api/src/main/java/io/casehub/eidos/api/
├── DispositionAxis.java          (add CONFLICT_MODE)
├── AgentDisposition.java         (add conflictMode field + Builder)
└── AgentDescriptor.java          (add axisVocabularies + vocabUriForAxis() + Builder)
```

**casehub-eidos-runtime:**
```
runtime/src/main/java/io/casehub/eidos/runtime/registry/jpa/
├── AgentDescriptorEntity.java    (add axis_vocabularies TEXT column)
└── AgentDescriptorMapper.java    (read/write axisVocabularies)
runtime/src/main/resources/db/eidos/migration/
└── V<N>__initial_schema.sql      (add axis_vocabularies TEXT — base migration update)
```

**casehub-eidos-vocab:**
```
vocab/src/main/java/io/casehub/eidos/vocab/
├── BelbinTerm.java                        (new)
├── BelbinVocabRegistrar.java              (new)
├── DiscTerm.java                          (new)
├── DiscVocabRegistrar.java                (new)
├── ThomasKilmannTerm.java                 (new)
├── ThomasKilmannVocabRegistrar.java       (new)
├── ConscientiousnessVocabRegistrar.java   (renamed from ConsciousnessVocabRegistrar)
└── ConsciousnessVocabRegistrar.java       (deleted)
```

**Documentation:**
```
docs/personality-frameworks.md   (multiple section updates — see §6)
```

---

## 5. Test Coverage

### AgentDisposition (api module)
- `conflictMode=null` → valid (optional field)
- `conflictMode=" "` (blank) → `AgentValidationException`
- `conflictMode` over 200 chars → `AgentValidationException`
- `get(CONFLICT_MODE)` on populated record → `Optional.of(value)`
- `get(CONFLICT_MODE)` with null → `Optional.empty()`
- `DispositionAxis.values().length == 5` — guard that catches undeclared axis count drift
- **Old JSON round-trip:** deserialize `{"socialOrient":"independent","ruleFollowing":"strict","riskAppetite":"conservative","autonomy":"semi-autonomous","delegation":false}` into new record → `conflictMode == null`; then serialize the new record and deserialize back → equals original

### AgentDescriptor (api module)
- `axisVocabularies=null` → valid (no per-axis overrides)
- `axisVocabularies` with a null value for a key → `AgentValidationException` with field name `"axisVocabularies[CONFLICT_MODE]"` (not NPE)
- `axisVocabularies` with a blank URI value → `AgentValidationException`
- `axisVocabularies` with a URI over 500 chars → `AgentValidationException`
- `axisVocabularies` is unmodifiable after construction
- Builder produces equivalent result to positional constructor
- **`vocabUriForAxis` — all cases:**
  - key present in `axisVocabularies` → returns that URI
  - key absent from non-null `axisVocabularies`, `dispositionVocabulary` set → returns `dispositionVocabulary`
  - `axisVocabularies=null`, `dispositionVocabulary` set → returns `dispositionVocabulary`
  - `axisVocabularies=null`, `dispositionVocabulary=null`, `domainVocabulary` set → returns `domainVocabulary`
  - all three null → `Optional.empty()`

### BelbinTerm (vocab module)
- All 9 constants registered and resolvable by value
- Alias resolution: "pl" → PLANT, "ri" → RESOURCE_INVESTIGATOR, etc.
- `axisExactMatch` inherits default empty for all axes and all targetVocabs
- `exactMatch` inherits default empty

### DiscTerm (vocab module)
- All 4 constants registered and resolvable by value and alias
- **axisExactMatch — ConscientiousnessTerm target:** all 4 types × 4 non-conflict axes return correct `ConscientiousnessTerm`
- **axisExactMatch — CONFLICT_MODE → ConscientiousnessTerm:** `Optional.empty()` for all 4 types
- **axisExactMatch — ThomasKilmannTerm target:** D→COMPETING, i→COLLABORATING, S→ACCOMMODATING, C→AVOIDING
- **axisExactMatch — non-conflict axes → ThomasKilmannTerm:** `Optional.empty()` for all 4 types
- **axisExactMatch — unknown targetVocab** (e.g., `SvoTerm.class`): `Optional.empty()` for all 4 types and all axes
- Registry `equivalentValues(fromUri, value, toUri, axis)` end-to-end: D → RISK_APPETITE → bold (Conscientiousness path)
- Registry `equivalentValues(fromUri, value, toUri, axis)` end-to-end: D → CONFLICT_MODE → competing (TK path)
- Axis-unaware `equivalentValues(fromUri, value, toUri)` → empty (DISC only implements `axisExactMatch`, not `exactMatch`)

### ThomasKilmannTerm (vocab module)
- All 5 constants registered and resolvable by value
- "competitive" → COMPETING, "avoidant" → AVOIDING, "deferring" → ACCOMMODATING, "compromise" → COMPROMISING
- `axisExactMatch` inherits default empty
- **Regression:** all `ConscientiousnessTerm` values remain resolvable after TK registration (guards against alias collision)

### AgentDescriptor JPA round-trip (runtime module)
- Persist + reload descriptor with `axisVocabularies = Map.of(CONFLICT_MODE, ThomasKilmannTerm.URI)` → round-trip equals original
- Persist + reload descriptor with `axisVocabularies = null` → round-trip equals original (column is NULL, reads back as null)

### Cross-vocab integration (examples module or vocab module)
- Belbin+Conscientiousness descriptor: construction and `get(axis)` for each axis
- DISC+TK descriptor: `vocabUriForAxis(SOCIAL_ORIENTATION)` returns disc URI; `vocabUriForAxis(CONFLICT_MODE)` returns thomas-kilmann URI
- `VocabularyRegistry.resolve(descriptor.vocabUriForAxis(CONFLICT_MODE).orElseThrow(), "competing")` → `Optional.of(ThomasKilmannTerm.COMPETING)` (assert with `hasValue(ThomasKilmannTerm.COMPETING)`; the Optional is not unwrapped)

---

## 6. Documentation Updates — personality-frameworks.md

### Currently incorrect sections (must be corrected, not just updated)

**§3.1 TK Vocabulary Role:** Replace "Reference only. TK modes describe conflict strategy (situational), not social preference (stable trait)" with: TK modes provide values for the `conflictMode` disposition axis via `urn:casehub:vocab:thomas-kilmann`. An agent's default conflict approach is treated as a stable prior, consistent with how all other axes are used.

**§3 Design Guide — Axis Assignment Errors, entry 6:** Remove "leave TK Competing, Avoiding, Accommodating, Compromising unmapped until eidos#38." Replace with resolved guidance: set `conflictMode` via `urn:casehub:vocab:thomas-kilmann` values; for CONFLICT_MODE, prefer the dedicated axis over mapping to socialOrient.

**§ Implementation Notes for eidos#26:** This section references the **old, incorrect** pre-eidos#40 API (`@Produces Vocabulary`, `Vocabulary(String, String, String, Map<String, VocabularyTerm>)` constructor, `VocabularyTerm` as a data class). That API does not exist. Replace entirely:

> Each vocabulary is a Java enum implementing `VocabularyTerm`, annotated with `@VocabularyMetadata(uri=..., name=..., version=...)`. A companion `@ApplicationScoped` class implementing `VocabularyRegistrar` returns the enum class via `vocabulary()`. `CdiVocabularyRegistry` discovers all registrars at startup via `Instance<VocabularyRegistrar>`. See `ConscientiousnessTerm` + `ConscientiousnessVocabRegistrar` as the canonical reference implementation.

### Sections to add or update

**DISC mapping table:** Add `conflictMode` column — D→competing, i→collaborating, S→accommodating, C→avoiding. Add COMPROMISING row with `—` for DISC column and note: no DISC quadrant occupies the center of TK's assertiveness×cooperativeness space.

**Belbin implied-disposition table:** Add `conflictMode` column (all `—`). Add footnote: "Belbin roles describe team contribution; Belbin→TK cross-vocabulary mappings would conflate role semantics with conflict-mode semantics. No such mapping is defined."

**New vocabulary draft table:** ThomasKilmannTerm table in same format as Belbin/DISC tables.

**Vocabulary Gap Notes:** Remove TK conflict modes entry (resolved). Add: "The COMPROMISING mode has no DISC equivalent — it occupies the center of TK's two-dimensional space, which DISC's four quadrant types do not cover."

**Combination patterns — all three templates:** Add `conflictMode` field and `axisVocabularies` guidance. Worked examples use pseudo-code notation consistent with the existing doc style (not Java); actual construction uses `Map.of(CONFLICT_MODE, "urn:casehub:vocab:thomas-kilmann")` passed to the builder.

Example for Belbin+DISC Profile (pseudo-code):
```
slotVocabulary        = "urn:casehub:vocab:belbin"
dispositionVocabulary = "urn:casehub:vocab:disc"
axisVocabularies      = {CONFLICT_MODE → "urn:casehub:vocab:thomas-kilmann"}
slot                  = "co-ordinator"
disposition.socialOrient  = "dominance"
disposition.ruleFollowing = "dominance"
disposition.riskAppetite  = "dominance"
disposition.autonomy      = "dominance"
disposition.conflictMode  = "competing"
disposition.delegation    = true
```

---

## 7. Issue Closure

- **eidos#39** — closed by ADR documenting the fixed-fields-vs-open-map decision
- **eidos#38** — closed by `CONFLICT_MODE` addition to DispositionAxis + AgentDisposition + ThomasKilmannTerm vocabulary
- **eidos#26** — closed by Belbin + DISC + TK vocabulary enum implementation

---

## 8. Breaking Changes

### AgentDisposition — new 6th positional parameter

The positional constructor changes from `(String, String, String, String, boolean)` to `(String, String, String, String, String, boolean)`. All ~30 existing call sites must pass `null` as the 5th argument:

```java
// Before:
new AgentDisposition("collaborative", "principled", "measured", "semi-autonomous", false)
// After:
new AgentDisposition("collaborative", "principled", "measured", "semi-autonomous", null, false)
```

New call sites in this implementation use `AgentDisposition.builder()`. Existing test sites updated positionally.

### AgentDescriptor — new `axisVocabularies` field

Positional constructor gains one field at position 11 (after `dispositionVocabulary`, before `slot`). All call sites pass `null`. The mapper's `toRecord()` uses the Builder.

### AgentDescriptor — `List.copyOf(capabilities)` immutability strengthening

The existing compact constructor assigned the input list directly (`capabilities != null ? capabilities : List.of()`). This spec changes it to `List.copyOf(capabilities)` — a defensive copy that produces an unmodifiable list and rejects null elements. This is a deliberate strengthening of the immutability invariant. Any call site that retains a reference to the original list and mutates it after construction will no longer see the mutation; any list containing a null element will now throw `NullPointerException` at construction. Neither is a supported usage pattern, but both were previously legal.

### Compile-time forcing function

Adding `CONFLICT_MODE` to `DispositionAxis` breaks `AgentDisposition.get(DispositionAxis)` at compile time. Fix it before patching call sites — the compiler identifies every affected location.

---

## 9. Out of Scope and Deferred

- **`AgentQuery` filtering by `conflictMode`** — disposition is JSON TEXT; JPA path queries required. Deferred.
- **Thomas-Kilmann assertiveness/cooperativeness facets** as separate fields — single `conflictMode` string is sufficient.
- **Axis key evolution / renaming** — renaming a `DispositionAxis` constant requires a DB migration to update persisted `axis_vocabularies` JSON keys. Document in `docs/operations.md` (Axis Evolution section); not in scope for this branch.
- **Migrating all existing `AgentDescriptor` call sites to Builder** — encouraged but deferred. A follow-up GitHub issue will be created before this branch closes to track the migration.
