# Decisions — Issue #215: External Identity Service

## D1: Resolution return type

**Choice:** Use existing `ResolvedIdentity(actorId, tenancyId, roles)` from qhorus-api
**Alternatives:**
- Enrich ResolvedIdentity with capabilities — couples qhorus-api to eidos-api
- New SPI in eidos-api returning AgentDescriptor subset — forces qhorus to depend on eidos for its own auth SPI
**Rationale:** Consumers that need capabilities do a follow-up `AgentRegistry.findById()`. The registry already has the full descriptor. Keeps the SPI boundary clean.
**Trade-offs:** Extra lookup for capability-aware consumers; acceptable since capability queries are not on every auth request.
**Sources:** `io.casehub.qhorus.api.spi.ResolvedIdentity`, `io.casehub.qhorus.api.spi.ApiKeyIdentityResolver`
**Exploration:** quick
**Status:** captured

## D2: SPI placement — eidos store, qhorus adapter

**Choice:** Credential store SPI and implementations in eidos. Qhorus (downstream) writes adapter from eidos store to its `ApiKeyIdentityResolver`.
**Alternatives:**
- Bridge module `casehub-eidos-qhorus` in eidos — wrong dependency direction; eidos is upstream of qhorus
- Promote `ApiKeyIdentityResolver` to platform-api — broadens platform-api scope; requires qhorus refactor
- Eidos depends on qhorus-api directly — creates upstream-to-downstream dependency
**Rationale:** Eidos is upstream of qhorus. Dependency flows naturally: qhorus → eidos-api. No bridge module needed. Qhorus already has the SPI and the `ApiKeyAuthenticationMechanism`; it just needs an adapter that delegates to eidos's store.
**Trade-offs:** Qhorus needs a small code change to wire the adapter. Acceptable — it's a downstream consumer change.
**Sources:** CLAUDE.md ecosystem context, cross-foundation-bridge-module-placement protocol, user clarification on dependency direction
**Exploration:** quick
**Status:** captured

## D3: Scope — CRUD + rotation; defer dynamic self-registration

**Choice:** Static CRUD (issue, resolve, revoke, list) + key rotation with grace period. Dynamic self-registration (agents registering their own credentials during group formation) deferred to follow-up issue.
**Alternatives:**
- All three (A+B+C) — dynamic self-registration depends on group formation APIs that don't exist yet
- CRUD only (no rotation) — rotation is essential for production credential management
**Rationale:** Dynamic self-registration requires the self-organising societies / group formation context (not yet designed). The SPI can accommodate it later via default methods.
**Trade-offs:** Dynamic registration deferred; follow-up issue to be filed.
**Sources:** Issue #215 body (three tiers described)
**Exploration:** quick
**Status:** captured

## D4: Domain type — own AgentCredential in eidos-api

**Choice:** Define `AgentCredential` record in `io.casehub.eidos.api`
**Alternatives:**
- Reuse `StaticCredentialRecord` from `io.casehub.platform.api.authn` — constrains the model to platform's shape (no rotation metadata, no grace period TTL)
**Rationale:** Eidos credentials carry agent-specific concerns: rotation state, grace period expiry, link to agentId within tenancy. `StaticCredentialRecord` doesn't model these. Keeps eidos self-contained per platform-api-scope protocol.
**Trade-offs:** Slight type duplication with platform's `StaticCredentialRecord`; justified by different domain concerns.
**Sources:** `io.casehub.platform.api.authn.StaticCredentialRecord`, `io.casehub.platform.api.authn.CredentialType`, platform-api-scope protocol
**Exploration:** quick
**Status:** captured

## D5: Key scope — full agent identity

**Choice:** A credential resolves to the full agent identity (actorId, tenancyId, roles). No capability scoping on credentials.
**Alternatives:**
- Capability-scoped keys (optional `Set<String> allowedCapabilities` on credential) — adds complexity to store and every consumer
**Rationale:** Capability filtering happens downstream via `AgentRegistry`/`CapabilityHealth` as it does today. Matches the qhorus `ResolvedIdentity` contract. Capability scoping can be added later as an optional field without breaking the SPI.
**Trade-offs:** No fine-grained key scoping; all keys grant full agent identity access.
**Sources:** `ResolvedIdentity(actorId, tenancyId, roles)` contract
**Exploration:** quick
**Status:** captured

