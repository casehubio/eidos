# io.casehub.eidos.api.CrossUnitEvidenceAssessment

**Package:** `io.casehub.eidos.api`

**Kind:** `record`

## Fields

### `UNKNOWN` (`io.casehub.eidos.api.CrossUnitEvidenceAssessment`)

### `crossUnitCount` (`int`)

### `level` (`io.casehub.eidos.api.EvidenceDiversityLevel`)

### `sameUnitCount` (`int`)

### `unknownCount` (`int`)

### `warnings` (`java.util.List<java.lang.String>`)

## Record Components

### `crossUnitCount` (`int`)

### `level` (`io.casehub.eidos.api.EvidenceDiversityLevel`)

### `sameUnitCount` (`int`)

### `unknownCount` (`int`)

### `warnings` (`java.util.List<java.lang.String>`)

## Constructors

### `public CrossUnitEvidenceAssessment(int sameUnitCount, int crossUnitCount, int unknownCount, io.casehub.eidos.api.EvidenceDiversityLevel level, java.util.List<java.lang.String> warnings)`

#### Parameters

- `sameUnitCount` (`int`)
- `crossUnitCount` (`int`)
- `unknownCount` (`int`)
- `level` (`io.casehub.eidos.api.EvidenceDiversityLevel`)
- `warnings` (`java.util.List<java.lang.String>`)

## Methods

### `public int crossUnitCount()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public io.casehub.eidos.api.EvidenceDiversityLevel level()`

### `public int sameUnitCount()`

### `public final java.lang.String toString()`

### `public int unknownCount()`

### `public java.util.List<java.lang.String> warnings()`
