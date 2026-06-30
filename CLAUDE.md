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
| blog       | workspace   | staged here; published to mdproctor.github.io via publish-blog |
| plans      | workspace   | stay in workspace permanently |
| design     | workspace   | epic journal stays in workspace |
| snapshots  | workspace   | stay in workspace permanently |
| handover   | workspace   | |

---

# CaseHub Eidos — Claude Code Project Guide

## Platform Context

This repo is one component of the casehubio multi-repo platform. **Before implementing anything — any feature, SPI, data model, or abstraction — run the Platform Coherence Protocol.**

**Platform architecture (fetch before any implementation decision):**
```
https://raw.githubusercontent.com/casehubio/parent/main/docs/PLATFORM.md
```

**This repo's deep-dive:**
```
https://raw.githubusercontent.com/casehubio/parent/main/docs/repos/casehub-eidos.md
```

**Other repo deep-dives** (fetch when your implementation touches their domain):
- casehub-ledger: `https://raw.githubusercontent.com/casehubio/parent/main/docs/repos/casehub-ledger.md`
- casehub-engine: `https://raw.githubusercontent.com/casehubio/parent/main/docs/repos/casehub-engine.md`

---

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

- **AgentDescriptor** — four-layer structured description (identity, slot, capabilities, disposition)
- **VocabularyRegistry** — pluggable domain vocabulary system; vocabularies are Java enums implementing `VocabularyTerm`; `CdiVocabularyRegistry` discovers `Instance<VocabularyRegistrar>` CDI beans at startup; axis-aware `equivalentValues()` with `DispositionAxis`
- **AgentRegistry** — store and query descriptors (blocking + reactive)
- **CapabilityHealth** — two-layer capability model: declared (descriptor) vs. operable now (runtime probe); `AgentStateStore` SPI records degradation state with TTL; `CapabilitySpecializationStore` SPI accumulates learned domain signals via `SpecializationSignal { DECLINE, SUCCESS }` (store-owned per-signal TTL). `NoOpAgentStateStore` @DefaultBean, `NoOpCapabilitySpecializationStore` @DefaultBean, `InMemoryAgentStateStore` + `InMemoryCapabilitySpecializationStore` @Alternative in casehub-eidos-memory, `JpaCapabilitySpecializationStore` @IfBuildProperty blocking-mode in runtime (Flyway V5, persistent per-signal TTL-based tracking)
- **AgentDescriptorRegistrar** — declarative SPI (`List<AgentDescriptor> descriptors()`); consumers provide `@ApplicationScoped` beans or a `META-INF/eidos/descriptors.yaml` classpath resource (YAML-driven via `ClasspathYamlDescriptorRegistrar`); `AgentDescriptorBootstrap` (@Observes StartupEvent, @IfBuildProperty blocking-mode gated) auto-discovers all registrars and registers descriptors with duplicate `(agentId, tenancyId)` detection
- **SystemPromptRenderer** — renders `AgentDescriptor + AgentPromptContext` (goal, resources, situational context) into a format-specific system prompt; `EidosSystemPromptRenderer` @ApplicationScoped (no @DefaultBean — consumers displace with @DefaultBean fallback per Pattern B) supports three formats: `MARKDOWN` (LLM or structural fallback), `PROSE` (LLM or structural fallback), `A2A_CARD` (JSON machine-readable card with slot, vocabulary-grounded disposition, frameworks index, and full capability routing signals — `qualityHint`, `latencyHintP50Ms`, `costHint`, `epistemicDomains` — plus `inputTypes`/`outputTypes` type schema)
- **casehub-eidos-vocab** — optional well-known vocabularies: SVO, Conscientiousness, CasehubSlot, Belbin (team roles), DISC (behavioral styles), Thomas-Kilmann (conflict modes)

### Key Design Decisions

**Slot is an open String** — domain apps define their own vocabulary (devtown: "planner"/"reviewer"; gastown: "mayor"/"polecat"). Platform never constrains. Optional `casehub-eidos-vocab` provides SVO, Conscientiousness, CasehubSlot, Belbin, DISC, and Thomas-Kilmann starting-point vocabularies.

**Nothing goes in casehub-platform-api** — `AgentDescriptor` et al. are Eidos domain types. Repos that need them depend on `casehub-eidos-api` (Tier 1, pure Java). See platform-api-scope protocol in the garden.

