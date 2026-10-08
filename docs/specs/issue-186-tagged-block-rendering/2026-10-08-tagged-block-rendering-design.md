# Tagged Block Rendering — Design Spec

**Issue:** casehubio/eidos#186
**Date:** 2026-10-08
**Status:** Draft

---

## Overview

The tagged block protocol (casehubio/neocortex#486) defines a structured prompt format where all content delivered to an agent is one of three types:

| Type | Marker | Purpose | Destination |
|------|--------|---------|-------------|
| Cognitive state | `[MOOD]`, `[BEHAVIORAL]`, etc. | Absorb and embody | User message |
| Command | `[DO]` | Execute (MCP calls, REST, messaging) | User message |
| Conversation | (no tag) | Respond naturally | User message |

The system prompt contains agent identity only (descriptor + cognitive brief template when applicable).

Eidos owns the protocol-level tags (`[DO]`, untagged conversation) and the block assembly pipeline. Neocortex owns cognitive tags internally — Eidos never enumerates them.

---

## Type Model

All types live in `io.casehub.eidos.api` (Tier 1, pure Java).

### PromptTier

Enum defining the four ordered tiers. Each tier maps to a prompt destination.

```java
public enum PromptTier {
    IDENTITY,       // system prompt — agent descriptor, cognitive brief
    COGNITIVE,      // user message — neocortex state blocks (absorb/embody)
    COMMAND,        // user message — [DO] instructions (execute)
    CONVERSATION;   // user message — untagged text (respond, always last)

    public boolean isSystemPrompt() {
        return this == IDENTITY;
    }
}
```

Declaration order is the sort order. No numeric ordinals — `Enum.ordinal()` provides the natural ordering.

### PromptBlock

Record representing a unit of prompt content.

```java
public record PromptBlock(
    PromptTier tier,
    String tag,         // e.g. "DO", "MOOD", null for untagged
    float salience,     // 0.0 default, higher = earlier within tier
    String content
) {
    public PromptBlock {
        Objects.requireNonNull(tier);
        if (content == null || content.isBlank()) {
            throw new AgentValidationException("promptBlock.content", "must not be null or blank");
        }
    }

    // Convenience factories
    public static PromptBlock identity(String content) {
        return new PromptBlock(PromptTier.IDENTITY, null, 0f, content);
    }

    public static PromptBlock cognitive(String tag, String content) {
        return new PromptBlock(PromptTier.COGNITIVE, tag, 0f, content);
    }

    public static PromptBlock command(String content) {
        return new PromptBlock(PromptTier.COMMAND, "DO", 0f, content);
    }

    public static PromptBlock conversation(String content) {
        return new PromptBlock(PromptTier.CONVERSATION, null, 0f, content);
    }
}
```

**Tag rendering format:** Blocks with a non-null `tag` render as the tag on its own line, content below (matching neocortex's `BlockTag.render()` convention):
```
[MOOD]
warmth: 0.7, arousal: 0.3
```
Blocks with null `tag` render as bare `content` (no prefix line). The assembler handles this.

**Block delimiters:** Blocks are separated by a single blank line (`\n\n`). Within a block, the tag line and content are separated by a single newline (`\n`).

### AssembledPrompt

Output of the assembly pipeline.

```java
public record AssembledPrompt(
    String systemPrompt,    // IDENTITY tier blocks, concatenated
    String userMessage       // COGNITIVE + COMMAND + CONVERSATION tier blocks, ordered
) {}
```

---

## SPI

### PromptContributor

```java
@FunctionalInterface
public interface PromptContributor {
    List<PromptBlock> contribute(AgentDescriptor descriptor, AgentPromptContext context);
}
```

CDI-discovered via `Instance<PromptContributor>`. Each implementation produces blocks with appropriate tiers. The assembler collects all contributions.

**Request-scoped state:** The SPI signature is intentionally identity-scoped — it receives the agent descriptor and render context, not per-turn cognitive state. Contributors that need request-scoped state (e.g., neocortex's current mood, drives) inject it via CDI `@RequestScoped` beans internally. The SPI stays clean; the contributor's CDI bean has access to whatever context it needs at injection time.

**Neocortex implementation example:** A `CognitivePromptContributor` (`@ApplicationScoped`) in neocortex's cognition module would:
1. Inject `@RequestScoped CognitionRenderContext` for current cognitive state
2. Check if the agent has cognitive capabilities
3. Produce `PromptBlock.identity(cognitiveBriefTemplate)` — the brief goes in the system prompt
4. Produce `PromptBlock.cognitive("MOOD", moodContent)`, etc. — cognitive state goes in the user message

**Platform implementation example:** A `CommandPromptContributor` could produce `PromptBlock.command("Call MCP gardenSearch ...")` for pending tool calls.

### PromptAssembler

```java
public interface PromptAssembler {
    AssembledPrompt assemble(AgentDescriptor descriptor, AgentPromptContext context);
}
```

The assembler is the entry point for consumers that want the full tagged prompt. It:
1. Renders the agent descriptor into IDENTITY blocks (using the existing `EidosRenderPipeline`)
2. Collects blocks from all `PromptContributor` instances
3. Sorts all blocks: by tier (enum order), then by salience (descending) within tier
4. Routes blocks to system prompt or user message based on `tier.isSystemPrompt()`
5. Renders tags: `[TAG] content` for tagged blocks, bare `content` for untagged
6. Concatenates within each destination
7. Returns `AssembledPrompt`

**Error handling:** If a `PromptContributor.contribute()` throws, the assembler logs the exception and skips that contributor (fail-open). A broken contributor must not abort the entire prompt assembly — the agent should still receive its identity and any blocks from healthy contributors.

---

## Assembly Order

The final user message follows this structure:

```
[MOOD] warmth: 0.7, arousal: 0.3
[BEHAVIORAL] You tend toward protective warmth...
[DRIVES] curiosity about the newcomer
[GOALS] Build trust with Peter-Perfect
...
[DO] Call MCP gardenSearch for recent observations
[DO] Check message queue for unread items
Hey, how are you doing today?
```

Within the COGNITIVE tier, neocortex controls internal ordering (its own BlockTag ordinals). The assembler uses a stable sort: blocks are sorted by tier (enum order), then by salience (descending) within tier. When salience is equal (the default), the contributor's insertion order is preserved. This means a single contributor's blocks maintain their relative order, and blocks from different contributors with equal salience interleave in CDI discovery order.

---

## Integration with Existing Renderer

The existing rendering pipeline is **unaffected**:

| Path | Entry point | Output | Use case |
|------|-------------|--------|----------|
| **Existing** | `SystemPromptRenderer.render(desc, ctx)` | `RenderedPrompt` (single string) | System prompt only — current consumers |
| **New** | `PromptAssembler.assemble(desc, ctx)` | `AssembledPrompt` (system + user) | Full tagged prompt — cognitive agents |

The `PromptAssembler` default implementation (`DefaultPromptAssembler` in `eidos-core`) uses `EidosRenderPipeline` internally for the IDENTITY tier — it calls `buildStage1()` and `assemble()` to produce the system prompt, then wraps the result as an IDENTITY block. This avoids duplicating the existing rendering logic.

Consumers choose which path to call:
- Apps without cognitive agents continue using `renderer.render()`
- Apps with cognitive agents (wacky-manor) switch to `assembler.assemble()` and get both system prompt and user message

---

## Module Placement

| Type | Module | Package |
|------|--------|---------|
| `PromptTier` | `eidos-api` | `io.casehub.eidos.api` |
| `PromptBlock` | `eidos-api` | `io.casehub.eidos.api` |
| `AssembledPrompt` | `eidos-api` | `io.casehub.eidos.api` |
| `PromptContributor` | `eidos-api` | `io.casehub.eidos.api.spi` |
| `PromptAssembler` | `eidos-api` | `io.casehub.eidos.api` |
| `DefaultPromptAssembler` | `eidos-core` | `io.casehub.eidos.core.renderer` |

All API types are Tier 1 pure Java. The runtime implementation lives in `eidos-core` alongside `EidosRenderPipeline`.

---

## Backward Compatibility

- No changes to `AgentDescriptor`, `AgentPromptContext`, `SystemPromptRenderer`, or `EidosRenderPipeline`
- No changes to existing YAML deserialization or A2A_CARD rendering
- `PromptContributor` has no `@DefaultBean` — when no contributors exist, the assembler produces IDENTITY blocks only (equivalent to `render()`)
- Existing consumers are unaffected

---

## Out of Scope

- **Cognitive tag enum in Eidos** — neocortex owns `BlockTag`. Eidos only defines `PromptTier` and the `[DO]` tag constant.
- **JPA persistence of blocks** — blocks are transient render-time objects, not stored.
- **Annotation support** — no `@PromptContributor` annotation processor. CDI discovery is sufficient.
- **A2A_CARD format** — tagged blocks are for LLM consumption. A2A_CARD is machine-readable JSON and doesn't use the block protocol.
- **LLM semantic enrichment of blocks** — the existing enrichment pipeline (disposition/goal narratives) applies to IDENTITY tier only. Contributors produce pre-rendered content.

---

## Future Direction

- **App-controlled assembly inversion** — the app constructs the full prompt, Eidos provides rendered text as building blocks. The `PromptAssembler` SPI already supports this: an app can provide its own implementation that reorders or filters blocks.
- **Additional tiers** — if new content categories emerge (e.g., OBSERVATION between COGNITIVE and COMMAND), the `PromptTier` enum can be extended. This is a protocol change requiring a version bump.

---

## References

- casehubio/neocortex#486 — tagged block protocol (BlockTag enum, cognitive-brief-template.md)
- `eidos-core/.../renderer/EidosRenderPipeline.java` — existing rendering pipeline
- `eidos-core/.../renderer/EidosSystemPromptRenderer.java` — two-step orchestration
- `api/.../spi/VocabularyRegistrar.java` — SPI pattern precedent
- `api/.../spi/TemplateRegistrar.java` — SPI pattern precedent
- `neocortex/cognition-api/.../prompt/BlockTag.java` (branch issue-486) — cognitive tag ordinals
- `neocortex/cognition-api/src/main/resources/cognitive-brief-template.md` — brief template
- `neocortex/cognition/.../prompt/DirectiveSection.java` (branch issue-486) — tag wrapping
- `examples/wacky-manor/.../PlaybookOrchestrator.java` — consumer rendering call pattern
- Commit `3bf55083` in neocortex — DO removed from BlockTag ("belongs in platform")
