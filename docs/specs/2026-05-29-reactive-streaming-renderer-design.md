# Reactive Streaming Renderer — Design Spec
**Date:** 2026-05-29  
**Issues:** eidos#18 (DefaultReactiveCapabilityHealth worker pool), eidos#17 (ReactiveSystemPromptRenderer via StreamingChatModel)  
**Branch:** issue-018-17-reactive-streaming

---

## Problem

Two inconsistencies in the reactive rendering layer introduced in eidos#7:

**#18 (one-liner):** `DefaultReactiveCapabilityHealth.probe()` wraps a blocking delegate without `.runSubscriptionOn(Infrastructure.getDefaultWorkerPool())`. All other reactive bridge impls in this repo (`DefaultReactiveSystemPromptRenderer`, `DefaultReactiveAgentStateStore`) correctly offload to the worker pool. This one was missed.

**#17 (architectural):** `DefaultReactiveSystemPromptRenderer` is a bridge: it offloads the blocking `EidosSystemPromptRenderer.render()` call to a worker thread. The LLM call inside (`ChatModel.chat(ChatRequest)`) holds that worker thread for the full duration of inference — often seconds. With `StreamingChatModel`, the LLM call is fire-and-forget; the thread is released immediately and the result arrives via `onCompleteResponse` callback. The reactive renderer should use this when available.

---

## Fix for #18

Add `.runSubscriptionOn(Infrastructure.getDefaultWorkerPool())` to `DefaultReactiveCapabilityHealth.probe()`:

```java
return Uni.createFrom()
          .item(() -> delegate.probe(descriptor, capabilityTag, context))
          .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
```

No other changes. The existing tests (`DefaultReactiveCapabilityHealthTest`, `DefaultReactiveCapabilityHealthDefaultProfileTest`) verify functional behaviour (Ready, Unavailable, EpistemicallyWeak) via `.await().indefinitely()`. They confirm the correct result is returned whether or not the fix is present — the blocking delegate still returns regardless. What they do not assert is that the probe runs on a worker thread. Thread-offloading is consistent with the pattern used by all other reactive bridge impls in this repo, and will be observable under Vert.x strict-mode or when a real `JpaAgentStateStore` blocks.

---

## Architecture for #17

### Shared pipeline extraction: `EidosRenderPipeline` (new, `@ApplicationScoped`)

All format-assembly, payload-building, and shared static state are extracted from `EidosSystemPromptRenderer` into `EidosRenderPipeline`. This class is pure computation plus cache management — no LLM calls, no I/O. It is declared `@ApplicationScoped` and injected by both renderers as a standard CDI bean.

`EidosRenderPipeline` owns:

**Shared static constants** (moved from blocking step classes):
- `PROMPT_TEMPLATE` — narrative enrichment prompt template (was in `EidosSystemPromptRenderer`, used by `SemanticEnrichmentStep`)
- `RESPONSE_FORMAT` — JSON response schema for narrative enrichment (was in `SemanticEnrichmentStep`)
- `A2A_PROMPT_TEMPLATE` — A2A enrichment prompt template (was in `A2ASemanticEnrichmentStep`)
- `A2A_RESPONSE_FORMAT` — JSON response schema for A2A enrichment (was in `A2ASemanticEnrichmentStep`)
- `TEMPLATE_HASH` — derived from `PROMPT_TEMPLATE` at class load; part of every cache key

**Methods:**
- `buildDescriptorPayload(AgentDescriptor) → ObjectNode`
- `buildContextPayload(AgentPromptContext) → ObjectNode`
- `buildLlmPayload(ObjectNode descriptorNode, ObjectNode contextNode) → ObjectNode` — goal-only slice; only constructed when `usesEnrichment(format)` is true
- `cacheKey(String descriptorHash, String contextHash, RenderFormat) → String`
- `usesEnrichment(RenderFormat) → boolean` (static) — `true` for CLAUDE_MD, OPENAI_SYSTEM, GEMINI; `false` for A2A_CARD
- `assemble(Optional<SemanticEnrichment>, Optional<A2AEnrichment>, AgentDescriptor, AgentPromptContext) → String` — the full format-specific switch containing all the assembly methods that currently live in `EidosSystemPromptRenderer`
- `assembleAndCache(String cacheKey, Optional<SemanticEnrichment>, Optional<A2AEnrichment>, AgentDescriptor, AgentPromptContext) → RenderedPrompt` — wraps `assemble()` with cache write
- `fingerprint(String) → String` (static)

