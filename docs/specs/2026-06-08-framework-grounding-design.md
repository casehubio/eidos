# Framework Grounding in AgentDescriptor and SystemPromptRenderer

**Issue:** eidos#27  
**Date:** 2026-06-08  
**Status:** Approved

---

## Problem

`EidosRenderPipeline` sends disposition axis values as raw open strings to the LLM
(`"socialOrient": "independent"`) with no indication of which theoretical framework
the value comes from. The LLM cannot produce Belbin-canonical, DISC-canonical, or
Thomas-Kilmann-canonical language because it has no grounding signal.

Three concrete gaps:

1. Slot vocabulary name is not included in the LLM payload — the LLM sees
   `slotLabel: "Monitor Evaluator"` but not `"Belbin Team Roles"`.
2. Disposition axis values are raw strings — no vocabulary resolution, no labels, no
   framework names.
3. `conflictMode` is completely absent from both the LLM payload and structural renderers
   despite being a field on `AgentDisposition` and a value in `DispositionAxis`.

The structural `assembleProse()` path has a further pre-existing bug: it renders only
`ruleFollowing` and `autonomy`, silently dropping `socialOrient`, `riskAppetite`, and
`conflictMode`.

---

## Decision: Option C — VocabularyMetadata enrichment + registry lookup + pipeline enrichment

Framework context belongs in the vocabulary layer, not on `AgentDescriptor`. The descriptor
stays at 17 fields. Grounding is expressed once at the vocabulary source and flows to all
consumers via the registry and pipeline.

---

## Changes

### 1. `@VocabularyMetadata` — add `description()`

```java
public @interface VocabularyMetadata {
    String uri();
    String name()        default "";
    String version()     default "";
    String description() default "";  // empirical basis / scope narrative, 1–3 sentences
}
```

Empty string means "not provided" (consistent with existing `name` convention).

### 2. `VocabularyRegistry` SPI — add `vocabularyMetadata()`

```java
/** Returns vocabulary-level metadata for the given URI. Empty if not registered. */
Optional<VocabularyMetadata> vocabularyMetadata(String uri);
```

On the SPI (not internal) because any consumer may need to surface framework provenance,
not just the pipeline.

**`CdiVocabularyRegistry` implementation:**

```java
@Override
public Optional<VocabularyMetadata> vocabularyMetadata(String uri) {
    var clazz = byUri.get(uri);
    if (clazz == null) return Optional.empty();
    // register() guarantees @VocabularyMetadata is present for anything in byUri —
    // Optional.of() is correct; ofNullable would contradict the registration invariant.
    return Optional.of(clazz.getAnnotation(VocabularyMetadata.class));
}
```

### 3. `EidosRenderPipeline` — new `addIfNonBlank` helper

Java annotation attributes return `""` (not `null`) when unset. The existing `addIfPresent`
null-checks would emit `"vocabularyName": ""` into the payload for any vocabulary that
omits those fields, polluting the LLM context with empty-string signals.

Add:

```java
private static void addIfNonBlank(final ObjectNode node, final String key, final String value) {
    if (value != null && !value.isEmpty()) node.put(key, value);
}
```

Use `addIfNonBlank` for all annotation-sourced fields (vocabulary name, vocabulary
description) and for `term.description()` (which also defaults to `""`). The existing
`addIfPresent` remains correct for fields that are truly nullable (e.g. raw String fields
on the descriptor).

### 4. `EidosRenderPipeline` — slot section

The vocabulary metadata lookup must be inside the existing
`if (descriptor.slotVocabulary() != null)` null guard — `ConcurrentHashMap.get(null)`
throws `NullPointerException`. Full replacement of the slot block:

```java
if (descriptor.slotVocabulary() != null) {
    vocab.resolve(descriptor.slotVocabulary(), descriptor.slot()).ifPresent(term -> {
        addIfPresent(node,  "slotLabel",       term.label());
        addIfNonBlank(node, "slotDescription", term.description());  // changed from addIfPresent
    });
    // NEW: vocabulary-level context
    vocab.vocabularyMetadata(descriptor.slotVocabulary()).ifPresent(meta -> {
        addIfNonBlank(node, "slotVocabularyName",        meta.name());
        addIfNonBlank(node, "slotVocabularyDescription", meta.description());
    });
}
```

### 5. `EidosRenderPipeline` — disposition section (full replacement)

Replace the flat-string disposition block (currently inside
`if (descriptor.disposition() != null)`) with a loop over `DispositionAxis.values()`.
The full replacement — including the outer null-check and `d` assignment — is:

