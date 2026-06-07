# Design: Vocab Registry Polish — #42, #43, #41

**Branch:** `issue-42-vocab-registry-polish`
**Date:** 2026-06-07
**Issues:** eidos#42 (robustness gaps), eidos#43 (Builder migration), eidos#41 (docs polish)

---

## #42 — VocabularyRegistry Robustness Gaps

### Context

`CdiVocabularyRegistry.register()` validates aliases against primary values and other
aliases via a `LinkedHashMap`. Three gaps were identified during the eidos#40 code review:

| Gap | Implementation status |
|-----|----------------------|
| Alias-vs-alias collision | Already guarded — `lookupMap.containsKey(alias)` catches it. Test missing. |
| Blank URI | Not guarded — `uri = ""` is valid in annotations, passes annotation read, gets stored as blank key. |
| `allTerms()` isolation | Implementation correct — `byClassOrdered` stores enum constants, not lookup keys. Test missing. |

### SPI contract update

`VocabularyRegistry.register()` must document the blank URI precondition so SPI
implementors are aware — a custom registry backed by a database would silently accept
blank URIs without it. Add to the Javadoc on `VocabularyRegistry.register()`:

```java
/**
 * Registers a vocabulary enum. The class must carry {@link VocabularyMetadata}.
 *
 * @throws IllegalArgumentException if the vocabulary URI is blank (annotation
 *         attributes cannot be null at runtime — blank is the only invalid state),
 *         if the vocabulary has no constants, if value/alias conflicts exist within
 *         the vocabulary, or if a different vocabulary is already registered under
 *         the same URI.
 */
```

This is a Javadoc-only change — not an API change. The enforcement stays in
`CdiVocabularyRegistry`.

### Guard (implementation change)

In `CdiVocabularyRegistry.register()`, add immediately after `var uri = meta.uri();`,
before the `byUri.get(uri)` read:

```java
if (uri.isBlank()) {
    throw new IllegalArgumentException(
        "Vocabulary URI must not be blank in @VocabularyMetadata on " + vocab.getName());
}
```

### New tests

**`register_alias_vs_alias_collision_throws()`**
Local enum (defined inside test method) with two constants where each declares the same
alias string. Asserts `IllegalArgumentException` with message containing `"conflicts"`.
Proves the guard covers alias-vs-alias in addition to the already-tested alias-vs-primary
case.

```
TERM_A("a", ..., List.of("shared"))
TERM_B("b", ..., List.of("shared"))   ← alias "shared" collides with TERM_A's alias
```

**`register_blank_uri_throws()`**
Local enum annotated `@VocabularyMetadata(uri = "")`. Asserts `IllegalArgumentException`.
Documents that blank URI is a contract violation, not just an unusual edge case.

**`allTerms_excludes_aliases_when_constants_have_aliases()`**
Uses class-level `SourceTerm` (already in scope — registration is idempotent, no
namespace pollution). `SourceTerm.ALPHA` has aliases `["a", "one"]`; `SourceTerm.BETA`
has alias `["b"]`. The lookup map has 5 entries (2 primary + 3 aliases). The test asserts:

1. `allTerms("urn:test:source")` has exactly 2 elements — not 5.
2. `assertThat(terms).extracting(VocabularyTerm::value).doesNotContain("a", "one", "b")`
   — proving alias strings do not appear as term values in the list.

`doesNotContainAnyElementsOf(List.of("a", "one", "b"))` would be vacuously true:
`terms` is `List<? extends VocabularyTerm>` and AssertJ uses `equals()`, so no
`VocabularyTerm` constant can ever equal a `String`. The `.extracting(VocabularyTerm::value)`
step materialises the value strings, making the assertion meaningful — it fails if a
broken implementation returned alias entries.

This angle is meaningfully distinct from `allTerms_returns_distinct_constants_in_declaration_order()`
which only asserts order and size. The new test asserts structural isolation from the
lookup map.

---

## #43 — Builder Migration

### Scope

