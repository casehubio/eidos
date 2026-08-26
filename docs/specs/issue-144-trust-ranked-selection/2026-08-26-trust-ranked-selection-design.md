# Trust-Ranked Agent Selection — Design Spec

**Issue:** casehubio/eidos#144
**Date:** 2026-08-26
**Scope:** S — one SPI, two implementations (fallback + bridge), one sealed result type, one new module

---

## Problem

Qhorus needs to resolve `role:X` capability targets to the best available agent using trust scores. Eidos has the discovery infrastructure (`AgentRegistry.find()` → `List<AgentMatch>`), and engine has the selection infrastructure (`AgentRoutingStrategy` → `TrustWeightedAgentStrategy`). But qhorus cannot depend on engine-api — it needs a way to reach engine's selection through eidos.

## Solution

A three-tier design that keeps the SPI surface in eidos (where qhorus can reach it) and delegates selection to engine (where the trust maturity model lives).

```
qhorus
  │ depends on eidos-api + eidos (runtime)
  │ calls AgentSelector.select(matches, context)
  ▼
┌─────────────────────────────────────────────────┐
│ eidos-api: AgentSelector SPI                    │
│   SelectionContext, AgentSelection (sealed)     │
│   EscalationKind enum                           │
└─────────────────────────────────────────────────┘
          │                          │
          ▼                          ▼
┌──────────────────────┐  ┌────────────────────────┐
│ eidos-runtime        │  │ eidos-routing (new)     │
│ SimpleAgentSelector  │  │ EngineAwareAgentSelector│
│ @DefaultBean         │  │ @Alternative @Priority(1)│
│ (no engine dep)      │  │ (depends on engine-api) │
│                      │  │                        │
│ Instance<TSS>        │  │ AgentMatch→AgentCandidate│
│ + CapabilityHealth   │  │ → AgentRoutingStrategy  │
│ health + trust rank  │  │ full maturity model     │
└──────────────────────┘  └────────────────────────┘
```

When `eidos-routing` is on the classpath, `EngineAwareAgentSelector` displaces `SimpleAgentSelector` via CDI priority. Qhorus never knows which path is active.

**Capability ownership:** Platform docs assign "Agent routing / selection" to `casehub-engine-api`. This spec places `AgentSelector` in `eidos-api` as a discovery-layer proxy to engine's routing — not a parallel mechanism. `capability-ownership.md` must be updated to distinguish agent *discovery+selection* (eidos) from agent *case-routing* (engine). This update is a required deliverable.

---

## Types — eidos-api

### AgentSelector

```java
package io.casehub.eidos.api;

import java.util.List;

public interface AgentSelector {
    AgentSelection select(List<AgentMatch> candidates, SelectionContext context);
}
```

Single method. Takes the output of `AgentRegistry.find()` and returns the best agent (or failure/escalation). Health probing, trust lookup, and ranking are implementation concerns — the SPI imposes no requirements beyond "pick one."

### SelectionContext

```java
package io.casehub.eidos.api;

import java.util.Objects;

public record SelectionContext(
    String tenancyId,
    String capabilityName,
    String taskDomain
) {
    public SelectionContext {
        Objects.requireNonNull(tenancyId, "tenancyId");
    }

    public static SelectionContext of(String tenancyId, String capabilityName) {
        return new SelectionContext(tenancyId, capabilityName, null);
    }

    public static SelectionContext of(String tenancyId, String capabilityName, String taskDomain) {
        return new SelectionContext(tenancyId, capabilityName, taskDomain);
    }
}
```

- `tenancyId` — required (first in all factories), used by the bridge to construct `AgentRoutingContext`
- `capabilityName` — the capability being selected for; used for trust score lookup
- `taskDomain` — optional, used for health probing via `CapabilityHealth.ProbeContext`

Factory parameter order matches the record field order (`tenancyId, capabilityName, taskDomain`) — consistent across all overloads, no position-swap trap when adding `taskDomain` to an existing call.

### EscalationKind

```java
package io.casehub.eidos.api;

public enum EscalationKind {
    BORDERLINE_STALEMATE,
    NO_QUALIFIED_AGENT
}
```

