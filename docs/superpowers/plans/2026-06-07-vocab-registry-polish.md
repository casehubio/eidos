# Vocab Registry Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close eidos#42 (vocab registry robustness: blank URI guard + 3 tests), eidos#43 (Builder migration across ~100 test/eval call sites), and eidos#41 (3 docs edits in personality-frameworks.md).

**Architecture:** #42 adds a one-line guard to `CdiVocabularyRegistry.register()` and a Javadoc precondition to the SPI; three new tests confirm existing and new behaviour. #43 is a pure mechanical refactor — 17-parameter positional constructors become named Builder calls, no logic changes. #41 is three targeted text edits to a docs file.

**Tech Stack:** Java 21, Quarkus 3, AssertJ, JUnit 5/Quarkus Test, Maven. Build: `JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn test -pl <module>`.

---

## File Map

**#42 — 3 files touched**
| File | Change |
|------|--------|
| `api/src/main/java/io/casehub/eidos/api/VocabularyRegistry.java` | Javadoc preconditions on `register()` |
| `runtime/src/main/java/io/casehub/eidos/runtime/vocabulary/CdiVocabularyRegistry.java` | Blank URI guard (1 line) + stale comment fix |
| `runtime/src/test/java/io/casehub/eidos/runtime/vocabulary/CdiVocabularyRegistryTest.java` | 3 new tests |

**#43 — 18 files touched (all test/eval, no production logic)**
| Module | Files | Calls |
|--------|-------|-------|
| `api/src/test/` | `AgentDescriptorTest.java`, `AgentDispositionTest.java` | 14, 15 |
| `runtime/src/test/` | `health/DefaultCapabilityHealthTest.java`, `health/DefaultCapabilityHealthDegradedTest.java`, `health/DefaultReactiveCapabilityHealthTest.java`, `health/DefaultReactiveCapabilityHealthDefaultProfileTest.java`, `registry/JpaAgentRegistryTest.java`, `registry/JpaReactiveAgentRegistryTest.java`, `renderer/EidosRenderPipelineTest.java`, `renderer/EidosSystemPromptRendererTest.java`, `renderer/DefaultReactiveSystemPromptRendererStreamingTest.java` | 2,2,2,2,4,2,3,3,2 |
| `persistence-memory/src/test/` | `InMemoryAgentRegistryTest.java`, `InMemoryReactiveAgentRegistryTest.java` | 4, 2 |
| `examples/agent-scenarios/src/test/` | `EpistemicDomainMatchingTest.java`, `MultiAgentTeamTest.java`, `SystemPromptRendererTest.java`, `TenancyIsolationTest.java` | 2,6,2,4 |
| `eval/src/main/` | `EvalDataset.java` | 15 |
| `eval/src/test/` | `AgentProfileLoaderTest.java`, `EvalReportTest.java`, `EvalReportWriterTest.java`, `EvalResultCompletenessTest.java`, `PairContrastJudgeTest.java`, `PersonalityPreservationReportTest.java`, `PromptJudgeTest.java`, `ProximityJudgeTest.java`, `TraitExpressionJudgeTest.java`, `ValidatedRecordTest.java`, `VocabularyExpressivenessJudgeTest.java` | 3,1,4,1,1,1,8,1,1,1,1 |

**Do NOT touch:** `runtime/src/main/.../AgentDescriptorMapper.java` — positional constructor is intentional compile-time field-completeness enforcement.

**#41 — 1 file touched**
| File | Change |
|------|--------|
| `docs/personality-frameworks.md` | 3 targeted text edits |

---

## Builder Transformation Pattern (reference for all #43 tasks)

**AgentDisposition — 6 params:** `(socialOrient, ruleFollowing, riskAppetite, autonomy, conflictMode, delegation)`

```java
// Before
new AgentDisposition("independent", "strict", "conservative", "directed", null, false)

// After — omit null conflictMode and false delegation (Builder defaults)
AgentDisposition.builder()
    .socialOrient("independent").ruleFollowing("strict")
    .riskAppetite("conservative").autonomy("directed")
    .build()

// When delegation = true, include it:
AgentDisposition.builder()
    .socialOrient("facilitative").ruleFollowing("principled")
    .riskAppetite("measured").autonomy("semi-autonomous")
    .delegation(true)
    .build()

// When conflictMode is non-null:
AgentDisposition.builder()
    .socialOrient("independent").ruleFollowing("flexible")
    .riskAppetite("bold").autonomy("autonomous")
    .conflictMode("competing")
    .build()
```

