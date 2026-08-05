# io.casehub.eidos.api.BehavioralSignalStore

**Package:** `io.casehub.eidos.api`

**Kind:** `interface`

## Methods

### `public abstract void clear(java.lang.String agentId, java.lang.String tenancyId, java.lang.String capabilityName, io.casehub.eidos.api.BehavioralSignal signal)`

Retracts all learned data of the given signal type for an
(agentId, tenancyId, capabilityName) triple.
Clears all qualifier entries regardless of TTL.

<p>`capabilityName` must be the agent's declared capability name —
see `.record` for details.

#### Parameters

- `agentId` (`java.lang.String`)
- `tenancyId` (`java.lang.String`)
- `capabilityName` (`java.lang.String`)
- `signal` (`io.casehub.eidos.api.BehavioralSignal`)

### `public abstract int count(java.lang.String agentId, java.lang.String tenancyId, java.lang.String capabilityName, java.lang.String qualifier, io.casehub.eidos.api.BehavioralSignal signal)`

Returns the count of unexpired records for the given signal type and qualifier.
0 when no unexpired records exist. Never negative.

<p>`capabilityName` must be the agent's declared capability name —
see `.record` for details.

#### Parameters

- `agentId` (`java.lang.String`)
- `tenancyId` (`java.lang.String`)
- `capabilityName` (`java.lang.String`)
- `qualifier` (`java.lang.String`)
- `signal` (`io.casehub.eidos.api.BehavioralSignal`)

### `public abstract java.util.Map<java.lang.String,java.lang.Integer> learned(java.lang.String agentId, java.lang.String tenancyId, java.lang.String capabilityName, io.casehub.eidos.api.BehavioralSignal signal)`

Returns qualifier to count of unexpired records for the given signal type,
for all qualifiers with at least one unexpired record.
Empty map when none. Never null.

<p>`capabilityName` must be the agent's declared capability name —
see `.record` for details.

#### Parameters

- `agentId` (`java.lang.String`)
- `tenancyId` (`java.lang.String`)
- `capabilityName` (`java.lang.String`)
- `signal` (`io.casehub.eidos.api.BehavioralSignal`)

### `public abstract void record(java.lang.String agentId, java.lang.String tenancyId, java.lang.String capabilityName, java.lang.String qualifier, io.casehub.eidos.api.BehavioralSignal signal)`

Records one signal event for the given agent, capability, and qualifier.
TTL is owned by the store implementation — per-signal TTL is supported.

<p>The `qualifier` parameter is a free-text key whose meaning depends
on signal type: task domain for DECLINE/SUCCESS signals, compliance dimension
key for COMPLIANT/VIOLATED signals.

<p>`capabilityName` must be the agent's declared capability name
(as returned by `AgentCapability.name()`), not a query/lookup term.
When the caller has a query tag instead, use
String, VocabularyRegistry)
to obtain the declared capability first.

#### Parameters

- `agentId` (`java.lang.String`)
- `tenancyId` (`java.lang.String`)
- `capabilityName` (`java.lang.String`)
- `qualifier` (`java.lang.String`)
- `signal` (`io.casehub.eidos.api.BehavioralSignal`)
