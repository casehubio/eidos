# Squash Plan — main (2026-06-10)

Range: `upstream/main..HEAD` — 8 commits → 3 commits

---

## Already Clean — 0 commits

All 8 commits are in action groups.

---

## Group 1 — DispositionAxis type safety (2 commits → 1)

**Final message:** `feat(eidos#46): DispositionAxis type safety — jsonKey(), description(), typed AXES throughout eval Refs #46`

| Commit | Action | Curated result |
|--------|--------|----------------|
| `c8181a6` feat(eidos#46): DispositionAxis.jsonKey() and description() — typed axis metadata | ✅ KEEP | *(see Final message above)* |
| `066f20a` feat(eidos#46): DispositionAxis type migration — VariantPair, PairContrastResult, AgentProfile, judge compute paths, YAML keys | 🔽 SQUASH ↑ | *(absorbed — same axis; unified migration of API and all downstream consumers)* |

> **Result:** 1 commit.

---

## Group 2 — Phase 3 behavioral eval (3 commits → 1)

**Final message:** `feat(eidos#46): Phase 3 behavioral eval — BehavioralJudge, AgentProviderChatModel bridge, pair-contrast harness Refs #46`

| Commit | Action | Curated result |
|--------|--------|----------------|
| `72b842e` feat(eidos#46): BehavioralPairResult and BehavioralReport — Phase 3 result types | ✅ KEEP | *(see Final message above)* |
| `dd0adf9` feat(eidos#46): AgentProviderChatModel bridge, BehavioralJudge, evaluateBehavioralScenarios — Phase 3 implementation | 🔀 MERGE ↑ | *(unified — result types and implementation are one deliverable)* |
| `ee100ad` fix(eidos#46): use pattern switch for AgentEvent.TextDelta — exhaustive over unsafe cast | 🔽 SQUASH ↑ | *(absorbed — code review fix to AgentProviderChatModel; < 5 lines)* |

> **Result:** 1 commit.

---

## Group 3 — Docs, protocols, spec/ADR promotion (3 commits → 1)

**Final message:** `docs(eidos#46): eval protocols, CLAUDE.md, spec and ADR-0005 promotion Refs #46`

| Commit | Action | Curated result |
|--------|--------|----------------|
| `8aee5e4` docs(eidos#46): update CLAUDE.md — DispositionAxis methods, eval module in structure and coordinates | ✅ KEEP | *(see Final message above)* |
| `2cb3f87` protocol: eval harness rules — behavioral-judge-blind, disposition-axis-string-boundary | 🔽 SQUASH ↑ | *(absorbed — protocols are docs; same branch-close docs group)* |
| `0350f82` feat: promote spec and ADR-0005 from issue-46-eval-baseline-behavioral | 🔽 SQUASH ↑ | *(absorbed — artifact promotion is docs; same group)* |

> **Result:** 1 commit.

---

## AFTER

```
8 commits (original)
-5 absorbed by squash
─────────────────────
3 commits — no content lost

Sample (most recent first):
  docs(eidos#46): eval protocols, CLAUDE.md, spec and ADR-0005 promotion
  feat(eidos#46): Phase 3 behavioral eval — BehavioralJudge, AgentProviderChatModel bridge, pair-contrast harness
  feat(eidos#46): DispositionAxis type safety — jsonKey(), description(), typed AXES throughout eval
```