**AgentDescriptor — 17 params:** `(agentId, name, version, provider, modelFamily, modelVersion, weightsFingerprint, domainVocabulary, slotVocabulary, dispositionVocabulary, axisVocabularies, slot, capabilities, disposition, jurisdiction, dataHandlingPolicy, tenancyId)`

```java
// Before
new AgentDescriptor(
    "agent-1", "Analyst", "1.0", "anthropic",
    "claude", "3-opus", null,
    "urn:casehub:vocab:conscientiousness", null, null, null,
    "monitor-evaluator", List.of(), disposition,
    null, null, "tenant-1")

// After — omit all null optional fields; only set non-null values
AgentDescriptor.builder()
    .agentId("agent-1").name("Analyst").version("1.0")
    .provider("anthropic").modelFamily("claude").modelVersion("3-opus")
    .domainVocabulary("urn:casehub:vocab:conscientiousness")
    .slot("monitor-evaluator").capabilities(List.of())
    .disposition(disposition)
    .tenancyId("tenant-1")
    .build()
```

**Rule:** Null positional args → omitted from Builder (null is the Java field default). The `boolean delegation` field defaults `false` → omit when false, include when true. `axisVocabularies(null)` and omitting `axisVocabularies()` are equivalent — the compact constructor branches on null, leaving the field null in both cases.

---

## Task 1: #42 — Write 3 new tests (before guard exists)

**Files:**
- Modify: `runtime/src/test/java/io/casehub/eidos/runtime/vocabulary/CdiVocabularyRegistryTest.java`

- [ ] **Step 1: Add the 3 new tests at the bottom of `CdiVocabularyRegistryTest`**

Add after the `// --- Typed path bypasses registration ---` block:

```java
// --- Robustness guards (#42) ---

@Test
void register_alias_vs_alias_collision_throws() {
    @VocabularyMetadata(uri = "urn:test:alias-vs-alias", name = "Alias vs Alias", version = "1.0")
    enum AliasVsAlias implements VocabularyTerm {
        TERM_A("a", "A", List.of("shared")),
        TERM_B("b", "B", List.of("shared"));  // "shared" collides with TERM_A's alias
        final String value, label; final List<String> aliases;
        AliasVsAlias(String v, String l, List<String> a) { value=v; label=l; aliases=a; }
        @Override public String value()         { return value; }
        @Override public String label()         { return label; }
        @Override public List<String> aliases() { return aliases; }
    }
    assertThatThrownBy(() -> registry.register(AliasVsAlias.class))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("conflicts");
}

@Test
void register_blank_uri_throws() {
    @VocabularyMetadata(uri = "")
    enum BlankUri implements VocabularyTerm {
        TERM("term", "Term", List.of());
        final String value, label; final List<String> aliases;
        BlankUri(String v, String l, List<String> a) { value=v; label=l; aliases=a; }
        @Override public String value()         { return value; }
        @Override public String label()         { return label; }
        @Override public List<String> aliases() { return aliases; }
    }
    assertThatThrownBy(() -> registry.register(BlankUri.class))
        .isInstanceOf(IllegalArgumentException.class);
}

@Test
void allTerms_excludes_aliases_when_constants_have_aliases() {
    // SourceTerm.ALPHA aliases ["a","one"], SourceTerm.BETA alias ["b"] → 5 lookup entries, 2 constants
    registry.register(SourceTerm.class);
    var terms = registry.allTerms("urn:test:source");
    assertThat(terms).hasSize(2);
    assertThat(terms).extracting(VocabularyTerm::value)
        .doesNotContain("a", "one", "b");
}
```

