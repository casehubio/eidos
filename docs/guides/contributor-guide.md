# casehub-eidos -- Contributor Guide

> Internal architecture, SPIs, and extension points for platform builders working on eidos itself.

**GitHub:** [casehubio/eidos](https://github.com/casehubio/eidos)

---

## Module Structure

| Module | artifactId | Type | Purpose |
|---|---|---|---|
| `api/` | `casehub-eidos-api` | Pure Java, no CDI | SPIs and domain types -- `AgentDescriptor`, `AgentRegistry`, `CapabilityHealth`, `VocabularyRegistry`, `SystemPromptRenderer`, `AgentStateStore`, `BehavioralSignalStore`, `DispositionHealth`, `DispositionEvolution`, `DispositionSignalStore`, `RenderedPromptCache`, `TemplateRegistry`; sealed types: `MatchDegree` (Exact, Plugin, Specialization, None), `CapabilityStatus`, `DispositionStatus`, `EvolutionResult`; utilities: `CapabilityResolver`, `BehavioralExpectations`, `AgentDescriptorComparator`; template types: `DescriptorTemplate`, `TemplateRef`; SPI package (`api.spi`): `AgentDescriptorRegistrar`, `VocabularyRegistrar`, `TemplateRegistrar`; graph SPIs (also in api/): `AgentGraphStore`, `AgentGraphQuery`, `AgentGraphBackfill`, `TaskSemanticEnricher` |
| `runtime/` | `casehub-eidos` | Quarkus extension | CDI registry, health implementations, renderer, JPA persistence, Flyway migrations. Subpackages: `registry/jpa/` (JPA registries, entity classes, mapper), `vocabulary/` (`CdiVocabularyRegistry`), `health/` (default health + signal stores + compliance), `health/jpa/` (JPA-backed signal stores), `registrar/` (bootstrap + YAML loading + descriptor collector), `template/` (`CdiTemplateRegistry`, `ClasspathYamlTemplateRegistrar`), `renderer/` (`EidosSystemPromptRenderer`, enrichment pipeline, `JsonExtractionUtil`, `NoOpRenderedPromptCache`), `preferences/` (preference keys and defaults), `graph/` (NoOp graph implementations) |
| `persistence-memory/` | `casehub-eidos-memory` | Optional module | `@Alternative @Priority(1)` in-memory implementations: `InMemoryAgentRegistry`, `InMemoryTemplateRegistry`, `InMemoryAgentStateStore`, `InMemoryBehavioralSignalStore` (per-signal TTL via `@ConfigProperty`), `InMemoryDispositionSignalStore` (ConcurrentHashMap + AtomicInteger, no TTL), `InMemoryRenderedPromptCache` |
| `deployment/` | `casehub-eidos-deployment` | Quarkus build step | `EidosProcessor` -- registers the `eidos` feature and includes Flyway SQL resources in native image builds |
| `vocab/` | `casehub-eidos-vocab` | Optional module | Well-known vocabularies: `SvoTerm`, `ConscientiousnessTerm`, `CasehubSlotTerm`, `BelbinTerm` (9 Belbin team roles), `DiscTerm` (4 DISC types, `axisExactMatch`), `ThomasKilmannTerm` (5 conflict modes), `CasehubCapabilityTerm` (hierarchical capability taxonomy), `JungianFunctionTerm` (8 cognitive functions with `axisExactMatch`, `shadow()`, `opposite()`, `compatibleAuxiliaries()`), `MbtiTypeTerm` (16 MBTI types with `specializes()` to `JungianFunctionTerm`, `defaultProfile()`), `JungianEvolutionType` (4 JPAF reflection types), `FunctionCategory` (JUDGING/PERCEIVING), `FunctionAttitude` (INTROVERTED/EXTRAVERTED). Each vocabulary enum accompanied by a `VocabularyRegistrar` bean; Jandex-indexed for CDI discovery |
| `graph/` | `casehub-eidos-graph` | Optional module (Jandex library) | JPA-backed knowledge graph: `JpaAgentGraphStore`, `JpaAgentGraphQuery`, `JpaAgentGraphBackfill` implementations. Entity classes: `AgentTaskEntity`, `AgentOutcomeEntity`, `AttestationRefEntity`. Flyway V3 migration. Activates by classpath presence |
| `eval/` | `casehub-eidos-eval` | Test-only, not deployed | Offline quality evaluation harness (see Eval Harness section below) |
| `examples/agent-scenarios/` | -- | Test-only | `@QuarkusTest` integration scenarios covering team composition, cross-vocabulary discovery, epistemic matching, tenancy isolation, disposition, degradation/recovery, template composition, capability subsumption, Jungian personality, learned exclusion, DraftHouse reviewer descriptors |

---

## Internal Architecture

### BehavioralSignalStore

Signal-parameterized SPI in `casehub-eidos-api` for learned behavioral patterns. Accumulates DECLINE/SUCCESS signals for routing specialization and COMPLIANT/VIOLATED signals for behavioral compliance checking.

- `BehavioralSignal` enum: `DECLINE`, `SUCCESS`, `COMPLIANT`, `VIOLATED`
- `record(agentId, tenancyId, capabilityName, qualifier, signal)` -- qualifier is task domain for DECLINE/SUCCESS, `ComplianceDimension` key for COMPLIANT/VIOLATED
- `clear(agentId, tenancyId, capabilityName, signal)` -- remove all learned data for a signal type
- `learned(agentId, tenancyId, capabilityName, signal)` returns `Map<String, Integer>` -- qualifier to count
- `count(agentId, tenancyId, capabilityName, qualifier, signal)` returns `int`
- Per-signal TTL independently configurable via `@ConfigProperty`
- V5 schema migration (table with `signal_type` discriminator column)

**capabilityName contract:** All methods require the agent's *declared* capability name (from `AgentCapability.name()`), not a query/lookup term. When the caller has a query tag, use `CapabilityResolver.resolve()` to obtain the declared capability first.

`ComplianceDimension` constants: `LATENCY`, `ATTESTATION_RATE`, `DELEGATION`, `ESCALATION`; conventions: `ATTESTOR_ID`, `TRUST_DIMENSION_PREFIX`, `LATENCY_VIOLATION_MULTIPLIER`.

`BehavioralExpectations` -- static utility extracting testable expectations from descriptor fields: `latencyBound(AgentCapability)`, `delegationExpected(AgentDisposition)`, `escalationExpected(AgentDisposition, String, VocabularyRegistry)`.

`ComplianceAttestations` -- bridges compliance observations to casehub-ledger `LedgerAttestation` (attestorId=`eidos:compliance`, trustDimension=`behavioral:<dimension>`).

`CapabilityResolver` -- shared subsumption resolution for probe and recording paths; callers of `record()` must pass the declared capability name.

Probe integration:
- Step 4: DECLINE count >= `EXCLUDE_THRESHOLD` (per-tenancy PreferenceProvider) triggers `Excluded(LEARNED, count)`
- Step 6: Per-dimension VIOLATED >= `COMPLIANCE_VIOLATION_THRESHOLD` triggers `BehavioralViolation(PER_DIMENSION)`; aggregate total >= `AGGREGATE_VIOLATION_THRESHOLD` triggers `BehavioralViolation(AGGREGATE)`

CDI ladder: `NoOpBehavioralSignalStore @DefaultBean`, `InMemoryBehavioralSignalStore @Alternative` in `casehub-eidos-memory`, `JpaBehavioralSignalStore @IfBuildProperty` in runtime.

### Behavioral Contracts

The descriptor IS the behavioral contract. Its fields (`latencyHintP50Ms`, `qualityHint`, `excludedDomains`, `delegation`, disposition axes) are implicit behavioral expectations.

Agents with `delegation: true` or vocabulary terms that imply supervision (e.g. `ConscientiousnessTerm.DELEGATING`, `DiscTerm.S_STEADY`) are expected to delegate and escalate appropriately. `BehavioralExpectations.escalationExpected()` derives these expectations from vocabulary terms via `VocabularyTerm.impliesSupervision()`.

Compliance tracking dimensions: `ComplianceDimension.DELEGATION` (whether agent delegates when expected), `ComplianceDimension.ESCALATION` (whether agent escalates when supervision is required).

Two-layer violation check in `DefaultCapabilityHealth.probe()` Step 6:
- Per-dimension threshold (default 3, per-tenancy via PreferenceProvider) catches single-dimension spikes -> `BehavioralViolation(exceeding, PER_DIMENSION)`
- Aggregate threshold (default 5, per-tenancy via PreferenceProvider) catches cross-dimensional drift -> `BehavioralViolation(all, AGGREGATE)`

`ViolationKind { PER_DIMENSION, AGGREGATE }` discriminates the two modes.

### AgentStateStore

SPI: `record(agentId, tenancyId, DegradationReason, expiresAt)`, `query(agentId, tenancyId)` returns `Optional<DegradationReason>`, `clear(agentId, tenancyId)`. `DegradationReason` is a top-level enum in `casehub-eidos-api`: `RATE_LIMITED`, `CONTEXT_EXHAUSTED`, `OVERLOADED`, `DOMAIN_MISMATCH`.

CDI ladder: `NoOpAgentStateStore @DefaultBean` (no tracking), `InMemoryAgentStateStore @Alternative @Priority(1)` in `casehub-eidos-memory` (TTL-based ConcurrentHashMap, entries expire on `query()`), `JpaAgentStateStore @IfBuildProperty` in runtime.

`DefaultCapabilityHealth` checks store first at probe time -- degraded state takes precedence over Ready/EpistemicallyWeak.

### DispositionSignalStore

SPI for cognitive function activation tracking, used by JPAF personality adaptation:

- `recordActivation(agentId, tenancyId, functionTerm)` -- increment activation count
- `activationCounts(agentId, tenancyId)` -- returns `Map<String, Integer>` of function term to count
- `decay(agentId, tenancyId, decayFactor)` -- multiplicative decay; `decayFactor` is retention fraction (0.0 = instant reset, 1.0 = no decay)
- `clear(agentId, tenancyId)` -- remove all activation data

No TTL -- decay is explicit via `decay()` or `clear()`.

CDI ladder: `NoOpDispositionSignalStore @DefaultBean`, `InMemoryDispositionSignalStore @Alternative` in `casehub-eidos-memory` (ConcurrentHashMap + AtomicInteger), `JpaDispositionSignalStore @IfBuildProperty` in runtime (Flyway V9).

### DispositionHealth and Evolution

**DefaultDispositionHealth** (`@ApplicationScoped`):

1. Reads `dispositionProfile` from descriptor -- returns `Aligned(empty)` if profile is empty
2. Loads activation counts from `DispositionSignalStore`
3. Computes effective weights: `effectiveWeight(f) = baseWeight(f) + activationCount(f) * REINFORCEMENT_DELTA`
4. Normalizes weights to sum to 1.0
5. Returns `Aligned` if no activations exist
6. Checks over-reinforcement: dominant's effective weight >= `OVER_REINFORCEMENT_THRESHOLD` triggers `Drifted`
7. Checks 4 evolution conditions (each yields `EvolutionPending` with specific `EvolutionType`):
   - **Dominant-auxiliary swap** -- auxiliary surpassed dominant
   - **Dominant replacement** -- non-stack function surpassed dominant (shadow detection via `VocabularyTerm.opposite()`)
   - **Auxiliary replacement** -- non-stack function surpassed auxiliary
   - **Structural reorganization** -- multiple non-stack functions exceeded threshold

**DefaultDispositionEvolution** (`@ApplicationScoped`):

- `evaluate(descriptor, EvolutionPending)` produces a new `dispositionProfile` with reordered weights (`Evolved`) or applies decay to signal store and returns `Dampened(decayFactor)`
- Decay factor configurable per-tenancy via `DispositionPreferenceKeys.DECAY_FACTOR` (default 0.20)

**EvolutionType** -- interface in api, `JungianEvolutionType` enum in vocab: `DOMINANT_AUXILIARY_SWAP`, `DOMINANT_REPLACEMENT`, `AUXILIARY_REPLACEMENT`, `STRUCTURAL_REORGANIZATION`.

### CapabilityResolver

Static utility in `casehub-eidos-api`. Provides shared subsumption resolution for both probe and recording paths.

- `match(capability, tag, registry)` returns `MatchDegree` (Exact, Plugin(depth), Specialization(depth), None)
- `resolve(capabilities, capabilityTag, registry)` returns best `ResolvedCapability(capability, degree)` preserving match metadata

Eliminates duplicated matching logic between `CapabilityHealth.probe()` and `BehavioralSignalStore` lookups.

### MatchDegree Sealed Hierarchy

Sealed interface: `Exact`, `Plugin(depth)`, `Specialization(depth)`, `None`. Implements `Comparable<MatchDegree>` with OWLS-MX ordering: Exact < Plugin < Specialization < None (lower depth within same type ranks higher).

Used in `ResolvedCapability` (result of `CapabilityResolver.resolve()`) and `AgentMatch` (result of `AgentRegistry.find()`). Results ordered by match quality when capability is queried.

### A2A_CARD Format

Machine-readable JSON format for agent-to-agent capability negotiation. Structure:
- `slot` with `vocabularyUri`/`vocabularyName`
- Per-axis `disposition` objects with vocabulary context
- `frameworks` array -- deduplicated index of actively-instantiated vocabulary URIs
- Full capability routing signals: `qualityHint` (Double, 0--1), `latencyHintP50Ms` (Long), `costHint` (String), `epistemicDomains` (Map<String,Double>), `excludedDomains` (Set<String>), `inputTypes`/`outputTypes` (String lists)

Separate `A2ASemanticEnrichmentStep` handles per-capability descriptions in `A2A_CARD` format. LLM-enriched description wins when non-blank; declared `description` is the fallback.

PROSE and MARKDOWN suppress all numeric routing signals (protocol PP-20260611-228599).

### Render Pipeline

`EidosRenderPipeline` orchestrates the two-stage rendering:

1. **Stage one (structural):** `StageOneResult` assembles descriptor fields into format-specific structure
2. **Stage two (semantic):** Optional LangChain4j `ChatModel` enrichment

`EidosSystemPromptRenderer` (`@ApplicationScoped`, not `@DefaultBean` -- consumers displace with `@DefaultBean` fallback per Pattern B) delegates to the pipeline.

Key renderer components:
- `SemanticEnrichmentStep` -- produces `dispositionNarrative` and `goalNarrative`. `buildEnrichmentPayload()` scoped to disposition-relevant fields only. Enriched section replaces structural section only when LLM produced non-empty output.
- `A2ASemanticEnrichmentStep` -- per-capability description enrichment for A2A_CARD format
- `A2AEnrichment` / `SemanticEnrichment` -- enrichment result records
- `JsonExtractionUtil` -- extracts JSON from LLM responses (handles markdown code fences, retries)
- `NoOpRenderedPromptCache` -- default cache implementation that does not cache

`RenderedPrompt.enriched` (boolean) -- set true when `SemanticEnrichmentStep` produced a disposition narrative or `A2ASemanticEnrichmentStep` ran. `descriptorHash` and `contextHash` enable cache invalidation.

### Descriptor Bootstrap

`AgentDescriptorBootstrap` (`@Observes StartupEvent`, `@IfBuildProperty` blocking-mode gated):

1. Discovers all `AgentDescriptorRegistrar` CDI beans
2. Collects all descriptors via `descriptors()` calls
3. Validates no duplicate `(agentId, tenancyId)` pairs across all registrars
4. Bulk-registers all descriptors with `AgentRegistry`

`DescriptorCollector` handles descriptor post-processing:
- Template ref resolution and argument completeness validation
- Auto-derivation: when `dispositionProfile` is populated and axes are empty, projects function weights onto 5 axes via cross-vocabulary `equivalentValues()`, populates `axisVocabularies`

`ClasspathYamlDescriptorRegistrar` (`@ApplicationScoped`):
- Scans `META-INF/eidos/descriptors.yaml` from classpath
- Multiple YAML files across JARs are merged
- Supports all `AgentDescriptor` fields including `mbtiType` (auto-resolves to `MbtiTypeTerm.defaultProfile()`) and `dispositionProfile`

### Template System

`CdiTemplateRegistry` (`@ApplicationScoped`):
- Discovers `TemplateRegistrar` CDI beans at startup
- Validates placeholder parameters match declared parameters on registration

`ClasspathYamlTemplateRegistrar`:
- Scans `META-INF/eidos/templates.yaml` from classpath

Template rendering:
- Single-pass regex substitution (`${variable}` to arg value)
- Rendered after capabilities, before disposition/briefing in MARKDOWN/PROSE
- Excluded from A2A_CARD

### Preferences Architecture

Behavioral thresholds use `PreferenceProvider` (per-tenancy resolution via ancestor-chain walk) instead of flat `@ConfigProperty`:

| Class | Key | Default | Domain |
|---|---|---|---|
| `ExcludeThresholdPreference` | `specialization.exclude-threshold` | 3 | Capability |
| `ComplianceViolationThresholdPreference` | `behavioral.compliance-violation-threshold` | 3 | Capability |
| `AggregateViolationThresholdPreference` | `behavioral.aggregate-violation-threshold` | 5 | Capability |
| `ReinforcementDeltaPreference` | `disposition.reinforcement-delta` | 0.06 | Disposition |
| `OverReinforcementThresholdPreference` | `disposition.over-reinforcement-threshold` | 0.50 | Disposition |
| `DecayFactorPreference` | `disposition.decay-factor` | 0.20 | Disposition |

All under namespace `casehub.eidos`. Resolved via `SettingsScope.root()`.

Only `casehub.eidos.epistemic.weak-threshold` remains as a flat `@ConfigProperty` (runtime, default 0.3).

### Jungian Personality Framework (JPAF)

Implemented across vocab, api, and runtime modules:

**Vocabulary layer (casehub-eidos-vocab):**
- `JungianFunctionTerm` -- 8 cognitive functions (Ti, Te, Fi, Fe, Ni, Ne, Si, Se) with `FunctionCategory` (JUDGING/PERCEIVING), `FunctionAttitude` (INTROVERTED/EXTRAVERTED), cross-vocabulary `axisExactMatch` mappings to `ConscientiousnessTerm` and `ThomasKilmannTerm`, `shadow()`, `opposite()`, `compatibleAuxiliaries()`
- `MbtiTypeTerm` -- 16 MBTI types, each grounded in dominant+auxiliary `JungianFunctionTerm` via `specializes()`. `defaultProfile()` generates a weighted `List<DispositionValue>` with dom=0.35, aux=0.20, remaining evenly distributed
- `JungianEvolutionType` -- 4 JPAF reflection types implementing `EvolutionType`

**API layer:**
- `DispositionValue(term, weight)` -- weighted function entries (0.0--1.0)
- `AgentDisposition.dispositionProfile` -- holistic cognitive function profile
- `DispositionSignalStore` -- activation tracking
- `DispositionHealth` / `DispositionEvolution` -- probing and evolution SPIs
- `EvolutionType` -- interface for evolution type extensibility
- `VocabularyTerm.opposite()`, `VocabularyTerm.defaultProfile()` -- Jungian extension points

**Runtime layer:**
- `DefaultDispositionHealth` -- effective weight computation, evolution condition detection
- `DefaultDispositionEvolution` -- profile restructuring or dampening
- `DescriptorCollector` -- auto-derivation of 5-axis disposition from `dispositionProfile` via cross-vocabulary equivalence

**YAML support:**
- `ClasspathYamlDescriptorRegistrar` supports `mbtiType` field (resolves to `MbtiTypeTerm.defaultProfile()`) and direct `dispositionProfile` specification

### Reactive Tier (Retired)

The reactive tier (`ReactiveAgentRegistry`, `ReactiveCapabilityHealth`, `ReactiveAgentStateStore`, `ReactiveAgentGraphQuery`, `ReactiveRenderedPromptCache`) was deleted in parent#384. All Hibernate Reactive, Mutiny, and reactive-panache dependencies were removed.

The `casehub.eidos.reactive.enabled` build-time property still exists as a guard for JPA implementations -- they activate when it is `false` or absent (the default). This is a vestigial guard from the blocking/reactive dual-mode era. New code does not need to reference it.

### Graph Module Internals

`casehub-eidos-graph` provides JPA-backed implementations:

- `JpaAgentGraphStore` / `JpaAgentGraphQuery` / `JpaAgentGraphBackfill` -- full JPA implementations
- Entities: `AgentTaskEntity`, `AgentOutcomeEntity`, `AttestationRefEntity`
- Flyway V3 migration (`graph/src/main/resources/db/eidos/migration/V3__agent_graph.sql`)
- `TaskSemanticEnricher` -- application-tier enrichment at query time (dispositionAxes, semanticEquivalence, significance)

When the graph module is absent from the classpath, runtime provides `NoOp` defaults:
- `NoOpAgentGraphStore`, `NoOpAgentGraphQuery`, `NoOpAgentGraphBackfill`, `NoOpTaskSemanticEnricher` (all `@DefaultBean`)

---

## Eval Harness

Test-only module (`casehub-eidos-eval`) for offline quality evaluation of system prompt rendering. Not deployed.

### Judges

| Judge | What it measures |
|---|---|
| `PromptJudge` | Structural completeness -- missing capabilities, format compliance |
| `ProximityJudge` | Descriptor-axis completeness -- whether rendered output expresses all populated `DispositionAxis` values with appropriate vocabulary |
| `DispositionPresenceJudge` | Disposition term presence in rendered output |
| `VocabularyExpressivenessJudge` | Vocabulary term preservation in rendered output |
| `TraitExpressionJudge` | Trait expression accuracy in rendered output |
| `PairContrastJudge` | Behavioral contrast between variant pair agents (carries `DispositionAxis primaryAxis` + `List<String> scenarioQuestions`) |
| `BehavioralJudge` | Behavioral validation via `AgentProviderChatModel` bridge |
| `MbtiAlignmentJudge` | MBTI-70 questionnaire alignment -- validates personality expression matches declared MBTI type |
| `FunctionActivationJudge` | TAA (Trait Activation Accuracy) -- cognitive function activation accuracy in specific scenarios |
| `PersonalityEvolutionJudge` | PSA (Personality Shift Accuracy) -- personality shift structural validity |

### Agent Profiles

**Standard profiles** (10 YAML profiles in `eval/src/test/resources/profiles/`):
clinical-researcher, customer-support-agent, data-engineer-autonomous, data-engineer-directed, product-manager, security-analyst-defensive, security-analyst-proactive, sw-engineer-bold, sw-engineer-careful, technical-writer. All include `briefing` field and full capability descriptions.

**Jungian profiles** (8 YAML profiles in `eval/src/test/resources/jungian-profiles/`):
enfj-lead, entj-architect, entp-explorer, estp-debugger, infp-advocate, intj-strategist, intp-analyst, istj-ops. Each specifies a Jungian cognitive function stack.

**Function activation scenarios** (24 scenarios in `eval/src/test/resources/function-scenarios/scenarios.yaml`):
Scenario-function pairs for testing cognitive function activation accuracy.

### Variant Pairs

3 variant pairs for behavioral contrast testing:
- RISK_APPETITE (sw-engineer-bold vs sw-engineer-careful)
- RULE_FOLLOWING (security-analyst-defensive vs security-analyst-proactive)
- AUTONOMY (data-engineer-autonomous vs data-engineer-directed)

`VariantPair` carries `DispositionAxis primaryAxis` + `List<String> scenarioQuestions`. All compute-path types use `DispositionAxis` enum.

### Multi-Backend Support

| Backend | Profile | Notes |
|---|---|---|
| Claude CLI | `eval` | Default -- requires claude CLI configured |
| Ollama | `eval,eval-ollama` | Requires Ollama on localhost:11434; `timeout=300s` required |
| GPULlama3 / TornadoVM Metal | `eval,eval-gpullama3` | Apple Silicon; requires `TORNADOVM_HOME` |
| Jlama NEON | `eval,eval-jlama` | Apple Silicon, Q4 models via native NEON |

`AgentProviderChatModel` bridge (`ChatModel` to `AgentProvider` SPI, `@DefaultBean`) enables all backends.

Independent judge comparison: `evaluateWithIndependentJudge()` uses saved renders-cache to judge Claude's renders with an alternative model (e.g. Qwen) for self-evaluation bias detection. `RenderCacheEntry` record (`caseName`, `format`, `content`, `boolean enriched`) with `toRenderedPrompt()`. Configurable renders-cache path via `casehub.eval.renders-cache.path`.

Baseline: `eval-baseline-2026-06-10.json` committed as reference.

---

## Dependencies

### Depends On

| Repo | How |
|---|---|
| `casehub-ledger` | Runtime dep -- `casehub-eidos` (runtime) depends on it for `LedgerAttestation` integration via `ComplianceAttestations` |
| `casehub-platform` | Runtime dep -- `casehub-eidos` (runtime) depends on `platform-api` for `PreferenceProvider`, `PreferenceKey`, `SettingsScope` |
| `dev.langchain4j:langchain4j-core` | `casehub-eidos` (runtime) -- `ChatModel` interface for optional LLM semantic pass |

### Depended On By

| Repo | Module | Nature |
|---|---|---|
| `casehub-engine` | `engine-api` | Optional compile dep -- `AgentDescriptor` on `Worker`; `CapabilityHealth.probe()` in `WorkOrchestrator` |
| `casehub-desiredstate` | runtime | Consumes `AgentDescriptorComparator` for drift detection |

---

## Flyway Migrations

| Version | Module | Content |
|---|---|---|
| V1 | runtime | Initial schema -- agent descriptor, capabilities |
| V2 | runtime | Agent degradation state |
| V3 | graph | Agent graph (task, outcome, attestation tables) |
| V4 | runtime | Capability specialization table |
| V5 | runtime | Behavioral signal table (signal_type discriminator) |
| V6 | runtime | Capability description column |
| V7 | runtime | Descriptor templates |
| V8 | runtime | Goals and constraints tables |
| V9 | runtime | Disposition signal table |

No existing installations -- no deployed instances in production. All schema changes go directly into base migration files.

---

## Integration Scenarios

`examples/agent-scenarios/` provides `@QuarkusTest` integration scenarios:

| Test | What it covers |
|---|---|
| `MultiAgentTeamTest` | Team composition, registry queries |
| `CrossVocabularyDiscoveryTest` | Cross-vocabulary equivalence discovery |
| `BelbinDiscTkVocabularyDiscoveryTest` | Belbin/DISC/Thomas-Kilmann vocabulary interop |
| `CapabilityVocabularyIntegrationTest` | Vocabulary-grounded capability matching |
| `CapabilitySubsumptionScenarioTest` | Subsumption hierarchy matching |
| `EpistemicDomainMatchingTest` | Epistemic domain confidence filtering |
| `TenancyIsolationTest` | Cross-tenancy isolation verification |
| `DispositionVocabularyTest` | Disposition axis vocabulary resolution |
| `DegradationAndRecoveryTest` | AgentStateStore TTL lifecycle |
| `DescriptorTemplateScenarioTest` | Template composition, ordering, sharing, YAML |
| `SystemPromptRendererTest` | Multi-format rendering verification |
| `LearnedExclusionSubsumptionTest` | Subsumption-aware learned exclusion |
| `DraftHouseReviewerScenarioTest` | YAML persona loading (DraftHouse reviewer descriptors) |
| `JungianPersonalityScenarioTest` | Jungian function activation, MBTI alignment, evolution |

---

## Design Documents

- [CLAUDE.md](https://raw.githubusercontent.com/casehubio/eidos/main/CLAUDE.md) -- stack, module coordinates, key design decisions
- [examples/README.md](https://raw.githubusercontent.com/casehubio/eidos/main/examples/README.md) -- capability coverage table across test scenarios
- [docs/personality-frameworks.md](https://raw.githubusercontent.com/casehubio/eidos/main/docs/personality-frameworks.md) -- maps personality and team-role frameworks to `AgentDescriptor` fields, documents JPAF (Jungian Personality Assessment Framework) findings