```java
if (descriptor.disposition() != null) {
    final AgentDisposition d = descriptor.disposition();
    final ObjectNode dispNode = node.putObject("disposition");
    for (DispositionAxis axis : DispositionAxis.values()) {
        d.get(axis).ifPresent(rawValue -> {
            final ObjectNode axisNode = dispNode.putObject(axisJsonKey(axis));
            axisNode.put("value", rawValue);
            descriptor.vocabUriForAxis(axis).ifPresent(uri -> {
                vocab.resolve(uri, rawValue).ifPresent(term -> {
                    addIfPresent(axisNode,  "label",       term.label());
                    addIfNonBlank(axisNode, "description", term.description());
                });
                vocab.vocabularyMetadata(uri).ifPresent(meta -> {
                    addIfNonBlank(axisNode, "vocabularyName",        meta.name());
                    addIfNonBlank(axisNode, "vocabularyDescription", meta.description());
                });
            });
        });
    }
    dispNode.put("canDelegate", d.delegation());
}
```

This fixes `conflictMode` omission automatically and makes future axis additions
zero-touch in the pipeline.

`axisJsonKey(DispositionAxis)` maps to camelCase JSON keys via an exhaustive switch with
no default branch — consistent with `AgentDisposition.get()` and the `axisExactMatch`
Javadoc pattern. Adding a new `DispositionAxis` value then causes a compile error here,
forcing explicit coverage:

| Axis | JSON key |
|------|----------|
| `SOCIAL_ORIENTATION` | `socialOrient` |
| `RULE_FOLLOWING` | `ruleFollowing` |
| `RISK_APPETITE` | `riskAppetite` |
| `AUTONOMY` | `autonomy` |
| `CONFLICT_MODE` | `conflictMode` |

When no vocab URI is available for an axis, the object has only `"value"` — no label or
vocab fields. This is correct: unregistered vocabularies get no enrichment.

**Resulting payload shape (example):**

```json
"disposition": {
  "socialOrient": {
    "value": "independent",
    "label": "Independent",
    "description": "Works alone by preference",
    "vocabularyName": "Conscientiousness Disposition Axes",
    "vocabularyDescription": "An operational axis vocabulary..."
  },
  "conflictMode": {
    "value": "avoiding",
    "label": "Avoiding",
    "vocabularyName": "Thomas-Kilmann Conflict Modes",
    "vocabularyDescription": "Five conflict-handling modes..."
  },
  "canDelegate": false
}
```

### 6. `PROMPT_TEMPLATE` and `RESPONSE_FORMAT` — disposition updates

Both the prompt instruction and the JSON schema description are updated together.
Leaving either stale would create conflicting signals for the LLM.

**`PROMPT_TEMPLATE` — `dispositionNarrative` instruction (full replacement):**

Before:
```
- dispositionNarrative (1-2 sentences): How the agent operates - autonomy,
  rule-following orientation, delegation authority.
```

After:
```
- dispositionNarrative (2-3 sentences): How the agent operates across all disposition
  axes present in the payload. The disposition object contains one nested object per axis;
  each has a "value" field and optionally "label", "description", and "vocabularyName".
  Cover all axes that have values: socialOrient, ruleFollowing, riskAppetite, autonomy,
  conflictMode. When "vocabularyName" is present, use that framework's canonical language
  rather than generic phrasing — e.g., "vocabularyName: Thomas-Kilmann Conflict Modes"
  → use TKI mode language; "vocabularyName: DISC Behavioral Styles" → use DISC canonical
  phrasing. Include delegation intent if canDelegate is true.
  Use "" if no disposition is present.
```

**`PROMPT_TEMPLATE` — `roleNarrative` instruction (append):**

```
  If slotVocabularyName is present, use that framework's canonical language —
  e.g., slotVocabularyName "Belbin Team Roles" → open with the Belbin archetype
  framing ("You are the team's Monitor Evaluator...").
```

**`RESPONSE_FORMAT` — `dispositionNarrative` schema description (replacement):**

Before:
```java
.addStringProperty("dispositionNarrative",
    "How the agent operates. Empty string if no disposition data.")
```

After:
```java
.addStringProperty("dispositionNarrative",
    "How the agent operates across all disposition axes in the payload. " +
    "Each axis object carries value, optional label, optional vocabularyName. " +
    "Use framework canonical language when vocabularyName is present. " +
    "2-3 sentences. Empty string if no disposition data.")
```

### 7. Structural renderers — `assembleMarkdownStructural()` and `assembleProse()`

Both methods replace their axis-by-axis if-blocks with a `DispositionAxis.values()` loop
using a shared private helper. `axisLabel(DispositionAxis)` also uses an exhaustive switch
with no default branch:

