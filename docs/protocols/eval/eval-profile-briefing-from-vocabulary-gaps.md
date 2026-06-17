---
id: PP-20260617-bfc66f
title: "Derive AgentDescriptor.briefing from vocabularyGap:FULL entries in eval profiles"
type: rule
scope: repo
applies_to: "casehub-eidos-eval — all YAML agent profiles under eval/src/test/resources/profiles/"
severity: guidance
refs:
  - eval/src/test/resources/profiles/sw-engineer-bold.yaml
  - eval/src/test/resources/profiles/sw-engineer-careful.yaml
violation_hint: "Briefing populated with generic prose, or left null, when the profile has FULL-loss vocabularyGap entries — the briefing then adds no information the structured axes already convey."
created: 2026-06-17
---

When adding or updating an eval agent profile's `AgentDescriptor.briefing` field, derive the content from the `vocabularyGap` entries where `loss: FULL`. These are concepts the structured disposition axes cannot express at all — they are the highest-value briefing candidates because the renderer cannot surface them any other way. PARTIAL-loss entries are already approximated by the axes and should be a secondary source only when no FULL entries exist. The briefing should be 2–3 sentences, second person, under 500 characters, covering the concept directly without repeating what the axes already say.
