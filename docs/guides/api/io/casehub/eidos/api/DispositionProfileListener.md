# io.casehub.eidos.api.DispositionProfileListener

**Package:** `io.casehub.eidos.api`

**Kind:** `interface`

Notification callback for disposition profile evolution events.

<p>Called after a successful JPAF type transition or cognitive function
reweighting. Implementations use this for side-effects (audit, event
emission) — not for persistence. Evolved state is owned by the
personality evolution subsystem (e.g. neocortex's PersonalityStateService),
not by the eidos registry.

<p>The default bean (`NoOpDispositionProfileListener`) discards
the notification. Consumers that need to observe evolution provide
their own implementation.

## Methods

### `public abstract void onProfileEvolved(java.lang.String agentId, java.lang.String tenancyId, java.util.List<io.casehub.eidos.api.DispositionValue> newProfile)`

#### Parameters

- `agentId` (`java.lang.String`)
- `tenancyId` (`java.lang.String`)
- `newProfile` (`java.util.List<io.casehub.eidos.api.DispositionValue>`)
