# io.casehub.eidos.api.AgentQuery

**Package:** `io.casehub.eidos.api`

**Kind:** `record`

## Fields

### `capabilityName` (`java.lang.String`)

### `goalName` (`java.lang.String`)

### `slot` (`java.lang.String`)

### `taskDomain` (`java.lang.String`)

### `tenancyId` (`java.lang.String`)

## Record Components

### `capabilityName` (`java.lang.String`)

### `goalName` (`java.lang.String`)

### `slot` (`java.lang.String`)

### `taskDomain` (`java.lang.String`)

### `tenancyId` (`java.lang.String`)

## Constructors

### `public AgentQuery(java.lang.String slot, java.lang.String capabilityName, java.lang.String tenancyId, java.lang.String taskDomain, java.lang.String goalName)`

#### Parameters

- `slot` (`java.lang.String`)
- `capabilityName` (`java.lang.String`)
- `tenancyId` (`java.lang.String`)
- `taskDomain` (`java.lang.String`)
- `goalName` (`java.lang.String`)

## Methods

### `public static io.casehub.eidos.api.AgentQuery all(java.lang.String tenancyId)`

#### Parameters

- `tenancyId` (`java.lang.String`)

### `public static io.casehub.eidos.api.AgentQuery byCapability(java.lang.String capabilityName, java.lang.String tenancyId)`

#### Parameters

- `capabilityName` (`java.lang.String`)
- `tenancyId` (`java.lang.String`)

### `public static io.casehub.eidos.api.AgentQuery byCapabilityAndDomain(java.lang.String capabilityName, java.lang.String taskDomain, java.lang.String tenancyId)`

#### Parameters

- `capabilityName` (`java.lang.String`)
- `taskDomain` (`java.lang.String`)
- `tenancyId` (`java.lang.String`)

### `public static io.casehub.eidos.api.AgentQuery byGoal(java.lang.String goalName, java.lang.String tenancyId)`

#### Parameters

- `goalName` (`java.lang.String`)
- `tenancyId` (`java.lang.String`)

### `public static io.casehub.eidos.api.AgentQuery bySlot(java.lang.String slot, java.lang.String tenancyId)`

#### Parameters

- `slot` (`java.lang.String`)
- `tenancyId` (`java.lang.String`)

### `public static io.casehub.eidos.api.AgentQuery bySlotAndCapability(java.lang.String slot, java.lang.String capabilityName, java.lang.String tenancyId)`

#### Parameters

- `slot` (`java.lang.String`)
- `capabilityName` (`java.lang.String`)
- `tenancyId` (`java.lang.String`)

### `public java.lang.String capabilityName()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public java.lang.String goalName()`

### `public final int hashCode()`

### `public java.lang.String slot()`

### `public java.lang.String taskDomain()`

### `public java.lang.String tenancyId()`

### `public final java.lang.String toString()`
