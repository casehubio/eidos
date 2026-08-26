# eidos Workspace
**Name:** eidos

**Project repo:** /Users/mdproctor/claude/casehub/eidos
**Workspace type:** public

## Session Start

Run `add-dir /Users/mdproctor/claude/public/casehub/eidos` before any other work.

## Artifact Locations

| Skill | Writes to |
|-------|-----------|
| brainstorming (specs) | `specs/` (workspace staging) |
| update-design / arc42stories | `ARC42STORIES.MD` (workspace staging) |
| writing-plans (plans) | `plans/` |
| handover | `HANDOFF.md` |
| idea-log | `IDEAS.md` |
| design-snapshot | `snapshots/` |
| java-update-design / update-primary-doc | `design/JOURNAL.md` (created by `epic`) |
| adr | `adr/` (workspace staging) |
| write-blog | `blog/` |

## Structure

- `HANDOFF.md` — session handover (single file, overwritten each session)
- `IDEAS.md` — idea log (single file)
- `specs/` — brainstorming / design specs (staging; promoted to project `docs/specs/` at epic close)
- `plans/` — implementation plans (ephemeral; stay in workspace only)
- `snapshots/` — design snapshots with INDEX.md (auto-pruned, max 10)
- `adr/` — architecture decision records (staging; promoted to project `docs/adr/` at epic close)
- `blog/` — project diary entries with INDEX.md
- `design/` — epic journal (created by `epic` at branch start)

## Git Discipline

Two git repositories are active in every session:
- **Workspace** (`/Users/mdproctor/claude/public/casehub/eidos`) — staging area for specs and ADRs; permanent home for blog, handover, plans, snapshots
- **Project repo** (`/Users/mdproctor/claude/casehub/eidos`) — source code + promoted specs (`docs/specs/`) + promoted ADRs (`docs/adr/`)

Before any git operation, run `git rev-parse --show-toplevel` to confirm which repo is currently active. Do not assume — the session may have opened in either.

- Source code commits → project repo (`origin` = mdproctor/eidos, `upstream` = casehubio/eidos)
- Specs and ADRs → workspace first, then promote to project repo at epic close

## Rules

- **Specs and ADRs are project knowledge** — final home is the project repo under `docs/specs/` and `docs/adr/`
- The workspace `specs/` and `adr/` directories are staging areas only — skills write there first
- **Promotion at epic close**: copy spec/ADR files to project repo, commit there; leave workspace copies in place
- Plans (`plans/`) are ephemeral — workspace only, never promoted
- Blog, handover, snapshots, design journal — workspace only, never promoted

## Routing

| Artifact   | Destination | Notes |
|------------|-------------|-------|
| adr        | project     | lands in `docs/adr/` — promoted at epic close |
| specs      | project     | lands in `docs/specs/` — promoted at epic close |
| arc42stories | project   | `ARC42STORIES.MD` at project root — promoted at epic close |
| blog       | project     | lands in `docs/blog/` — promoted at work end |
| plans      | workspace   | stay in workspace permanently |
| design     | workspace   | epic journal stays in workspace |
| snapshots  | workspace   | stay in workspace permanently |
| handover   | workspace   | |

---

# CaseHub Eidos — Claude Code Project Guide