- [ ] **Step 2: Run the tests — expect 2 pass, 1 fail**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn test -pl runtime -Dtest=CdiVocabularyRegistryTest
```

Expected: `register_alias_vs_alias_collision_throws` PASSES (guard already exists), `allTerms_excludes_aliases_when_constants_have_aliases` PASSES (implementation correct), `register_blank_uri_throws` FAILS (guard not yet added). If all three pass, stop — something is wrong.

---

## Task 2: #42 — Add blank URI guard, fix stale comment, add Javadoc — commit

**Files:**
- Modify: `runtime/src/main/java/io/casehub/eidos/runtime/vocabulary/CdiVocabularyRegistry.java`
- Modify: `api/src/main/java/io/casehub/eidos/api/VocabularyRegistry.java`

- [ ] **Step 1: Add blank URI guard in `CdiVocabularyRegistry.register()`**

Find the block starting with `var uri = meta.uri();`. Replace it with:

```java
var uri = meta.uri();
if (uri.isBlank()) {
    throw new IllegalArgumentException(
        "Vocabulary URI must not be blank in @VocabularyMetadata on " + vocab.getName());
}
// Validate URI and state before any map operations (fast-fail)
var existing = byUri.get(uri);
```

The old comment read `// Validate URI before any map writes (fast-fail)` — the guard now precedes a map read too, so update the comment as shown.

- [ ] **Step 2: Add Javadoc precondition to `VocabularyRegistry.register()`**

Replace the existing one-liner Javadoc:

```java
/** Registers a vocabulary enum. The class must carry {@link VocabularyMetadata}. */
<T extends Enum<T> & VocabularyTerm> void register(Class<T> vocab);
```

With:

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
<T extends Enum<T> & VocabularyTerm> void register(Class<T> vocab);
```

- [ ] **Step 3: Run all 3 new tests — expect all pass**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn test -pl runtime -Dtest=CdiVocabularyRegistryTest
```

Expected: all 3 new tests PASS. Full suite:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn test -pl runtime,api
```

Expected: BUILD SUCCESS, no failures.

- [ ] **Step 4: Commit**

```bash
git -C /Users/mdproctor/claude/casehub/eidos add \
  runtime/src/main/java/io/casehub/eidos/runtime/vocabulary/CdiVocabularyRegistry.java \
  api/src/main/java/io/casehub/eidos/api/VocabularyRegistry.java \
  runtime/src/test/java/io/casehub/eidos/runtime/vocabulary/CdiVocabularyRegistryTest.java
