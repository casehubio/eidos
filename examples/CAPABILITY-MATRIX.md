# Annotation Capability Matrix

Maps every annotation capability to the example that demonstrates it and the test that verifies it.

## Examples

| Example | Domain | Module |
|---------|--------|--------|
| **Customer Support Triage** | Support routing | `examples/customer-support-triage` |
| **Code Review Agent** | Dev tooling | `examples/code-review-agent` |
| **Medical Scribe Agent** | Healthcare compliance | `examples/medical-scribe-agent` |
| **Creative Director Agent** | Content creation | `examples/creative-director-agent` |
| **Child Companion Bot** | Child care | `examples/child-companion-bot` |
| **Tutor Agent** | Education | `examples/tutor-agent` |

## Capability → Example Matrix

| Capability | Annotation | Customer Support | Code Review | Medical Scribe | Creative Director | Child Companion | Tutor | Deployment Tests |
|-----------|------------|:----------------:|:-----------:|:--------------:|:-----------------:|:---------------:|:-----:|:----------------:|
| Slot | `@Identity(slot)` | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | EidosAnnotationsProcessorTest |
| Briefing | `@Identity(briefing)` | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | EidosAnnotationsProcessorTest |
| Explicit ID | `@Identity(id)` | — | — | ✓ | — | — | — | EidosAnnotationsProcessorTest |
| Explicit name | `@Identity(name)` | — | — | ✓ | — | — | — | EidosAnnotationsProcessorTest |
| Provider | `@Identity(provider)` | — | ✓ | — | — | — | — | — |
| Model family | `@Identity(modelFamily)` | — | ✓ | — | — | — | — | — |
| Jurisdiction | `@Identity(jurisdiction)` | — | — | ✓ | — | — | — | EidosAnnotationsProcessorTest |
| Data handling policy | `@Identity(dataHandlingPolicy)` | — | — | ✓ | — | — | — | — |
| Domain vocabulary | `@Identity(vocabulary)` | — | — | — | ✓ | — | — | — |
| Slot vocabulary | `@Identity(slotVocabulary)` | — | — | — | ✓ | — | — | — |
| Disposition vocabulary | `@Identity(dispositionVocabulary)` | — | — | — | ✓ | — | — | — |
| Style vocabulary | `@Identity(styleVocabulary)` | — | — | — | ✓ | — | — | — |
| Version | `@Identity(version)` | — | — | ✓ | — | — | — | — |
| Social orientation | `@Disposition(socialOrient)` | — | ✓ | ✓ | — | ✓ | ✓ | EidosAnnotationsProcessorTest |
| Rule following | `@Disposition(ruleFollowing)` | — | ✓ | ✓ | — | ✓ | ✓ | EidosAnnotationsProcessorTest |
| Risk appetite | `@Disposition(riskAppetite)` | — | ✓ | ✓ | — | ✓ | ✓ | — |
| Autonomy | `@Disposition(autonomy)` | — | ✓ | ✓ | — | ✓ | ✓ | — |
| Conflict mode | `@Disposition(conflictMode)` | — | ✓ | ✓ | — | ✓ | ✓ | — |
| Delegation | `@Disposition(delegation)` | — | — | — | ✓ | — | — | — |
| Disposition profile | `@Disposition(dispositionProfile)` | — | — | — | ✓ | — | ✓ | — |
| Style profile | `@Disposition(styleProfile)` | — | — | — | ✓ | — | — | — |
| Capabilities | `@Discoverable(capabilities)` | — | ✓ | ✓ | ✓ | ✓ | ✓ | EidosAnnotationsProcessorTest |
| Goal declaration | `@AgentGoalDef(name, description)` | — | — | ✓ | — | ✓ | ✓ | EidosAnnotationsProcessorTest |
| Goal priority | `GoalPriority.PRIMARY / SECONDARY` | — | — | ✓ | — | ✓ | ✓ | EidosAnnotationsProcessorTest |
| Goal visibility | `Visibility.PRIVATE` | — | — | ✓ | — | ✓ | — | EidosAnnotationsProcessorTest |
| Goal-capability mapping | `@AgentGoalDef(capabilities)` | — | — | ✓ | — | ✓ | ✓ | EidosAnnotationsProcessorTest |
| Constraint declaration | `@AgentConstraintDef(name, description)` | — | — | ✓ | — | ✓ | ✓ | EidosAnnotationsProcessorTest |
| Constraint severity | `ConstraintSeverity.HARD / SOFT` | — | — | ✓ | — | ✓ | ✓ | EidosAnnotationsProcessorTest |
| Constraint visibility | `Visibility.PRIVATE` | — | — | ✓ | — | ✓ | — | EidosAnnotationsProcessorTest |
| Auto ID derivation | Class name → kebab-case | ✓ | ✓ | — | ✓ | ✓ | ✓ | EidosAnnotationsProcessorTest |
| Auto name derivation | Class name → display name | ✓ | ✓ | — | ✓ | ✓ | ✓ | EidosAnnotationsProcessorTest |
| Identity-only (no disposition) | `@Identity` without `@Disposition` | ✓ | — | — | — | — | — | EidosAnnotationsProcessorTest |
| Axes + profile coexistence | `@Disposition` axes and `dispositionProfile` together | — | — | — | — | — | ✓ | — |
| Duplicate ID detection | Build-time error on colliding ids | — | — | — | — | — | — | BuildTimeValidationTest |
| Goal-capability cross-validation | Build-time error on invalid capability ref | — | — | — | — | — | — | GoalCapabilityValidationTest |
| Vocabulary term validation | Build-time warning on unknown disposition terms | — | — | — | — | — | — | EidosAnnotationsProcessorTest |
| `@Discoverable` without `@Identity` | Build-time warning | — | — | — | — | — | — | EidosAnnotationsProcessorTest |

## Coverage Summary

| Category | Total | In Examples | In Deployment Tests Only |
|----------|-------|-------------|--------------------------|
| `@Identity` fields | 13 | 13 | — |
| `@Disposition` fields | 8 | 8 | — |
| `@Discoverable` | 1 | 1 | — |
| `@AgentGoals` / `@AgentGoalDef` | 4 | 4 | — |
| `@AgentConstraints` / `@AgentConstraintDef` | 3 | 3 | — |
| Build-time behaviours | 7 | 3 | 4 |
| **Total** | **36** | **32** | **4** |

## How to Run

```bash
# All annotation examples
JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn test -pl examples/customer-support-triage,examples/code-review-agent,examples/medical-scribe-agent,examples/creative-director-agent,examples/child-companion-bot,examples/tutor-agent

# All deployment tests (includes build-time validation)
JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn test -pl annotations-deployment

# Everything (annotation examples + deployment tests)
JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn test -pl annotations-deployment,examples/customer-support-triage,examples/code-review-agent,examples/medical-scribe-agent,examples/creative-director-agent,examples/child-companion-bot,examples/tutor-agent
```
