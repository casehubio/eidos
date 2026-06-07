# eidos Operations Guide

## Axis Evolution

`DispositionAxis` enum constants are serialised as their Java name (e.g. `CONFLICT_MODE`)
in the `axis_vocabularies` JSON column of the `agent_descriptor` table. Jackson uses
`DispositionAxis.valueOf(keyString)` to deserialise map keys.

**Safe operations:**
- Adding a new constant to `DispositionAxis` — old rows have no entry for the new axis;
  they deserialise cleanly with that axis absent from the map.

**Breaking operations (require DB migration):**
- Renaming a `DispositionAxis` constant — any `agent_descriptor` row whose
  `axis_vocabularies` JSON contains the old name will throw `InvalidFormatException` on
  load. Before renaming a constant, run a migration that updates the JSON:
  ```sql
  UPDATE agent_descriptor
  SET axis_vocabularies = REPLACE(axis_vocabularies, '"OLD_NAME"', '"NEW_NAME"')
  WHERE axis_vocabularies LIKE '%"OLD_NAME"%';
  ```
  Apply this migration **before** deploying the code that removes the old constant.

- Removing a `DispositionAxis` constant — same issue. Remove from JSON first, then
  remove from code.