git -C /Users/mdproctor/claude/casehub/eidos commit -m "test(eidos#42): vocab registry robustness — blank URI guard, alias-vs-alias + allTerms isolation tests Refs #42"
```

---

## Task 3: #43 — api/src/test/ conversion

**Files:**
- Modify: `api/src/test/java/io/casehub/eidos/api/AgentDescriptorTest.java` (14 calls)
- Modify: `api/src/test/java/io/casehub/eidos/api/AgentDispositionTest.java` (15 calls)

- [ ] **Step 1: Convert `minimal()` helper in `AgentDescriptorTest.java`**

The `minimal()` static helper (lines ~10–19) currently uses `new AgentDescriptor(...)`. Replace with:

```java
static AgentDescriptor minimal(String agentId, String tenancyId) {
    return AgentDescriptor.builder()
        .agentId(agentId).name("name").version("1.0").provider("provider")
        .modelFamily("modelFamily").modelVersion("modelVersion")
        .slot("slot").capabilities(List.of())
        .disposition(AgentDisposition.builder()
            .socialOrient("collaborative").ruleFollowing("principled")
            .riskAppetite("measured").autonomy("semi-autonomous")
            .build())
        .tenancyId(tenancyId)
        .build();
}
```

- [ ] **Step 2: Repurpose `builder_produces_same_result_as_minimal_constructor()` in `AgentDescriptorTest.java`**

Rename and rewrite the test (currently around line 258–270):

```java
@Test
void builder_with_explicit_nulls_equals_builder_with_nulls_omitted() {
    var explicit = AgentDescriptor.builder()
        .agentId("agent-1").name("name").version("1.0").provider("provider")
        .modelFamily("modelFamily").modelVersion("modelVersion")
        .weightsFingerprint(null)
        .domainVocabulary(null).slotVocabulary(null).dispositionVocabulary(null)
        .axisVocabularies(null)          // null → compact constructor skips Map.copyOf
        .slot("slot").capabilities(List.of())
        .disposition(AgentDisposition.builder()
            .socialOrient("collaborative").ruleFollowing("principled")
            .riskAppetite("measured").autonomy("semi-autonomous")
            .build())
        .jurisdiction(null).dataHandlingPolicy(null).tenancyId("default")
        .build();
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
}
```

- [ ] **Step 3: Convert remaining positional calls in `AgentDescriptorTest.java`**

Find all remaining `new AgentDescriptor(` or `new AgentDisposition(` calls in the file:

```bash
grep -n "new AgentDescriptor\|new AgentDisposition" \
  /Users/mdproctor/claude/casehub/eidos/api/src/test/java/io/casehub/eidos/api/AgentDescriptorTest.java
```

Apply the Builder transformation pattern (reference: top of this plan) to each. Null args are omitted; non-null args become named setters.

- [ ] **Step 4: Convert all positional calls in `AgentDispositionTest.java`**

```bash
grep -n "new AgentDisposition" \
  /Users/mdproctor/claude/casehub/eidos/api/src/test/java/io/casehub/eidos/api/AgentDispositionTest.java
```

15 calls. Apply the AgentDisposition Builder pattern to each.

- [ ] **Step 5: Verify no positional calls remain in either file**

```bash
grep -c "new AgentDescriptor\|new AgentDisposition" \
  /Users/mdproctor/claude/casehub/eidos/api/src/test/java/io/casehub/eidos/api/AgentDescriptorTest.java \
  /Users/mdproctor/claude/casehub/eidos/api/src/test/java/io/casehub/eidos/api/AgentDispositionTest.java
```

Expected: `0` for both. (`AgentDescriptor.java` and `AgentDisposition.java` themselves will still show `new AgentDescriptor`/`new AgentDisposition` inside their own `Builder.build()` methods — those are correct and should not be converted.)

- [ ] **Step 6: Run api tests**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn test -pl api
```

Expected: BUILD SUCCESS.

- [ ] **Step 7: Commit**

```bash
git -C /Users/mdproctor/claude/casehub/eidos add \
  api/src/test/java/io/casehub/eidos/api/AgentDescriptorTest.java \
  api/src/test/java/io/casehub/eidos/api/AgentDispositionTest.java
git -C /Users/mdproctor/claude/casehub/eidos commit -m "refactor(eidos#43): Builder migration — api/src/test/ Refs #43"
```

---

## Task 4: #43 — runtime/src/test/ conversion

**Files (9 files, 22 total calls):**
- Modify: `runtime/src/test/java/io/casehub/eidos/runtime/health/DefaultCapabilityHealthTest.java` (2)
- Modify: `runtime/src/test/java/io/casehub/eidos/runtime/health/DefaultCapabilityHealthDegradedTest.java` (2)
- Modify: `runtime/src/test/java/io/casehub/eidos/runtime/health/DefaultReactiveCapabilityHealthTest.java` (2)
- Modify: `runtime/src/test/java/io/casehub/eidos/runtime/health/DefaultReactiveCapabilityHealthDefaultProfileTest.java` (2)
- Modify: `runtime/src/test/java/io/casehub/eidos/runtime/registry/JpaAgentRegistryTest.java` (4)
- Modify: `runtime/src/test/java/io/casehub/eidos/runtime/registry/JpaReactiveAgentRegistryTest.java` (2)
- Modify: `runtime/src/test/java/io/casehub/eidos/runtime/renderer/EidosRenderPipelineTest.java` (3)
- Modify: `runtime/src/test/java/io/casehub/eidos/runtime/renderer/EidosSystemPromptRendererTest.java` (3)
- Modify: `runtime/src/test/java/io/casehub/eidos/runtime/renderer/DefaultReactiveSystemPromptRendererStreamingTest.java` (2)

- [ ] **Step 1: Find all positional calls across all 9 files**

```bash
grep -rn "new AgentDescriptor\|new AgentDisposition" \
  /Users/mdproctor/claude/casehub/eidos/runtime/src/test/
```

- [ ] **Step 2: Convert all positional calls in all 9 files**

Apply the Builder transformation pattern from the top of this plan. For each file: null positional args are omitted, non-null become named setters, `delegation=false` is omitted, `conflictMode=null` is omitted.

- [ ] **Step 3: Verify no positional calls remain**

```bash
grep -rc "new AgentDescriptor\|new AgentDisposition" \
  /Users/mdproctor/claude/casehub/eidos/runtime/src/test/
```

Expected: all files show `0`.

- [ ] **Step 4: Run runtime tests**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn test -pl runtime
```

Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git -C /Users/mdproctor/claude/casehub/eidos add runtime/src/test/
git -C /Users/mdproctor/claude/casehub/eidos commit -m "refactor(eidos#43): Builder migration — runtime/src/test/ Refs #43"
```

---

## Task 5: #43 — persistence-memory/src/test/ conversion

**Files (2 files, 6 total calls):**
- Modify: `persistence-memory/src/test/java/io/casehub/eidos/memory/InMemoryAgentRegistryTest.java` (4)
- Modify: `persistence-memory/src/test/java/io/casehub/eidos/memory/InMemoryReactiveAgentRegistryTest.java` (2)

- [ ] **Step 1: Find all positional calls**

```bash
grep -rn "new AgentDescriptor\|new AgentDisposition" \
  /Users/mdproctor/claude/casehub/eidos/persistence-memory/src/test/
```

- [ ] **Step 2: Convert all 6 calls using the Builder pattern**

Apply the Builder transformation pattern from the top of this plan.

- [ ] **Step 3: Verify no positional calls remain**

```bash
grep -rc "new AgentDescriptor\|new AgentDisposition" \
  /Users/mdproctor/claude/casehub/eidos/persistence-memory/src/test/
```

Expected: both files show `0`.

- [ ] **Step 4: Run persistence-memory tests**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn test -pl persistence-memory
```

Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git -C /Users/mdproctor/claude/casehub/eidos add persistence-memory/src/test/
git -C /Users/mdproctor/claude/casehub/eidos commit -m "refactor(eidos#43): Builder migration — persistence-memory/src/test/ Refs #43"
```

---

## Task 6: #43 — examples/agent-scenarios/src/test/ conversion

**Files (4 files, 14 total calls):**
- Modify: `examples/agent-scenarios/src/test/java/io/casehub/eidos/examples/EpistemicDomainMatchingTest.java` (2)
- Modify: `examples/agent-scenarios/src/test/java/io/casehub/eidos/examples/MultiAgentTeamTest.java` (6)
- Modify: `examples/agent-scenarios/src/test/java/io/casehub/eidos/examples/SystemPromptRendererTest.java` (2)
- Modify: `examples/agent-scenarios/src/test/java/io/casehub/eidos/examples/TenancyIsolationTest.java` (4)

- [ ] **Step 1: Find all positional calls**

```bash
grep -rn "new AgentDescriptor\|new AgentDisposition" \
  /Users/mdproctor/claude/casehub/eidos/examples/agent-scenarios/src/test/
```

- [ ] **Step 2: Convert all 14 calls using the Builder pattern**

Apply the Builder transformation pattern from the top of this plan.

- [ ] **Step 3: Verify no positional calls remain**

```bash
grep -rc "new AgentDescriptor\|new AgentDisposition" \
  /Users/mdproctor/claude/casehub/eidos/examples/agent-scenarios/src/test/
```

Expected: all 4 files show `0`.

- [ ] **Step 4: Run examples tests**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn test -pl examples/agent-scenarios
```

Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git -C /Users/mdproctor/claude/casehub/eidos add examples/agent-scenarios/src/test/
git -C /Users/mdproctor/claude/casehub/eidos commit -m "refactor(eidos#43): Builder migration — examples/agent-scenarios Refs #43"
```

---

## Task 7: #43 — eval/ conversion (src/main + src/test)

**Files (12 files, 23 total calls):**

`eval/src/main/`:
- Modify: `eval/src/main/java/io/casehub/eidos/eval/EvalDataset.java` (15)

`eval/src/test/`:
- Modify: `eval/src/test/java/io/casehub/eidos/eval/AgentProfileLoaderTest.java` (3)
- Modify: `eval/src/test/java/io/casehub/eidos/eval/EvalReportTest.java` (1)
- Modify: `eval/src/test/java/io/casehub/eidos/eval/EvalReportWriterTest.java` (4)
- Modify: `eval/src/test/java/io/casehub/eidos/eval/EvalResultCompletenessTest.java` (1)
- Modify: `eval/src/test/java/io/casehub/eidos/eval/PairContrastJudgeTest.java` (1)
- Modify: `eval/src/test/java/io/casehub/eidos/eval/PersonalityPreservationReportTest.java` (1)
- Modify: `eval/src/test/java/io/casehub/eidos/eval/PromptJudgeTest.java` (8)
- Modify: `eval/src/test/java/io/casehub/eidos/eval/ProximityJudgeTest.java` (1)
- Modify: `eval/src/test/java/io/casehub/eidos/eval/TraitExpressionJudgeTest.java` (1)
- Modify: `eval/src/test/java/io/casehub/eidos/eval/ValidatedRecordTest.java` (1)
- Modify: `eval/src/test/java/io/casehub/eidos/eval/VocabularyExpressivenessJudgeTest.java` (1)

- [ ] **Step 1: Find all positional calls across all eval files**

```bash
grep -rn "new AgentDescriptor\|new AgentDisposition" \
  /Users/mdproctor/claude/casehub/eidos/eval/
```

- [ ] **Step 2: Convert all 23 calls in eval/src/main/ and eval/src/test/**

Apply the Builder transformation pattern from the top of this plan. Note: `EvalDataset.java` is in `src/main/` — it is not production SPI code, it is test dataset configuration. Apply the same pattern.

- [ ] **Step 3: Verify no positional calls remain in eval/**

```bash
grep -rc "new AgentDescriptor\|new AgentDisposition" \
  /Users/mdproctor/claude/casehub/eidos/eval/
```

Expected: all files show `0`.

- [ ] **Step 4: Run eval tests**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn test -pl eval
```

Expected: BUILD SUCCESS.

- [ ] **Step 5: Full build verification**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 26) mvn clean install
```

Expected: BUILD SUCCESS across all modules. This confirms no positional calls were missed.

- [ ] **Step 6: Commit**

```bash
git -C /Users/mdproctor/claude/casehub/eidos add eval/
git -C /Users/mdproctor/claude/casehub/eidos commit -m "refactor(eidos#43): Builder migration — eval/ Closes #43"
```

---

## Task 8: #41 — personality-frameworks.md three doc edits

**Files:**
- Modify: `docs/personality-frameworks.md`

- [ ] **Step 1: Edit 1A — SFIA × `slot` cell in cross-reference table (§5)**

Find the row: `| \`slot\` | **slot** | — | — | — | — | — | — | — | capabilities* | capabilities* |`

The last column is SFIA. Change `capabilities*` (last cell) to `—`:

```
| `slot` | **slot** | — | — | — | — | — | — | — | capabilities* | — |
```

- [ ] **Step 2: Edit 1B — footnote text below the cross-reference table**

Find the footnote line:
```
*O\*NET and SFIA provide occupation codes that may be used as `slot` values when technical
```

Change to:
```
*O\*NET provides occupation codes that may be used as `slot` values when technical
```

SFIA provides skill codes and responsibility levels, not occupation codes. Only O*NET qualifies.

- [ ] **Step 3: Edit 2 — KAI score range in §3.3**

Find the sentence in §3.3: `Developed by Michael Kirton (1976); measured by the KAI inventory.`

Change to: `Developed by Michael Kirton (1976); measured by the KAI inventory (32–160).`

- [ ] **Step 4: Edit 3 — Section 6 preamble ratings list**

Find in §6: `combinations that are meaningfully Additive, Redundant, or` (line break) `Contradictory.`

Change to: `combinations that are meaningfully Additive, Redundant, Reference, Inadvisable, or Partial.`

(The table uses five ratings: Additive, Redundant, Reference, Inadvisable, Partial. Contradictory does not appear.)

- [ ] **Step 5: Verify the 4 edits**

```bash
grep -n "SFIA provide\|SFIA provides\|KAI inventory\|Contradictory\|Inadvisable" \
  /Users/mdproctor/claude/casehub/eidos/docs/personality-frameworks.md
```

Expected output: "SFIA provide" absent, "SFIA provides" absent (changed to "provides" alone), "KAI inventory (32–160)" present, "Contradictory" absent from §6, "Inadvisable" present in §6.

- [ ] **Step 6: Commit**

```bash
git -C /Users/mdproctor/claude/casehub/eidos add docs/personality-frameworks.md
git -C /Users/mdproctor/claude/casehub/eidos commit -m "docs(eidos#41): personality-frameworks.md polish — SFIA slot cell, footnote, KAI range, §6 ratings Closes #41"
```

---

## Verification grep (run at end)

Confirm no positional constructor calls remain anywhere except the two `Builder.build()` methods and `AgentDescriptorMapper`:

```bash
grep -rn "new AgentDescriptor\|new AgentDisposition" \
  /Users/mdproctor/claude/casehub/eidos --include="*.java" | grep -v worktrees \
  | grep -Ev "(AgentDescriptor\.java|AgentDisposition\.java|AgentDescriptorMapper\.java)"
```

Expected: no output.
