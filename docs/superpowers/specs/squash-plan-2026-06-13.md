# Squash Plan — 2026-06-13

Range: `upstream/main..HEAD` (8 commits → 2)

## Already Clean

None — all commits are action items.

---

## Group 1 — feat(eidos#49): suppress capability numeric metadata in PROSE/MARKDOWN; complete A2A_CARD
*Compaction group — 7 commits → 1*

| Commit | Action | Curated result |
|--------|--------|----------------|
| `922cf40` feat(eidos#49): suppress capability numeric metadata in PROSE/MARKDOWN; complete A2A_CARD | ✅ KEEP | *(message adequate — unchanged)* |
| `ad36675` docs(eidos#49): design spec — suppress capability numeric metadata in PROSE/MARKDOWN | 🔽 SQUASH ↑ | *(absorbed — pre-implementation spec; 4 spec iteration rounds form the design history)* |
| `a3a9cb0` docs(eidos#49): revise design spec — RESPONSE_FORMAT, buildStage1, A2A type fields, test consolidation | 🔽 SQUASH ↑ | *(absorbed — spec revision round 1)* |
| `32a62a6` docs(eidos#49): revise spec — PROSE/MARKDOWN structural asymmetry, costHint cache rationale, TEMPLATE_HASH gap (eidos#50) | 🔽 SQUASH ↑ | *(absorbed — spec revision round 2; eidos#50 captured separately)* |
| `49c659b` docs(eidos#49): fix Layer 3 side effects — A2A_CARD cache invalidation is load-bearing for Layer 4 | 🔽 SQUASH ↑ | *(absorbed — spec revision round 3)* |
| `1c76898` docs(eidos#49): update CLAUDE.md — A2A_CARD capability fields and format-discriminated rendering | 🔽 SQUASH ↑ | *(absorbed — doc follow-on; A2A_CARD description in CLAUDE.md)* |
| `b01b8d8` test(eidos#49): tighten doesNotContain assertions to exact old rendered strings | 🔽 SQUASH ↑ | *(absorbed — test hardening post code-review)* |

> **Result:** 1 commit. `Closes #49`

---

## Group 2 — protocol(PP-20260613-608684): a2a-structural-assembly-hash-coverage
*Standalone — 1 commit, no action*

| Commit | Action |
|--------|--------|
| `c9a2ffd` protocol(PP-20260613-608684): a2a-structural-assembly-hash-coverage | ✅ KEEP |

> **Result:** 1 commit.

---

## AFTER

```
Before: 8 commits
Squash: -6 absorbed
─────────────────
After:  2 commits

git log --oneline upstream/main..HEAD:
  <sha>  protocol(PP-20260613-608684): a2a-structural-assembly-hash-coverage
  <sha>  feat(eidos#49): suppress capability numeric metadata in PROSE/MARKDOWN; complete A2A_CARD
```
