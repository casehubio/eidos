# Design: Reactive Parity, GEMINI Rendering, and A2A Per-Capability Narratives

**Branch:** `issue-007-reactive-parity-rendering`  
**Issues:** eidos#7 (reactive parity + JPA), eidos#13 (A2A capability narratives), eidos#14 (GEMINI + rename)  
**Date:** 2026-05-29

---

## Overview

Three related improvements to casehub-eidos:

1. **Reactive parity** — `ReactiveAgentStateStore` and `ReactiveSystemPromptRenderer` SPIs with bridge-pattern defaults, plus JPA persistence for `AgentStateStore`.
2. **GEMINI rendering** — implement proper header-free prose output for the GEMINI format; remove the placeholder that delegated to CLAUDE_MD output.
3. **A2A per-capability narratives** — introduce a dedicated `A2ASemanticEnrichmentStep` (separate from the narrative enrichment path) so A2A cards carry LLM-generated prose per capability without token waste on non-A2A renders.
4. **Class rename** — `ClaudeMarkdownRenderer` → `EidosSystemPromptRenderer` to reflect multi-format reality.
5. **Fix `DefaultReactiveCapabilityHealth`** — remove incorrect `@IfBuildProperty` gate; align with platform bridge pattern.

---

## Issue #7 — Reactive parity + JPA persistence for AgentStateStore

### New SPIs (casehub-eidos-api)

**`ReactiveAgentStateStore`**

Exact reactive mirror of `AgentStateStore`:
```java
Uni<Void>                        record(String agentId, DegradationReason reason, Instant expiresAt)
Uni<Optional<DegradationReason>> query(String agentId)
Uni<Void>                        clear(String agentId)
```

**`ReactiveSystemPromptRenderer`**

```java
Uni<RenderedPrompt> render(AgentDescriptor descriptor, AgentPromptContext context)
```

This SPI exists to prepare the call-site contract for eidos#17 (truly async rendering via `StreamingChatModel`). The current bridge impl is synchronous-under-the-hood; consumers can already write reactive call sites today and the upgrade to real async in eidos#17 requires no caller change.

### Platform pattern: bridge, not build-gate

The canonical reactive parity pattern (from `BlockingToReactiveBridge` in casehub-platform, `NoOpReactivePlanItemStore` in casehub-engine) is:
- No-op / bridge reactive impl: `@DefaultBean @ApplicationScoped` — **never** `@IfBuildProperty`-gated
- JPA reactive: `@IfBuildProperty(reactive=true)` — gated only because it needs Hibernate Reactive
- InMemory: `@Alternative @Priority(1)` — activated by classpath presence

`DefaultReactiveCapabilityHealth` currently deviates from this pattern (it is `@IfBuildProperty`-gated with no Hibernate Reactive dependency). This branch corrects it: remove the build gate, add `@DefaultBean`.

### Runtime implementations (casehub-eidos)

**`DefaultReactiveAgentStateStore`** — `@DefaultBean @ApplicationScoped`

Bridges to any `AgentStateStore` delegate via `Uni.createFrom().item(supplier).runSubscriptionOn(Infrastructure.getDefaultWorkerPool())`. The `runSubscriptionOn` offload is required because the real delegate (`JpaAgentStateStore`) is a blocking JPA call; the bridge must not run on the Vert.x event loop thread. Always available — no build flag needed.

**`DefaultReactiveSystemPromptRenderer`** — `@DefaultBean @ApplicationScoped`

Bridges to `SystemPromptRenderer` delegate via `Uni.createFrom().item(supplier).runSubscriptionOn(Infrastructure.getDefaultWorkerPool())`. `runSubscriptionOn` is required: `render()` makes a blocking HTTP call to the LLM endpoint (500ms–5s); running that on the event loop thread would trigger a Vert.x blocked-thread warning. Truly async rendering (via `StreamingChatModel`) is deferred to eidos#17.

**`DefaultReactiveCapabilityHealth` fix** — remove `@IfBuildProperty`, add `@DefaultBean`. No logic change.

