# Squash Plan — 2026-06-08 — issue-42-vocab-registry-polish

Range: `upstream/main..HEAD` — 13 commits → 3 commits

---

## Group A — #42 Vocab Registry Robustness (7 commits → 1)

The 6 `docs:` commits (design spec, spec revisions, plan) are pre-implementation
planning noise absorbed into the implementation commit.

**Note:** spec commits are chronologically OLDER than the KEEP — todo reorders them.

**Final message:** `test(eidos#42): vocab registry robustness — blank URI guard, alias-vs-alias + allTerms isolation tests Refs #42`

| Commit | Action | Curated result |
|--------|--------|----------------|
| `eb86418` test(eidos#42): vocab registry robustness — blank URI guard... | ✅ KEEP | *(see Final message above)* |
| `622c45f` docs: design spec for vocab registry polish (#42 #43 #41) | 🔽 SQUASH ↑ | *(absorbed — pre-implementation spec; Refs #42 #43 #41 already on KEEP)* |
| `636bfb9` docs: revise vocab registry polish spec — address review findings | 🔽 SQUASH ↑ | *(absorbed — spec revision rounds)* |
| `fc5db90` docs: fix allTerms test assertion + Javadoc null claim in spec | 🔽 SQUASH ↑ | *(absorbed — spec revision rounds)* |
| `6e8a21d` docs: complete @throws Javadoc + add builder test sketch in spec | 🔽 SQUASH ↑ | *(absorbed — spec revision rounds)* |
| `5e23ba1` docs: add implementation notes to spec | 🔽 SQUASH ↑ | *(absorbed — spec revision rounds)* |
| `8b3c580` docs: implementation plan for vocab registry polish (#42 #43 #41) | 🔽 SQUASH ↑ | *(absorbed — planning doc)* |

> **Result:** 1 commit.

---

## Group B — #43 Builder Migration (5 commits → 1)

Five module-by-module commits squashed into one. Each module commit is a partial
step; together they form the complete Builder migration.

**Final message:** `refactor(eidos#43): migrate ~100 positional AgentDescriptor/AgentDisposition constructors to Builder pattern across api, runtime, persistence-memory, examples, eval Closes #43`

| Commit | Action | Curated result |
|--------|--------|----------------|
| `4690e23` refactor(eidos#43): Builder migration — api/src/test/ Refs #43 | ✅ KEEP | *(see Final message above)* |
| `fc5aad0` refactor(eidos#43): Builder migration — runtime/src/test/ Refs #43 | 🔽 SQUASH ↑ | *(absorbed — module step)* |
| `080ea69` refactor(eidos#43): Builder migration — persistence-memory/src/test/ Refs #43 | 🔽 SQUASH ↑ | *(absorbed — module step)* |
| `6008bea` refactor(eidos#43): Builder migration — examples/agent-scenarios Refs #43 | 🔽 SQUASH ↑ | *(absorbed — module step)* |
| `056d981` refactor(eidos#43): Builder migration — eval/ Closes #43 | 🔽 SQUASH ↑ | *(absorbed — final module step; Closes #43 moved to KEEP message)* |

> **Result:** 1 commit.

---

## Group C — #41 Docs Polish (1 commit → 1)

Already clean — no action.

✅ KEEP `c33db15` docs(eidos#41): personality-frameworks.md polish — SFIA slot cell, footnote, KAI range, §6 ratings Closes #41

> **Result:** 1 commit (unchanged).

---

## AFTER — estimated result

```
  13  commits (original)
  -10  absorbed by squash (6 spec/plan + 4 Builder module steps)
  ──────────────────────────────────────────────
   3  commits — no content lost

Estimated (from KEEP commits):
  c33db15  docs(eidos#41): personality-frameworks.md polish… Closes #41
  <new>    refactor(eidos#43): migrate ~100 positional constructors… Closes #43
  <new>    test(eidos#42): vocab registry robustness… Refs #42
```

---

## Rebase todo

```
pick eb86418  ← KEEP for #42 (moved above spec commits)
squash 622c45f
squash 636bfb9
squash fc5db90
squash 6e8a21d
squash 5e23ba1
squash 8b3c580
pick 4690e23  ← KEEP for #43
squash fc5aad0
squash 080ea69
squash 6008bea
squash 056d981
pick c33db15  ← KEEP for #41 (already clean)
```
