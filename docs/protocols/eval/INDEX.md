# Eval Protocols — casehub-eidos-eval

Rules specific to the offline quality evaluation harness.

| File | Rule Summary | Applies To |
|------|-------------|------------|
| [behavioral-judge-blind.md](behavioral-judge-blind.md) | Behavioral judge prompts must never include expected trait values | BehavioralJudge and any future blind-compare judge |
| [disposition-axis-string-boundary.md](disposition-axis-string-boundary.md) | AXES constants are List<DispositionAxis>; result maps stay Map<String,...>; use axis.jsonKey() at every boundary | AXES/NUMERIC_AXES constants, judge parse(), computeDiagnoses() |
| [eval-profile-briefing-from-vocabulary-gaps.md](eval-profile-briefing-from-vocabulary-gaps.md) | briefing field derives from vocabularyGap:FULL entries — highest-value concepts the structured axes cannot express | All YAML agent profiles in eval/src/test/resources/profiles/ |
