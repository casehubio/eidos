# casehub-eidos -- Consumer Guide

> Structured agent identity for LLM agents on the CaseHub platform.

**GitHub:** [casehubio/eidos](https://github.com/casehubio/eidos)
**Tier:** Extension (API: Tier 1 pure Java; Runtime: Quarkus extension)

---

## Purpose

Any Quarkus app that depends on `casehub-eidos` can register agents with structured descriptors (identity, slot, capabilities, disposition, goals, constraints), discover agents by slot, capability, domain, or goal, probe whether a declared capability is currently operable, render system prompts from descriptors in multiple formats, and track personality alignment via disposition health probing.

---

## Modules to Depend On

| artifactId | When to use | What you get |
|---|---|---|
| `casehub-eidos-api` | Always -- compile dependency | Domain types: `AgentDescriptor`, `AgentCapability`, `AgentDisposition`, `AgentGoal`, `AgentConstraint`, `AgentMatch`, `AgentQuery`, `AgentRegistry`, `CapabilityHealth`, `SystemPromptRenderer`, `VocabularyRegistry`, `TemplateRegistry`, `DispositionHealth`, `DispositionEvolution`, `AgentStateStore`, `BehavioralSignalStore`, `DispositionSignalStore`. SPIs in `api.spi`: `AgentDescriptorRegistrar`, `VocabularyRegistrar`, `TemplateRegistrar`. Utilities: `CapabilityResolver`, `BehavioralExpectations`, `AgentDescriptorComparator`. Sealed types: `MatchDegree`, `CapabilityStatus`, `DispositionStatus`, `EvolutionResult`. Pure Java, no CDI. |
| `casehub-eidos` | Always -- runtime dependency | Quarkus extension: CDI registry, health implementations, renderer, JPA persistence, Flyway migrations. `@DefaultBean` for all SPIs. |
| `casehub-eidos-memory` | Tests and prototyping | `@Alternative @Priority(1)` in-memory implementations: `InMemoryAgentRegistry`, `InMemoryTemplateRegistry`, `InMemoryAgentStateStore`, `InMemoryBehavioralSignalStore` (per-signal TTL via `@ConfigProperty`), `InMemoryDispositionSignalStore` (ConcurrentHashMap + AtomicInteger, no TTL), `InMemoryRenderedPromptCache`. Activate by adding as dependency. |
| `casehub-eidos-vocab` | Optional -- domain vocabularies | Well-known vocabularies: `SvoTerm`, `ConscientiousnessTerm`, `CasehubSlotTerm`, `BelbinTerm` (9 team roles), `DiscTerm` (4 DISC types, `axisExactMatch`), `ThomasKilmannTerm` (5 conflict modes), `CasehubCapabilityTerm` (hierarchical capability taxonomy), `JungianFunctionTerm` (8 cognitive functions with `axisExactMatch`, `shadow()`, `opposite()`, `compatibleAuxiliaries()`), `MbtiTypeTerm` (16 MBTI types with `specializes()` to `JungianFunctionTerm`, `defaultProfile()`), `JungianEvolutionType` (4 JPAF reflection types). All optional -- consumers define their own vocabularies. |
| `casehub-eidos-annotations` | Optional -- annotation-driven identity | Quarkus extension: `@Identity`, `@Disposition`, `@AgentGoals`, `@AgentConstraints` generate `AgentDescriptorRegistrar` beans at build time. Also processes `@Discoverable` (from eidos-api) for capability auto-registration. Depends on `casehub-eidos`. |
| `casehub-eidos-graph` | Optional -- knowledge graph | Graph SPIs: `AgentGraphStore` (write task/outcome/attestation events), `AgentGraphQuery` (read agent history, rank agents by outcome), `AgentGraphBackfill` (ledger ingestion), `TaskSemanticEnricher` (application-tier enrichment). JPA persistence with Flyway V3. Activates by classpath presence. |

**Maven coordinates:** `groupId: io.casehub`, root package: `io.casehub.eidos`, API package: `io.casehub.eidos.api`, SPI package: `io.casehub.eidos.api.spi`.

---

## Key Abstractions

### AgentDescriptor

Structured agent identity record with these fields:

- **Identity:** `agentId`, `name`, `version`, `provider`, `modelFamily`, `modelVersion`, `weightsFingerprint`
- **Slot:** open `String` -- domain-defined (e.g. `"planner"`, `"reviewer"`). Platform never constrains.
- **Capabilities:** `List<AgentCapability>` -- what the agent can do. Names must be unique within a descriptor.
- **Disposition:** `AgentDisposition` -- behavioural profile with weighted multi-valued axes.
- **Goals:** `List<AgentGoal>` -- standing, identity-level goals (BDI-inspired). Unique names enforced.
- **Constraints:** `List<AgentConstraint>` -- operational limits with severity (HARD/SOFT). Unique names enforced.
- **Briefing:** free-text field for holistic personality prose (supports newlines).
- **Templates:** `List<TemplateRef>` -- references to reusable prose templates with variable substitution.
- **Tenancy:** `tenancyId` is always required. All operations are tenancy-scoped.

**Vocabulary resolution:** `domainVocabulary` sets the default vocabulary URI for all fields. Per-field overrides: `slotVocabulary`, `dispositionVocabulary`. Per-axis override: `axisVocabularies(Map<DispositionAxis, String>)` -- most specific wins.

**Visibility:** Goals and constraints carry `Visibility.PRIVATE` or `Visibility.PUBLIC`. Private items appear in the owning agent's prompt only -- absent from `A2A_CARD`.

### AgentCapability

Declares a named capability with operational metadata:

- `name` (String, required) -- capability identifier, unique within descriptor
- `description` (String, optional, <=500 chars) -- human- and machine-readable capability semantics
- `capabilityVocabulary` (String, optional) -- grounds the capability name in a registered vocabulary for subsumption matching
- `qualityHint` (Double, 0--1) -- self-declared quality prior
- `latencyHintP50Ms` (Long) -- expected p50 latency in milliseconds
- `costHint` (String) -- free-text cost signal
- `inputTypes` / `outputTypes` (List<String>) -- type schema for capability I/O
- `tags` (List<String>) -- free-form tags
- `epistemicDomains` (Map<String, Double>) -- domain-specific confidence, e.g. `{"java": 0.95, "rust": 0.42}`
- `excludedDomains` (Set<String>) -- domains the agent refuses to handle; validated against `epistemicDomains` at construction (no overlap allowed). Persisted as `@ElementCollection` in JPA, used by `AgentQuery.taskDomain` pre-filter.

`epistemicDomains` qualifies *how well* the agent handles the declared capability in specific subject domains -- it is not a list of separate capabilities.

### AgentDisposition

Multi-valued behavioural profile with weighted axes:

- `socialOrient`, `ruleFollowing`, `riskAppetite`, `autonomy`, `conflictMode` -- each `List<DispositionValue>` (term + weight 0.0--1.0)
- `delegation` -- boolean, whether the agent may delegate tasks (separate from axes)
- `dispositionProfile` -- holistic cognitive profile as `List<DispositionValue>`, used for Jungian personality modeling

Single-valued convenience: `Builder.socialOrient("collaborative")` creates a single `DispositionValue` with weight 1.0. Multi-valued: `Builder.socialOrient(DispositionValue.of("collaborative"), new DispositionValue("independent", 0.3))`.

`get(DispositionAxis)` maps enum value to the corresponding axis field. `primaryTerm(DispositionAxis)` returns the first term or null.

### AgentGoal

First-class goal record: `name`, `description`, `priority` (PRIMARY/SECONDARY), `visibility` (PUBLIC/PRIVATE), `capabilities` (List<String>, maps to declared `AgentCapability.name()` on the same descriptor). Empty list = cross-cutting goal affected by any capability failure; non-empty = affected only when a listed capability fails. Cross-validated at construction: every name must match a declared capability. BDI-inspired naming: goals are what the agent *wants* (standing, identity-level). `GoalContext` on `AgentPromptContext` is what the agent is doing right now (ephemeral, per-invocation).

### AgentConstraint

Operational constraint record: `name`, `description`, `visibility` (PUBLIC/PRIVATE), `severity` (HARD/SOFT). Severity-discriminated rendering: HARD constraints render with emphasis, SOFT constraints render as preferences.

### AgentRegistry

SPI for register, findById, and find(AgentQuery).

- `register(AgentDescriptor)` -- stores or updates a descriptor
- `findById(agentId, tenancyId)` -- exact lookup, returns `Optional<AgentDescriptor>`
- `find(AgentQuery)` -- returns `List<AgentMatch>`, each carrying the matched `AgentDescriptor` + nullable `ResolvedCapability` (the declared capability that matched and the OWLS-MX `MatchDegree`). Results ordered by match quality (Exact > Plugin > Specialization) when capability is queried.

### AgentQuery

Query builder with factory methods:

| Factory | Filters by |
|---|---|
| `bySlot(slot, tenancyId)` | Slot only |
| `byCapability(capabilityName, tenancyId)` | Capability name |
| `bySlotAndCapability(slot, capabilityName, tenancyId)` | Both slot and capability |
| `byCapabilityAndDomain(capabilityName, taskDomain, tenancyId)` | Capability + domain pre-filter (agents whose `excludedDomains` contain the taskDomain are filtered out) |
| `byGoal(goalName, tenancyId)` | Goal name (exact match, no subsumption) |
| `all(tenancyId)` | All descriptors in tenancy |

`tenancyId` is always required -- all queries are tenancy-scoped.

### CapabilityHealth

SPI: `probe(AgentDescriptor, capabilityTag, ProbeContext)` returns sealed `CapabilityStatus`.

**ProbeContext:** `taskDomain` is the *subject domain* of the task (e.g. `"rust"` within a `"code-review"` capability). `taskMetadata` carries additional key-value context. Factory: `ProbeContext.of(taskDomain)`.

**CapabilityStatus** sealed hierarchy:

| Status | Meaning |
|---|---|
| `Ready` | Capability is operable |
| `Degraded(reason, detail)` | Agent is in temporary degradation (rate-limited, context-exhausted, overloaded, domain-mismatch) |
| `Unavailable(reason)` | Capability not declared |
| `EpistemicallyWeak(domain, confidence)` | Confidence below threshold for the task domain |
| `Excluded(domain, source, declineCount)` | Domain excluded -- source is `DECLARED` (from `excludedDomains`) or `LEARNED` (from accumulated DECLINE signals) |
| `BehavioralViolation(violations, kind)` | Compliance violation -- `PER_DIMENSION` (single dimension spike) or `AGGREGATE` (cross-dimensional drift) |

**Probe order:** Degraded -> Unavailable -> Excluded(DECLARED) -> Excluded(LEARNED) -> EpistemicallyWeak -> BehavioralViolation -> Ready.

**Engine integration:** `WorkOrchestrator` calls `probe()` at dispatch time. Workers without a descriptor skip the probe and are assumed capable.

### DispositionHealth

Disposition-level health probing for personality alignment:

- `probe(AgentDescriptor, ProbeContext)` returns sealed `DispositionStatus`:
  - `Aligned(effectiveWeights)` -- personality is stable
  - `Drifted(effectiveWeights, mostActivated, driftMagnitude)` -- personality has drifted from baseline
  - `EvolutionPending(type, candidateFunction, effectiveWeights)` -- structural personality change detected

### DispositionEvolution

- `evaluate(AgentDescriptor, EvolutionPending)` returns sealed `EvolutionResult`:
  - `Evolved(newProfile, previousTypeLabel, newTypeLabel)` -- profile updated
  - `Dampened(decayFactor)` -- evolution rejected, signals decayed

### SystemPromptRenderer

SPI: `render(AgentDescriptor, AgentPromptContext)` returns `RenderedPrompt(content, format, descriptorHash, contextHash, enriched)`.

**AgentPromptContext:** carries `Optional<GoalContext>`, `List<Resource>`, `situationalContext`, `RenderFormat`. Re-renderable as agent context evolves. Factory: `AgentPromptContext.forFormat(format)`.

**Three output formats:**

| Format | Purpose | Includes |
|---|---|---|
| `MARKDOWN` | Claude and markdown-capable models | Structured markdown with capability names, descriptions, I/O types. Numeric routing signals suppressed. |
| `PROSE` | OpenAI, Gemini, Grok, Qwen, Mistral, Llama | Flowing paragraphs. Numeric routing signals suppressed. |
| `A2A_CARD` | Machine-readable agent card (JSON) | Full routing signals: `qualityHint`, `latencyHintP50Ms`, `costHint`, `epistemicDomains`, `inputTypes`/`outputTypes`, `excludedDomains`. Slot and disposition with vocabulary context. `frameworks` array of actively-instantiated vocabulary URIs. |

**Semantic enrichment:** Two-step render pipeline -- structural assembly then optional LangChain4j `ChatModel` semantic pass. Falls back to structural output when no `ChatModel` is available. `RenderedPrompt.enriched` is true when LLM enrichment was applied.

**Caching:** `RenderedPromptCache` SPI enables prompt caching. `descriptorHash` and `contextHash` on `RenderedPrompt` enable cache invalidation.

### VocabularyRegistry

SPI for term registration, resolution, hierarchy, and cross-vocabulary equivalence.

**Registration:** Vocabularies are Java enums implementing `VocabularyTerm`, annotated with `@VocabularyMetadata(uri, name, version, description)`. `CdiVocabularyRegistry` discovers `VocabularyRegistrar` CDI beans at startup.

**Resolution:** `resolve(vocabUri, value)` resolves primary value or alias. Typed overload: `resolve(Class<T>, value)` returns typed constant.

**Cross-vocabulary equivalence:** `equivalentValues(fromUri, value, toUri)` for axis-unaware mapping. `equivalentValues(fromUri, value, toUri, DispositionAxis)` for axis-aware mapping. Via `VocabularyTerm.exactMatch()` and `VocabularyTerm.axisExactMatch()`.

**Hierarchy:** XKOS-style hierarchy via `VocabularyTerm.specializes()` enables cross-vocabulary subsumption. `match()` returns `MatchDegree` (Exact, Plugin(depth), Specialization(depth), None). `subsumes()`, `ancestors()`, `descendants()`, `expandForMatchingByVocabulary()`.

### TemplateRegistry / DescriptorTemplate

Reusable prose fragments for agent system prompts:

- `DescriptorTemplate(id, name, parameters, content)` -- template with `${variable}` placeholders
- `TemplateRef(templateId, args)` on `AgentDescriptor.templates()` -- reference with argument binding
- `TemplateRegistry` SPI: `register(template)`, `resolve(id)`, `all()`
- Three-layer validation: compact constructors (structural), registry (placeholder vs declared parameters), collector (ref resolution + arg completeness)
- Rendered after capabilities, before disposition/briefing in MARKDOWN/PROSE; excluded from A2A_CARD

### AgentDescriptorRegistrar SPI

`@FunctionalInterface` SPI in `api.spi`: `List<AgentDescriptor> descriptors()`. CDI beans implementing this interface are discovered at startup and their descriptors bulk-registered. Bootstrap validates no duplicate `(agentId, tenancyId)` pairs.

**ClasspathYamlDescriptorRegistrar** -- `@ApplicationScoped` implementation scanning `META-INF/eidos/descriptors.yaml` from the classpath. Multiple YAML files across JARs are merged. Supports full `AgentDescriptor` fields including `briefing`, `axisVocabularies`, `disposition` (with `mbtiType` and `dispositionProfile`), and `capabilities` (with `excludedDomains`).

**Annotation-driven registration** (requires `casehub-eidos-annotations`) -- annotate an interface with `@Identity(slot = "analyst")` and optionally `@Disposition`, `@AgentGoals`, `@AgentConstraints`, `@Discoverable`. The build extension generates `AgentDescriptorRegistrar` beans at build time. `agentId` defaults to kebab-cased class name (acronym-aware: `HTMLParser` → `html-parser`). `tenancyId` from MicroProfile config `casehub.eidos.annotations.default-tenancy-id` (default: `"default"`). When vocabulary modules are on the classpath, disposition terms are validated at build time.

### AgentDescriptorComparator

Utility for drift detection between desired and actual descriptors. `compare(AgentDescriptor desired, AgentDescriptor actual)` returns `ComparisonResult(List<FieldDrift>)` -- empty list means match. Compares 19 top-level fields, 7 disposition fields, 10 per-capability fields, 3 per-goal fields, and 3 per-constraint fields.

---

## Knowledge Graph (casehub-eidos-graph)

Optional module providing agent task and outcome tracking:

- `AgentGraphStore` -- write interface: `recordTask(AgentTask)`, `recordOutcome(AgentTaskId, AgentOutcome)`, `linkAttestation(AgentTaskId, AttestationRef)`
- `AgentGraphQuery` -- read interface: `agentHistory(agentId, tenancyId)`, `historyByCapability(agentId, capabilityTag, tenancyId)`, `topAgentsByOutcome(capabilityTag, taskDomain, tenancyId, limit)` (Wilson lower bound ranking), `attestationsFor(agentId, tenancyId)`
- `AgentGraphBackfill` -- ledger ingestion: `backfillAgent(agentId, tenancyId)`, `backfillAll(tenancyId)`, `backfillDelta(tenancyId, since)`
- `TaskSemanticEnricher` -- application-tier enrichment: `dispositionAxes(capabilityTag, taskDomain)`, `semanticallyEquivalent(domainA, domainB)`, `significance(capabilityTag, taskDomain)`

Activates by classpath presence. JPA-backed with Flyway V3 migration. Runtime provides `NoOp*` `@DefaultBean` implementations for all four SPIs when the graph module is absent.

---

## Tenancy

`AgentDescriptor.tenancyId` is always required. All registry queries are tenancy-scoped -- `AgentQuery.tenancyId` is mandatory. The registry never returns descriptors across tenancy boundaries.

---

## Schema Management

JPA/Flyway -- version range V1--V999 in `classpath:db/eidos/migration`. Current migrations:

| Version | Module | Content |
|---|---|---|
| V1 | runtime | Full schema -- agent descriptor, capabilities, degradation state, capability specialization, behavioral signals, goals, constraints, disposition signals, goal signals |
| V3 | graph | Agent graph (task, outcome, attestation tables) |

No existing installations -- no deployed instances in production. All schema changes go directly into base migration files.

---

## Configuration

### Capability Health (casehub-eidos runtime)

| Property | Type | Default | Purpose |
|---|---|---|---|
| `casehub.eidos.epistemic.weak-threshold` | Runtime (`@ConfigProperty`) | `0.3` | Confidence below this triggers `EpistemicallyWeak` |

### Capability Health (PreferenceProvider -- per-tenancy)

| Preference Key | Default | Purpose |
|---|---|---|
| `casehub.eidos / specialization.exclude-threshold` | `3` | DECLINE count triggering `Excluded(LEARNED)` |
| `casehub.eidos / behavioral.compliance-violation-threshold` | `3` | Per-dimension VIOLATED count triggering `BehavioralViolation(PER_DIMENSION)` |
| `casehub.eidos / behavioral.aggregate-violation-threshold` | `5` | Cross-dimensional VIOLATED total triggering `BehavioralViolation(AGGREGATE)` |

### Disposition Health (PreferenceProvider -- per-tenancy)

| Preference Key | Default | Purpose |
|---|---|---|
| `casehub.eidos / disposition.reinforcement-delta` | `0.06` | Per-activation weight increment (JPAF parameter) |
| `casehub.eidos / disposition.over-reinforcement-threshold` | `0.50` | Effective weight ceiling for dominant function |
| `casehub.eidos / disposition.decay-factor` | `0.20` | Retention fraction for activation count decay (0.0 = instant reset, 1.0 = no decay) |

### Build-Time

| Property | Type | Default | Purpose |
|---|---|---|---|
| `casehub.eidos.reactive.enabled` | Build-time | `false` | Legacy guard -- JPA implementations activate when this is `false` or absent. Reactive tier has been retired; leave at default. |

---

## What This Repo Does NOT Do

- Trust scoring -- that is `casehub-ledger`
- Agent-to-agent messaging -- that is `casehub-qhorus`
- Work item or case orchestration -- that is `casehub-engine`
- Constrain slot or disposition vocabulary -- consumers define their own; `casehub-eidos-vocab` is optional
- Put `AgentDescriptor` or vocabulary types in `casehub-platform-api` -- descriptor types are Eidos domain types; repos that need them depend on `casehub-eidos-api` directly
