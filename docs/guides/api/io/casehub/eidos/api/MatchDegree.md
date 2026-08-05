# io.casehub.eidos.api.MatchDegree

**Package:** `io.casehub.eidos.api`

**Kind:** `interface`

OWLS-MX semantic match degree between a declared capability and a requested capability.
Based on the OWL-S Matchmaker (OWLS-MX) matching framework.

<p>Match degrees in descending priority:
<ul>
  <li>`Exact` — declared and requested are identical
  <li>`Plugin` — declared subsumes requested (declared is more general)
  <li>`Specialization` — requested subsumes declared (declared is more specific)
  <li>`None` — no semantic relationship
</ul>

<p>Ordering reflects OWLS-MX priority: Exact &lt; Plugin &lt; Specialization &lt; None.
Plugin ranks above Specialization because a Plugin match guarantees the agent covers
the request (declared is more general); a Specialization covers only a subset.
Within Plugin and Specialization, lower depth (closer in hierarchy) ranks higher.

## Methods

### `public default int compareTo(io.casehub.eidos.api.MatchDegree other)`

#### Parameters

- `other` (`io.casehub.eidos.api.MatchDegree`)

### `private int ordinal()`