Eidos-local mirror of engine's `EscalationReason` — only the variants relevant to agent selection. Keeps eidos-api free of engine-api dependency. The bridge maps engine's `EscalationReason` to `EscalationKind`.

### AgentSelection

```java
package io.casehub.eidos.api;

import jakarta.annotation.Nullable;

public sealed interface AgentSelection {

    record Selected(
        AgentDescriptor agent,
        @Nullable ResolvedCapability resolvedCapability,
        double trustScore,
        String reason
    ) implements AgentSelection {}

    record NoneQualified(String reason) implements AgentSelection {}

    record Escalated(
        String capabilityName,
        EscalationKind kind,
        String reason
    ) implements AgentSelection {}
}
```

- **Selected** — an agent was chosen. `resolvedCapability` is nullable (mirrors `AgentMatch` — null for slot-only queries). `trustScore` is the score used for ranking (capability, global, or bootstrap default — 0.0 when no trust source is available). `reason` explains why this agent won.
- **NoneQualified** — no agent met the criteria. Reason explains why (empty input, all unhealthy, all below threshold).
- **Escalated** — agents exist but human oversight is required. `kind` distinguishes borderline stalemate (agents are uncertain) from no qualified agent (none passed threshold). Only the engine-delegating path produces this.

---

## Implementation — eidos-runtime

### SimpleAgentSelector

```java
package io.casehub.eidos.runtime.selector;

@DefaultBean
@ApplicationScoped
public class SimpleAgentSelector implements AgentSelector {

    private final CapabilityHealth capabilityHealth;
    private final Instance<TrustScoreSource> trustSourceInstance;

    @ConfigProperty(name = "casehub.eidos.selector.trust-threshold", defaultValue = "0.0")
    double trustThreshold;

    @ConfigProperty(name = "casehub.eidos.selector.bootstrap-default-score", defaultValue = "0.5")
    double bootstrapDefaultScore;
}
```

**This is NOT a trust-maturity-model implementation.** It provides basic trust-informed selection for deployments without engine. For trust-maturity-model-compliant selection (BOOTSTRAP/BORDERLINE/QUALIFIED phases, policy-driven thresholds, escalation), deploy with `casehub-eidos-routing` + `casehub-engine`.

`TrustScoreSource` is injected via `Instance<TrustScoreSource>` — it is a ledger SPI with no `@DefaultBean` in eidos. When no `TrustScoreSource` provider is deployed, the selector operates in health-only mode: all agents get `trustScore=0.0`, selection is by `MatchDegree` tie-break only.

**Algorithm:**

1. **Empty guard** — if candidates is empty, return `NoneQualified("no candidates")`
2. **Health filter** — for each candidate, probe `CapabilityHealth.probe(descriptor, capabilityTag, ProbeContext.of(taskDomain))` where `capabilityTag` is `resolvedCapability.capability().name()` (the declared capability name, not the queried tag — subsumption may have matched a different name). For slot-only queries where `resolvedCapability` is null, use `context.capabilityName()`. Keep only `Ready`, `Degraded`, `EpistemicallyWeak`, and `BehavioralViolation`. Filter out `Unavailable` and `Excluded`.
3. **Trust resolution** — if `trustSourceInstance.isResolvable()`:
   - `TrustScoreSource.capabilityScore(agentId, capabilityName)` — if present, use it
   - else `TrustScoreSource.globalScore(agentId)` — if present, use it (CAPABILITY→GLOBAL fallback)
   - else `bootstrapDefaultScore` (configurable, default 0.5)
   - If `trustSourceInstance.isUnsatisfied()`: all candidates get `trustScore=0.0` (health-only mode)
4. **Threshold filter** — remove candidates below `trustThreshold` (default 0.0 means no filtering in health-only mode)
5. **Rank** — sort by trust score descending. Tie-break by `MatchDegree` (Exact wins — lower compareTo value). Second tie-break by agentId for determinism.
6. **Return** — `Selected` with the top candidate, or `NoneQualified` if all were filtered out

