# Protocol Index — casehub-eidos

## Eval harness

| File | Rule Summary | Applies To |
|------|-------------|------------|
| [eval/behavioral-judge-blind.md](eval/behavioral-judge-blind.md) | Behavioral judge prompts must never include expected trait values | BehavioralJudge |
| [eval/disposition-axis-string-boundary.md](eval/disposition-axis-string-boundary.md) | AXES constants typed; result maps camelCase; axis.jsonKey() at every boundary | Eval computation code |
| [eval/eval-profile-briefing-from-vocabulary-gaps.md](eval/eval-profile-briefing-from-vocabulary-gaps.md) | briefing derives from vocabularyGap:FULL entries | All eval YAML agent profiles |

→ Full eval protocol listing: [eval/INDEX.md](eval/INDEX.md)

## Renderer

| File | Rule Summary | Applies To |
|------|-------------|------------|
| [renderer/capability-metadata-rendering.md](renderer/capability-metadata-rendering.md) | Capability numeric metadata renders only in A2A_CARD; PROSE/MARKDOWN render names only | EidosSystemPromptRenderer |
| [renderer/a2a-structural-assembly-hash-coverage.md](renderer/a2a-structural-assembly-hash-coverage.md) | assembleA2aCard() fields read from descriptor directly must be in A2A_CARD hash payload | EidosRenderPipeline A2A_CARD path |

→ Full renderer protocol listing: [renderer/INDEX.md](renderer/INDEX.md)