## Platform Docs
- [Platform Index](https://raw.githubusercontent.com/casehubio/parent/main/docs/INDEX.md) — discovery index (start here)
- [Building Platform](https://raw.githubusercontent.com/casehubio/parent/main/docs/guides/building-platform.md) — platform contributor guide

## Repo Guide

This repo owns its own documentation, synced to parent via CI:
- `docs/guides/consumer-guide.md` — for app builders: modules, APIs, quick start
- `docs/guides/contributor-guide.md` — for platform builders: architecture, SPIs, internals

Update the relevant guide in the same session when implementation changes modules, SPIs, or public APIs. Do not defer — drift compounds.

Read `docs/guides/consumer-guide.md` for app-level work. Only read `docs/guides/contributor-guide.md` when modifying this repo's internals or extension points.

## Reference Documents (casehub-parent)

| Document | What it covers |
|----------|---------------|
| `../garden/docs/protocols/casehub/FOUNDATION-INDEX.md` | CaseHub foundation protocols |

---

## Project Type

type: java

**Stack:** Java 21 (on Java 26 JVM), Quarkus 3.32.2

---

## What This Project Is

`casehub-eidos` is a CaseHub extension providing structured agent identity for LLM agents.
Any Quarkus app adds `io.casehub:casehub-eidos` as a dependency and gets:

- **AgentDescriptor** — structured agent description (identity, slot, capabilities, disposition, goals, constraints)
- **VocabularyRegistry** — pluggable domain vocabulary system; vocabularies are Java enums implementing `VocabularyTerm`; `CdiVocabularyRegistry` discovers `Instance<VocabularyRegistrar>` CDI beans at startup; axis-aware `equivalentValues()` with `DispositionAxis`; XKOS-style hierarchy via `VocabularyTerm.specializes()` — global DAG across vocabulary boundaries (cross-vocabulary subsumption: application terms can specialize foundation terms); two-pass registration (term registration → hierarchy construction); per-vocabulary index injection for bidirectional `match()` across vocabularies; inline collision detection (native-vs-injected, injected-vs-injected); `subsumes()`, `match()` → `MatchDegree` (Exact, Plugin(depth), Specialization(depth), None), `ancestors()`, `descendants()`, `expandForMatchingByVocabulary()` (groups cross-vocabulary terms by declaring vocabulary URI)
- **AgentRegistry** — store and query descriptors (blocking + reactive); `find(AgentQuery)` returns `List<AgentMatch>` carrying descriptor + `ResolvedCapability` (matched capability and OWLS-MX `MatchDegree`); results ordered by match quality when capability is queried; `AgentQuery.goalName` filters by declared goal name (exact match, no subsumption — goals are identity-level, not vocabulary-grounded); `AgentDescriptor.hasGoal(String)`/`hasConstraint(String)` convenience methods
- **CapabilityHealth** — two-layer capability model: declared (descriptor) vs. operable now (runtime probe); `AgentStateStore` SPI records degradation state with TTL; `BehavioralSignalStore` SPI accumulates learned domain signals via `BehavioralSignal { DECLINE, SUCCESS, COMPLIANT, VIOLATED }` (store-owned per-signal TTL); two-layer compliance: declared (expectations) + learned (signals). `NoOpAgentStateStore` @DefaultBean, `NoOpBehavioralSignalStore` @DefaultBean, `InMemoryAgentStateStore` + `InMemoryBehavioralSignalStore` @Alternative in casehub-eidos-memory, `JpaBehavioralSignalStore` @IfBuildProperty blocking-mode in runtime (persistent per-signal TTL-based tracking). `DefaultCapabilityHealth.probe()` is subsumption-aware via `CapabilityResolver.resolve()`: when the probed capability tag is not declared exactly, it finds the structurally closest vocabulary-grounded capability via `VocabularyRegistry.match()` (best-match-by-depth selection). `CapabilityResolver` (static utility in api/, Tier 1) provides shared subsumption resolution for both probe and recording paths — `resolve()` returns `ResolvedCapability(capability, degree)` preserving match metadata; callers of `BehavioralSignalStore.record()` must pass the declared capability name and `ComplianceDimension` qualifier (use `CapabilityResolver.resolve()` to obtain capability name from a query tag). Probe steps: Degraded → Unavailable → Excluded(DECLARED) → Excluded(LEARNED) → EpistemicallyWeak → BehavioralViolation → Ready
- **DispositionHealth** — disposition-level health probing paralleling CapabilityHealth; `DispositionSignalStore` SPI accumulates function activation counts (no TTL, explicit decay/clear); `DispositionHealth.probe()` returns sealed `DispositionStatus` (Aligned/Drifted/EvolutionPending); `DispositionEvolution.evaluate()` returns sealed `EvolutionResult` (Evolved/Dampened); `EvolutionType` interface in api, `JungianEvolutionType` enum in vocab (4 JPAF reflection types). `DefaultDispositionHealth` @ApplicationScoped computes effective weights (base + activationCount × REINFORCEMENT_DELTA, normalized), checks 4 evolution conditions: dominant-auxiliary swap, dominant replacement (shadow via `VocabularyTerm.opposite()`), auxiliary replacement, structural reorganization. Over-reinforcement ceiling via `DispositionPreferenceKeys.OVER_REINFORCEMENT_THRESHOLD`. CDI ladder: `NoOpDispositionSignalStore` @DefaultBean, `InMemoryDispositionSignalStore` @Alternative in casehub-eidos-memory, `JpaDispositionSignalStore` @IfBuildProperty. Auto-derivation in `DescriptorCollector`: when dispositionProfile is populated and axes empty, projects function weights onto 5 axes via cross-vocabulary `equivalentValues()`, populates `axisVocabularies`.
- **AgentDescriptorRegistrar** — declarative SPI (`List<AgentDescriptor> descriptors()`); consumers provide `@ApplicationScoped` beans or a `META-INF/eidos/descriptors.yaml` classpath resource (YAML-driven via `ClasspathYamlDescriptorRegistrar`); `AgentDescriptorBootstrap` (@Observes StartupEvent, @IfBuildProperty blocking-mode gated) auto-discovers all registrars and registers descriptors with duplicate `(agentId, tenancyId)` detection
- **SystemPromptRenderer** — renders `AgentDescriptor + AgentPromptContext` (goal, resources, situational context) into a format-specific system prompt; `EidosSystemPromptRenderer` @ApplicationScoped (no @DefaultBean — consumers displace with @DefaultBean fallback per Pattern B) supports three formats: `MARKDOWN` (LLM or structural fallback), `PROSE` (LLM or structural fallback), `A2A_CARD` (JSON machine-readable card with slot, vocabulary-grounded disposition, frameworks index, and full capability routing signals — `qualityHint`, `latencyHintP50Ms`, `costHint`, `epistemicDomains` — plus `inputTypes`/`outputTypes` type schema)
- **AgentSelector** — SPI for selecting the best agent from `AgentRegistry.find()` results; `SimpleAgentSelector` `@DefaultBean` in runtime (health filtering + optional trust ranking via `Instance<TrustScoreSource>`); `EngineAwareAgentSelector` `@Alternative @Priority(1)` in casehub-eidos-routing (bridges to engine's `AgentRoutingStrategy` trust maturity model). `SelectionContext(tenancyId, capabilityName, taskDomain)` carries query context. `AgentSelection` sealed result: `Selected(agent, resolvedCapability, trustScore, reason)`, `NoneQualified(reason)`, `Escalated(capabilityName, kind, reason)`. `EscalationKind { BORDERLINE_STALEMATE, NO_QUALIFIED_AGENT }`.
- **casehub-eidos-annotations** — opt-in Quarkus extension for annotation-driven agent identity; `@Identity`, `@Disposition`, `@AgentGoals`, `@AgentConstraints` generate `AgentDescriptorRegistrar` beans via `EidosAnnotationsProcessor` build extension; `@Discoverable` (in eidos-api) declares capability names for registry auto-registration; hybrid vocabulary validation at build time when vocab modules are on classpath; `AnnotatedAgentConfig` data class + `EidosAnnotationsRecorder` handle build→runtime boundary via Quarkus recorder pattern; `EidosAnnotationProcessedBuildItem` enables coordination with blocks build extension
- **casehub-eidos-vocab** — optional well-known vocabularies: SVO, Conscientiousness, CasehubSlot, Belbin (team roles with axisExactMatch to Conscientiousness + Thomas-Kilmann), DISC (behavioral styles), Thomas-Kilmann (conflict modes), CasehubCapability (hierarchical capability taxonomy), JungianFunctionTerm (8 cognitive functions with cross-vocab axisExactMatch, shadow(), opposite(), compatibleAuxiliaries()), MbtiTypeTerm (16 MBTI types with specializes() to JungianFunctionTerm, defaultProfile()), JungianEvolutionType (4 JPAF reflection types), BigFiveTerm (O/E/A/N high+low poles with axisExactMatch), EnneagramTerm (9 motivation-based types with axisExactMatch), SdiTerm (4 relationship-focused conflict motivation types with axisExactMatch)

### Key Design Decisions

**Slot is an open String** — domain apps define their own vocabulary (devtown: "planner"/"reviewer"; gastown: "mayor"/"polecat"). Platform never constrains. Optional `casehub-eidos-vocab` provides SVO, Conscientiousness, CasehubSlot, Belbin, DISC, and Thomas-Kilmann starting-point vocabularies.

**Nothing goes in casehub-platform-api** — `AgentDescriptor` et al. are Eidos domain types. Repos that need them depend on `casehub-eidos-api` (Tier 1, pure Java). See platform-api-scope protocol in the garden.

**Goals and constraints as first-class fields** — `AgentGoal` (standing objectives with `GoalPriority`, `Visibility`, and `List<String> capabilities`) and `AgentConstraint` (behavioral guardrails with `Visibility` and `ConstraintSeverity`) are Tier 1 records on `AgentDescriptor`. BDI-inspired naming: goals are what the agent wants (standing, identity-level); `GoalContext` on `AgentPromptContext` is what the agent is doing right now (ephemeral, per-invocation). Rendered as "Objectives" section heading (avoids collision with "Current Goal"). `Visibility.PRIVATE` items appear in owning agent's prompt only — completely absent from A2A_CARD. `ConstraintSeverity { HARD, SOFT }` — severity-discriminated rendering: MARKDOWN uses `[HARD]`/`[SOFT]` label prefix (parallels goals `[PRIMARY]`/`[SECONDARY]`); PROSE groups by severity; A2A_CARD includes severity field. `AgentGoal.capabilities` maps goals to declared `AgentCapability.name()` values on the same descriptor — empty list = cross-cutting goal (affected by any capability failure); non-empty = affected only when a listed capability fails. Cross-validated in `AgentDescriptor` compact constructor. Goals sorted by priority then name; constraints sorted by severity then name. All rendered structurally (not enriched via LLM). JPA via `agent_goal` and `agent_constraint` tables.

**Domain vocabulary via domainVocabulary field** — `AgentDescriptor.domainVocabulary` sets the default vocabulary URI for all fields. Optional per-field overrides: `slotVocabulary`, `dispositionVocabulary`. Per-axis override: `axisVocabularies(Map<DispositionAxis, String>)` — most specific wins. `vocabUriForAxis(DispositionAxis)` implements the three-step resolution: `axisVocabularies.get(axis)` → `dispositionVocabulary` → `domainVocabulary`. `vocabUriForSlot()` implements the two-step resolution: `slotVocabulary` → `domainVocabulary` (`dispositionVocabulary` excluded — it grounds disposition axes, not slot).

**Two-layer capability model** — `AgentDescriptor` (static, declared at registration) + `CapabilityHealth.probe()` (dynamic, checked at dispatch time by casehub-engine). `MAX_CAPABILITIES = 20`; capability names must be unique within a descriptor (validated in compact constructor, enforced by DDL `UNIQUE (descriptor_id, name)`). `AgentCapability.description` (optional, ≤500 chars) provides human/LLM-readable description of what the capability does. `epistemicDomains` on `AgentCapability` qualifies declared capability by domain (e.g. `{"java": 0.95, "rust": 0.42}`). `excludedDomains: Set<String>` declares categorical exclusions. `AgentCapability.capabilityVocabulary` optionally grounds the capability name in a registered vocabulary — enables subsumption matching via `VocabularyRegistry.match()` returning `MatchDegree` (Exact, Plugin(depth), Specialization(depth), None). `BehavioralSignalStore` SPI accumulates learned signals via `BehavioralSignal { DECLINE, SUCCESS, COMPLIANT, VIOLATED }` (per-signal TTL, store-owned via `@ConfigProperty <signal>-ttl-days`); probe steps: Degraded → Unavailable → Excluded(DECLARED) → Excluded(LEARNED) → EpistemicallyWeak → BehavioralViolation → Ready. Default probe uses DECLINE signals only; positive evidence consumption is a strategy concern for domain-specific implementations. `AgentRegistry.find()` and `CapabilityHealth.probe()` are subsumption-aware: vocabulary-grounded capabilities match by hierarchy in addition to exact name; `CapabilityVocabularyValidator` validates vocabulary URI + term existence at registration time.

**Behavioral contracts and compliance checking** — the descriptor IS the behavioral contract; its fields (`latencyHintP50Ms`, `qualityHint`, `excludedDomains`, `delegation`, disposition axes) are implicit behavioral expectations. `BehavioralExpectations` utility extracts testable expectations from descriptor fields. Consumers (engine, application repos) observe agent behavior, compare against expectations, and record COMPLIANT/VIOLATED signals into `BehavioralSignalStore` using `ComplianceDimension` qualifier keys. `DefaultCapabilityHealth.probe()` Step 6 queries accumulated VIOLATED signals; two-layer check: per-dimension threshold (`ComplianceViolationThresholdPreference`, default 3) catches single-dimension spikes → `BehavioralViolation(exceeding, PER_DIMENSION)`; aggregate threshold (`AggregateViolationThresholdPreference`, default 5) catches cross-dimensional drift when no single dimension exceeds per-dimension threshold → `BehavioralViolation(all, AGGREGATE)`. `BehavioralViolation.ViolationKind { PER_DIMENSION, AGGREGATE }` discriminates the two modes; `PER_DIMENSION` violations map contains only exceeding dimensions, `AGGREGATE` contains all dimensions with violations. `ComplianceAttestations` utility constructs `LedgerAttestation` records from compliance observations for trust scoring impact (attestorId=`eidos:compliance`, trustDimension=`behavioral:<dimension>`).

**Generative** — `SystemPromptRenderer` renders an `AgentDescriptor + AgentPromptContext` into LLM instructions. `EidosSystemPromptRenderer` is `@ApplicationScoped` (no `@DefaultBean`): two-step pipeline — structural assembly then optional LangChain4j `ChatModel` semantic pass for MARKDOWN/PROSE; separate `A2ASemanticEnrichmentStep` for capability descriptions in A2A_CARD. Falls back to structural output when no `ChatModel` is available. `AgentPromptContext` accumulates goal (`GoalContext`), resources (`Resource`), and situational context — re-renderable as the agent's context evolves. Capability rendering is format-discriminated: PROSE/MARKDOWN surface names, `description` (when present), and `inputTypes`/`outputTypes`; numeric routing signals (`qualityHint`, `latencyHintP50Ms`, `costHint`, `epistemicDomains`) appear in A2A_CARD only — protocol PP-20260611-228599. A2A_CARD: LLM-enriched description wins when non-blank, declared `description` is the fallback.

**Descriptor templates** — `DescriptorTemplate` records (id, name, parameters, content) provide reusable prose fragments for agent system prompts. Templates are identity — declared on `AgentDescriptor.templates()` as ordered `List<TemplateRef>`, each carrying `templateId` + `args`. `TemplateRegistry` SPI with `CdiTemplateRegistry` (@PostConstruct, discovers `Instance<TemplateRegistrar>`) in runtime, `InMemoryTemplateRegistry` (@Alternative) in persistence-memory. `ClasspathYamlTemplateRegistrar` loads from `META-INF/eidos/templates.yaml`. Three-layer validation: compact constructors (structural), `CdiTemplateRegistry.register()` (placeholder vs declared parameters), `DescriptorCollector` (ref resolution + arg completeness). Single-pass regex substitution (`${variable}` → arg value, no cross-parameter injection). Render pipeline composes templates after capabilities, before disposition/briefing in MARKDOWN/PROSE; excluded from A2A_CARD.

**Research backing:** `research/eidos.md` in the eidos workspace contains full research, rationale, and ecosystem position.

---

## Maven Coordinates

| Element | Value |
|---|---|
| GitHub repo | `casehubio/eidos` |
| groupId | `io.casehub` |
| Parent artifactId | `casehub-eidos-parent` |
| API artifactId | `casehub-eidos-api` |
| Runtime artifactId | `casehub-eidos` |
| In-memory artifactId | `casehub-eidos-memory` |
| Deployment artifactId | `casehub-eidos-deployment` |
| Vocabulary artifactId | `casehub-eidos-vocab` |
| Eval artifactId | `casehub-eidos-eval` |
| Root Java package | `io.casehub.eidos` |
| API package | `io.casehub.eidos.api` |
| Runtime package | `io.casehub.eidos.runtime` |
| Annotations artifactId | `casehub-eidos-annotations` |
| Annotations Deployment artifactId | `casehub-eidos-annotations-deployment` |
| Annotations package | `io.casehub.eidos.annotations` |
| Feature name | `eidos` |
| Annotations Feature name | `eidos-annotations` |
| Routing artifactId | `casehub-eidos-routing` |
| Routing package | `io.casehub.eidos.routing` |

---

## Project Structure

```
casehub-eidos/  (local folder: ~/claude/casehub/eidos)
├── api/
│   └── src/main/java/io/casehub/eidos/api/
│       ├── AgentDescriptor.java         — four-layer agent description record (tenancyId always required); goals (List<AgentGoal>), constraints (List<AgentConstraint>); publicGoals()/publicConstraints() filter by Visibility.PUBLIC
│       ├── AgentGoal.java               — standing objective record: name, description, GoalPriority (PRIMARY/SECONDARY), Visibility (PUBLIC/PRIVATE), List<String> capabilities (maps to declared AgentCapability names; empty = cross-cutting); validated in compact constructor
│       ├── AgentConstraint.java         — behavioral guardrail record: name, description, Visibility (PUBLIC/PRIVATE), ConstraintSeverity (HARD/SOFT); validated in compact constructor
│       ├── GoalPriority.java            — enum: PRIMARY, SECONDARY
│       ├── Visibility.java              — enum: PUBLIC, PRIVATE — shared by AgentGoal and AgentConstraint; PUBLIC = all formats including A2A_CARD, PRIVATE = owning agent's prompt only
│       ├── AgentCapability.java         — capability with description (optional human-readable) + capabilityVocabulary (optional vocab grounding) + qualityHint (Double) + epistemicDomains + excludedDomains (Set<String>, declared negative specialization); Builder inner class
│       ├── CapabilityVocabularyValidator.java — shared validation: capabilityVocabulary URI + term existence
│       ├── MatchDegree.java              — sealed interface: Exact, Plugin(depth), Specialization(depth), None; implements `Comparable<MatchDegree>` with OWLS-MX ordering (Exact < Plugin < Specialization < None, lower depth within same type)
│       ├── ResolvedCapability.java       — result of CapabilityResolver.resolve(): capability + MatchDegree
│       ├── AgentMatch.java               — result of AgentRegistry.find(): descriptor + optional ResolvedCapability
│       ├── AgentSelector.java           — SPI: select(List<AgentMatch>, SelectionContext) → AgentSelection
│       ├── SelectionContext.java        — query context for selection: tenancyId, capabilityName, taskDomain
│       ├── AgentSelection.java          — sealed result: Selected, NoneQualified, Escalated
│       ├── EscalationKind.java          — enum: BORDERLINE_STALEMATE, NO_QUALIFIED_AGENT
│       ├── AgentDisposition.java        — weighted disposition axes (List<DispositionValue> per axis) + delegation boolean + dispositionProfile (holistic cognitive profile) + get(DispositionAxis) returns List<DispositionValue>; Builder accepts both String (convenience) and List<DispositionValue>
│       ├── AgentQuery.java              — criteria record for find(): slot, capabilityName, tenancyId (required), taskDomain (optional pre-filter by excludedDomains), goalName (optional filter by declared goal name)
│       ├── ConstraintSeverity.java      — enum: HARD, SOFT
│       ├── DispositionAxis.java         — enum: SOCIAL_ORIENTATION, RULE_FOLLOWING, RISK_APPETITE, AUTONOMY, CONFLICT_MODE; jsonKey() → camelCase JSON key; description() → axis description for LLM judge prompts
│       ├── VocabularyMetadata.java      — annotation: uri (required), name, version on vocabulary enum classes
│       ├── VocabularyTerm.java          — interface implemented by vocabulary enum constants; exactMatch + axisExactMatch + impliesSupervision() + opposite() (vocabulary-generic structural opposite/shadow)
│       ├── VocabularyRegistry.java      — SPI: register(Class<T>), isRegistered, registeredUris, resolve, allTerms, equivalentValues (typed + string-based + axis-aware)
│       ├── DescriptorTemplate.java     — reusable prose fragment record: id, name, parameters, content; compact constructor validation
│       ├── TemplateRef.java            — descriptor's reference to a template: templateId + args (Map<String, String>)
│       ├── TemplateRegistry.java       — SPI: register, resolve(id), all()
│       └── spi/
│           ├── VocabularyRegistrar.java — @FunctionalInterface CDI SPI; @ApplicationScoped beans auto-register vocab enums
│           ├── AgentDescriptorRegistrar.java — @FunctionalInterface CDI SPI; declarative List<AgentDescriptor> descriptors()
│           └── TemplateRegistrar.java  — @FunctionalInterface CDI SPI; List<DescriptorTemplate> templates()
│       ├── DispositionValue.java       — weighted disposition term record: term + weight [0.0–1.0]; of(String) convenience
│       ├── DispositionSignalStore.java  — SPI: recordActivation, activationCounts, decay, clear — cumulative counts, no TTL
│       ├── DispositionHealth.java       — SPI: probe(descriptor, context) → sealed DispositionStatus (Aligned/Drifted/EvolutionPending)
│       ├── DispositionEvolution.java    — SPI: evaluate(descriptor, pending) → sealed EvolutionResult (Evolved/Dampened)
│       ├── EvolutionType.java           — framework-agnostic evolution type marker interface
│       ├── AgentRegistry.java           — SPI: register, findById(id,tenancyId), find(AgentQuery)
│       ├── ReactiveAgentRegistry.java   — SPI: Uni<T> reactive mirror
│       ├── AgentPromptContext.java      — render-time context: Optional<GoalContext>, List<Resource>, situationalContext, RenderFormat
│       ├── AgentStateStore.java         — SPI: record/query/clear degradation state with TTL
│       ├── CapabilityHealth.java        — SPI: probe(AgentDescriptor, capabilityTag, ProbeContext); returns Degraded/Unavailable/Excluded/EpistemicallyWeak/BehavioralViolation/Ready; ExclusionSource { DECLARED, LEARNED }
│       ├── CapabilityResolver.java      — static utility: resolve(capabilities, capabilityTag, registry) → best-matching AgentCapability; match(capability, tag, registry) → MatchDegree; shared by probe and recording paths
│       ├── BehavioralSignalStore.java   — SPI: record/clear/learned/count — signal-parameterized (BehavioralSignal { DECLINE, SUCCESS, COMPLIANT, VIOLATED }); qualifier parameter (ComplianceDimension); store-owned per-signal TTL via @ConfigProperty
│       ├── BehavioralSignal.java        — enum: DECLINE, SUCCESS, COMPLIANT, VIOLATED — discriminator for BehavioralSignalStore methods
│       ├── ComplianceDimension.java     — constants: LATENCY, ATTESTATION_RATE, DELEGATION, ESCALATION dimension keys; ATTESTOR_ID, TRUST_DIMENSION_PREFIX, LATENCY_VIOLATION_MULTIPLIER conventions
│       ├── BehavioralExpectations.java  — static utility: latencyBound(AgentCapability) → OptionalLong, delegationExpected(AgentDisposition) → boolean, escalationExpected(AgentDisposition, String, VocabularyRegistry) → boolean
│       ├── DegradationReason.java       — top-level enum: RATE_LIMITED, CONTEXT_EXHAUSTED, OVERLOADED, DOMAIN_MISMATCH
│       ├── GoalContext.java             — structured goal: description, subGoals, caseRef
│       ├── ReactiveCapabilityHealth.java — SPI: Uni<CapabilityStatus> probe(...)
│       ├── Resource.java               — uri/label/type record for agent-accessible resources
│       └── SystemPromptRenderer.java   — SPI: render(AgentDescriptor, AgentPromptContext) → RenderedPrompt
├── annotations/                         — casehub-eidos-annotations: annotation definitions (@Identity, @Disposition, @AgentGoals, @AgentConstraints) + NameDerivation utility + Quarkus recorder
│   └── src/main/java/io/casehub/eidos/annotations/
│       ├── Identity.java                — @Identity: agent identity metadata (id, name, slot, provider, modelFamily, jurisdiction, briefing, vocabulary)
│       ├── Disposition.java             — @Disposition: 5 personality axes + dispositionProfile + styleProfile + delegation
│       ├── AgentGoals.java / AgentGoalDef.java — @AgentGoals: standing objectives with priority, visibility, capability references
│       ├── AgentConstraints.java / AgentConstraintDef.java — @AgentConstraints: behavioral guardrails with severity
│       ├── NameDerivation.java          — class name → kebab-case agentId / display name derivation (acronym-aware)
│       └── runtime/
│           ├── EidosAnnotationsRecorder.java — @Recorder: constructs AgentDescriptor at runtime from build-time-extracted config
│           └── AnnotatedAgentConfig.java    — recordable data class for build→runtime value transfer
├── annotations-deployment/              — casehub-eidos-annotations-deployment: Quarkus build extension
│   └── src/main/java/io/casehub/eidos/annotations/deployment/
│       ├── EidosAnnotationsProcessor.java — @BuildStep: Jandex scan, annotation extraction, synthetic bean generation, hybrid vocab validation
│       └── EidosAnnotationProcessedBuildItem.java — coordination build item for blocks interop
├── runtime/
│   └── src/main/java/io/casehub/eidos/runtime/
│       ├── registry/jpa/                — JpaAgentRegistry (@ApplicationScoped), JpaReactiveAgentRegistry (@IfBuildProperty)
│       ├── vocabulary/                  — CdiVocabularyRegistry (@ApplicationScoped, discovers Instance<VocabularyRegistrar>; three-map: byUri/byClass/byClassOrdered)
│       ├── health/                      — DefaultCapabilityHealth (checks AgentStateStore + BehavioralSignalStore; Instance<PreferenceProvider> for per-tenancy exclude + compliance thresholds), DefaultReactiveCapabilityHealth, NoOpAgentStateStore (@DefaultBean), NoOpBehavioralSignalStore (@DefaultBean), JpaBehavioralSignalStore (@IfBuildProperty blocking-mode, per-signal TTL), JpaAgentStateStore (@IfBuildProperty blocking-mode), ComplianceAttestations (static utility: constructs LedgerAttestation from compliance observations), DefaultDispositionHealth (@ApplicationScoped, JPAF threshold probe with 4 evolution conditions), DefaultDispositionEvolution (@ApplicationScoped, rule-based JPAF reflection — 4 evolution types with signal decay), NoOpDispositionHealth (@DefaultBean), NoOpDispositionEvolution (@DefaultBean), NoOpDispositionSignalStore (@DefaultBean), JpaDispositionSignalStore (@IfBuildProperty)
│       ├── preferences/                 — EidosPreferenceKeys (EXCLUDE_THRESHOLD, COMPLIANCE_VIOLATION_THRESHOLD, AGGREGATE_VIOLATION_THRESHOLD PreferenceKeys), ExcludeThresholdPreference (SingleValuePreference, default 3), ComplianceViolationThresholdPreference (SingleValuePreference, default 3), AggregateViolationThresholdPreference (SingleValuePreference, default 5), DispositionPreferenceKeys (REINFORCEMENT_DELTA default 0.06, OVER_REINFORCEMENT_THRESHOLD default 0.50), ReinforcementDeltaPreference, OverReinforcementThresholdPreference
│       ├── registrar/                   — AgentDescriptorBootstrap (@Observes StartupEvent, @IfBuildProperty blocking-mode, injects TemplateRegistry), ReactiveAgentDescriptorBootstrap (@IfBuildProperty reactive-mode), DescriptorCollector (shared validation + template ref validation), ClasspathYamlDescriptorRegistrar (META-INF/eidos/descriptors.yaml)
│       ├── template/                    — CdiTemplateRegistry (@ApplicationScoped, @PostConstruct discovers Instance<TemplateRegistrar>), ClasspathYamlTemplateRegistrar (META-INF/eidos/templates.yaml)
│       └── renderer/                    — EidosSystemPromptRenderer (@ApplicationScoped, LangChain4j ChatModel optional)
├── persistence-memory/                  — casehub-eidos-memory: @Alternative @Priority(1) in-memory; InMemoryAgentRegistry, InMemoryTemplateRegistry, InMemoryAgentStateStore, InMemoryBehavioralSignalStore (per-signal TTL via @ConfigProperty <signal>-ttl-days), InMemoryDispositionSignalStore (ConcurrentHashMap + AtomicInteger, no TTL)
├── deployment/                          — casehub-eidos-deployment: @BuildStep EidosProcessor + EidosBuildTimeConfig
├── vocab/                               — casehub-eidos-vocab: SvoTerm, ConscientiousnessTerm, CasehubSlotTerm, BelbinTerm (9 roles with axisExactMatch), DiscTerm, ThomasKilmannTerm, CasehubCapabilityTerm, JungianFunctionTerm (8 functions, shadow(), opposite(), axisExactMatch), MbtiTypeTerm (16 types, specializes(), defaultProfile()), JungianEvolutionType (4 JPAF reflection types), BigFiveTerm (O/E/A/N), EnneagramTerm (9 types), SdiTerm (4 types) enums + VocabularyRegistrar beans
├── routing/                             — casehub-eidos-routing: engine-aware agent selection bridge; EngineAwareAgentSelector @Alternative @Priority(1) converts AgentMatch→AgentCandidate, delegates to engine's AgentRoutingStrategy; depends on eidos-api + engine-api + ledger-api
├── eval/                                — casehub-eidos-eval: offline quality evaluation harness (not deployed); judges: PromptJudge, ProximityJudge, VocabularyExpressivenessJudge, TraitExpressionJudge, PairContrastJudge, BehavioralJudge, MbtiAlignmentJudge (MBTI-70 questionnaire alignment), FunctionActivationJudge (TAA — cognitive function activation accuracy), PersonalityEvolutionJudge (PSA — personality shift structural validity); AgentProviderChatModel bridge (ChatModel → AgentProvider SPI); 8 YAML agent profiles + 8 Jungian profiles + 24 function activation scenarios; pair-contrast behavioral validation (Phase 3 — eidos#46)
└── examples/
    └── agent-scenarios/                 — @QuarkusTest examples: team, cross-vocab, epistemic, tenancy, disposition
```

---

## Build and Test

```bash
# Build all modules
JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn clean install

# Run all tests
JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn test

# Run tests for a specific module
JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn test -pl api
JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn test -pl runtime
```

**Use `mvn` not `./mvnw`** — maven wrapper not configured on this machine.

```bash
# Run eval harness — Claude CLI (default, requires claude CLI configured)
JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn test -pl eval -Peval \
  -Dtest=PromptEvalTest#evaluateAllScenarios

# Run eval harness — Ollama (requires Ollama running on localhost:11434)
# Note: first Ollama run requires mvn clean to force Quarkus re-augmentation with the Ollama extension.
# -Dquarkus.langchain4j.ollama.timeout=300s is required — default 10s is too short for local LLMs.
JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn clean test -pl eval -Peval,eval-ollama \
  -Dtest=PromptEvalTest#evaluateAllScenarios \
  -Dcasehub.eval.claude-provider.enabled=false \
  "-Dquarkus.langchain4j.ollama.chat-model.model-name=<model>" \
  "-Dquarkus.langchain4j.ollama.chat-model.format=json" \
  "-Dquarkus.langchain4j.ollama.timeout=300s"

# Independent judge comparison — Phase 2b (run AFTER evaluateRealWorldScenarios with Claude)
# Uses saved target/renders-cache.json; judges Claude's renders with Qwen to check self-evaluation bias.
JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn clean test -pl eval -Peval,eval-ollama \
  -Dtest=PromptEvalTest#evaluateWithIndependentJudge \
  -Dcasehub.eval.claude-provider.enabled=false \
  "-Dquarkus.langchain4j.ollama.chat-model.model-name=qwen3.6:35b-a3b" \
  "-Dquarkus.langchain4j.ollama.chat-model.format=json" \
  "-Dquarkus.langchain4j.ollama.timeout=300s" \
  -Dcasehub.eval.model.label=qwen3-35b

# Run minimal briefing experiment — isolates framework vs briefing contribution (eidos#129)
# Via Vertex AI (recommended — uses ANTHROPIC_VERTEX_PROJECT_ID and CLOUD_ML_REGION env vars):
JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn clean test -pl eval -Peval \
  -Dtest=MinimalBriefingEvalTest#compareBriefingContribution \
  -Dcasehub.eval.vertex.enabled=true \
  -Dcasehub.eval.claude-provider.enabled=false \
  -Dcasehub.eval.model.label=vertex-sonnet

# Via Claude CLI (legacy — requires claude CLI configured):
JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn clean test -pl eval -Peval \
  -Dtest=MinimalBriefingEvalTest#compareBriefingContribution

# Run eval harness — GPULlama3 via TornadoVM Metal (Apple Silicon)
# Requires: export TORNADOVM_HOME=~/tornadovm-metal/tornadovm-4.0.0-jdk25-metal
#           Generate argfile: sed "s|\${TORNADOVM_HOME}|$TORNADOVM_HOME|g" $TORNADOVM_HOME/tornado-argfile.template > $TORNADOVM_HOME/tornado-argfile
JAVA_HOME=$(/usr/libexec/java_home -v 25) mvn test -pl eval -Peval,eval-gpullama3 \
  -Dtest=PromptEvalTest#evaluateAllScenarios \
  -Dcasehub.eval.claude-provider.enabled=false \
  "-Dquarkus.langchain4j.gpu-llama3.chat-model.model-name=<model>" \
  "-Dquarkus.langchain4j.gpu-llama3.chat-model.format=json"

# Run eval harness — Jlama NEON (Apple Silicon, Q4 models via native NEON)
# Requires jlama-native osx-aarch_64 classifier on classpath (eval-jlama profile)
JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn test -pl eval -Peval,eval-jlama \
  -Dtest=PromptEvalTest#evaluateAllScenarios \
  -Dcasehub.eval.claude-provider.enabled=false \
  "-Dquarkus.langchain4j.jlama.chat-model.model-name=tjake/Llama-3.2-3B-Instruct-JQ4"
```

---

## Java and GraalVM on This Machine

```bash
# Java 26 (Oracle, system default) — use for dev and tests
JAVA_HOME=$(/usr/libexec/java_home -v 26)
```

---

## Ecosystem Context

```
casehub-platform-api    (ActorType, CurrentPrincipal — no Eidos types)
casehub-ledger          (evidence layer: trust scores, attestations, signing)
    ↑
casehub-eidos           (this project — agent identity, discovery, generation)
    ↑              ↑
  devtown        claudony    (consumers — register agents, query registry)
    ↑
  casehub-engine          (calls CapabilityHealth.probe() at dispatch time)
```

---

## Schema Convention

**No existing installations** — no deployed instances of `casehub-eidos` in production. All schema changes go directly into the base migration files. Treat every schema change as a clean-slate design decision.

---

## Work Tracking

**Issue tracking:** enabled
**GitHub repo:** casehubio/eidos

**Automatic behaviours:**
- Before implementation begins — check if an active issue exists. If not, create one before writing any code.
- All commits should reference an issue — `Refs #N` (ongoing) or `Closes #N` (done).
