# A2A Card: Theoretical Framework References

**Issue:** eidos#45
**Date:** 2026-06-09
**Status:** Approved (revised after review)

---

## Problem

`assembleA2aCard()` currently exposes only `name`, `agentId`, `version`, and `capabilities`.
A receiving agent or orchestrator has no way to discover:

- What slot role this agent occupies, or which vocabulary grounds it
- How the agent is disposed across the five axes, or which theoretical frameworks ground those values
- Whether this agent uses Belbin at all (requiring a full AgentRegistry query rather than card inspection)

eidos#27 added vocabulary-grounded disposition payload to the LLM rendering pipeline. The A2A card
does not benefit from that work.

---

## Pre-existing gap closed by this spec: `vocabUriForSlot()`

`vocabUriForAxis()` resolves via a three-step fallback:
`axisVocabularies.get(axis)` → `dispositionVocabulary` → `domainVocabulary`.

The slot lookup in `buildDescriptorPayload()` currently checks only `slotVocabulary != null` — no
fallback to `domainVocabulary`. The CLAUDE.md architecture note says `domainVocabulary` is the
default for all fields, but the slot implementation does not honour it. This is a pre-existing gap.

This spec fixes it. A new `vocabUriForSlot()` method is added to `AgentDescriptor` with two-step
resolution: `slotVocabulary` → `domainVocabulary`. It is used in both `buildDescriptorPayload()`
and `assembleA2aCard()`. The fix applies to both because the inconsistency affects LLM prompt
quality (slot gets no vocabulary context when only `domainVocabulary` is set) as much as it affects
the A2A card. Fixing both in one pass is cleaner than deferring the LLM fix.

---

## Decision

Three additions to the A2A card, all structural (no LLM involvement):

1. **`slot` object** — the agent's role, vocabulary-resolved via `vocabUriForSlot()`
2. **`disposition` object** — per-axis values with vocabulary context via `vocabUriForAxis()`
3. **`frameworks` array** — deduplicated index of all actively-instantiated vocabulary URIs in the card

`usesEnrichment()` remains `false` for `A2A_CARD`. Slot, disposition, and frameworks are registry
lookups, not inference. The existing `A2AEnrichment` path (capability descriptions) is orthogonal
and untouched.

### Why `frameworks` is not redundant

A consumer of an A2A card makes four kinds of decision:

1. **Capability match** — "Can this agent do X?" → `capabilities[]`
2. **Role match** — "Is this agent a Belbin Monitor Evaluator?" → `slot.vocabularyUri` + `slot.value`
3. **Behavioral compatibility** — "What is this agent's conflict mode?" → `disposition.conflictMode`
4. **Framework discovery** — "Does this agent use Belbin at all?" → `frameworks[]`

Queries 2 and 3 are **navigation** — the consumer knows the field it wants. Query 4 is
**discovery** — the consumer doesn't know which field uses Belbin; scanning all axes is O(N).

`frameworks` is derived at assembly time from the same `vocab` lookups that populate
`slot.vocabularyUri` and `disposition.*.vocabularyUri`. It cannot go out of sync because it
shares the source of truth.

### The frameworks invariant

> `frameworks` contains exactly those vocabulary URIs that are (a) reachable by
> `vocabUriForSlot()` or by `vocabUriForAxis(axis)` for an axis with a non-null value,
> AND (b) registered in `VocabularyRegistry` with a non-blank `name()`. A URI that fails
> either condition cannot appear in `frameworks`.

This means an unregistered URI can appear as `vocabularyUri` in a slot or axis object
(because the resolution chain supplies it) while being absent from `frameworks` (because
it fails condition (b)). The invariant is a conjunction, not a mirror.

---

## Schema

New fields inserted between `version` and `capabilities`:

```json
{
  "name": "Strategic Planner",
  "agentId": "planner-1",
  "version": "1.0",

  "slot": {
    "value": "co-ordinator",
    "label": "Co-ordinator",
    "vocabularyUri": "urn:casehub:vocab:belbin",
    "vocabularyName": "Belbin Team Roles"
  },

  "disposition": {
    "socialOrient": {
      "value": "facilitative",
      "label": "Facilitative",
      "vocabularyUri": "urn:casehub:vocab:conscientiousness",
      "vocabularyName": "Conscientiousness Disposition Axes"
    },
    "conflictMode": {
      "value": "collaborating",
      "label": "Collaborating",
      "vocabularyUri": "urn:casehub:vocab:thomas-kilmann",
      "vocabularyName": "Thomas-Kilmann Conflict Modes"
    },
    "canDelegate": true
  },

  "frameworks": [
    {
      "uri": "urn:casehub:vocab:belbin",
      "name": "Belbin Team Roles",
      "description": "Nine team contribution roles based on Belbin's research at Henley."
    },
    {
      "uri": "urn:casehub:vocab:conscientiousness",
      "name": "Conscientiousness Disposition Axes",
      "description": "Big Five-grounded disposition vocabulary."
    },
    {
      "uri": "urn:casehub:vocab:thomas-kilmann",
      "name": "Thomas-Kilmann Conflict Modes",
      "description": "Five conflict handling strategies defined by assertiveness and cooperativeness."
    }
  ],

  "capabilities": [...]
}
```

### Omission rules

| Field | Omitted when |
|---|---|
| `slot.label` | term not resolved or label blank |
| `slot.vocabularyUri` | `vocabUriForSlot()` returns empty |
| `slot.vocabularyName` | vocabulary not registered or name blank |
| Disposition axis object | axis value is null |
| `disposition.*.label` | term not resolved or label blank |
| `disposition.*.vocabularyUri` | `vocabUriForAxis(axis)` returns empty |
| `disposition.*.vocabularyName` | not registered or name blank |
| `disposition` block | `descriptor.disposition()` is null |
| `frameworks` array key | no entries qualify (empty result) |
| Framework entry | not registered OR `name()` blank |
| `frameworks[].description` | `meta.description()` blank |

Note on `slot.vocabularyUri`: the omission condition is now "`vocabUriForSlot()` returns empty"
— not "`slotVocabulary` null". An agent configured with only `domainVocabulary` will have
`slot.vocabularyUri` populated via the fallback. This is intentional and consistent with how
`disposition.*.vocabularyUri` has always worked.

### `frameworks` derivation (pseudocode)

```java
LinkedHashSet<String> uris = new LinkedHashSet<>();
// 1. slot contribution (order: slot-first)
descriptor.vocabUriForSlot().ifPresent(uris::add);
// 2. disposition contributions (order: DispositionAxis declaration order)
if (descriptor.disposition() != null) {
    for (DispositionAxis axis : DispositionAxis.values()) {
        descriptor.disposition().get(axis).ifPresent(
            value -> descriptor.vocabUriForAxis(axis).ifPresent(uris::add)); // value present → include URI
    }
}
// 3. build array — registered + non-blank name only
for (String uri : uris) {
    vocab.vocabularyMetadata(uri).ifPresent(meta -> {
        if (!meta.name().isEmpty()) { /* add entry */ }
    });
}
```

`LinkedHashSet` preserves insertion order (slot vocab first, then disposition axes in enum order)
and deduplicates. `HashSet` would produce non-deterministic ordering across JVM runs.

### A2A axis objects vs LLM payload axis objects

`buildDescriptorPayload()` (LLM payload) produces per-axis objects with:
`value`, `label`, `description` (term-level) + `vocabularyName`, `vocabularyDescription` (vocab-level)
— but no `vocabularyUri`.

`assembleA2aCard()` (this spec) produces per-axis objects with:
`value`, `label`, `vocabularyUri`, `vocabularyName`
— but no `description`, no `vocabularyDescription`.

The asymmetry is intentional:

- LLM consumers route on natural language names; `vocabularyUri` is opaque to LLMs.
- Machine consumers route on URIs; `description` and `vocabularyDescription` are documentation,
  not routing signals. Term-level `description` is excluded entirely from A2A axis objects.
  Vocabulary-level `description` belongs in `frameworks[].description`, where a consumer reads
  it once per framework — not repeated across every axis that shares a vocabulary (a
  Conscientiousness agent would otherwise repeat the same description four times).

### DISC/conflictMode note

With `dispositionVocabulary="urn:casehub:vocab:disc"` and `conflictMode="dominance"`:

```json
"conflictMode": {
  "value": "dominance",
  "label": "Dominance",
  "vocabularyUri": "urn:casehub:vocab:disc",
  "vocabularyName": "DISC Behavioral Styles"
}
```

The label is the DISC type label, not the TK equivalent. The card faithfully represents what was
declared. Consumers needing TK equivalence use `VocabularyRegistry.equivalentValues()`.
Using `axisVocabularies = {CONFLICT_MODE: "urn:casehub:vocab:thomas-kilmann"}` produces TK
labels directly.

---

## Implementation

### New: `AgentDescriptor.vocabUriForSlot()`

```java
/**
 * Resolves the vocabulary URI for the slot field.
 * Precedence: slotVocabulary → domainVocabulary.
 */
public Optional<String> vocabUriForSlot() {
    if (slotVocabulary != null)  return Optional.of(slotVocabulary);
    if (domainVocabulary != null) return Optional.of(domainVocabulary);
    return Optional.empty();
}
```

### Changed: `buildDescriptorPayload()` slot block

Replace the `if (descriptor.slotVocabulary() != null)` guard with `vocabUriForSlot()`:

```java
descriptor.vocabUriForSlot().ifPresent(uri -> {
    vocab.resolve(uri, descriptor.slot()).ifPresent(term -> {
        addIfNonBlank(node, "slotLabel",       term.label());
        addIfNonBlank(node, "slotDescription", term.description());
    });
    vocab.vocabularyMetadata(uri).ifPresent(meta -> {
        addIfNonBlank(node, "slotVocabularyName",        meta.name());
        addIfNonBlank(node, "slotVocabularyDescription", meta.description());
    });
});
```

### Changed: `assembleA2aCard()` — full replacement

```
1. name, agentId, version  (unchanged)
2. slot block — use vocabUriForSlot()
   - value (always)
   - vocabularyUri, label, vocabularyName (from vocabUriForSlot() result)
3. disposition block (when disposition non-null)
   - per-axis objects for non-null axes only (in DispositionAxis declaration order)
   - each axis: value, vocabularyUri (from vocabUriForAxis()), label, vocabularyName
   - canDelegate boolean (always, placed after axis objects)
4. frameworks array (when non-empty; see derivation above)
5. capabilities (unchanged)
```

No other production class changes in code. However, `A2ASemanticEnrichmentStep.enrich()` receives
`s1.descriptorNode()` — the output of `buildDescriptorPayload()`. After the `vocabUriForSlot()`
fix, agents that have `domainVocabulary` set but no `slotVocabulary` will now have `slotLabel` and
`slotVocabularyName` in the node passed to the enrichment step, where previously those fields were
absent. Two observable consequences: (1) the LLM may produce richer per-capability descriptions for
those agents; (2) the descriptor hash changes (node content changes), so existing cache entries for
those agents will miss on the first render after deployment. Both are correct and beneficial — the
cache miss ensures the enrichment uses the updated context.

The reactive renderer picks up the `assembleA2aCard()` change automatically via `pipeline.assemble()`.

---

## Testing

### Unit tests — `EidosRenderPipelineTest`

Uses existing in-file `TestSlotTerm` and `TestDispTerm` enum fixtures. No new test infrastructure needed.