Convert `new AgentDescriptor(...)` and `new AgentDisposition(...)` positional constructor
call sites to Builder pattern in:

- `api/src/test/`
- `runtime/src/test/`
- `persistence-memory/src/test/`
- `examples/`
- `eval/`

**Do not change:**
- `runtime/src/main/java/.../AgentDescriptorMapper.java` — see rationale below
- `AgentDescriptor.Builder.build()` — internally uses positional constructor; correct
- `AgentDisposition.Builder.build()` — internally uses positional constructor; correct

### AgentDescriptorMapper — preserve positional constructor deliberately

`AgentDescriptorMapper.toRecord()` uses the positional constructor intentionally, not by
oversight. The positional constructor provides compile-time field-completeness enforcement:
if `AgentDescriptor` gains a new field, `toRecord()` will not compile until the new field
is supplied. A Builder call would silently default any new field to null — exactly the
wrong behavior for a full-fidelity JPA→record mapping where every field must be
explicitly sourced from the entity.

This is the only production call site with this property. The positional constructor must
remain non-private for the same reason.

### `builder_produces_same_result_as_minimal_constructor()` — repurpose

`AgentDescriptorTest.builder_produces_same_result_as_minimal_constructor()` currently
compares a positional constructor call (`minimal()`) against a Builder call. After
migration, `minimal()` itself becomes a Builder call — the test would then compare
builder-to-builder and prove nothing about field identity.

**Resolution:** Repurpose the test as
`builder_with_explicit_nulls_equals_builder_with_nulls_omitted()`. It should assert that
a Builder call with all optional fields explicitly set to null produces the same record as
a Builder call with those same fields simply omitted. This documents the default-null
behavior of the Builder and retains the test's value after `minimal()` is migrated.

`axisVocabularies` requires explicit attention: the compact constructor branches on it
(`if (axisVocabularies != null)`). Both the explicit-null and the omitted call must
produce `axisVocabularies == null` in the record. The sketch is:

```java
// explicit-null half
var explicit = AgentDescriptor.builder()
    .agentId("agent-1").name("name").version("1.0").provider("provider")
    .modelFamily("modelFamily").modelVersion("modelVersion")
    .weightsFingerprint(null)
    .domainVocabulary(null).slotVocabulary(null).dispositionVocabulary(null)
    .axisVocabularies(null)          // branch: null → compact constructor skips Map.copyOf
    .slot("slot").capabilities(List.of())
    .disposition(AgentDisposition.builder()
        .socialOrient("collaborative").ruleFollowing("principled")
        .riskAppetite("measured").autonomy("semi-autonomous")
        .build())
    .jurisdiction(null).dataHandlingPolicy(null).tenancyId("default")
    .build();

// omitted half — same record expected
var omitted = AgentDescriptor.builder()
    .agentId("agent-1").name("name").version("1.0").provider("provider")
    .modelFamily("modelFamily").modelVersion("modelVersion")
    .slot("slot").capabilities(List.of())
    .disposition(AgentDisposition.builder()
        .socialOrient("collaborative").ruleFollowing("principled")
        .riskAppetite("measured").autonomy("semi-autonomous")
        .build())
    .tenancyId("default")
    .build();

assertThat(explicit).isEqualTo(omitted);
```

All 6 optional String fields (`weightsFingerprint`, `domainVocabulary`, `slotVocabulary`,
`dispositionVocabulary`, `jurisdiction`, `dataHandlingPolicy`) and `axisVocabularies` are
covered by the explicit-null half. The `AgentDisposition` inside also uses Builder,
so the nested positional constructor call is gone.

### Transformation pattern

Null positional args are omitted from the Builder call (null is the default for unset
fields). Non-null args become named setters. The `boolean delegation` field defaults
`false` in Java; omit the setter when value is `false`.

```java
// Before
new AgentDisposition("independent", "strict", "conservative", "directed", null, false)

// After
AgentDisposition.builder()
    .socialOrient("independent").ruleFollowing("strict")
    .riskAppetite("conservative").autonomy("directed")
    .build()
```