**Domain vocabulary via domainVocabulary field** — `AgentDescriptor.domainVocabulary` sets the default vocabulary URI for all fields. Optional per-field overrides: `slotVocabulary`, `dispositionVocabulary`. Per-axis override: `axisVocabularies(Map<DispositionAxis, String>)` — most specific wins. `vocabUriForAxis(DispositionAxis)` implements the three-step resolution: `axisVocabularies.get(axis)` → `dispositionVocabulary` → `domainVocabulary`. `vocabUriForSlot()` implements the two-step resolution: `slotVocabulary` → `domainVocabulary` (`dispositionVocabulary` excluded — it grounds disposition axes, not slot).

**Two-layer capability model** — `AgentDescriptor` (static, declared at registration) + `CapabilityHealth.probe()` (dynamic, checked at dispatch time by casehub-engine). `epistemicDomains` on `AgentCapability` qualifies declared capability by domain (e.g. `{"java": 0.95, "rust": 0.42}`). `excludedDomains: Set<String>` declares categorical exclusions. `CapabilitySpecializationStore` SPI accumulates learned signals via `SpecializationSignal { DECLINE, SUCCESS }` (per-signal TTL, store-owned via `@ConfigProperty decline-ttl-days=30` / `success-ttl-days=30`); probe steps: Degraded → Unavailable → Excluded(DECLARED) → Excluded(LEARNED) → EpistemicallyWeak → Ready. Default probe uses DECLINE signals only; positive evidence consumption is a strategy concern for domain-specific implementations.

**Generative** — `SystemPromptRenderer` renders an `AgentDescriptor + AgentPromptContext` into LLM instructions. `EidosSystemPromptRenderer` is `@ApplicationScoped` (no `@DefaultBean`): two-step pipeline — structural assembly then optional LangChain4j `ChatModel` semantic pass for MARKDOWN/PROSE; separate `A2ASemanticEnrichmentStep` for capability descriptions in A2A_CARD. Falls back to structural output when no `ChatModel` is available. `AgentPromptContext` accumulates goal (`GoalContext`), resources (`Resource`), and situational context — re-renderable as the agent's context evolves. Capability rendering is format-discriminated: PROSE/MARKDOWN surface names + `inputTypes`/`outputTypes` only; numeric routing signals (`qualityHint`, `latencyHintP50Ms`, `costHint`, `epistemicDomains`) appear in A2A_CARD only — protocol PP-20260611-228599.

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
| Feature name | `eidos` |

---

## Project Structure

