---
id: PP-20260610-de090d
title: "Behavioral judge prompts must not include expected trait values"
type: rule
scope: repo
applies_to: "casehub-eidos-eval — BehavioralJudge and any future behavioral judge class"
severity: important
refs:
  - docs/superpowers/specs/2026-06-09-eval-baseline-behavioral-design.md
violation_hint: "A judge system prompt that includes HIGH/LOW/NEUTRAL or expected axis values is circular — the judge knows the expected answer before reading the responses. Result: inflated accuracy with no diagnostic value."
created: 2026-06-10
---

Behavioral judge prompts present two agent responses (A and B) and ask which expresses the
specified axis more strongly. The prompt must contain only the axis description and the two
responses — never the expected trait polarity, profile name, or any wording that reveals which
profile is "higher." The judge must arrive at its answer from the response text alone. Including
expected values makes evaluation circular: the judge is scoring against known expectations
rather than making a blind behavioural comparison, which produces inflated accuracy results
with no validity.
