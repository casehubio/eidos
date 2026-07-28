# CaseHub Eidos — Examples

Executable examples demonstrating eidos capabilities. Each example is a `@QuarkusTest`
that runs with in-memory stores (zero datasource) and well-known vocabularies.

## Running

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn test -pl examples/agent-scenarios --also-make
```

## Capability Coverage

| Capability | Example | Status |
|---|---|---|
| AgentDescriptor creation | MultiAgentTeamTest | ✅ |
| AgentRegistry.register | MultiAgentTeamTest | ✅ |
| AgentRegistry.findById | MultiAgentTeamTest, TenancyIsolationTest | ✅ |
| AgentQuery.bySlot | MultiAgentTeamTest, DraftHouseReviewerScenarioTest | ✅ |
| AgentQuery.byCapability | MultiAgentTeamTest, DraftHouseReviewerScenarioTest | ✅ |
| AgentQuery.bySlotAndCapability | MultiAgentTeamTest | ✅ |
| AgentQuery.all | TenancyIsolationTest | ✅ |
| AgentQuery.goalName | — | Covered in runtime unit tests |
| Tenancy isolation | TenancyIsolationTest | ✅ |
| VocabularyRegistry.resolve | CrossVocabularyDiscoveryTest, DispositionVocabularyTest | ✅ |
| VocabularyRegistry.equivalentValues | CrossVocabularyDiscoveryTest | ✅ |
| VocabularyRegistry.subsumes | CapabilityVocabularyIntegrationTest | ✅ |
| VocabularyRegistry.match | CapabilityVocabularyIntegrationTest | ✅ |
| VocabularyRegistry.ancestors/descendants | CapabilityVocabularyIntegrationTest | ✅ |
| VocabularyRegistry.expandForMatchingByVocabulary | CapabilityVocabularyIntegrationTest | ✅ |
| Cross-vocabulary specialization (XKOS) | CapabilityVocabularyIntegrationTest | ✅ |
| SVO vocabulary | CrossVocabularyDiscoveryTest | ✅ |
| CasehubSlot vocabulary | CrossVocabularyDiscoveryTest | ✅ |
| Conscientiousness vocabulary (12 terms, 4 axes) | DispositionVocabularyTest | ✅ |
| Belbin vocabulary | BelbinDiscTkVocabularyDiscoveryTest | ✅ |
| DISC vocabulary + axisExactMatch | BelbinDiscTkVocabularyDiscoveryTest | ✅ |
| Thomas-Kilmann vocabulary | BelbinDiscTkVocabularyDiscoveryTest | ✅ |
| CasehubCapability vocabulary (hierarchy) | CapabilityVocabularyIntegrationTest | ✅ |
| Jungian vocabulary (8 functions, shadow, compatibleAuxiliaries) | JungianPersonalityScenarioTest | ✅ |
| MBTI vocabulary (16 types, specializes, defaultProfile) | JungianPersonalityScenarioTest | ✅ |
| Jungian → Conscientiousness cross-vocab projection | JungianPersonalityScenarioTest | ✅ |
| Jungian → Thomas-Kilmann cross-vocab projection | JungianPersonalityScenarioTest | ✅ |
| Weighted DispositionValue on AgentDisposition | JungianPersonalityScenarioTest | ✅ |
| dispositionProfile (holistic cognitive profile) | JungianPersonalityScenarioTest | ✅ |
| CapabilityHealth.probe → Ready | DegradationAndRecoveryTest, EpistemicDomainMatchingTest | ✅ |
| CapabilityHealth.probe → Degraded | DegradationAndRecoveryTest | ✅ |
| CapabilityHealth.probe → Unavailable | EpistemicDomainMatchingTest | ✅ |
| CapabilityHealth.probe → EpistemicallyWeak | EpistemicDomainMatchingTest | ✅ |
| CapabilityHealth.probe → Excluded (learned) | LearnedExclusionSubsumptionTest | ✅ |
| Capability subsumption matching | CapabilitySubsumptionScenarioTest | ✅ |
| AgentStateStore (degradation + TTL + clear) | DegradationAndRecoveryTest | ✅ |
| BehavioralSignalStore (learned exclusion) | LearnedExclusionSubsumptionTest | ✅ |
| DispositionSignalStore (activation, drift, evolution) | JungianPersonalityScenarioTest | ✅ |
| DispositionHealth.probe → Aligned/Drifted/EvolutionPending | JungianPersonalityScenarioTest | ✅ |
| DispositionEvolution.evaluate → Evolved | JungianPersonalityScenarioTest | ✅ |
| SystemPromptRenderer (MARKDOWN) | SystemPromptRendererTest, JungianPersonalityScenarioTest | ✅ |
| SystemPromptRenderer (PROSE) | SystemPromptRendererTest | ✅ |
| SystemPromptRenderer (A2A_CARD) | SystemPromptRendererTest, JungianPersonalityScenarioTest | ✅ |
| Cognitive profile rendering (MARKDOWN) | JungianPersonalityScenarioTest | ✅ |
| Cognitive profile rendering (A2A_CARD) | JungianPersonalityScenarioTest | ✅ |
| Weighted axes rendering ("primarily X, with Y tendencies") | JungianPersonalityScenarioTest | ✅ |
| Vocabulary-resolved labels in rendered output | SystemPromptRendererTest | ✅ |
| A2A card framework references | SystemPromptRendererTest | ✅ |
| Descriptor hash/context hash | SystemPromptRendererTest | ✅ |
| Descriptor templates | DescriptorTemplateScenarioTest | ✅ |
| Goals and constraints | DraftHouseReviewerScenarioTest | ✅ |
| DraftHouse multi-reviewer scenario | DraftHouseReviewerScenarioTest | ✅ |
| Knowledge graph (V1 Wilson score routing) | V1GraphScenarioTest | ✅ |
| Knowledge graph (V2 semantic enrichment) | V2GraphScenarioTest | ✅ |

## Module Index

| Module | Focus | Dependencies |
|---|---|---|
| `agent-scenarios` | Core identity, discovery, vocabulary, health probing, disposition, rendering, graph | eidos + eidos-memory + eidos-vocab |
