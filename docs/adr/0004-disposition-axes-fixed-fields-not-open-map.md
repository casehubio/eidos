# ADR 0004 — Disposition Axes: Fixed Fields, Not Open Map

**Status:** Accepted  
**Date:** 2026-06-07  
**Issues:** eidos#39

## Context

`AgentDisposition` was designed with four fixed String fields (`socialOrient`, `ruleFollowing`,
`riskAppetite`, `autonomy`) and a boolean `delegation`. An alternative design —
`Map<String, String> axes` — was proposed, where vocabulary authors define axis names and
the record is open to arbitrary axes.

## Decision

Keep fixed fields. `DispositionAxis` enum is the extensibility point for axis names.

## Rationale

**The compile-time forcing function.** `VocabularyTerm.axisExactMatch(Class<?>, DispositionAxis)`
uses an exhaustive switch over `DispositionAxis`. When a new axis is added to the enum, any
`axisExactMatch` implementation using an exhaustive switch fails to compile — vocabulary authors
are forced to handle the new axis. An open `Map<String, String>` eliminates this guarantee:
vocabulary authors implement `axisExactMatch` against arbitrary strings with no compile-time
completeness check.

**"Open-String" means values, not keys.** The CLAUDE.md description of "open-String axes"
refers to the vocabulary term VALUES (open strings resolved through the registry) — not the
axis key names (which are typed via the enum).

**Adding axes is safe and mechanical.** Adding a new axis means updating `DispositionAxis` and
`AgentDescriptor` together. The compiler identifies every affected call site. Jackson
serialises `AgentDisposition` as JSON TEXT; adding a nullable field costs zero schema migration.

## Consequences

- New axes require a breaking API change (enum + record update).
- Existing callers pass `null` for unknown axes; migration is always mechanical.
- Vocabulary completeness for all axes is enforced at compile time.