**`JpaAgentStateStore`** — `@IfBuildProperty(name="casehub.eidos.reactive.enabled", stringValue="false", enableIfMissing=true)`, `@ApplicationScoped`

Replaces `NoOpAgentStateStore` when reactive is disabled. Uses `EntityManager` + `@Transactional`. `record()` is an upsert: DELETE by agent_id, then `em.flush(); em.clear()` (required to make the bulk delete visible to the session and drop the first-level cache so `persist()` doesn't see a phantom — identical pattern to `JpaAgentRegistry.register()`), then `em.persist(new entity)`. `query()` selects by agent_id with `expires_at > :now` — expired rows are not returned but remain until overwritten or cleared. `clear()` deletes by agent_id. No bulk expiry cleanup — TTL is enforced read-side only, matching `InMemoryAgentStateStore` semantics. Follows `JpaAgentRegistry` build-gating pattern.

**`JpaReactiveAgentStateStore`** — `@IfBuildProperty(name="casehub.eidos.reactive.enabled", stringValue="true")`, `@ApplicationScoped`

Hibernate Reactive Panache. Same TTL semantics. Follows `JpaReactiveAgentRegistry` pattern.

### Schema (V2 migration)

V2 is the next sequential migration after V1 (`V1__initial_schema.sql`).

`db/eidos/migration/V2__agent_degradation_state.sql`:
```sql
CREATE TABLE agent_degradation_state (
    agent_id            VARCHAR(255)             NOT NULL PRIMARY KEY,
    degradation_reason  VARCHAR(50)              NOT NULL,
    expires_at          TIMESTAMP WITH TIME ZONE NOT NULL
);
```

`TIMESTAMP WITH TIME ZONE` (not `TIMESTAMP`): stores UTC, safe for comparison against `Instant.now()` regardless of DB session timezone. `TIMESTAMP` (without timezone) compares against session-local time and produces wrong results when the DB session is not UTC.

Deployment processor (`EidosProcessor`) gains a `@BuildStep` registering `db/eidos/migration/*.sql` via `NativeImageResourcePatternsBuildItem` per PP-20260528-flyway-ext-reg.

### Persistence-memory (casehub-eidos-memory)

**`InMemoryReactiveAgentStateStore`** — `@Alternative @Priority(1)`

Delegates to `@Inject InMemoryAgentStateStore` (same CDI instance). Wraps each call in `Uni.createFrom().item(...)` — no `runSubscriptionOn` needed; in-memory ConcurrentHashMap operations are microsecond-level. Follows `InMemoryReactiveAgentRegistry` pattern exactly.

### Testing

**Extend `BlockingReactiveParityTest`** — add method-name parity assertions for:
- `AgentStateStore` / `ReactiveAgentStateStore`
- `SystemPromptRenderer` / `ReactiveSystemPromptRenderer`

**Abstract contract tests** (api module, following engine's `PlanItemStoreContractTest` pattern):
- `AgentStateStoreContractTest` — abstract; asserts `record`/`query`/`clear` and TTL expiry semantics
- `ReactiveAgentStateStoreContractTest` — abstract reactive mirror

**Concrete test extensions:**
- `InMemoryAgentStateStoreTest` extends `AgentStateStoreContractTest` (already exists; ensure it covers TTL)
- `InMemoryReactiveAgentStateStoreTest` extends `ReactiveAgentStateStoreContractTest`
- `JpaAgentStateStoreTest` — `@QuarkusTest` with H2 (default test profile)
- `JpaReactiveAgentStateStoreTest` — `@QuarkusTest` with `@TestProfile(ReactiveTestProfile.class)` (Postgres via Dev Services)

**`DefaultReactiveCapabilityHealth` tests** — two tests are needed:
- Existing test under `@TestProfile(ReactiveTestProfile.class)` — verifies behaviour when reactive is enabled
- New test under default profile (no `@TestProfile`) — verifies the bean activates correctly without the build property; previously the `@IfBuildProperty` gate meant this case was untestable

---

## Issue #14 — GEMINI rendering + class rename

### Rename

`ClaudeMarkdownRenderer` → `EidosSystemPromptRenderer`. The class renders four formats; the old name implies Claude-only. IntelliJ refactoring updates all references including `SemanticEnrichmentStep` (which references the class's `PROMPT_TEMPLATE` constant), test file (`ClaudeMarkdownRendererTest` → `EidosSystemPromptRendererTest`), and all comments.

### GEMINI enriched path

Gemini system instructions expect plain prose — no `#` markdown headers. The enriched narratives are already prose, so assembly concatenates them with blank-line separators:

```
{identityNarrative} {roleNarrative}

{capabilityNarrative}

{dispositionNarrative, if present}

{constraintNarrative, if present}

{goalNarrative, if present}

Resources: {comma-separated label(uri) list, if present}

{situationalContext, if present}
```

**Resources format delta:** GEMINI uses `label(uri)` (no space before paren). OPENAI_SYSTEM uses `label (uri)` (space before paren). This is a deliberate divergence: GEMINI prose targets Gemini's instruction style; the format difference is explicit and tested.

### GEMINI structural path

No markdown headers. Produces the same structural content shape as `assembleOpenAiSystem()` structural path: dense prose organized into paragraphs by concern (identity, capabilities, disposition, goal). Kept as a separate method so future divergence (e.g., formatting differences for Gemini 2.x) requires no structural change.

### Test updates

- `gemini_structural_produces_same_content_as_claude_md_structural` — **deleted**. It was testing placeholder behaviour that the fix deliberately removes.
- New: `gemini_structural_has_no_markdown_headers()` — GEMINI structural output contains no `#` chars.
- New: `gemini_enriched_has_no_markdown_headers()` — GEMINI enriched path uses prose format.
- New: `gemini_enriched_contains_identity_and_role_narrative()` — narratives flow through.
- New: `gemini_enriched_resources_format_uses_no_space_before_paren()` — verifies `label(uri)` not `label (uri)`.

---

## Issue #13 — A2A_CARD per-capability prose narratives

### Design: separated A2A enrichment path

A2A enrichment is structurally different from narrative enrichment and must not share the schema or payload:

1. **Token waste:** `usesEnrichment()` returns `true` for CLAUDE_MD, OPENAI_SYSTEM, and GEMINI. Adding `capabilityNarratives` to the shared `RESPONSE_FORMAT` schema would generate N capability descriptions on every non-A2A render and discard them. For an agent with 5 capabilities that's 5–10 extra LLM sentences per render — paid for and never used.

2. **Payload contamination:** `buildLlmPayload()` includes the goal from context. A2A cards describe structural capability metadata — they are descriptor-only and context-independent. Including a transient render goal in an A2A LLM call would colour capability narratives with goal context.

The fix: a dedicated `A2ASemanticEnrichmentStep` with its own schema and a descriptor-only payload. The main enrichment path (`SemanticEnrichmentStep`) and `SemanticEnrichment` record are unchanged.

### New types (package-private, `io.casehub.eidos.runtime.renderer`)

**`A2AEnrichment`** record:
```java
record A2AEnrichment(List<CapabilityNarrative> capabilityNarratives) {
    record CapabilityNarrative(String name, String description) {}
}
```

**`A2ASemanticEnrichmentStep`** class:
- Has its own `A2A_PROMPT_TEMPLATE`: requests only `capabilityNarratives` — one entry per declared capability, name copied exactly, description 1–2 sentences in second person
- Has its own `A2A_RESPONSE_FORMAT` JSON schema: `capabilityNarratives` array with `name` + `description` required per item
- Takes `ObjectNode descriptorNode` only (no context/goal payload)
- Returns `Optional<A2AEnrichment>` (empty on LLM failure or parse failure — same fallback discipline as `SemanticEnrichmentStep`)

### `render()` flow change

The existing `render()` method is extended with a parallel enrichment branch for A2A_CARD:

```
Stage 2a (unchanged): narrative enrichment
  if (llm != null && usesEnrichment(format)):  // A2A_CARD still false here
      enrichment = enrichmentStep.enrich(llm, buildLlmPayload(descriptorNode, contextNode))

Stage 2b (new): A2A enrichment
  if (format == A2A_CARD && llm != null):
      a2aEnrichment = a2aEnrichmentStep.enrich(llm, descriptorNode)  // descriptor-only

Stage 3 (updated assembleA2aCard signature):
  A2A_CARD -> assembleA2aCard(a2aEnrichment, descriptor)
```

`usesEnrichment(A2A_CARD)` remains `false`.

### A2A_CARD assembly

`assembleA2aCard(Optional<A2AEnrichment> enrichment, AgentDescriptor descriptor)`. When enrichment is present, each capability JSON object gains `"description"` from the narratives list, matched by capability name (exact string equality).

```json
// Enriched
{"name":"code-review","qualityHint":0.95,"description":"You conduct thorough Java code reviews..."}

// Structural (no LLM)
{"name":"code-review","qualityHint":0.95}
```

**Name matching vs index matching:** Name matching is chosen over index matching (matching by array position). Index matching is more robust to LLM paraphrasing but assumes the LLM returns narratives in the same order as declared capabilities — an ordering guarantee that is not in the schema. Name matching is explicit and verifiable. The trade-off: an LLM that reformats a capability name (capitalisation, whitespace) results in a graceful omission (`description` absent for that capability) rather than a wrong match. Tests cover both the happy path and the mismatch/omission case.

Mismatch handling:
- LLM returns a name not in declared capabilities → silently ignored
- A declared capability has no matching narrative (LLM omission) → `description` omitted for that capability, no failure, no placeholder

### Test updates

- `a2a_card_skips_llm_even_when_llm_is_configured` — **deleted** in the #13 commit (atomic deletion). A2A_CARD now invokes LLM.
- New: `a2a_card_enriched_includes_capability_descriptions()` — with LLM configured, capability objects contain `description`.
- New: `a2a_card_structural_omits_descriptions()` — without LLM, no `description` field.
- New: `a2a_card_enriched_matches_capability_names()` — narrative names match capability names exactly.
- New: `a2a_card_enriched_ignores_unmatched_narrative_names()` — LLM returns a name not in declared capabilities; output omits `description` for that capability and does not throw.

---

## Execution order

1. **#7** — new SPIs, bridge impls, JPA impls, migration, InMemory reactive, parity test extension, contract tests
2. **#14** — rename `ClaudeMarkdownRenderer` → `EidosSystemPromptRenderer` (IntelliJ refactoring), then implement `assembleGemini()` proper, update tests
3. **#13** — add `A2AEnrichment`, `A2ASemanticEnrichmentStep`, update `render()` flow and `assembleA2aCard()`, update tests

Each issue commits separately with its own `Closes #N`.

---

## Platform coherence

| Protocol | Status |
|----------|--------|
| `llm-pass-structural-fallback` (PP-20260529-35f3bd) | ✅ GEMINI structural now format-specific |
| `renderer-cache-key-includes-format` (PP-20260529-5c883f) | ✅ already in place |
| `alternative-extension-patterns` | ✅ InMemory reactive uses Pattern B |
| CDI priority ladder | ✅ `@DefaultBean` → JPA (`@IfBuildProperty`) → InMemory (`@Alternative @Priority(1)`) |
| Reactive bridge pattern | ✅ bridges are `@DefaultBean @ApplicationScoped`, no build gate; blocking bridges use `runSubscriptionOn(Infrastructure.getDefaultWorkerPool())` |
| Flyway path scoping (PP-20260528-flyway-ext-reg) | ✅ V2 at `db/eidos/migration/`; `NativeImageResourcePatternsBuildItem` in deployment |

## Deferred

| Issue | Reason |
|-------|--------|
| eidos#17 — Truly async `ReactiveSystemPromptRenderer` via `StreamingChatModel` | Requires investigating `ResponseFormat` + streaming compatibility in LangChain4j 1.14.x |
| parent#92 — Write `persistence-backend-cdi-priority.md` universal protocol | Parent repo docs work, separate concern |
