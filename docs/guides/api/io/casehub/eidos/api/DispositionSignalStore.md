# io.casehub.eidos.api.DispositionSignalStore

**Package:** `io.casehub.eidos.api`

**Kind:** `interface`

Persistent store for cognitive function activation signals used by JPAF personality adaptation.

<p>Activation counts drive effective weight computation:
`effectiveWeight(f) = baseWeight(f) + activationCount(f) × \u0394w`.

## Methods

### `public abstract java.util.Map<java.lang.String,java.lang.Integer> activationCounts(java.lang.String agentId, java.lang.String tenancyId)`

#### Parameters

- `agentId` (`java.lang.String`)
- `tenancyId` (`java.lang.String`)

### `public abstract void clear(java.lang.String agentId, java.lang.String tenancyId)`

#### Parameters

- `agentId` (`java.lang.String`)
- `tenancyId` (`java.lang.String`)

### `public abstract void decay(java.lang.String agentId, java.lang.String tenancyId, double decayFactor)`

Multiplicative decay of all activation counts for an agent.

<p>`decayFactor` is the <em>retention fraction</em>: each count is multiplied
by this value. Semantics: 0.0 = instant reset (retain nothing),
1.0 = no decay (retain everything). JPAF default is 0.2 (retain 20%).

#### Parameters

- `agentId` (`java.lang.String`)
- `tenancyId` (`java.lang.String`)
- `decayFactor` (`double`)

### `public abstract void recordActivation(java.lang.String agentId, java.lang.String tenancyId, java.lang.String functionTerm)`

#### Parameters

- `agentId` (`java.lang.String`)
- `tenancyId` (`java.lang.String`)
- `functionTerm` (`java.lang.String`)
