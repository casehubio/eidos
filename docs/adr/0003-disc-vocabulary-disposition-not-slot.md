# 0003 — DISC types registered as disposition vocabulary, not slot vocabulary

Date: 2026-06-05
Status: Accepted

## Context and Problem Statement

`casehub-eidos-vocab` needs to expose DISC personality type terms (Dominance, Influence,
Steadiness, Conscientiousness) for agent descriptor construction. The question is which
AgentDescriptor field they target: `slot` (via `slotVocabulary`) or the disposition axes
(via `dispositionVocabulary`). The initial spec draft treated DISC as a slot vocabulary —
a category error that would prevent an agent from simultaneously holding a Belbin team role
and a DISC behavioral style.

## Decision Drivers

* DISC types describe behavioral patterns that an agent brings to every context — they answer
  "how do you behave?" not "what role do you play on this team?"
* Belbin roles are assigned by a team; DISC types are measured personality patterns that
  transcend any single team context
* AgentDescriptor has one `slot` field — using it for a personality type precludes using it
  for a team role assignment simultaneously
* The Belbin + DISC combination (team role + personality style) is additive and must be
  representable in one descriptor

## Considered Options

* **Option A** — DISC as slot vocabulary: DISC type names as `slot` values
* **Option B** — DISC as disposition vocabulary: DISC type names as values resolved via
  `dispositionVocabulary`
* **Option C** — DISC and Belbin unified into one slot vocabulary: both frameworks contribute
  to a single combined slot taxonomy

## Decision Outcome

Chosen option: **Option B**, because DISC types are behavioral pattern descriptors (stable
personality traits), not role assignments. Registering DISC as `dispositionVocabulary` lets
an agent simultaneously carry a Belbin slot (team role) and a DISC type (personality style),
which is the additive combination the platform needs for full behavioral profiling.

### Positive Consequences

* Belbin + DISC profiles are expressible in a single AgentDescriptor
* DISC type resolution maps naturally to the existing Conscientiousness vocabulary terms
  via `VocabularyRegistry.equivalentValues()`
* Vocabulary category is semantically correct — querying on `slot` returns role assignments,
  querying on disposition returns behavioral style

### Negative Consequences / Tradeoffs

* Axis-aware resolution of DISC types requires extending `VocabularyRegistry.equivalentValues()`
  with an axis parameter (a single DISC type maps to different Conscientiousness terms per axis)
  — tracked in eidos#40; `DiscVocabularyProducer` implementation is blocked until resolved

## Pros and Cons of the Options

### Option A — DISC as slot vocabulary

* ✅ Simple: producer structure is identical to BelbinVocabularyProducer
* ❌ Category error: personality style ≠ role assignment
* ❌ Prevents simultaneous Belbin role + DISC type in one descriptor (one `slot` field)
* ❌ Queries on `slot` return personality types mixed with role assignments

### Option B — DISC as disposition vocabulary (chosen)

* ✅ Semantically correct: behavioral pattern → disposition
* ✅ Belbin + DISC combination is additive and representable
* ✅ Disposition axis queries return behavioral signal, not role assignments
* ❌ Requires axis-aware API extension (eidos#40) before DiscVocabularyProducer can be implemented

### Option C — DISC and Belbin unified slot vocabulary

* ✅ Single vocabulary URI simplifies descriptor construction
* ❌ Conflates two orthogonal frameworks (role assignment vs. personality style)
* ❌ Makes it impossible to distinguish "this agent is a Shaper" from "this agent has D-type behavior"
* ❌ Loses the additive information when role and personality diverge

## Links

* eidos#29 — personality framework mapping (design analysis that identified the category error)
* eidos#26 — Belbin/DISC vocabulary module implementation
* eidos#40 — axis-aware `equivalentValues()` API extension (blocks DiscVocabularyProducer)
* docs/personality-frameworks.md §Architecture: DISC as Disposition Vocabulary