| Axis | Display label |
|------|---------------|
| `SOCIAL_ORIENTATION` | `Social orientation` |
| `RULE_FOLLOWING` | `Rule following` |
| `RISK_APPETITE` | `Risk appetite` |
| `AUTONOMY` | `Autonomy` |
| `CONFLICT_MODE` | `Conflict mode` |

**Shared helper:**

```java
private String resolveAxisDisplay(DispositionAxis axis, String raw,
                                   AgentDescriptor descriptor) {
    Optional<String> vocabUri = descriptor.vocabUriForAxis(axis);
    String label = vocabUri
        .flatMap(uri -> vocab.resolve(uri, raw))
        .map(VocabularyTerm::label)
        .orElse(raw);
    String vocabName = vocabUri
        .flatMap(uri -> vocab.vocabularyMetadata(uri))
        .map(VocabularyMetadata::name)
        .filter(n -> !n.isEmpty())
        .orElse(null);
    return vocabName != null ? label + " (" + vocabName + ")" : label;
}
```

**`assembleMarkdownStructural()` — disposition block:**

Before:
```
- Social orientation: independent
- Rule following: strict
- Risk appetite: conservative
- Autonomy: semi-autonomous
- Can delegate: no
[conflictMode silently absent]
```

After:
```
- Social orientation: Independent (Conscientiousness Disposition Axes)
- Rule following: Strict Rule Following (Conscientiousness Disposition Axes)
- Risk appetite: Conservative Risk (Conscientiousness Disposition Axes)
- Autonomy: Semi-Autonomous (Conscientiousness Disposition Axes)
- Conflict mode: Avoiding (Thomas-Kilmann Conflict Modes)
- Can delegate: no
```

Full replacement block (the outer null-check, `d` assignment, and `## How You Operate`
heading are preserved; only the axis-by-axis if-blocks are replaced):

```java
if (descriptor.disposition() != null) {
    final AgentDisposition d = descriptor.disposition();
    sb.append("\n## How You Operate\n");
    for (DispositionAxis axis : DispositionAxis.values()) {
        d.get(axis).ifPresent(raw ->
            sb.append("- ").append(axisLabel(axis)).append(": ")
              .append(resolveAxisDisplay(axis, raw, descriptor)).append("\n"));
    }
    sb.append("- Can delegate: ").append(d.delegation() ? "yes" : "no").append("\n");
}
```

**`assembleProse()` — disposition block:**

The existing path (lines 423–429) renders only `ruleFollowing` and `autonomy` as inline
prose — a pre-existing bug. "Dense prose, no headers" means everything on one line.

Before:
```
Operating style: strict rule-following. Autonomy: semi-autonomous. Can delegate: no.
```

After (single continuous line; wrapped here for readability):
```
Operating style: Social orientation: Independent (Conscientiousness Disposition Axes). Rule following: Strict Rule Following (Conscientiousness Disposition Axes). Risk appetite: Conservative Risk (Conscientiousness Disposition Axes). Autonomy: Semi-Autonomous (Conscientiousness Disposition Axes). Conflict mode: Avoiding (Thomas-Kilmann Conflict Modes). Can delegate: no.
```

Full replacement block (the outer null-check and `d` assignment are preserved; only the
axis-by-axis if-blocks are replaced). `sb.append(" ")` between axes produces the
single-line dense-prose format:

```java
if (descriptor.disposition() != null) {
    final AgentDisposition d = descriptor.disposition();
    sb.append("\nOperating style:");
    for (DispositionAxis axis : DispositionAxis.values()) {
        d.get(axis).ifPresent(raw ->
            sb.append(" ").append(axisLabel(axis)).append(": ")
              .append(resolveAxisDisplay(axis, raw, descriptor)).append("."));
    }
    sb.append(" Can delegate: ").append(d.delegation() ? "yes" : "no").append(".\n");
}
```

### 8. Vocabulary enum `@VocabularyMetadata` descriptions

`name` fields are unchanged. Only `description` is added to each.

