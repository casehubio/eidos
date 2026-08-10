# CaseHub Eidos — Examples

Executable examples demonstrating eidos capabilities. Each example is a `@QuarkusTest`
that runs with in-memory stores (zero datasource) and well-known vocabularies.

## Running

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn test -pl examples/agent-scenarios --also-make
```

## Capability Matrix

Examples as rows, capability areas as columns. Each `●` marks a capability the example exercises.

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