**Never returns Escalated** — no policy infrastructure. If all candidates are below threshold, returns `NoneQualified("all N candidates below trust threshold X")`.

**No health-based score demotion.** `Degraded`, `EpistemicallyWeak`, and `BehavioralViolation` agents are kept in the pool at their actual trust score. The engine-aware path handles health demotion via the strategy's scoring algorithm. Consumers needing health-weighted ranking must use the engine-aware path.

---

## Implementation — eidos-routing (new module)

### Module Structure

```
routing/
├── pom.xml                          (depends on eidos-api, engine-api, ledger-api)
└── src/main/java/io/casehub/eidos/routing/
    └── EngineAwareAgentSelector.java
```

No deployment module needed — Quarkus discovers `@Alternative @Priority(1)` beans automatically.

### Maven Dependencies

```xml
<dependencies>
    <dependency>
        <groupId>io.casehub</groupId>
        <artifactId>casehub-eidos-api</artifactId>
    </dependency>
    <dependency>
        <groupId>io.casehub</groupId>
        <artifactId>casehub-engine-api</artifactId>
    </dependency>
    <dependency>
        <groupId>io.casehub</groupId>
        <artifactId>casehub-ledger-api</artifactId>
    </dependency>
</dependencies>
```

`CapabilityHealth` is in eidos-api (SPI interface). `TrustScoreSource` is in ledger-api. No dependency on eidos-runtime — the bridge only needs API types. Runtime implementations are provided by the consumer's deployment.

### EngineAwareAgentSelector

```java
package io.casehub.eidos.routing;

@Alternative
@Priority(1)
@ApplicationScoped
public class EngineAwareAgentSelector implements AgentSelector {

    private final Instance<AgentRoutingStrategy> routingStrategies;
    private final CapabilityHealth capabilityHealth;
    private final Instance<TrustScoreSource> trustSourceInstance;
}
```

**Algorithm:**

1. **Convert AgentMatch → AgentCandidate** for each candidate:
   - `workerId` = `descriptor.agentId()`
   - `capabilities` = `descriptor.capabilities().stream().map(AgentCapability::name).collect(toSet())`
   - `runningJobs` = `0` (eidos has no workload tracking)
   - `health` = probe via `CapabilityHealth`, map `CapabilityStatus` to `AgentHealth`:
     - `Ready` → `AgentHealth.READY`
     - `Degraded` → `AgentHealth.DEGRADED`
     - `EpistemicallyWeak` → `AgentHealth.EPISTEMICALLY_WEAK`
     - `BehavioralViolation` → `AgentHealth.BEHAVIORAL_VIOLATION`
     - `Unavailable`, `Excluded` → filter out (engine expects pre-filtered candidates)
   - `agentDescriptor` = `descriptor` (AgentCandidate already carries this)
   - `matchDegree` = `resolvedCapability != null ? resolvedCapability.degree() : null`
   - `violations` = from `BehavioralViolation.violations()` if applicable, else null
2. **Construct AgentRoutingContext** from `SelectionContext`:
   - `caseId` = `null` (see Implementation Notes on null safety)
   - `capabilityName` = `context.capabilityName()`
   - `caseContext` = `null`
   - `tenancyId` = `context.tenancyId()`
   - `experiences` = `List.of()`
   - `cognitiveDemand` = `null`
   - `routingSignalWeights` = `null`
3. **Delegate to AgentRoutingStrategy** — resolve the highest-priority strategy: `routingStrategies.stream().sorted(comparingPriorityDesc()).findFirst()`. Call `select(routingContext, candidates)`.
4. **Convert RoutingResult → AgentSelection**:
   - `Selected(assignments)` → call `single()` to get the single assignment. Find the matching `AgentDescriptor` by `assignment.executorId()`. Look up trust score via `TrustScoreSource` (if available). Return `AgentSelection.Selected(descriptor, resolvedCapability, trustScore, reason)`.
   - `Unresolvable(reason)` → `AgentSelection.NoneQualified(reason)`
   - `Escalated(capability, escalationReason, rationale)` → map `EscalationReason` to `EscalationKind`, return `AgentSelection.Escalated(capability, kind, rationale)`