| Enum | `name` (unchanged) | `description` (new) |
|------|-------------------|---------------------|
| `SvoTerm` | `"SVO Roles"` | "A simplified three-role model (Coordinator, Performer, Evaluator) for agent function in multi-agent workflows. Derived from Subject-Verb-Object role theory. Intended as a lightweight slot vocabulary." |
| `ConscientiousnessTerm` | `"Conscientiousness Disposition Axes"` | "An operational axis vocabulary for agent behavioral disposition, grounded in Big Five Conscientiousness research. Covers rule-following, risk appetite, social orientation, and autonomy as independent dimensions." |
| `CasehubSlotTerm` | `"CaseHub Slot Roles"` | "CaseHub's native slot vocabulary defining four platform-standard roles: Planner, Executor, Reviewer, Supervisor. Use when an external team-role framework is not required." |
| `BelbinTerm` | `"Belbin Team Roles"` | "Nine complementary team-role archetypes developed by Meredith Belbin from observational research at Henley Management College (1981). Roles describe what a person contributes to a team's function. Medium scientific validity; widely adopted in UK and EU management development." |
| `DiscTerm` | `"DISC Behavioral Styles"` | "A four-quadrant behavioral style model (Dominance, Influence, Steadiness, Conscientiousness-DISC) used as a disposition shorthand. Correlates with Big Five Extraversion × Agreeableness. Low independent scientific validity, but bounded imprecision makes it usable in practice." |
| `ThomasKilmannTerm` | `"Thomas-Kilmann Conflict Modes"` | "Five conflict-handling modes from the Thomas-Kilmann Conflict Mode Instrument, based on the assertiveness × cooperativeness framework. Widely adopted in management and applied psychology. Maps to the CONFLICT_MODE disposition axis." |

---

## Design notes

**Unresolved terms still show vocabulary name.** `resolveAxisDisplay` produces
`"raw-value (VocabName)"` when a vocabulary URI resolves but the raw value has no
matching term. The payload builder (Change 5) behaves identically — `vocabularyName`
is added regardless of whether the term resolved. This is intentional: the vocabulary
signals the framework; unresolved values represent custom terms or data errors, and the
LLM context still benefits from knowing which framework applies.

**`vocabularyDescription` is in the payload but not named in the PROMPT_TEMPLATE
instruction.** The LLM will see and use it as background context without explicit
instruction. This is sufficient for well-known frameworks (Belbin, DISC, TK) where the
LLM has training knowledge. If a fully custom vocabulary requires description-driven
grounding, the instruction should be updated at that point.

---

## Cache behaviour

Two distinct invalidation scopes:

1. **Descriptor hash** — `buildDescriptorPayload()` output changes (disposition is now a
   richer nested structure; slot section adds vocabulary name/description). The descriptor
   hash changes for any descriptor with disposition or a registered slot vocabulary.

2. **Full cache cold-start** — `PROMPT_TEMPLATE` changes in Section 6, which changes
   `TEMPLATE_HASH`. Because `TEMPLATE_HASH` is part of every cache key:
   ```
   descriptorHash + ":" + contextHash + ":" + format.name() + ":" + TEMPLATE_HASH
   ```
   every cache entry is invalidated, regardless of whether the descriptor has disposition
   set. This is correct — stale entries from the old prompt would produce wrongly-shaped
   prompts against the new payload structure.

Both scopes follow GE-20260528-e9ed9f (cache key must hash all output-affecting context).

---

## What does NOT change

- `AgentDescriptor` fields (17 fields, unchanged)
- `AgentDisposition` fields
- `DispositionAxis` enum values
- `VocabularyTerm` interface
- JPA entity or mapper
- A2A card format — deferred to eidos#45

---

## Tests

**`CdiVocabularyRegistryTest`:**
- `vocabularyMetadata_registered_uri_returns_annotation()` — assert `name()`, `description()` match the annotation
- `vocabularyMetadata_unregistered_uri_returns_empty()`

**`EidosRenderPipelineTest`:**
- `disposition_payload_is_nested_object_per_axis()` — Conscientiousness vocab registered; assert each axis is a nested object with `value`, `label`, `vocabularyName`
- `conflict_mode_included_in_payload_when_set()` — previously silently omitted
- `disposition_without_registered_vocab_has_value_only()` — unregistered URI → `{"value": "custom-value"}` only, no label/vocab fields
- `empty_vocab_name_not_emitted_in_payload()` — vocab with `name=""` → `vocabularyName` key absent from payload (tests `addIfNonBlank` correctness)
- `slot_payload_includes_vocabulary_name_and_description()`
- `structural_markdown_shows_axis_label_not_raw_value()`
- `structural_markdown_includes_conflict_mode()`
- `structural_prose_includes_all_disposition_axes()` — assert `socialOrient`, `riskAppetite`, and `conflictMode` present in assembleProse output (were previously absent)
- `different_disposition_vocab_produces_different_descriptor_hash()` — cache regression

**`SystemPromptRendererTest`** (examples module):
- `belbin_slot_payload_includes_framework_name()`
- `thomas_kilmann_conflict_mode_in_payload()`
- `conscientiousness_disposition_fully_resolved()`

---

## Deferred

- **eidos#45** — A2A card: expose theoretical framework references for machine-to-machine
  capability negotiation
- **parent#192** — PLATFORM.md: update "System prompt generation" entry (stale
  `ClaudeMarkdownRenderer` reference; add vocabulary-grounded payload description)
