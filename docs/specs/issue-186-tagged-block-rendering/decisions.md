# Decisions — Issue #186: Tagged Block Rendering

## D1: Cognitive brief discovery mechanism

**Choice:** SPI injection with lifecycle callback points for pluggable enrichment
**Alternatives:**
- Classpath resource convention (`META-INF/eidos/cognitive-brief.md`) — simple but inflexible, no metadata, no conditional injection
- AgentPromptContext field — pushes responsibility to consumer, loses the "Eidos assembles the complete prompt" benefit
**Rationale:** Follows existing Eidos SPI patterns (VocabularyRegistrar, TemplateRegistrar). Eidos stays decoupled from neocortex at compile time. Lifecycle callbacks generalize beyond cognitive brief — any module can enrich the system prompt or user message at defined hook points.
**Trade-offs:** More API surface than a classpath convention. SPI contract must be stable.
**Sources:** EidosSystemPromptRenderer.java, CognitivePreambleGenerator in neocortex, VocabularyRegistrar SPI pattern
**Exploration:** quick
**Status:** captured

## D2: Block type model ownership

**Choice:** `PromptBlock` type lives in Eidos api/ (Tier 1, pure Java). Eidos owns assembly. Future optional inversion: app constructs the brief, Eidos provides existing text for the app to wrap.
**Alternatives:**
- String-based protocol (no shared type) — simpler but pushes assembly to every consumer, ordering rules reimplemented per app
- Shared type in platform-api — violates the "nothing Eidos goes in platform-api" rule
**Rationale:** Eidos already owns prompt assembly. A Tier 1 type means any module can produce blocks without depending on neocortex or Eidos runtime. Keeps the door open for a future inversion where the app takes control of assembly using Eidos-provided content.
**Trade-offs:** Eidos takes on assembly responsibility. Future inversion will need a clean SPI boundary.
**Sources:** AgentDescriptor.java (Tier 1 pattern), issue #186 "renderer block abstraction"
**Exploration:** quick
**Status:** captured

## D3: Block ordering mechanism

**Choice:** Tier bands with intra-tier autonomy, plus optional salience (float, default 0.0) as an escape hatch within a tier
**Alternatives:**
- Fixed numeric ordinals (neocortex's current model) — creates implicit cross-module coupling via coordinated numbers
- Salience-only scoring (0.0–1.0) — collisions between modules produce non-deterministic order without tiebreakers
- Relative constraints ("after X", "before Y") — expressive but heavyweight, reintroduces coupling via naming
**Rationale:** Tiers match the issue's three-type taxonomy exactly, with identity as a natural first tier. Each module controls order within its own tier. Optional salience (default 0.0, not shown in normal use) provides an escape hatch for edge cases without polluting the common API. Deterministic: tier order is fixed, intra-tier order is module-controlled, salience breaks ties only when explicitly used.
**Trade-offs:** The tier list must be stable — adding a new tier is a protocol change. Salience escape hatch could be misused if overspecified (mitigated by default 0.0 and not promoting it).
**Sources:** BlockTag ordinals in neocortex (100-530), issue #186 ordering requirements
**Exploration:** quick
**Depends on:** D2 (block type model)
**Status:** captured

## D4: Tier list and destination mapping

**Choice:** Four tiers spanning both system prompt and user message. Tier determines position AND destination.
**Tiers:**
- `IDENTITY` → system prompt (agent descriptor, cognitive brief)
- `COGNITIVE` → user message (neocortex state blocks, absorb/embody)
- `COMMAND` → user message ([DO] instructions, execute)
- `CONVERSATION` → user message (untagged text, respond — always last)
**Alternatives:**
- User-message-only tiers (system prompt stays a separate path) — simpler but loses the single ordered model; callers must coordinate two outputs
- Fine-grained tiers (split COGNITIVE into sub-tiers matching neocortex's identity/state/knowledge/strategy/signals) — unnecessary coupling; neocortex owns intra-COGNITIVE ordering
**Rationale:** A single tier enum gives one ordered model of the full prompt. The assembler routes blocks to system prompt or user message based on tier. Consumers get two outputs from one input (List<PromptBlock> → system prompt string + user message string). Four tiers match the protocol's three content types plus identity framing.
**Trade-offs:** The assembler must handle the system/user split. Adding a tier between COGNITIVE and COMMAND (e.g., OBSERVATION) requires an enum change.
**Sources:** Issue #186 three-type table, BlockTag tiers in neocortex
**Depends on:** D3 (ordering mechanism)
**Exploration:** quick
**Status:** captured

## D5: Lifecycle callback SPI shape

**Choice:** Single `PromptContributor` SPI with one method returning `List<PromptBlock>`. Tier on each block determines positioning and destination. CDI-discovered (`Instance<PromptContributor>`).
**Alternatives:**
- Multi-phase hooks (before-identity, after-identity, before-command, etc.) — more control but redundant with the tier model; more API surface for no benefit
- Event-based (CDI `@Observes` on render events) — harder to collect return values; fire-and-forget pattern doesn't fit "contribute blocks"
**Rationale:** One method, one SPI. The tier model already handles positioning — contributors produce blocks with tiers, the assembler sorts and routes. No phase coordination needed. Follows Eidos's CDI discovery pattern (like `VocabularyRegistrar`, `TemplateRegistrar`).
**Trade-offs:** Contributors can't condition on what other contributors produced (no inter-contributor dependency). Acceptable — blocks are independent by design (D3).
**Sources:** VocabularyRegistrar SPI pattern, TemplateRegistrar SPI pattern
**Depends on:** D2 (block type model), D4 (tier list)
**Exploration:** quick
**Status:** captured

## D6: Assembler output shape

**Choice:** New `AssembledPrompt(String systemPrompt, String userMessage)` record. Separate from existing `RenderedPrompt`. New `assemble(List<PromptBlock>)` method alongside existing `render()`.
**Alternatives:**
- Extend `RenderedPrompt` with optional `userMessage` field — fewer types but muddies the existing single-string contract; backward compat risk
- Single concatenated string — loses system/user distinction needed by LLM APIs
**Rationale:** Clean separation. Existing `render(descriptor, context) → RenderedPrompt` path is unaffected. Consumers that want tagged blocks use the new `assemble()` path. New type makes the two-output nature explicit in the API.
**Trade-offs:** Two rendering paths to maintain. Consumers must choose which to call. Acceptable — the paths serve different use cases (identity-only vs full tagged assembly).
**Sources:** SystemPromptRenderer.RenderedPrompt, EidosSystemPromptRenderer.render()
**Depends on:** D4 (tier list determines routing)
**Exploration:** quick
**Status:** captured