```
casehub-eidos/  (local folder: ~/claude/casehub/eidos)
├── api/
│   └── src/main/java/io/casehub/eidos/api/
│       ├── AgentDescriptor.java         — four-layer agent description record (tenancyId always required)
│       ├── AgentCapability.java         — capability with qualityHint (Double) + epistemicDomains + excludedDomains (Set<String>, declared negative specialization); Builder inner class
│       ├── AgentDisposition.java        — open-String disposition axes + delegation boolean + get(DispositionAxis)
│       ├── AgentQuery.java              — criteria record for find(): slot, capabilityName, tenancyId (required), taskDomain (optional pre-filter by excludedDomains)
│       ├── DispositionAxis.java         — enum: SOCIAL_ORIENTATION, RULE_FOLLOWING, RISK_APPETITE, AUTONOMY, CONFLICT_MODE; jsonKey() → camelCase JSON key; description() → axis description for LLM judge prompts
│       ├── VocabularyMetadata.java      — annotation: uri (required), name, version on vocabulary enum classes
│       ├── VocabularyTerm.java          — interface implemented by vocabulary enum constants; exactMatch + axisExactMatch
│       ├── VocabularyRegistry.java      — SPI: register(Class<T>), isRegistered, resolve, allTerms, equivalentValues (typed + string-based + axis-aware)
│       └── spi/
│           ├── VocabularyRegistrar.java — @FunctionalInterface CDI SPI; @ApplicationScoped beans auto-register vocab enums
│           └── AgentDescriptorRegistrar.java — @FunctionalInterface CDI SPI; declarative List<AgentDescriptor> descriptors()
│       ├── AgentRegistry.java           — SPI: register, findById(id,tenancyId), find(AgentQuery)
│       ├── ReactiveAgentRegistry.java   — SPI: Uni<T> reactive mirror
│       ├── AgentPromptContext.java      — render-time context: Optional<GoalContext>, List<Resource>, situationalContext, RenderFormat
│       ├── AgentStateStore.java         — SPI: record/query/clear degradation state with TTL
│       ├── CapabilityHealth.java        — SPI: probe(AgentDescriptor, capabilityTag, ProbeContext); returns Ready/Degraded/Unavailable/EpistemicallyWeak/Excluded; ExclusionSource { DECLARED, LEARNED }
│       ├── CapabilitySpecializationStore.java — SPI: record/clear/learned/count — signal-parameterized (SpecializationSignal { DECLINE, SUCCESS }); store-owned per-signal TTL via @ConfigProperty
│       ├── SpecializationSignal.java    — enum: DECLINE, SUCCESS — discriminator for CapabilitySpecializationStore methods
│       ├── DegradationReason.java       — top-level enum: RATE_LIMITED, CONTEXT_EXHAUSTED, OVERLOADED, DOMAIN_MISMATCH
│       ├── GoalContext.java             — structured goal: description, subGoals, caseRef
│       ├── ReactiveCapabilityHealth.java — SPI: Uni<CapabilityStatus> probe(...)
│       ├── Resource.java               — uri/label/type record for agent-accessible resources
│       └── SystemPromptRenderer.java   — SPI: render(AgentDescriptor, AgentPromptContext) → RenderedPrompt
├── runtime/
│   └── src/main/java/io/casehub/eidos/runtime/
│       ├── registry/jpa/                — JpaAgentRegistry (@ApplicationScoped), JpaReactiveAgentRegistry (@IfBuildProperty)
│       ├── vocabulary/                  — CdiVocabularyRegistry (@ApplicationScoped, discovers Instance<VocabularyRegistrar>; three-map: byUri/byClass/byClassOrdered)
│       ├── health/                      — DefaultCapabilityHealth (checks AgentStateStore + CapabilitySpecializationStore; Instance<PreferenceProvider> for per-tenancy exclude threshold), DefaultReactiveCapabilityHealth, NoOpAgentStateStore (@DefaultBean), NoOpCapabilitySpecializationStore (@DefaultBean), JpaCapabilitySpecializationStore (@IfBuildProperty blocking-mode, Flyway V5, per-signal TTL), JpaAgentStateStore (@IfBuildProperty blocking-mode)
│       ├── preferences/                 — EidosPreferenceKeys (EXCLUDE_THRESHOLD PreferenceKey), ExcludeThresholdPreference (SingleValuePreference, default 3)
│       ├── registrar/                   — AgentDescriptorBootstrap (@Observes StartupEvent, @IfBuildProperty blocking-mode), ReactiveAgentDescriptorBootstrap (@IfBuildProperty reactive-mode), DescriptorCollector (shared validation), ClasspathYamlDescriptorRegistrar (META-INF/eidos/descriptors.yaml)
│       └── renderer/                    — EidosSystemPromptRenderer (@ApplicationScoped, LangChain4j ChatModel optional)
├── persistence-memory/                  — casehub-eidos-memory: @Alternative @Priority(1) in-memory; InMemoryAgentRegistry, InMemoryAgentStateStore, InMemoryCapabilitySpecializationStore (per-signal TTL via @ConfigProperty decline-ttl-days=30 / success-ttl-days=30)
├── deployment/                          — casehub-eidos-deployment: @BuildStep EidosProcessor + EidosBuildTimeConfig
├── vocab/                               — casehub-eidos-vocab: SvoTerm, ConscientiousnessTerm, CasehubSlotTerm, BelbinTerm, DiscTerm, ThomasKilmannTerm enums + VocabularyRegistrar beans
├── eval/                                — casehub-eidos-eval: offline quality evaluation harness (not deployed); judges: PromptJudge, ProximityJudge, VocabularyExpressivenessJudge, TraitExpressionJudge, PairContrastJudge, BehavioralJudge; AgentProviderChatModel bridge (ChatModel → AgentProvider SPI); 8 YAML agent profiles; pair-contrast behavioral validation (Phase 3 — eidos#46)
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