| Test | What it verifies |
|---|---|
| `a2a_card_slot_value_always_present` | `slot.value` present with no vocab configured |
| `a2a_card_slot_includes_vocab_fields_when_slot_vocabulary_registered` | `label`, `vocabularyUri`, `vocabularyName` in slot when slotVocabulary registered |
| `a2a_card_slot_includes_vocab_fields_via_domain_vocabulary_fallback` | `slot.vocabularyUri` populated when only domainVocabulary set, no slotVocabulary |
| `a2a_card_slot_omits_vocab_fields_when_no_vocabulary` | no vocab fields in slot when neither slotVocabulary nor domainVocabulary set |
| `a2a_card_disposition_axis_present_when_value_set` | axis object with `value` field present |
| `a2a_card_disposition_axis_omitted_when_value_null` | unset axis produces no key in disposition |
| `a2a_card_disposition_with_delegation_only_emits_can_delegate` | disposition non-null, all String axes null, no vocab configured → `{"canDelegate": false}` only; no axis objects; no `frameworks` key |
| `a2a_card_disposition_includes_vocab_uri_and_name_when_registered` | `vocabularyUri` + `vocabularyName` in axis object when vocab registered |
| `a2a_card_disposition_omits_vocab_fields_when_no_uri` | clean axis object when no vocabulary configured for that axis |
| `a2a_card_disposition_null_produces_no_disposition_block` | no `disposition` key when `descriptor.disposition()` null |
| `a2a_card_frameworks_lists_instantiated_vocabularies` | frameworks array contains slot vocab + disposition vocabs, deduplicated |
| `a2a_card_frameworks_deduplicates_same_uri` | same URI on slot and multiple disposition axes → one frameworks entry |
| `a2a_card_frameworks_omitted_when_no_vocabularies` | no `frameworks` key in JSON when no vocab URIs configured |
| `a2a_card_frameworks_excludes_unregistered_uri` | unregistered URI absent from frameworks AND present as `vocabularyUri` in the axis object |
| `a2a_card_frameworks_omits_description_when_blank` | entry with blank metadata description has no `description` key |
| `a2a_card_frameworks_includes_uri_from_domain_vocabulary_fallback` | agent with only domainVocabulary + disposition values → that URI appears in frameworks |

Note on `a2a_card_frameworks_excludes_unregistered_uri`: this test must assert two things:
(1) the URI does NOT appear in `frameworks`, and (2) the URI DOES appear as `vocabularyUri` in the
axis object. Both are part of the same specification.

### Unit tests — `buildDescriptorPayload()` (new, in `EidosRenderPipelineTest`)

| Test | What it verifies |
|---|---|
| `descriptor_payload_slot_vocab_via_domain_vocabulary_fallback` | when only domainVocabulary set, `slotVocabularyName` appears in LLM payload |

### Integration tests — `SystemPromptRendererTest` (`@QuarkusTest`)

Uses real CDI vocabulary registry with `casehub-eidos-vocab` beans.

| Test | What it verifies |
|---|---|
| `a2a_card_belbin_slot_exposes_slot_and_framework` | slotVocabulary=belbin, slot="co-ordinator" → `slot.label="Co-ordinator"`, Belbin entry in frameworks |
| `a2a_card_conscientiousness_disposition_exposes_framework` | dispositionVocabulary=conscientiousness → axes have vocabularyName, Conscientiousness entry in frameworks |
| `a2a_card_thomas_kilmann_conflict_mode_in_frameworks` | axisVocabularies override for Thomas-Kilmann conflict mode → TK entry in frameworks, conflictMode axis has TK label and vocabularyUri |
| `a2a_card_slot_no_vocab_disposition_has_vocab` | slotVocabulary null but dispositionVocabulary set → slot block has value only (no uri/label/name), disposition axes have vocab context, frameworks sourced from disposition only |
| `a2a_card_no_vocab_agent_has_no_frameworks_key` | plain agent (no vocabulary URIs configured) → no `frameworks` key in JSON |

---

## Out of scope

- `jurisdiction` / `dataHandlingPolicy` in the A2A card — not required for framework routing
- `version` in framework entries — internal eidos metadata, not needed for consumer routing decisions
- Static URI constants on vocab enums — worth considering as a future improvement; out of scope here
- Cross-vocabulary translation at card assembly time — consumer responsibility via `equivalentValues()`
