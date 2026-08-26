# Decisions — Issue #144: Trust-ranked agent selection

## D1: SPI naming — generic AgentSelector

**Choice:** SPI is `AgentSelector` (generic contract).
**Alternatives:**
- `TrustRankedSelector` (issue proposal) — bakes one ranking strategy into the contract name
**Rationale:** Follows established eidos SPI naming pattern — SPIs name the abstraction (`AgentRegistry`, `CapabilityHealth`), implementations name the strategy.
**Trade-offs:** Slightly less obvious what the default impl does from the SPI name alone.
**Sources:** AgentRegistry.java, CapabilityHealth.java, SystemPromptRenderer.java
**Exploration:** quick
**Status:** captured

## D2: CapabilityHealth is an implementation detail

**Choice:** Health probing lives inside implementations, not in the `AgentSelector` SPI contract.
**Alternatives:**
- SPI contract mandates health filtering — forces all implementations to probe health
**Rationale:** Consistent with how eidos handles cross-cutting concerns. `AgentRegistry.find()` doesn't enforce health probing either.
**Trade-offs:** Nothing enforces that custom selectors check health.
**Sources:** CapabilityHealth.java (SPI boundary), DefaultCapabilityHealth.java
**Exploration:** quick
**Status:** captured

## D3: Bootstrap — CAPABILITY→GLOBAL fallback, then configurable default

**Choice:** Three-step trust resolution: `capabilityScore(agentId, capabilityName)` → `globalScore(agentId)` → configurable default (0.5 via `@ConfigProperty`). Applies to `SimpleAgentSelector` fallback only; the engine-delegating path uses the full trust maturity model.
**Alternatives:**
- Flat default score only (original D3) — ignores global trust signal for agents with established reputation but no capability-specific history (R1-04 finding)
- Exclude bootstrap agents entirely — breaks fresh deployments
**Rationale:** CAPABILITY→GLOBAL fallback preserves earned reputation. A trusted agent (globalScore=0.85) assigned a new capability gets scored 0.85, not demoted to 0.5. Only truly new agents with no history at all fall to the configurable default.
**Trade-offs:** Two TrustScoreSource calls per bootstrap-phase agent instead of one. Negligible for typical candidate list sizes.
**Sources:** TrustScoreSource.java, TrustGateService.java (CAPABILITY→GLOBAL pattern), R1-04 review finding
**Exploration:** quick → revised after decision review (R1-04)
**Status:** revised

## D4: Bridge architecture — SPI in eidos, selection in engine

