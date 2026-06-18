# Squash Plan — main — 2026-06-17

Range: `origin/main..HEAD` (11 commits → 4 commits)

## Summary

- Already clean: 0
- SQUASH candidates: 3 (code-review fixups, test-only, cross-issue final fix)
- MERGE candidates: 4 (same-issue feature parts)
- DROP: 0
- Result: 11 → 4 commits — 7 absorbed, no content lost

---

## Group 1 — eidos#56 enrichment-mechanics
*Compaction: 4 commits → 1*

**Final message:** `feat(eidos#56): enrichment-mechanics — JsonExtractionUtil, 2-field SemanticEnrichment, extractJson+retry, selective override, buildEnrichmentPayload Closes #56`

| Commit | Action | Curated result |
|--------|--------|----------------|
| `d446fd6` feat(eidos#56): JsonExtractionUtil — extractJson for code fences and prose preamble | ✅ KEEP | *(see Final message above)* |
| `1f6d5b5` fix(eidos#56): JsonExtractionUtil — null guard, first != -1 | 🔽 SQUASH ↑ | *(absorbed — < 10 lines, same-issue fixup from code review)* |
| `a615cd0` feat(eidos#56): enrichment-mechanics — 2-field SemanticEnrichment, extractJson+retry, selective override, buildEnrichmentPayload | 🔀 MERGE ↑ | *(unified — same issue #56, all parts of the enrichment-mechanics deliverable)* |
| `0da55bb` fix(eidos#56): enrichment-mechanics quality — retry test, optional() dedup, dead code removed, selective override tests | 🔽 SQUASH ↑ | *(absorbed — code review fixup for the above)* |

> **Result:** 1 commit.

---

## Group 2 — eidos#57 briefing-field
*Compaction: 4 commits → 1*

**Final message:** `feat(eidos#57): briefing-field — AgentDescriptor.briefing, persistence, renderer, cache key, structural fallback MARKDOWN+PROSE Closes #57`

| Commit | Action | Curated result |
|--------|--------|----------------|
| `4bb400a` feat(eidos#57): AgentDescriptor.briefing — nullable String, MAX_BRIEFING=500 | ✅ KEEP | *(see Final message above)* |
| `07d2f69` test(eidos#57): briefing blank rejection test | 🔽 SQUASH ↑ | *(absorbed — test-only, same-issue, < 15 lines)* |
| `f675f3f` feat(eidos#57): briefing persistence — schema, entity, mapper | 🔀 MERGE ↑ | *(unified — same issue #57)* |
| `9458089` feat(eidos#57): briefing in renderer — cache key, enrichment payload, structural fallback MARKDOWN+PROSE | 🔀 MERGE ↑ | *(unified — same issue #57, completes the briefing feature)* |

> **Result:** 1 commit.

---

## Group 3 — eidos#58 proximity-eval-redesign
*Compaction: 2 commits → 1*

**Final message:** `feat(eidos#58): ProximityJudge — descriptor-axis completeness, DispositionAxis.jsonKey null-safe, null-disposition guard; final review polish Closes #58`

| Commit | Action | Curated result |
|--------|--------|----------------|
| `bb4af96` feat(eidos#58): ProximityJudge — descriptor-axis completeness, DispositionAxis.jsonKey null-safe, null-disposition guard Closes #58 | ✅ KEEP | *(see Final message above)* |
| `8c89403` fix(eidos#56,#57,#58): final review — ProximityJudge retry fix, schema/validator sync, extractJson parity, A2A briefing test, Logger, @Column | 🔽 SQUASH ↑ | *(absorbed — post-review polish; covers all three issues but #58 is the latest KEEP)* |

> **Result:** 1 commit.

---

## Group 4 — eidos#54 eval judge resilience
*No action — already clean, standalone issue*

| Commit | Action |
|--------|--------|
| `0a65eb0` fix(eidos#54): eval judge resilience — extractJson stripping + retry-on-non-JSON; configurable renders-cache path Closes #54 | ✅ KEEP |

> **Result:** 1 commit (unchanged).

---

## AFTER — what `git log --oneline` will show

```
11  commits (original)
-7  absorbed by squash
─────────────────────────────────────────────
4   commits — no content lost

Sample (most recent first):
  <sha>  fix(eidos#54): eval judge resilience — extractJson stripping + retry-on-non-JSON; configurable renders-cache path Closes #54
  <sha>  feat(eidos#58): ProximityJudge — descriptor-axis completeness, DispositionAxis.jsonKey null-safe, null-disposition guard; final review polish Closes #58
  <sha>  feat(eidos#57): briefing-field — AgentDescriptor.briefing, persistence, renderer, cache key, structural fallback MARKDOWN+PROSE Closes #57
  <sha>  feat(eidos#56): enrichment-mechanics — JsonExtractionUtil, 2-field SemanticEnrichment, extractJson+retry, selective override, buildEnrichmentPayload Closes #56
```

---

**Rebase todo:**
```
pick d446fd6
squash 1f6d5b5
squash a615cd0
squash 0da55bb
pick 4bb400a
squash 07d2f69
squash f675f3f
squash 9458089
pick bb4af96
squash 8c89403
pick 0a65eb0
```
