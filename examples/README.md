# CaseHub Eidos — Examples

Executable examples demonstrating eidos capabilities. Each example is a `@QuarkusTest`
that runs with in-memory stores (zero datasource) and well-known vocabularies.

## Running

```bash
# All examples
JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn test -pl examples/agent-scenarios,examples/customer-support-triage,examples/code-review-agent,examples/medical-scribe-agent,examples/creative-director-agent,examples/child-companion-bot,examples/tutor-agent --also-make
```

## Annotation Examples

Annotation-driven agent identity — declare an agent with `@Identity`, `@Disposition`,
`@Discoverable`, `@AgentGoals`, and `@AgentConstraints`. The Quarkus build extension
generates `AgentDescriptorRegistrar` beans that auto-register with the `AgentRegistry`.

See [CAPABILITY-MATRIX.md](CAPABILITY-MATRIX.md) for the full capability cross-reference.

| Module | Domain | What it demonstrates |
|--------|--------|---------------------|
| `customer-support-triage` | Support routing | `@Identity` only — bare minimum, auto-derived id/name, no disposition |
| `code-review-agent` | Dev tooling | Identity + all 5 disposition axes + capabilities + provider/modelFamily |
| `medical-scribe-agent` | Healthcare | Explicit id/name, jurisdiction, data handling, version, PRIVATE goals/constraints, goal-capability mapping |
| `creative-director-agent` | Content creation | dispositionProfile, styleProfile, delegation, all 4 vocabulary URIs |
| `child-companion-bot` | Child care | Warm disposition, HARD safety constraints, PRIVATE escalation to parents |
| `tutor-agent` | Education | Axes + dispositionProfile coexistence, multi-capability goal mapping |

## Programmatic Examples (agent-scenarios)

Core identity, discovery, vocabulary, health probing, disposition, rendering, and graph
scenarios using the programmatic API (no annotations).

| Example | Descriptor | Registry | Vocabulary | Health | Stores | Disposition | Rendering | Templates | Tenancy | Graph |
|---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| CapabilitySubsumption | ● | ● | ● | ● | | | | | ● | |
| CapabilityVocabularyIntegration | | | ● | | | | | | | |
| CostAwareRouting | ● | ● | ● | | | | ● | | | |
| CrossVocabularyAgentDesign | ● | ● | ● | | | ● | ● | | | |
| CrossVocabularyDiscovery | | | ● | | | | | | | |
| BelbinDiscTkVocabularyDiscovery | | | ● | | | | | | | |
| DescriptorTemplate | ● | ● | | | | | ● | ● | | |
| DispositionVocabulary | | | ● | | | | | | | |
| DraftHouseReviewer | ● | ● | ● | | | | ● | ● | ● | |
| FullProbe | ● | ● | ● | ● | ● | | | | | |
| JungianPersonality | ● | ● | ● | | ● | ● | ● | | | |
| LearnedSpecialization | ● | ● | ● | ● | ● | | | | | |
| MultiAgentTeam | ● | ● | | | | | | | ● | |
| PersonalityAwareDispatch | ● | ● | | | | ● | ● | | | |
| SystemPromptRenderer | ● | ● | ● | | | ● | ● | | | |
| V1Graph | | | | | | | | | | ● |
| V2Graph | | | | | | | | | | ● |

## Module Index

| Module | Focus | Dependencies |
|---|---|---|
| `agent-scenarios` | Core identity, discovery, vocabulary, health probing, disposition, rendering, graph | eidos + eidos-memory + eidos-vocab |
| `customer-support-triage` | Minimal annotation-driven identity | eidos-annotations + eidos-memory |
| `code-review-agent` | Common-case annotations (identity + disposition + capabilities) | eidos-annotations + eidos-memory |
| `medical-scribe-agent` | Compliance-focused annotations (jurisdiction, goals, constraints, visibility) | eidos-annotations + eidos-memory |
| `creative-director-agent` | Personality-driven annotations (profiles, vocabularies, delegation) | eidos-annotations + eidos-memory + eidos-vocab |
| `child-companion-bot` | Safety-focused annotations (HARD constraints, PRIVATE escalation) | eidos-annotations + eidos-memory |
| `tutor-agent` | Adaptive annotations (axes + profile coexistence, multi-capability goals) | eidos-annotations + eidos-memory |
