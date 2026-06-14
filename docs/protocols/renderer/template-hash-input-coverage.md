---
id: PP-20260614-templatehash
title: "TEMPLATE_HASH must cover all strings that influence LLM enrichment output"
type: rule
scope: repo
applies_to: "EidosRenderPipeline — TEMPLATE_HASH static constant and any future hash constant governing enrichment cache keys"
severity: important
violation_hint: "TEMPLATE_HASH computed from PROMPT_TEMPLATE only — schema description strings in RESPONSE_FORMAT or A2A_RESPONSE_FORMAT changed without invalidating the enrichment cache"
created: 2026-06-14
---

`TEMPLATE_HASH` governs enrichment cache invalidation. It must be computed from the
concatenation of **every string that the LLM sees** during semantic enrichment:

1. `PROMPT_TEMPLATE` — the system prompt instructing the LLM
2. `A2A_PROMPT_TEMPLATE` — the A2A-specific system prompt
3. All `RESPONSE_FORMAT` schema description strings — these specify what each JSON field
   should contain and directly influence LLM output
4. All `A2A_RESPONSE_FORMAT` schema description strings — same reason

**Why schema descriptions matter:** `ResponseFormat` schema descriptions are sent to the
LLM as part of the structured output contract. Changing a description (e.g. making the
`capabilityNarrative` field description more specific) changes what the LLM writes in
that field. If only `PROMPT_TEMPLATE` is hashed, such a change produces no cache
invalidation, and the cache silently serves stale enriched prompts.

**Implementation pattern:** extract schema descriptions into named `List<String>` constants
(`RESPONSE_FORMAT_SCHEMA_DESCRIPTIONS`, `A2A_RESPONSE_FORMAT_SCHEMA_DESCRIPTIONS`) and use
them both in the `ResponseFormat` builder and in the `TEMPLATE_HASH` fingerprint input.
This makes the dependency mechanical and compiler-visible rather than a discipline gap.

**Static initializer ordering:** `TEMPLATE_HASH` is a static constant. All inputs
(`PROMPT_TEMPLATE`, `A2A_PROMPT_TEMPLATE`, description lists) must be declared before
`TEMPLATE_HASH` in the class body. Java static initializers execute in declaration order;
a forward reference produces a silent `null` input, not a compile error.