The blocking step classes (`SemanticEnrichmentStep`, `A2ASemanticEnrichmentStep`) reference their constants via `EidosRenderPipeline.PROMPT_TEMPLATE`, `EidosRenderPipeline.RESPONSE_FORMAT`, etc.

`EidosSystemPromptRenderer` becomes a thin orchestrator: injects `EidosRenderPipeline`, `SemanticEnrichmentStep`, `A2ASemanticEnrichmentStep`; its `render()` method delegates Stage 1 payload building and Stage 3 assembly to the pipeline. The ~200 lines of assembly methods (`assembleClaudeMarkdown`, `assembleOpenAiSystem`, `assembleGemini`, `assembleA2aCard`, etc.) all move to `EidosRenderPipeline`.

### Reactive enrichment steps (new, package-private)

Two new classes in `io.casehub.eidos.runtime.renderer`:

**`ReactiveSemanticEnrichmentStep`**
- Constructor: `ReactiveSemanticEnrichmentStep(ObjectMapper mapper)` — stateless
- Method: `Uni<Optional<SemanticEnrichment>> enrich(StreamingChatModel llm, ObjectNode payload)`
- Uses `EidosRenderPipeline.PROMPT_TEMPLATE` and `EidosRenderPipeline.RESPONSE_FORMAT`
- Implemented via `CompletableFuture` + `completionStage()` (idiomatic for one-shot async bridging; timeout-trivial):
  ```java
  CompletableFuture<Optional<SemanticEnrichment>> future = new CompletableFuture<>();
  llm.chat(request, new StreamingChatResponseHandler() {
      public void onCompleteResponse(ChatResponse r) {
          future.complete(parseOrEmpty(r.aiMessage().text()));
      }
      public void onError(Throwable t) { future.completeExceptionally(t); }
  });
  return Uni.createFrom().completionStage(
          future.orTimeout(EidosRenderPipeline.STREAMING_TIMEOUT_SECONDS, TimeUnit.SECONDS))
      .onFailure().recoverWithItem(e -> { log.warn(...); return Optional.empty(); });
  ```
- `parseOrEmpty()` returns `Optional.of(parsed)` on success, `Optional.empty()` on `JsonProcessingException`
- `onFailure().recoverWithItem()` swallows both timeout and streaming errors — fallback to structural rendering is always preserved

**`ReactiveA2ASemanticEnrichmentStep`**
- Same pattern: `Uni<Optional<A2AEnrichment>> enrich(StreamingChatModel llm, ObjectNode descriptorNode)`
- Uses `EidosRenderPipeline.A2A_PROMPT_TEMPLATE` and `EidosRenderPipeline.A2A_RESPONSE_FORMAT`

`EidosRenderPipeline.STREAMING_TIMEOUT_SECONDS` is a configurable constant (default 30s); future iterations can make this a `@ConfigProperty` if needed.

### `DefaultReactiveSystemPromptRenderer` (rewritten)

Injects:
- `@Any Instance<StreamingChatModel> streamingLlmInstance` — optional, resolved to `null` at construction time (same `@Dependent`-scope caveat as `ChatModel` in `EidosSystemPromptRenderer` applies: Quarkus LangChain4j always registers `StreamingChatModel` as `@ApplicationScoped`, so `instance.get()` does not leak)
- `SystemPromptRenderer blockingDelegate` — for fallback when streaming LLM absent
- `EidosRenderPipeline pipeline` — CDI-injected shared pipeline
- `ObjectMapper mapper`

At construction: resolves `streamingLlm` from instance; builds `ReactiveSemanticEnrichmentStep`, `ReactiveA2ASemanticEnrichmentStep`.

`render(AgentDescriptor, AgentPromptContext)` flow:

