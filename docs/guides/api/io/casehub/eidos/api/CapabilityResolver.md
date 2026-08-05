# io.casehub.eidos.api.CapabilityResolver

**Package:** `io.casehub.eidos.api`

**Kind:** `class`

Shared subsumption resolution utility for capabilities.
Used by both probe paths and recording paths to match declared capabilities
against requested capability tags using vocabulary-grounded subsumption.

<p>This utility eliminates duplication and ensures consistent matching logic
across String, ProbeContext)
and `BehavioralSignalStore` learned exclusion lookups.

## Constructors

### `private CapabilityResolver()`

## Methods

### `public static io.casehub.eidos.api.MatchDegree match(io.casehub.eidos.api.AgentCapability capability, java.lang.String capabilityTag, io.casehub.eidos.api.VocabularyRegistry registry)`

Computes the match degree between a declared capability and a requested capability tag.

<p>Matching logic:
<ul>
  <li>Exact name match → `MatchDegree.Exact`
  <li>Ungrounded capability (no `capabilityVocabulary`) → `MatchDegree.None`
  <li>Grounded capability → delegate to String, String)
</ul>

#### Parameters

- `capability` (`io.casehub.eidos.api.AgentCapability`) — the declared capability from an `AgentDescriptor`
- `capabilityTag` (`java.lang.String`) — the requested capability tag (e.g., from probe or query)
- `registry` (`io.casehub.eidos.api.VocabularyRegistry`) — the vocabulary registry for subsumption resolution

#### Returns

the match degree (Exact, Plugin, Specialization, or None)

### `public static io.casehub.eidos.api.ResolvedCapability resolve(java.util.List<io.casehub.eidos.api.AgentCapability> capabilities, java.lang.String capabilityTag, io.casehub.eidos.api.VocabularyRegistry registry)`

Resolves the best matching capability from a list of declared capabilities.

<p>Selection uses `MatchDegree.compareTo` — Exact wins immediately,
then the lowest-ranked (best) non-None degree. First in list wins at equal rank.

#### Parameters

- `capabilities` (`java.util.List<io.casehub.eidos.api.AgentCapability>`) — the list of declared capabilities to search
- `capabilityTag` (`java.lang.String`) — the requested capability tag
- `registry` (`io.casehub.eidos.api.VocabularyRegistry`) — the vocabulary registry for subsumption resolution

#### Returns

the best matching capability with its degree, or `null` if no match found