**Single-assignment limitation:** The bridge calls `RoutingResult.Selected.single()` — it only supports strategies that return one assignment. `TrustWeightedAgentStrategy` and `SemanticAgentRoutingStrategy` both return single assignments. Multi-assignment strategies (if any exist in the future) would throw `IllegalStateException`. This is acceptable for the current consumer (qhorus resolves one `role:X` target at a time).

**Trust score on Selected:** The bridge looks up the winning agent's score via `TrustScoreSource.capabilityScore()` → `globalScore()` → `0.0` (same fallback chain as `SimpleAgentSelector`). Injected via `Instance<TrustScoreSource>` with unsatisfied guard.

---

## Consumer Integration

### Qhorus usage pattern

```java
@Inject AgentRegistry registry;
@Inject AgentSelector selector;

List<AgentMatch> matches = registry.find(AgentQuery.byCapability("code-review", tenancyId));
AgentSelection selection = selector.select(matches,
    SelectionContext.of(tenancyId, "code-review"));

switch (selection) {
    case AgentSelection.Selected s -> dispatch(s.agent(), s.reason());
    case AgentSelection.NoneQualified nq -> handleNoAgent(nq.reason());
    case AgentSelection.Escalated e -> escalateToHuman(e.capabilityName(), e.kind(), e.reason());
}
```

Qhorus depends on `casehub-eidos-api` (SPI types) and `casehub-eidos` (runtime). If the deployment includes engine, adding `casehub-eidos-routing` activates the bridge. Qhorus code never changes — the CDI priority ladder handles the switch.

---

## Configuration

| Property | Default | Applies to | Description |
|---|---|---|---|
| `casehub.eidos.selector.trust-threshold` | `0.0` | SimpleAgentSelector | Minimum trust score to qualify |
| `casehub.eidos.selector.bootstrap-default-score` | `0.5` | SimpleAgentSelector | Score for agents with no trust history (capability or global) |

The engine-delegating path ignores these — it uses engine's `TrustRoutingPolicy` for thresholds and phase classification.

---

## Testing

### eidos-api — unit tests

- `SelectionContext` validation: null tenancyId rejected, factory methods produce correct records with consistent field ordering
- `AgentSelection` sealed variants: pattern matching exhaustiveness
- `EscalationKind` enum values match expected set

### eidos-runtime — SimpleAgentSelector unit tests

- Empty candidate list → `NoneQualified`
- All candidates unhealthy → `NoneQualified`
- Single healthy candidate → `Selected`
- Multiple candidates with different trust scores → highest score wins
- Trust score tie → `MatchDegree` tie-break (Exact > Plugin > Specialization)
- CAPABILITY→GLOBAL fallback: agent with no capability score but global score → uses global
- True bootstrap: no capability or global score → uses `bootstrapDefaultScore`
- Threshold filtering: candidate below `trustThreshold` → excluded
- All below threshold → `NoneQualified`
- Never returns `Escalated`
- **No TrustScoreSource available** (Instance unsatisfied) → health-only mode, all trustScore=0.0, selection by MatchDegree
- Health status: Degraded/EpistemicallyWeak/BehavioralViolation agents kept at actual trust score (no demotion)

Test infrastructure: mock `CapabilityHealth` (always Ready) and stub `TrustScoreSource` with predetermined scores, or leave `Instance<TrustScoreSource>` unsatisfied for health-only tests.

### eidos-routing — EngineAwareAgentSelector unit tests

- `AgentMatch → AgentCandidate` conversion: all fields mapped correctly
- Health status mapping: each `CapabilityStatus` variant → correct `AgentHealth`
- Unavailable/Excluded filtered before delegation
- `RoutingResult.Selected` → `AgentSelection.Selected` with correct descriptor lookup via `single()`
- `RoutingResult.Unresolvable` → `AgentSelection.NoneQualified`
- `RoutingResult.Escalated` → `AgentSelection.Escalated` with correct `EscalationKind` mapping
- Trust score populated on Selected result (with unsatisfied TrustScoreSource guard)
- CDI priority resolution: highest-priority strategy wins

