---
id: PP-20260610-70478e
title: "Eval computation iterates List<DispositionAxis>; result records keep Map<String,...> with axis.jsonKey() bridges"
type: rule
scope: repo
applies_to: "casehub-eidos-eval — AXES/NUMERIC_AXES constants, judge parse() methods, PersonalityPreservationReport.computeDiagnoses()"
severity: guidance
refs:
  - docs/superpowers/specs/2026-06-09-eval-baseline-behavioral-design.md
violation_hint: "Iterating DispositionAxis.values() in a judge that prompts for only 4 axes (e.g., TraitExpressionJudge) causes MalformedJudgeResponseException for CONFLICT_MODE. Using String axis keys directly in computation code breaks when a type migration occurs."
created: 2026-06-10
---

AXES and NUMERIC_AXES constants in the eval module must be `List<DispositionAxis>` containing
exactly the axes the judge's LLM prompt asks for — never `DispositionAxis.values()` unless all
five axes are in the prompt. Result records (`TraitExpressionResult.expressionScores`,
`directionMatches`) keep `Map<String, Integer>` / `Map<String, Boolean>` with camelCase keys for
stable JSON serialisation. Anywhere computation code writes into a String-keyed map, bridge with
`axis.jsonKey()`. This boundary keeps type-safe iteration in computation while preserving
backwards-compatible camelCase output, and avoids the silent `Object.equals()` false-return
bug that occurs when enum types and String keys diverge during a partial migration.
