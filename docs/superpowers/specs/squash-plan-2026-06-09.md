# Squash Plan — issue-27-theoretical-framework-grounding
**Range:** `upstream/main..HEAD` — 20 commits → 9 commits
**Date:** 2026-06-09

## Already Clean — 0 commits (all commits are in action groups)

## Action Groups

---

### Group 1 — Design spec (docs iterations → 1)
*5 commits → 1*

✅ KEEP `c93fd01` docs(eidos#27): framework grounding design spec
> Absorbed: `3378ed5` address review Bug 1-3 Gap 4-7; `c94039b` address second review; `a44369b` address third review; `2b80d06` add design notes

> **Result:** 1 commit — `docs(eidos#27): framework grounding design spec`

---

### Group 2 — @VocabularyMetadata.description() API
*1 commit — standalone*

✅ KEEP `b77b120` feat(eidos#27): add description() to @VocabularyMetadata
> *(standalone — no noise to absorb)*

---

### Group 3 — VocabularyRegistry.vocabularyMetadata() SPI + impl + tests
*2 commits → 1*

| Commit | Action | Curated result |
|--------|--------|----------------|
| `3f59028` feat(eidos#27): add VocabularyRegistry.vocabularyMetadata(uri) — SPI + impl + tests | ✅ KEEP | *(message adequate — unchanged)* |
| `bd1738f` refactor(eidos#27): improve vocabularyMetadata() Javadoc and test coverage | 🔽 SQUASH ↑ | *(absorbed — Javadoc + test polish)* |

> **Result:** 1 commit.

---

### Group 4 — addIfNonBlank helper + slot vocabulary context in payload
*2 commits → 1*

| Commit | Action | Curated result |
|--------|--------|----------------|
| `178178b` feat(eidos#27): addIfNonBlank helper + slot vocabulary context in payload | ✅ KEEP | *(message adequate — unchanged)* |
| `e1519c1` test(eidos#27): label TestDispTerm scaffolding, add slotVocabularyDescription suppression assertion | 🔽 SQUASH ↑ | *(absorbed — test scaffolding annotation + assertion addition)* |

> **Result:** 1 commit.

---

### Group 5 — Per-axis nested disposition payload (fixes conflictMode omission)
*2 commits → 1*

| Commit | Action | Curated result |
|--------|--------|----------------|
| `b8cfa83` feat(eidos#27): per-axis nested disposition payload with vocabulary context — fixes conflictMode omission | ✅ KEEP | *(message adequate — unchanged)* |
| `d1143e3` refactor(eidos#27): use addIfNonBlank for label, improve disposition test coverage | 🔽 SQUASH ↑ | *(absorbed — label helper fix + test coverage improvement from review)* |

> **Result:** 1 commit.

---

### Group 6 — PROMPT_TEMPLATE and RESPONSE_FORMAT update
*1 commit — standalone*

✅ KEEP `6c340ac` feat(eidos#27): update PROMPT_TEMPLATE and RESPONSE_FORMAT for framework-grounded disposition
> *(standalone — distinct concern from payload structure)*

---

### Group 7 — Structural renderers + helpers
*2 commits → 1*

| Commit | Action | Curated result |
|--------|--------|----------------|
| `5395be5` feat(eidos#27): structural renderers — vocab-resolved axis labels, conflictMode, all prose axes | ✅ KEEP | *(message adequate — unchanged)* |
| `fa0c869` refactor(eidos#27): strengthen raw-value assertion, add blank-label guard in resolveAxisDisplay | 🔽 SQUASH ↑ | *(absorbed — two targeted review fixes to same files)* |

> **Result:** 1 commit.

---

### Group 8 — Vocab enum descriptions
*2 commits → 1*

| Commit | Action | Curated result |
|--------|--------|----------------|
| `51c9c9d` feat(eidos#27): add framework description to all 6 vocab enum @VocabularyMetadata annotations | ✅ KEEP | *(message adequate — unchanged)* |
| `31a9afb` refactor(eidos#27): clarify ConscientiousnessTerm covers 4 of 5 axes, not CONFLICT_MODE | 🔽 SQUASH ↑ | *(absorbed — description refinement from code review)* |

> **Result:** 1 commit.

---

### Group 9 — Integration tests
*3 commits → 1*

| Commit | Action | Curated result |
|--------|--------|----------------|
| `89eb79e` test(eidos#27): integration tests — vocab-resolved labels, TK conflict mode, cache hash uniqueness Closes #27 | ✅ KEEP | *(message adequate — unchanged)* |
| `0418a13` test(eidos#27): add vocab registration guard to hash uniqueness test | 🔽 SQUASH ↑ | *(absorbed — guard assertion added in review pass)* |
| `f39383b` refactor(eidos#27): consistent addIfNonBlank for slotLabel, remove plan task ref from comment | 🔽 SQUASH ↑ | *(absorbed — final polish: slotLabel consistency fix + comment cleanup)* |

> **Result:** 1 commit.

---

## AFTER — what `git log --oneline` will show

```
  20  commits (original)
  -11  absorbed by squash
  ──────────────────────────────────────────────
   9  commits — no content lost

  test(eidos#27): integration tests — vocab-resolved labels, TK conflict mode Closes #27
  feat(eidos#27): add framework description to all 6 vocab enum @VocabularyMetadata annotations
  feat(eidos#27): structural renderers — vocab-resolved axis labels, conflictMode, all prose axes
  feat(eidos#27): update PROMPT_TEMPLATE and RESPONSE_FORMAT for framework-grounded disposition
  feat(eidos#27): per-axis nested disposition payload with vocabulary context — fixes conflictMode omission
  feat(eidos#27): addIfNonBlank helper + slot vocabulary context in payload
  feat(eidos#27): add VocabularyRegistry.vocabularyMetadata(uri) — SPI + impl + tests
  feat(eidos#27): add description() to @VocabularyMetadata
  docs(eidos#27): framework grounding design spec
```