Test infrastructure: mock `AgentRoutingStrategy` and `CapabilityHealth`.

### Integration test (examples module)

- End-to-end: register agents → find by capability → select best → verify result
- Uses `InMemoryAgentRegistry` + `InMemoryBehavioralSignalStore` from eidos-memory

---

## Implementation Notes

- **Health probing capabilityTag:** Use the declared capability name (`resolvedCapability.capability().name()`), not the queried tag. Subsumption matching may have mapped "code-review" → "review" (Plugin match). Health is probed against the agent's actual capability declaration.
- **AgentRoutingContext.caseId:** The bridge passes `null` — eidos context has no case ID. `TrustCandidateClassifier.classify()` does not reference caseId — it operates on candidates, capabilityName, policy, and source. `TrustWeightedAgentStrategy` passes the context through to scoring but workload scoring uses `runningJobs` from the candidate, not caseId. If null-safety is an issue during implementation, use a sentinel UUID (`UUID(0, 0)`).
- **CDI priority resolution:** Use `Instance<AgentRoutingStrategy>.stream()` sorted by `@Priority` annotation value descending. Pick the first (highest priority). This matches engine's `WorkOrchestrator` resolution semantics.

## Required Deliverables Beyond Code

- **Update `capability-ownership.md`** in casehub-parent: distinguish agent *discovery+selection* (eidos — `AgentSelector` SPI) from agent *case-routing* (engine — `AgentRoutingStrategy` SPI). Without this update, the platform docs will show conflicting ownership.

## Not in Scope

- **Reactive parity** (`ReactiveAgentSelector`) — follow-on issue when reactive consumers need it
- **Workload tracking from eidos context** — `runningJobs=0` in the bridge. Engine's full pipeline provides workload; the bridge doesn't.
- **Wilson ranking integration** — `AgentGraphQuery.topAgentsByOutcome()` is a separate ranking signal. Reconciling Wilson and Bayesian Beta is a future concern.
- **`casehub-eidos-routing` deployment module** — not needed for a simple `@Alternative @Priority(1)` bean
- **Health-based score demotion in SimpleAgentSelector** — Degraded/Weak/Violated agents kept at actual trust score. Engine-aware path handles demotion.

---

## References

- [AgentRoutingStrategy.java](/Users/mdproctor/claude/casehub/engine/api/src/main/java/io/casehub/api/spi/routing/AgentRoutingStrategy.java) — engine SPI for agent selection
- [TrustCandidateClassifier.java](/Users/mdproctor/claude/casehub/engine/ledger/src/main/java/io/casehub/ledger/routing/TrustCandidateClassifier.java) — trust maturity phase classification
- [TrustScoreSource.java](/Users/mdproctor/claude/casehub/ledger/api/src/main/java/io/casehub/ledger/api/spi/TrustScoreSource.java) — ledger-api trust score SPI
- [AgentMatch.java](/Users/mdproctor/claude/casehub/eidos/api/src/main/java/io/casehub/eidos/api/AgentMatch.java) — eidos discovery result
- [AgentCandidate.java](/Users/mdproctor/claude/casehub/engine/api/src/main/java/io/casehub/api/spi/routing/AgentCandidate.java) — engine candidate with health, workload, descriptor
- [RoutingResult.java](/Users/mdproctor/claude/casehub/engine/api/src/main/java/io/casehub/api/spi/routing/RoutingResult.java) — engine routing result (Selected/Unresolvable/Escalated)
- [capability-ownership.md](/Users/mdproctor/claude/casehub/parent/docs/platform/capability-ownership.md) — "Agent routing / selection → casehub-engine-api"
- [routing.md](/Users/mdproctor/claude/casehub/parent/docs/platform/routing.md) — four-layer routing architecture
- Decision review findings R1-01/R1-02 (capability ownership), R1-04 (CAPABILITY→GLOBAL fallback), R1-10 (escalation variant)
- Spec review findings R1-01 (Instance injection), R1-02 (maturity model scope), R1-03 (EscalationKind), R1-04 (ownership update), R1-05 (factory ordering), R1-06 (single assignment), R1-11 (routing deps)
