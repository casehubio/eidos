---
id: PP-20260611-228599
title: "Render capability names only in PROSE and MARKDOWN; numeric metadata to A2A_CARD only"
type: rule
scope: platform
applies_to: "EidosSystemPromptRenderer — PROSE and MARKDOWN format paths"
severity: important
violation_hint: "latencyHintP50Ms, qualityHint, or epistemicDomains scores appearing as numbers or 'strong expertise' labels in PROSE/MARKDOWN output"
created: 2026-06-11
---

`epistemicDomains`, `qualityHint`, `latencyHintP50Ms`, and `costHint` are routing signals for casehub-engine dispatch — they are not behavioural instructions. PROSE and MARKDOWN renders must surface capability names only ("You can perform sprint-planning and estimation"). A2A_CARD carries the full numeric fields for consuming engines and agents. Rendering numbers the LLM cannot act on produces FACTUAL_FIDELITY score penalties in eval and clutters agent instructions with infrastructure metadata that is meaningless without calibration.