**Choice:** Three-tier design. SPI in `eidos-api`, simple fallback `@DefaultBean` in `eidos-runtime`, engine-delegating bridge in new `eidos-routing` module.
**Alternatives:**
- Pure eidos SPI (original D4) — creates parallel selection mechanism conflicting with capability ownership (R1-01/R1-02)
- Qhorus uses engine directly — violates the constraint that qhorus cannot depend on engine-api
- Internal qhorus concern — duplicates existing platform logic
**Rationale:** Decision review surfaced that capability ownership assigns "Agent routing / selection" to `casehub-engine-api` (`AgentRoutingStrategy` SPI), and `TrustWeightedAgentStrategy` in engine-ledger already implements the full trust maturity model. Creating a parallel selector in eidos violates platform boundaries. The bridge pattern respects both boundaries: eidos owns the SPI surface (qhorus depends on eidos, not engine), engine owns the selection logic (eidos-routing delegates to engine's priority ladder).
**Trade-offs:** New module (`eidos-routing`). Bridge path loses workload scoring (`runningJobs=0` from eidos context). Acceptable — trust maturity is the critical path; workload balancing is an engine-pipeline bonus.
**Sources:** capability-ownership.md, routing.md, AgentRoutingStrategy.java, TrustWeightedAgentStrategy, TrustCandidateClassifier.java, R1-01/R1-02 review findings
**Exploration:** quick → revised after decision review (R1-01, R1-02)
**Status:** revised

## D5: SelectionContext carries query context, not implementation config

**Choice:** `SelectionContext(tenancyId, capabilityName, taskDomain)` — generic fields any selector might need. Trust-specific config (`trustThreshold`, `bootstrapDefaultScore`) lives on implementations via `@ConfigProperty`.
**Depends on:** D1 (generic SPI), D2 (health is impl detail), D3 (bootstrap config)
**Alternatives:**
- Include `trustThreshold` on SelectionContext — couples generic context to one implementation's config
- Reuse `AgentQuery` instead of a new type — conflates "criteria for finding" with "context for selecting"
**Rationale:** Clean separation: context is what happened (the query parameters), config is how to act on it. The bridge path extracts `tenancyId` for `AgentRoutingContext` construction.
**Trade-offs:** Callers wanting per-request threshold tuning need a custom selector.
**Sources:** AgentQuery.java, AgentRoutingContext.java (tenancyId field)
**Exploration:** quick
**Status:** captured

## D6: SimpleAgentSelector uses TrustScoreSource (ledger-api)

**Choice:** `SimpleAgentSelector` injects `TrustScoreSource` from `casehub-ledger-api` with explicit CAPABILITY→GLOBAL fallback. `EngineAwareAgentSelector` delegates to engine's full pipeline which handles trust via `TrustCandidateClassifier`.
**Depends on:** D4 (bridge architecture)
**Alternatives:**
- Depend on `TrustGateService` from `casehub-ledger` runtime — escalates dependency; unnecessary since the fallback selector implements its own two-step resolution
**Rationale:** No new dependency. The simple fallback calls `capabilityScore()` then `globalScore()` — same logic as `TrustGateService.currentScore(actorId, capabilityTag)` but explicit. The engine-delegating path doesn't need it — engine's pipeline handles trust internally.
**Trade-offs:** Simple fallback lacks TrustGateService policy logic (tenant overrides). Acceptable — the engine path handles policy.
**Sources:** TrustScoreSource.java, TrustGateService.java, R1-04 review finding
**Exploration:** quick → revised after decision review (R1-04)
**Status:** revised

## D7: AgentSelection has three variants

**Choice:** `Selected(AgentDescriptor, ResolvedCapability, double, String)`, `NoneQualified(String)`, `Escalated(String capabilityName, String reason)`.
**Depends on:** D4 (bridge architecture)
**Alternatives:**
- Two variants only (original: Selected + NoneQualified) — collapses "no agents exist" and "agents exist but need human review" into one result, losing actionable information (R1-10)
**Rationale:** `Escalated` surfaces engine's escalation signal through the SPI. When the engine path detects a borderline stalemate, the caller (qhorus) learns it should escalate to a human rather than just fail. The simple fallback never returns `Escalated` — it has no policy infrastructure.
**Trade-offs:** Third variant adds one case for callers to handle. Trivial cost.
**Sources:** RoutingResult.java (Selected/Unresolvable/Escalated), R1-10 review finding
**Exploration:** quick → revised after decision review (R1-10)
**Status:** revised

## D8: Separate eidos-routing module for the engine bridge

**Choice:** `EngineAwareAgentSelector` lives in a new `eidos-routing` module (`@Alternative @Priority(1)`), not in `eidos-runtime`.
**Depends on:** D4 (bridge architecture)
**Alternatives:**
- Optional dependency in eidos-runtime with `Instance<AgentRoutingStrategy>` — fails at Quarkus build time when engine-api classes aren't on the classpath. `<optional>true</optional>` only prevents transitive resolution; it doesn't handle missing classes during augmentation.
- `@IfBuildProperty` conditional in eidos-runtime — requires engine-api as a compile dep, leaks engine types into eidos-runtime's bytecode
**Rationale:** Separate module follows eidos's established optional-module pattern (eidos-vocab, eidos-memory, eidos-annotations). Module activates by classpath presence. `@Alternative @Priority(1)` displaces the `@DefaultBean` in eidos-runtime automatically via CDI priority. No deployment module needed — Quarkus discovers the bean.
**Trade-offs:** One more module in the build. Acceptable — follows the pattern.
**Sources:** eidos module structure (vocab, memory, annotations pattern)
**Exploration:** quick
**Status:** captured