## D6: Persistence — both tiers in one design

**Choice:** Design the full model (SPI, in-memory, JPA) in one spec. Implement in-memory first per eidos-poc-gate protocol.
**Alternatives:**
- In-memory only, defer JPA design — risks SPI rework when JPA constraints surface
**Rationale:** Having the JPA design upfront avoids SPI rework. Follows the existing eidos CDI ladder pattern (NoOp → InMemory → JPA).
**Trade-offs:** Larger design scope; mitigated by in-memory-first implementation order.
**Sources:** eidos-poc-gate-before-jpa-infrastructure protocol, existing CDI ladder in eidos (NoOpAgentStateStore → InMemoryAgentStateStore → JpaAgentStateStore)
**Exploration:** quick
**Status:** captured

## D7: Rotation model — grace period with configurable TTL

**Choice:** Rotation creates a new key while the old one remains valid for a configurable TTL (store-owned, via `@ConfigProperty`).
**Alternatives:**
- Atomic swap — old key immediately invalid; causes auth failures during propagation delay
- Explicit overlap — both keys valid until caller explicitly revokes old one; no time pressure but requires cleanup discipline
**Rationale:** Prevents dropped requests during rotation in distributed systems. Store-owned TTL follows the store-owned-retention-mechanism protocol.
**Trade-offs:** Slightly more complex store (must track rotation state and expiry). Two keys valid simultaneously during grace period.
**Sources:** store-owned-retention-mechanism protocol, `BehavioralSignalStore` per-signal TTL pattern
**Exploration:** quick
**Status:** captured

## D8: Module structure — existing eidos modules

**Choice:** SPI in `eidos-api`. InMemory impl in `persistence-memory`. JPA impl in `runtime`. No new module.
**Alternatives:**
- New `casehub-eidos-identity` module — adds Maven module overhead for 3-4 classes; can always extract later
**Rationale:** Credential identity is an agent-level concern. Follows established pattern — `AgentStateStore`, `BehavioralSignalStore`, `DispositionSignalStore` all live in the existing module structure.
**Trade-offs:** Slightly larger existing modules; acceptable given the small surface area.
**Sources:** Existing module structure in CLAUDE.md
**Exploration:** quick
**Status:** captured

## D9: Trust integration — existing auth flow sufficient

**Choice:** No special credential→ledger hooks. The existing `SecurityIdentity` → `CurrentPrincipal` path means API-key agents get the same trust profile as OIDC agents.
**Alternatives:**
- Explicit trust hooks — credential lifecycle events (issued, rotated, revoked) that the ledger observes; couples credential store to ledger concerns
**Rationale:** `ApiKeyAuthenticationMechanism` already converts `ResolvedIdentity` to `SecurityIdentity` with `tenancyId` attribute. Ledger attestations use `actorId` regardless of auth method. The auth method is transparent to the trust system.
**Trade-offs:** No credential lifecycle auditing via ledger. Can be added later if needed without SPI changes (CDI events from the store implementation).
**Sources:** `ApiKeyAuthenticationMechanism` in qhorus mesh, `SecurityIdentityCurrentPrincipal` in platform-oidc
**Exploration:** quick
**Status:** captured

## D10: SPI shape — thin store with separate operations

**Choice:** `CredentialIdentityStore` SPI with individual methods: `issue`, `resolve`, `rotate`, `revoke`, `findByAgent`. Pure data store — key generation is a runtime concern.
**Alternatives:**
- Command-style store (`execute(CredentialCommand)`) — over-engineered for 5 operations, unlike any existing eidos SPI
- Split resolution/management (two SPIs) — cleaner separation but doubles interface surface; hot-path optimization can happen inside single SPI implementation
**Rationale:** Matches established eidos SPI patterns (`AgentStateStore`, `BehavioralSignalStore`, `DispositionSignalStore` all use individual methods). Each operation is independently testable.
**Trade-offs:** Less extensible than command pattern for adding operations; mitigated by default methods on the interface.
**Sources:** `AgentStateStore`, `BehavioralSignalStore`, `DispositionSignalStore` SPI patterns
**Exploration:** quick
**Status:** captured