```
// Fast path: no streaming LLM — delegate to blocking renderer offloaded to worker pool
if (streamingLlm == null):
    return Uni.createFrom().item(() -> blockingDelegate.render(descriptor, context))
              .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());

// Stage 1 — payload build + cache check on worker pool
// (not on the event loop — ObjectMapper serialization is CPU work)
return Uni.createFrom().item(() -> {
    ObjectNode descriptorNode = pipeline.buildDescriptorPayload(descriptor);
    ObjectNode contextNode    = pipeline.buildContextPayload(context);
    String descriptorHash     = pipeline.fingerprint(descriptorNode.toString());
    String contextHash        = pipeline.fingerprint(contextNode.toString());
    String cacheKey           = pipeline.cacheKey(descriptorHash, contextHash, context.format());
    Optional<RenderedPrompt> cached = cache.get(cacheKey);
    return new StageOneResult(descriptorNode, contextNode, cacheKey, cached.orElse(null));
}).runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
.chain(s1 -> {
    // Cache hit: return synchronously (already on worker pool)
    if (s1.cached != null) return Uni.createFrom().item(s1.cached);

    // Stage 2 — reactive enrichment (fire-and-forget to streaming provider)
    RenderFormat format = context.format();
    boolean needsEnrichment = pipeline.usesEnrichment(format);
    boolean needsA2A        = format == RenderFormat.A2A_CARD;

    ObjectNode llmPayload = needsEnrichment ? pipeline.buildLlmPayload(s1.descriptorNode, s1.contextNode) : null;

    Uni<Optional<SemanticEnrichment>> enrichUni = needsEnrichment
        ? reactiveEnrichStep.enrich(streamingLlm, llmPayload)
        : Uni.createFrom().item(Optional.empty());
    Uni<Optional<A2AEnrichment>> a2aUni = needsA2A
        ? reactiveA2aStep.enrich(streamingLlm, s1.descriptorNode)
        : Uni.createFrom().item(Optional.empty());

    // Stage 3 — assembly on worker pool (not on streaming callback thread)
    return Uni.combine().all().unis(enrichUni, a2aUni).asTuple()
        .emitOn(Infrastructure.getDefaultWorkerPool())
        .map(t -> pipeline.assembleAndCache(s1.cacheKey, t.getItem1(), t.getItem2(), descriptor, context));
});
```

`StageOneResult` is a private record carrying the intermediate state between the two Uni stages.

### What does NOT change

- `SemanticEnrichmentStep` public API — `enrich(ChatModel, ObjectNode)` signature unchanged
- `A2ASemanticEnrichmentStep` public API — `enrich(ChatModel, ObjectNode)` signature unchanged
- `EidosSystemPromptRenderer` public API — `render(AgentDescriptor, AgentPromptContext)` signature unchanged
- All `@DefaultBean`/`@ApplicationScoped` annotations on existing classes — unchanged
- The fallback guarantee: no LLM configured → structural rendering always works

---

## ResponseFormat compatibility with StreamingChatModel

`StreamingChatModel.chat(ChatRequest, handler)` accepts the full `ChatRequest` including `ResponseFormat`. The `onCompleteResponse(ChatResponse)` callback delivers the same accumulated `ChatResponse` as the blocking `ChatModel.chat(ChatRequest)`. For structured JSON output, partial tokens are irrelevant — parsing happens once in `onCompleteResponse`. This is the same fallback-on-parse-failure contract as the blocking path.

Provider-level streaming compatibility with `ResponseFormat.JSON` varies, but the fallback (`Optional.empty()` → structural rendering) handles any provider that doesn't respect it.

The `CompletableFuture.orTimeout()` wrapping handles providers that neither complete nor fail. The provider's own HTTP client timeout is the primary mechanism; `STREAMING_TIMEOUT_SECONDS` is the Mutiny-level backstop.

---

## Testing

### #18

`DefaultReactiveCapabilityHealthTest` and `DefaultReactiveCapabilityHealthDefaultProfileTest` verify functional behaviour. No new tests required — the fix is a one-liner consistent with the established bridge pattern.

### #17

**`ReactiveSemanticEnrichmentStepTest`** (pure Java, no Quarkus):

Parsing logic (`parse(String json) → SemanticEnrichment`) moves to `EidosRenderPipeline`. Full parse coverage (required fields, optional fields, blank handling, all six fields) is tested via `EidosRenderPipelineTest` (or the existing `SemanticEnrichmentStepTest` refactored). `ReactiveSemanticEnrichmentStepTest` covers only the async bridging contract:

- `enrich_completes_with_enrichment_when_llm_succeeds` — mock `StreamingChatModel.chat(ChatRequest, handler)` invokes `onCompleteResponse` with valid JSON; verify `Optional` contains parsed enrichment
- `enrich_falls_back_to_empty_when_llm_fires_on_error` — mock invokes `onError`; verify `Optional.empty()`
- `enrich_falls_back_to_empty_when_parse_fails` — mock invokes `onCompleteResponse` with malformed JSON; verify `Optional.empty()`
- `enrich_falls_back_to_empty_on_timeout` — mock never invokes either callback; set `STREAMING_TIMEOUT_SECONDS` to 0; verify `Optional.empty()`
- `enrich_invokes_streaming_api_not_blocking_overload` — verify `chat(ChatRequest, StreamingChatResponseHandler)` is called, not `chat(ChatRequest)` (the blocking overload)

**`ReactiveA2ASemanticEnrichmentStepTest`** — same structure, 5 analogous tests.

**`DefaultReactiveSystemPromptRendererStreamingTest`** (`@QuarkusTest`, `ReactiveTestProfile`):

- `renders_with_streaming_llm_when_present` — mock `StreamingChatModel @Alternative @Priority(2)` calls `onCompleteResponse` with valid enrichment JSON; verify `render()` returns non-null `RenderedPrompt` containing expected content
- `uses_streaming_api_not_blocking_overload` — verify mock's `chat(ChatRequest, StreamingChatResponseHandler)` is called (not the blocking overload)
- `falls_back_to_structural_when_streaming_llm_on_error` — mock `StreamingChatModel` calls `onError`; verify structural rendering returned (non-null `RenderedPrompt`)
- `falls_back_to_blocking_delegate_when_streaming_llm_absent` — no mock `StreamingChatModel` registered; verify `render()` still works and the blocking delegate is used
- `cache_hit_returns_without_any_llm_call` — prime cache; verify render returns cached result and mock's `chat()` is never called

**`DefaultReactiveSystemPromptRendererDefaultProfileTest`** (existing, if not already present) — covers blocking fallback under default profile. Inherits from the existing renderer test structure.

---

## Files changed

| File | Change |
|---|---|
| `runtime/.../health/DefaultReactiveCapabilityHealth.java` | Add `.runSubscriptionOn(workerPool)` |
| `runtime/.../renderer/EidosRenderPipeline.java` | New `@ApplicationScoped` — Stage 1 payloads, Stage 3 assembly, all shared constants, `STREAMING_TIMEOUT_SECONDS` |
| `runtime/.../renderer/EidosSystemPromptRenderer.java` | Substantively stripped — all assembly methods + constants move to `EidosRenderPipeline`; slim orchestrator remains |
| `runtime/.../renderer/SemanticEnrichmentStep.java` | Constants reference via `EidosRenderPipeline.*`; otherwise unchanged |
| `runtime/.../renderer/A2ASemanticEnrichmentStep.java` | Constants reference via `EidosRenderPipeline.*`; otherwise unchanged |
| `runtime/.../renderer/ReactiveSemanticEnrichmentStep.java` | New — streaming LLM → `Uni<Optional<SemanticEnrichment>>` via `CompletableFuture` |
| `runtime/.../renderer/ReactiveA2ASemanticEnrichmentStep.java` | New — streaming A2A → `Uni<Optional<A2AEnrichment>>` via `CompletableFuture` |
| `runtime/.../renderer/DefaultReactiveSystemPromptRenderer.java` | Rewrite — full reactive pipeline with worker-pool stage 1, streaming stage 2, worker-pool stage 3 |
| `runtime/src/test/...ReactiveSemanticEnrichmentStepTest.java` | New (5 tests — async bridging contract only) |
| `runtime/src/test/...ReactiveA2ASemanticEnrichmentStepTest.java` | New (5 tests) |
| `runtime/src/test/...DefaultReactiveSystemPromptRendererStreamingTest.java` | New (5 tests) |

## Follow-up issues

**eidos#19 — ReactiveRenderedPromptCache SPI:** The synchronous `RenderedPromptCache.get()` call in the reactive path is safe today because the only implementation is an in-memory no-op. If a future implementation is backed by Redis or another external store, that call becomes a blocking I/O operation on the event loop. File as a follow-up to add a `ReactiveRenderedPromptCache` SPI alongside the blocking one.
