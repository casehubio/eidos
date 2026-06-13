---
id: PP-20260613-608684
title: "Fields rendered by assembleA2aCard() via direct descriptor access must appear in the A2A_CARD descriptor payload"
type: rule
scope: repo
applies_to: "EidosRenderPipeline — A2A_CARD format path in buildDescriptorPayload() and assembleA2aCard()"
severity: important
violation_hint: "field excluded from buildDescriptorPayload(A2A_CARD) for LLM cleanliness but still rendered structurally in assembleA2aCard() via AgentDescriptor direct read — produces stale A2A cache entries when that field changes"
garden_ref: GE-20260613-3fa95a
created: 2026-06-13
---

`assembleA2aCard(AgentDescriptor descriptor)` reads capability fields directly from the `AgentDescriptor` record, not from `s1.descriptorNode()`. The cache key hash is derived from `buildDescriptorPayload(descriptor, A2A_CARD)`. Any field that `assembleA2aCard()` reads and renders must therefore appear in the A2A_CARD format's `buildDescriptorPayload()` output — otherwise changing that field produces no change in the descriptor hash and the cache serves stale output indefinitely. Excluding a field from the A2A_CARD descriptor payload is only permissible if that field does not appear in the A2A_CARD structural output.