```java
// Before — AgentDescriptor (17 params, many null)
new AgentDescriptor("agent-1", "Analyst", "1.0", "anthropic",
    "claude", "3-opus", null,
    "urn:casehub:vocab:conscientiousness", null, null, null,
    "monitor-evaluator", List.of(), disposition,
    null, null, "tenant-1")

// After
AgentDescriptor.builder()
    .agentId("agent-1").name("Analyst").version("1.0")
    .provider("anthropic").modelFamily("claude").modelVersion("3-opus")
    .domainVocabulary("urn:casehub:vocab:conscientiousness")
    .slot("monitor-evaluator").capabilities(List.of())
    .disposition(disposition).tenancyId("tenant-1")
    .build()
```

### No logic changes

Pure mechanical refactor. No test logic, no assertion, no test data changes.
Compile + test suite green is the only success criterion.

---

## #41 — personality-frameworks.md Polish

Three targeted edits. No structural changes to the document.

### Edit 1 — SFIA × `slot` in cross-reference table (§5) + footnote

**Change A — cell:** SFIA column of the `slot` row: `capabilities*` → `—`

**Change B — footnote text:** The footnote currently reads:
> *O\*NET and SFIA provide occupation codes that may be used as `slot` values…*

Change to:
> *O\*NET provides occupation codes that may be used as `slot` values…*

SFIA does not have occupation codes — it has skill codes and responsibility levels. After
Change A the `*` anchor exists only in the O*NET column; Change B removes the factually
incorrect SFIA claim from the footnote text.

### Edit 2 — KAI score range (§3.3)

**Current:** "measured by the KAI inventory."
**Change:** "measured by the KAI inventory (32–160)."

Assists a developer translating a numeric KAI score to a Conscientiousness term
(low end → `strict` / `conservative`; high end → `flexible` / `bold`).

### Edit 3 — Section 6 preamble ratings list

**Current:** "meaningfully Additive, Redundant, or Contradictory"
**Change:** "meaningfully Additive, Redundant, Reference, Inadvisable, or Partial"

The table uses five distinct ratings. Contradictory does not appear in the table.
Reference, Inadvisable, and Partial do.

### Out of scope (eidos#44 filed)

The cross-reference table (§5) has no `conflictMode` row, though it was added as the
5th disposition axis in eidos#38. Filed as eidos#44.

---

## Testing Summary

| Issue | Type | Count |
|-------|------|-------|
| #42 | Javadoc update on `VocabularyRegistry.register()` | 1 |
| #42 | New guard (1 line) in `CdiVocabularyRegistry.register()` | 1 |
| #42 | New tests | 3 |
| #43 | Repurpose `builder_produces_same_result_as_minimal_constructor()` | 1 |
| #43 | Test/example/eval/persistence-memory call site conversions | ~100 |
| #41 | Doc edits (2 in table, 1 in §6) | 3 |

No new files beyond this spec. No Flyway migrations.

---

## Implementation Notes

Two non-obvious details to avoid subtle bugs during implementation:

**1. Update the stale comment at `CdiVocabularyRegistry.java` line 79.**
The existing comment reads `// Validate URI before any map writes (fast-fail)`. After the
blank URI guard is inserted before `byUri.get(uri)`, the guard precedes a map *read*, not
only map writes. Update the comment to something like
`// Validate URI and state before any map operations (fast-fail)` in the same change.

**2. Use a distinct URI in `register_alias_vs_alias_collision_throws()`.**
The class-level `AliasCollision` enum already claims `"urn:test:alias-collision"`. If the
new local enum accidentally reuses that URI and `AliasCollision` has already been
registered in an earlier test within the same container lifecycle, the throw will come
from the duplicate-URI branch, not the alias-conflict branch, and
`.hasMessageContaining("conflicts")` will fail. Use `"urn:test:alias-vs-alias"` (or any
URI not used elsewhere in the test class).
