# io.casehub.eidos.api.AgentDescriptorValidator

**Package:** `io.casehub.eidos.api`

**Kind:** `class`

## Fields

### `MAX_AGENT_ID` (`int`)

### `MAX_ARCHETYPE` (`int`)

### `MAX_ARCHETYPE_ADJECTIVE` (`int`)

### `MAX_ARCHETYPE_ADJECTIVES` (`int`)

### `MAX_BRIEFING` (`int`)

### `MAX_CAPABILITIES` (`int`)

### `MAX_CAPABILITY_NAME` (`int`)

### `MAX_CAPABILITY_STRING` (`int`)

### `MAX_CONSTRAINTS` (`int`)

### `MAX_CONSTRAINT_DESCRIPTION` (`int`)

### `MAX_CONSTRAINT_NAME` (`int`)

### `MAX_DATA_HANDLING_POLICY` (`int`)

### `MAX_DESCRIPTION` (`int`)

### `MAX_DISPOSITION_AXIS` (`int`)

### `MAX_EXTENSION_DATA_SIZE` (`int`)

### `MAX_GOALS` (`int`)

### `MAX_GOAL_DESCRIPTION` (`int`)

### `MAX_GOAL_NAME` (`int`)

### `MAX_JURISDICTION` (`int`)

### `MAX_MODEL_IDENTIFIER` (`int`)

### `MAX_NAME` (`int`)

### `MAX_PARAMETER_NAME` (`int`)

### `MAX_PROVIDER` (`int`)

### `MAX_SLOT` (`int`)

### `MAX_TEMPLATE_CONTENT` (`int`)

### `MAX_TEMPLATE_ID` (`int`)

### `MAX_TEMPLATE_NAME` (`int`)

### `MAX_TENANCY_ID` (`int`)

### `MAX_VERSION` (`int`)

### `MAX_VOCABULARY_URI` (`int`)

### `MAX_WEIGHTS_FINGERPRINT` (`int`)

## Constructors

### `AgentDescriptorValidator()`

## Methods

### `private static boolean isAllowed(int cp, int[] allowedCodePoints)`

#### Parameters

- `cp` (`int`)
- `allowedCodePoints` (`int[]`)

### `private static boolean isBanned(int cp)`

#### Parameters

- `cp` (`int`)

### `static void validate(java.lang.String agentId, java.lang.String name, java.lang.String slot, java.lang.String tenancyId)`

#### Parameters

- `agentId` (`java.lang.String`)
- `name` (`java.lang.String`)
- `slot` (`java.lang.String`)
- `tenancyId` (`java.lang.String`)

### `private static void validateField(java.lang.String fieldName, java.lang.String value, int maxLength)`

#### Parameters

- `fieldName` (`java.lang.String`)
- `value` (`java.lang.String`)
- `maxLength` (`int`)

### `private static void validateField(java.lang.String fieldName, java.lang.String value, int maxLength, int[] allowedCodePoints)`

#### Parameters

- `fieldName` (`java.lang.String`)
- `value` (`java.lang.String`)
- `maxLength` (`int`)
- `allowedCodePoints` (`int[]`)

### `static void validateItems(java.lang.String fieldName, java.lang.Iterable<java.lang.String> items, int maxLength)`

#### Parameters

- `fieldName` (`java.lang.String`)
- `items` (`java.lang.Iterable<java.lang.String>`)
- `maxLength` (`int`)

### `static void validateMapKeys(java.lang.String fieldName, java.util.Set<java.lang.String> keys, int maxLength)`

#### Parameters

- `fieldName` (`java.lang.String`)
- `keys` (`java.util.Set<java.lang.String>`)
- `maxLength` (`int`)

### `static void validateOptional(java.lang.String fieldName, java.lang.String value, int maxLength)`

#### Parameters

- `fieldName` (`java.lang.String`)
- `value` (`java.lang.String`)
- `maxLength` (`int`)

### `static void validateOptional(java.lang.String fieldName, java.lang.String value, int maxLength, int[] allowedCodePoints)`

#### Parameters

- `fieldName` (`java.lang.String`)
- `value` (`java.lang.String`)
- `maxLength` (`int`)
- `allowedCodePoints` (`int[]`)

### `static void validateRequired(java.lang.String fieldName, java.lang.String value, int maxLength)`

#### Parameters

- `fieldName` (`java.lang.String`)
- `value` (`java.lang.String`)
- `maxLength` (`int`)

### `static void validateRequired(java.lang.String fieldName, java.lang.String value, int maxLength, int[] allowedCodePoints)`

#### Parameters

- `fieldName` (`java.lang.String`)
- `value` (`java.lang.String`)
- `maxLength` (`int`)
- `allowedCodePoints` (`int[]`)
