# Squash Plan — issue-26-belbin-disc-vocabulary

**Range:** upstream/main..HEAD (after filter-repo: 16 commits → 8)  
**Filter-repo pruned:** 2 commits (scaffold init became empty; implementation plan filtered as workspace artifact)

---

## Already Clean — 0 commits

All commits are classified for action.

---

## Action Groups

### Group 1 — Design spec (5 commits → 1)

**Final message:** `docs(spec): disposition axis fifth axis + Belbin/DISC/TK vocabulary design Refs #26 #38 #39`

| Commit | Action | Curated result |
|--------|--------|----------------|
| `e1e2cb2` docs(spec): disposition axis fifth axis + Belbin/DISC/TK vocabulary design Refs #26 #38 #39 | ✅ KEEP | *(see Final message above)* |
| `4ee5c43` docs(spec): spec self-review — add URI constant convention to BelbinTerm/DiscTerm/TKVocabTerm Refs #26 | 🔽 SQUASH ↑ | *(absorbed — spec review fixup pass 1)* |
| `c9006ce` docs(spec): spec self-review — add URI constant convention to BelbinTerm/DiscTerm/TKVocabTerm Refs #26 | 🔽 SQUASH ↑ | *(absorbed — duplicate spec self-review; same message as above — session boundary artefact)* |
| `ebdf4c9` docs(spec): address second review — vocabUriForAxis API, null-map validation, COMPROMISING gap, per-constant axisExactMatch, TK leaf-value reasoning Refs #26 #38 #39 | 🔽 SQUASH ↑ | *(absorbed — spec review fixup pass 2)* |
| `86d6115` docs(spec): third review pass — validateRequired exists, ops guidance location, COLLABORATING alias, test assertion precision Refs #26 | 🔽 SQUASH ↑ | *(absorbed — spec review fixup pass 3)* |

> **Result:** 1 commit.

---

### Group 2 — CONFLICT_MODE axis foundation (1 commit, no squash)

✅ KEEP `dc2f04d` feat(eidos#38): add CONFLICT_MODE to DispositionAxis + fix AgentDisposition.get() switch Refs #38

---

### Group 3 — AgentDisposition conflictMode field (2 commits → 1)

**Final message:** `feat(eidos#38): add conflictMode field + Builder to AgentDisposition; migrate all call sites Refs #38`

| Commit | Action | Curated result |
|--------|--------|----------------|
| `39235ea` feat(eidos#38): add conflictMode field + Builder to AgentDisposition Refs #38 | ✅ KEEP | *(see Final message above)* |
| `f712505` fix(eidos#38): migrate AgentDisposition call sites — add null conflictMode arg Refs #38 | 🔀 MERGE ↑ | *(unified — call-site migration is part of the same API change)* |

> **Result:** 1 commit.

---

### Group 4 — AgentDescriptor axisVocabularies (2 commits → 1)

**Final message:** `feat(eidos#26 eidos#39): add axisVocabularies + vocabUriForAxis + Builder to AgentDescriptor; migrate call sites + JPA + V1 migration Refs #26 #39`

| Commit | Action | Curated result |
|--------|--------|----------------|
| `cb47403` feat(eidos#26 eidos#39): add axisVocabularies + vocabUriForAxis + Builder to AgentDescriptor Refs #26 #39 | ✅ KEEP | *(see Final message above)* |
| `6da5466` feat(eidos#26): AgentDescriptor call-site migration + JPA axisVocabularies column + V1 migration Refs #26 #39 | 🔀 MERGE ↑ | *(unified — migration, JPA, and SQL are part of the same feature)* |

> **Result:** 1 commit.

---

### Group 5 — ThomasKilmannTerm vocabulary (1 commit, no squash)

✅ KEEP `f95a09e` feat(eidos#38 eidos#26): ThomasKilmannTerm vocab + rename ConscientiousnessVocabRegistrar Refs #38 #26

---

### Group 6 — BelbinTerm vocabulary (1 commit, no squash)

✅ KEEP `b75b440` feat(eidos#26): BelbinTerm slot vocabulary + registrar Refs #26

---

### Group 7 — DiscTerm vocabulary (1 commit, no squash)

✅ KEEP `2f5936a` feat(eidos#26): DiscTerm disposition vocabulary with axisExactMatch → Conscientiousness + TK Refs #26

---

### Group 8 — Documentation, ADR, CLAUDE.md (3 commits → 1)

**Final message:** `docs(eidos#26 eidos#38 eidos#39): personality-frameworks, ADR-0004, operations guide, CLAUDE.md sync; CDI integration tests; fix stale eidos#40 refs Refs #26 #38 #39`

| Commit | Action | Curated result |
|--------|--------|----------------|
| `8b46d53` docs(eidos#26 eidos#38 eidos#39): personality-frameworks correction, operations guide, ADR-0004 Refs #26 #38 #39 | ✅ KEEP | *(see Final message above)* |
| `c92a516` fix: stale eidos#40 blocker references + CDI integration test for Belbin/DISC/TK vocab discovery Refs #26 | 🔽 SQUASH ↑ | *(absorbed — docs fix + test follow-on from code review)* |
| `999b26f` docs: sync CLAUDE.md — add Belbin/DISC/TK vocab, CONFLICT_MODE axis, axisVocabularies field Refs #26 #38 | 🔽 SQUASH ↑ | *(absorbed — docs follow-on, same session)* |

> **Result:** 1 commit.

---

## AFTER — what `git log --oneline` will show

```
18 commits (original)
- 2  pruned by filter-repo (scaffold init, implementation plan)
- 8  absorbed by squash
────────────────────────────────────────────────
 8  commits — no content lost

Sample (most recent first):
  <new> docs(eidos#26 eidos#38 eidos#39): personality-frameworks, ADR-0004, operations guide, CLAUDE.md sync; CDI integration tests; fix stale eidos#40 refs
  <new> feat(eidos#26): DiscTerm disposition vocabulary with axisExactMatch → Conscientiousness + TK
  <new> feat(eidos#26): BelbinTerm slot vocabulary + registrar
  <new> feat(eidos#38 eidos#26): ThomasKilmannTerm vocab + rename ConscientiousnessVocabRegistrar
  <new> feat(eidos#26 eidos#39): add axisVocabularies + vocabUriForAxis + Builder to AgentDescriptor; migrate call sites + JPA + V1 migration
  <new> feat(eidos#38): add conflictMode field + Builder to AgentDisposition; migrate all call sites
  <new> feat(eidos#38): add CONFLICT_MODE to DispositionAxis + fix AgentDisposition.get() switch
  <new> docs(spec): disposition axis fifth axis + Belbin/DISC/TK vocabulary design
```
