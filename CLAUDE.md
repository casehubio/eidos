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

## Project Type

type: java

**Stack:** Java 21 (on Java 26 JVM), Quarkus 3.32.2

---

## What This Project Is

`casehub-eidos` is a CaseHub extension providing structured agent identity for LLM agents.
Any Quarkus app adds `io.casehub:casehub-eidos` as a dependency and gets:

- **AgentDescriptor** — four-layer structured description (identity, slot, capabilities, disposition)
- **VocabularyRegistry** — pluggable domain vocabulary system; CdiVocabularyRegistry discovers `Instance<Vocabulary>` CDI beans
- **AgentRegistry** — store and query descriptors (blocking + reactive)
- **CapabilityHealth** — two-layer capability model: declared (descriptor) vs. operable now (runtime probe)
- **SystemPromptRenderer** — generates CLAUDE.md sections (or any LLM system prompt) from descriptor + case goal
- **casehub-eidos-vocab** — optional well-known vocabularies: SVO, Conscientiousness, CasehubSlot

### Key Design Decisions

**Slot is an open String** — domain apps define their own vocabulary (devtown: "planner"/"reviewer"; gastown: "mayor"/"polecat"). Platform never constrains. Optional `casehub-eidos-vocab` provides SVO, Conscientiousness, and CasehubSlot starting-point vocabularies.

**Nothing goes in casehub-platform-api** — `AgentDescriptor` et al. are Eidos domain types. Repos that need them depend on `casehub-eidos-api` (Tier 1, pure Java). See platform-api-scope protocol in the garden.

**Domain vocabulary via domainVocabulary field** — `AgentDescriptor.domainVocabulary` sets the default vocabulary URI for all fields. Optional per-field overrides: `slotVocabulary`, `dispositionVocabulary`.

**Two-layer capability model** — `AgentDescriptor` (static, declared at registration) + `CapabilityHealth.probe()` (dynamic, checked at dispatch time by casehub-engine). `epistemicDomains` on `AgentCapability` qualifies declared capability by domain (e.g. `{"java": 0.95, "rust": 0.42}`).

**Generative** — `SystemPromptRenderer` turns a descriptor + case goal into rendered agent instructions. `ClaudeMarkdownRenderer` is the `@DefaultBean` (current target). Format-agnostic from day one.

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
│       ├── AgentDescriptor.java         — four-layer agent description record
│       ├── AgentCapability.java         — capability with qualityHint + epistemicDomains
│       ├── AgentDisposition.java        — open-String disposition axes + delegation boolean
│       ├── Vocabulary.java              — named versioned vocabulary
│       ├── VocabularyTerm.java          — term with aliases and exactMatches
│       ├── VocabularyRegistry.java      — SPI: register, find, resolve, equivalentValues
│       ├── AgentRegistry.java           — SPI: blocking store + discovery
│       ├── ReactiveAgentRegistry.java   — SPI: Uni<T> reactive mirror
│       ├── CapabilityHealth.java        — SPI + sealed CapabilityStatus + ProbeContext
│       └── SystemPromptRenderer.java   — SPI + RenderedPrompt + RenderContext + RenderFormat
├── runtime/
│   └── src/main/java/io/casehub/eidos/runtime/
│       ├── registry/                    — JpaAgentRegistry (@DefaultBean)
│       ├── vocabulary/                  — CdiVocabularyRegistry (discovers Instance<Vocabulary>)
│       ├── health/                      — DefaultCapabilityHealth
│       └── renderer/                    — ClaudeMarkdownRenderer (@DefaultBean)
├── persistence-memory/                  — casehub-eidos-memory: @Alternative @Priority(1) in-memory
├── deployment/                          — casehub-eidos-deployment: @BuildStep EidosProcessor
└── vocab/                               — casehub-eidos-vocab: SVO, Conscientiousness, CasehubSlot
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
