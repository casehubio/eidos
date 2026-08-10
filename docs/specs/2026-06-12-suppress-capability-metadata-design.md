# Design: Suppress Capability Numeric Metadata in PROSE/MARKDOWN Renders

**Issue:** eidos#49  
**Protocol:** PP-20260611-228599  
**Branch:** issue-49-suppress-capability-metadata  
**Date:** 2026-06-12

---

## Problem

`epistemicDomains`, `qualityHint`, `latencyHintP50Ms`, and `costHint` are routing signals
for casehub-engine dispatch — not behavioural instructions. Rendering them in PROSE and
MARKDOWN formats produces FACTUAL_FIDELITY score penalties in eval and clutters agent
instructions with infrastructure metadata the LLM cannot act on without calibration context.

**Current violations:**

1. **Structural MARKDOWN fallback** — `assembleMarkdownStructural()` renders raw numbers:
   `- **code-review**: quality 0.95, p50 150ms` and `Domains: {java=0.95, rust=0.3}`

2. **LLM-enriched path** — `buildDescriptorPayload()` includes numeric fields in the payload
   sent to the LLM; `PROMPT_TEMPLATE` instructs it to convert `epistemicDomains` to
   "strong expertise" / "working knowledge" / "limited familiarity" labels; `RESPONSE_FORMAT`
   schema description says "including domain confidence" — stale LLM contract that will
   mislead anyone debugging unexpected `capabilityNarrative` output.

