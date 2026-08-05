# io.casehub.eidos.api.MatchDegree.Specialization

**Package:** `io.casehub.eidos.api`

**Kind:** `record`

Specialization match — declared capability is more specific (child) than requested.

## Fields

### `depth` (`int`)

## Record Components

### `depth` (`int`)

distance in the hierarchy (1 = direct child, 2 = grandchild, etc.)

## Constructors

### `public Specialization(int depth)`

#### Parameters

- `depth` (`int`)

## Methods

### `public int depth()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public final java.lang.String toString()`
