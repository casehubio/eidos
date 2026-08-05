# io.casehub.eidos.api.AgentDescriptorComparator

**Package:** `io.casehub.eidos.api`

**Kind:** `class`

## Fields

### `COMPARED_CAPABILITY_FIELD_COUNT` (`int`)

### `COMPARED_CONSTRAINT_FIELD_COUNT` (`int`)

### `COMPARED_DISPOSITION_FIELD_COUNT` (`int`)

### `COMPARED_FIELD_COUNT` (`int`)

### `COMPARED_GOAL_FIELD_COUNT` (`int`)

## Constructors

### `private AgentDescriptorComparator()`

## Methods

### `public static io.casehub.eidos.api.AgentDescriptorComparator.ComparisonResult compare(io.casehub.eidos.api.AgentDescriptor desired, io.casehub.eidos.api.AgentDescriptor actual)`

#### Parameters

- `desired` (`io.casehub.eidos.api.AgentDescriptor`)
- `actual` (`io.casehub.eidos.api.AgentDescriptor`)

### `private static void compareAxisVocabularies(java.util.List<io.casehub.eidos.api.AgentDescriptorComparator.FieldDrift> drifts, java.util.Map<io.casehub.eidos.api.DispositionAxis,java.lang.String> desired, java.util.Map<io.casehub.eidos.api.DispositionAxis,java.lang.String> actual)`

#### Parameters

- `drifts` (`java.util.List<io.casehub.eidos.api.AgentDescriptorComparator.FieldDrift>`)
- `desired` (`java.util.Map<io.casehub.eidos.api.DispositionAxis,java.lang.String>`)
- `actual` (`java.util.Map<io.casehub.eidos.api.DispositionAxis,java.lang.String>`)

### `private static void compareCapabilities(java.util.List<io.casehub.eidos.api.AgentDescriptorComparator.FieldDrift> drifts, java.util.List<io.casehub.eidos.api.AgentCapability> desired, java.util.List<io.casehub.eidos.api.AgentCapability> actual)`

#### Parameters

- `drifts` (`java.util.List<io.casehub.eidos.api.AgentDescriptorComparator.FieldDrift>`)
- `desired` (`java.util.List<io.casehub.eidos.api.AgentCapability>`)
- `actual` (`java.util.List<io.casehub.eidos.api.AgentCapability>`)

### `private static void compareCapability(java.util.List<io.casehub.eidos.api.AgentDescriptorComparator.FieldDrift> drifts, java.lang.String capName, io.casehub.eidos.api.AgentCapability desired, io.casehub.eidos.api.AgentCapability actual)`

#### Parameters

- `drifts` (`java.util.List<io.casehub.eidos.api.AgentDescriptorComparator.FieldDrift>`)
- `capName` (`java.lang.String`)
- `desired` (`io.casehub.eidos.api.AgentCapability`)
- `actual` (`io.casehub.eidos.api.AgentCapability`)

### `private static void compareConstraints(java.util.List<io.casehub.eidos.api.AgentDescriptorComparator.FieldDrift> drifts, java.util.List<io.casehub.eidos.api.AgentConstraint> desired, java.util.List<io.casehub.eidos.api.AgentConstraint> actual)`

#### Parameters

- `drifts` (`java.util.List<io.casehub.eidos.api.AgentDescriptorComparator.FieldDrift>`)
- `desired` (`java.util.List<io.casehub.eidos.api.AgentConstraint>`)
- `actual` (`java.util.List<io.casehub.eidos.api.AgentConstraint>`)

### `private static void compareDisposition(java.util.List<io.casehub.eidos.api.AgentDescriptorComparator.FieldDrift> drifts, io.casehub.eidos.api.AgentDisposition desired, io.casehub.eidos.api.AgentDisposition actual)`

#### Parameters

- `drifts` (`java.util.List<io.casehub.eidos.api.AgentDescriptorComparator.FieldDrift>`)
- `desired` (`io.casehub.eidos.api.AgentDisposition`)
- `actual` (`io.casehub.eidos.api.AgentDisposition`)

### `private static void compareField(java.util.List<io.casehub.eidos.api.AgentDescriptorComparator.FieldDrift> drifts, java.lang.String field, java.lang.Object desired, java.lang.Object actual)`

#### Parameters

- `drifts` (`java.util.List<io.casehub.eidos.api.AgentDescriptorComparator.FieldDrift>`)
- `field` (`java.lang.String`)
- `desired` (`java.lang.Object`)
- `actual` (`java.lang.Object`)

### `private static void compareGoals(java.util.List<io.casehub.eidos.api.AgentDescriptorComparator.FieldDrift> drifts, java.util.List<io.casehub.eidos.api.AgentGoal> desired, java.util.List<io.casehub.eidos.api.AgentGoal> actual)`

#### Parameters

- `drifts` (`java.util.List<io.casehub.eidos.api.AgentDescriptorComparator.FieldDrift>`)
- `desired` (`java.util.List<io.casehub.eidos.api.AgentGoal>`)
- `actual` (`java.util.List<io.casehub.eidos.api.AgentGoal>`)

### `private static void compareSimpleFields(java.util.List<io.casehub.eidos.api.AgentDescriptorComparator.FieldDrift> drifts, io.casehub.eidos.api.AgentDescriptor desired, io.casehub.eidos.api.AgentDescriptor actual)`

#### Parameters

- `drifts` (`java.util.List<io.casehub.eidos.api.AgentDescriptorComparator.FieldDrift>`)
- `desired` (`io.casehub.eidos.api.AgentDescriptor`)
- `actual` (`io.casehub.eidos.api.AgentDescriptor`)