3. **A2A_CARD incompleteness** — `assembleA2aCard()` only includes `qualityHint` per
   capability. `latencyHintP50Ms`, `epistemicDomains`, `costHint` are missing numeric fields.
   `inputTypes` and `outputTypes` are also absent — an engine routing on type compatibility
   cannot determine what a capability accepts or produces without parsing prose. A2A_CARD is
   a machine-readable card; structured type schema metadata belongs there for the same reason
   `slot` and `disposition` were added as structured objects rather than embedded in prose
   (eidos#27, eidos#45).

**Structural PROSE** is already compliant (renders capability names only). No change needed.

---

## Design

Five independent layers. Each is self-contained and testable in isolation.

### Layer 1: Format-discriminated `buildDescriptorPayload` and `buildStage1`

**Signature change:**
```java
// Before
ObjectNode buildDescriptorPayload(AgentDescriptor descriptor)

// After
ObjectNode buildDescriptorPayload(AgentDescriptor descriptor, RenderFormat format)
```

The capability node construction block is the only format-discriminated part. All other
fields (identity, slot, disposition, jurisdiction) are format-agnostic and unchanged.

**PROSE/MARKDOWN** — capability node: `{name, inputTypes?, outputTypes?}`  
**A2A_CARD** — capability node: `{name, qualityHint?, latencyHintP50Ms?, costHint?, epistemicDomains?, inputTypes?, outputTypes?}`

`tags` remain excluded from all formats (internal routing labels with no value to LLM or A2A consumers).

`inputTypes` and `outputTypes` are qualitative descriptors — what types the capability accepts
and produces. They belong in all formats.

**`buildStage1` — the load-bearing change:**
```java
// Before
final ObjectNode descriptorNode = buildDescriptorPayload(descriptor);

// After
final ObjectNode descriptorNode = buildDescriptorPayload(descriptor, context.format());
```

This is the structural hinge of the whole design: it makes the cache key hash format-correct.
Without it, changing `qualityHint` would still invalidate PROSE/MARKDOWN cache entries even
though those formats no longer render it.

**Cache key consequence:** The descriptor hash is computed from the format-specific node.
Changing `qualityHint` invalidates the A2A_CARD cache entry only — not PROSE/MARKDOWN. This
is correct: PROSE/MARKDOWN output does not depend on numeric capability fields after this change.

`buildLlmPayload()` signature is **unchanged** — the descriptorNode it receives is already
format-correct. The A2A enrichment step (`a2aEnrichmentStep.enrich(llm, s1.descriptorNode())`)
receives the A2A descriptor node including numeric fields; the A2A prompt generates prose
descriptions and does not use numeric fields — harmless.

### Layer 2: Structural MARKDOWN capability block

**`assembleMarkdownStructural()` capability section:**

```java
// Before
sb.append("- **").append(cap.name()).append("**");
if (cap.qualityHint() != null) sb.append(": quality ").append(cap.qualityHint());
if (cap.latencyHintP50Ms() != null)
    sb.append(", p50 ").append(cap.latencyHintP50Ms()).append("ms");
sb.append("\n");
if (cap.epistemicDomains() != null && !cap.epistemicDomains().isEmpty()) {
    sb.append("  Domains: ").append(cap.epistemicDomains()).append("\n");
}

// After
sb.append("- **").append(cap.name()).append("**");
if (cap.inputTypes() != null && !cap.inputTypes().isEmpty())
    sb.append(": accepts ").append(String.join(", ", cap.inputTypes()));
if (cap.outputTypes() != null && !cap.outputTypes().isEmpty())
    sb.append(" → ").append(String.join(", ", cap.outputTypes()));
sb.append("\n");
```

Produces: `- **code-review**: accepts code → review`

**Structural PROSE vs MARKDOWN asymmetry — intentional.** `assembleProse()` structural
fallback renders capability names only and is not changed here. The dense, no-header PROSE
format cannot accommodate inline type annotations without breaking the register. MARKDOWN
is inherently structured and carries `- **name**: accepts X → Y` naturally. When a LLM
is available, both formats receive `inputTypes`/`outputTypes` via the descriptor payload
and the LLM can incorporate them into prose; the asymmetry only applies to the structural
no-LLM fallback path.

### Layer 3: LLM prompt template and RESPONSE_FORMAT schema

Both the prompt instruction and the JSON schema description are updated together — leaving
either stale creates conflicting signals for the LLM (established precedent: eidos#27 §6).

**`PROMPT_TEMPLATE` — `capabilityNarrative` instruction:**
```
// Remove
For epistemicDomains, use natural language confidence:
    >= 0.7 -> "strong expertise", 0.4-0.69 -> "working knowledge", < 0.4 -> "limited familiarity".

// Replace with
List capabilities by name only. Include inputTypes and outputTypes when present
to describe what the agent accepts and produces.
```

**`RESPONSE_FORMAT` — `capabilityNarrative` schema description:**
```java
// Before
.addStringProperty("capabilityNarrative",
    "What the agent can do, including domain confidence. Second person.")

// After
.addStringProperty("capabilityNarrative",
    "What the agent can do, including input and output types when present. Second person.")
```

**Side effects (all correct):**
- `TEMPLATE_HASH` changes (computed from `PROMPT_TEMPLATE`) → all cache entries invalidated
  on deploy for all three formats. PROSE/MARKDOWN: stale LLM-enriched entries mentioning
  "strong expertise" or domain scores are regenerated cleanly. A2A_CARD: required by Layer 4 —
  stale entries would be missing the six new capability fields added by `assembleA2aCard()`.
  `cacheKey()` appends `TEMPLATE_HASH` unconditionally regardless of format.
- Layer 1 already strips numeric fields from the LLM payload — the instruction and schema
  removals are belt-and-suspenders. Both layers enforce the protocol independently.

**Cache gap — RESPONSE_FORMAT descriptions are not independently invalidating.**
`TEMPLATE_HASH = fingerprint(PROMPT_TEMPLATE)` covers only the prompt string. The
`RESPONSE_FORMAT.capabilityNarrative` description change in this spec is covered by the
concurrent `PROMPT_TEMPLATE` change (which already invalidates all PROSE/MARKDOWN entries).
But the coupling is implicit: a future change to `RESPONSE_FORMAT` alone — without touching
`PROMPT_TEMPLATE` — would not be cache-invalidating and could leave stale enriched prompts
in a persistent cache. Tracked as eidos#50 to extend `TEMPLATE_HASH` to cover schema
description strings.

`A2A_PROMPT_TEMPLATE` and its `A2A_RESPONSE_FORMAT` are unchanged.

### Layer 4: A2A_CARD capability completeness

A2A_CARD is a machine-readable capability card for agent-to-agent negotiation. Structured
fields belong there. `slot` and `disposition` were added as structured objects in eidos#27
and eidos#45 precisely because machines benefit from structured data rather than parsing
prose. The same argument applies to capability type schema (`inputTypes`, `outputTypes`) and
all numeric routing signals.

**`assembleA2aCard()` capability block — field order: name → numeric routing signals → type schema → prose description:**

```java
// Before
capNode.put("name", cap.name());
if (cap.qualityHint() != null) capNode.put("qualityHint", cap.qualityHint());
final String desc = descriptionByName.get(cap.name());
if (desc != null) capNode.put("description", desc);

// After
capNode.put("name", cap.name());
if (cap.qualityHint() != null)      capNode.put("qualityHint", cap.qualityHint());
if (cap.latencyHintP50Ms() != null) capNode.put("latencyHintP50Ms", cap.latencyHintP50Ms());
if (cap.costHint() != null)         capNode.put("costHint", cap.costHint());
if (cap.epistemicDomains() != null && !cap.epistemicDomains().isEmpty()) {
    final ObjectNode domains = capNode.putObject("epistemicDomains");
    cap.epistemicDomains().forEach(domains::put);
}
if (cap.inputTypes() != null && !cap.inputTypes().isEmpty()) {
    final ArrayNode arr = capNode.putArray("inputTypes");
    cap.inputTypes().forEach(arr::add);
}
if (cap.outputTypes() != null && !cap.outputTypes().isEmpty()) {
    final ArrayNode arr = capNode.putArray("outputTypes");
    cap.outputTypes().forEach(arr::add);
}
final String desc = descriptionByName.get(cap.name());
if (desc != null) capNode.put("description", desc);
```

**This is additive.** Existing A2A consumers reading `qualityHint` are unaffected. The eval
baseline for A2A_CARD (SCORE_FLOOR 5.00) holds — we're adding structured data, not changing
the format.

---

## Files Changed

| File | Change |
|------|--------|
| `runtime/src/main/java/io/casehub/eidos/runtime/renderer/EidosRenderPipeline.java` | Layers 1, 2, 3, 4 |
| `runtime/src/test/java/io/casehub/eidos/runtime/renderer/EidosRenderPipelineTest.java` | Update callers + new tests |

No other files change. `EidosSystemPromptRenderer` is unchanged — it passes `context.format()`
and uses the pipeline; the pipeline changes are transparent to it. `StageOneResult` is a plain
record holding `ObjectNode`; no change needed.

---

## Tests

**Existing tests — signature update only:**
All `buildDescriptorPayload(desc)` call sites become `buildDescriptorPayload(desc, MARKDOWN)`.
`descriptor_payload_capability_excludes_cost_hint_and_tags` keeps its existing MARKDOWN format
assertion unchanged (just adds format parameter) — `costHint` and `tags` remain excluded
from PROSE/MARKDOWN.

**New tests:**

| Test | Assertion |
|------|-----------|
| `descriptor_payload_prose_omits_numeric_capability_metadata` | `qualityHint`, `latencyHintP50Ms`, `epistemicDomains` absent from PROSE descriptor node |
| `descriptor_payload_a2a_includes_all_numeric_capability_metadata` | `qualityHint`, `latencyHintP50Ms`, `costHint`, `epistemicDomains` all present in A2A_CARD descriptor node — single comprehensive test, no split of existing test |
| `descriptor_payload_format_differences_produce_different_descriptor_hash` | PROSE and A2A_CARD produce different `descriptorHash` (not `lookupKey` — that always differs because `format.name()` is appended) for the same descriptor with numeric metadata |
| `descriptor_payload_prose_and_markdown_produce_same_descriptor_hash` | `buildDescriptorPayload(desc, PROSE)` and `buildDescriptorPayload(desc, MARKDOWN)` produce identical JSON and identical `descriptorHash` — protects the cache-correctness invariant that PROSE and MARKDOWN numeric-only changes do not split the effective cache |
| `structural_markdown_capability_shows_name_and_io_types_no_numeric` | structural MARKDOWN: `- **code-review**: accepts code → review`; no "quality", "p50", "Domains" |
| `a2a_card_capability_includes_all_numeric_and_type_fields` | `assembleA2aCard()` includes `latencyHintP50Ms`, `epistemicDomains`, `costHint`, `inputTypes`, `outputTypes` alongside existing `qualityHint` |
| `a2a_card_capability_numeric_fields_absent_when_null` | capability with name only → A2A card omits all optional numeric fields and type arrays |

---

## Eval Impact

Phase 1 SCORE_FLOORs remain the gates: MARKDOWN 3.95 / PROSE 4.50 / A2A_CARD 5.00.

- PROSE/MARKDOWN: FACTUAL_FIDELITY should improve (no numeric noise). Other dimensions unchanged.
- A2A_CARD: complete numeric + type schema set; existing SCORE_FLOOR 5.00 holds or improves
  (additive structured data).
