# Design Spec: Personality Framework Mapping Document
**Date:** 2026-06-05
**Issue:** eidos#29
**Deliverable:** `docs/personality-frameworks.md`
**Downstream consumer:** eidos#26 (Belbin/DISC vocabulary module implementation)

---

## Purpose

Ground the eidos vocabulary system in established personality, team-role, and occupational
frameworks. The mapping document is the prerequisite for eidos#26 — by completing the
intellectual work here (which dimensions map where, using exactly which vocabulary terms),
the BelbinVocabularyProducer implementation becomes mechanical: read the Belbin draft table,
write the Java. DiscVocabularyProducer is blocked on eidos#40 and requires a design decision
before implementation can begin.

---

## Document Structure

One file, two parts. The reference (Part I) is encyclopedic; the design guide (Part II)
is opinionated. Neither part duplicates the other — the guide uses the reference, never
restates it.

---

## Part I — Reference

### Preamble

- Purpose: prerequisite for #26 and grounding for all future eidos vocabulary design
- Scope: 11 frameworks across five categories
- How to read mapping tables: dimension values reference Conscientiousness vocabulary
  term keys directly (`bold`, `strict`, `collaborative`, `autonomous`, etc.)
- Note on MBTI: included for completeness; unsuitable for vocabulary design due to
  poor test-retest reliability (~50% of people receive a different type one month later);
  use Big Five instead
- Note on DISC: included as a disposition vocabulary source; validity is Low (see entry),
  but widely adopted in practice. Unlike MBTI, DISC's imprecision is bounded — it
  correlates with Big Five Extraversion × Agreeableness — making it usable as shorthand
  despite low scientific standing. **Implementation note:** full DISC resolution requires
  an axis-aware API extension; see eidos#40 before implementing DiscVocabularyProducer
- Note on Situational Leadership: included as conceptual framing for the autonomy axis
  but not as a vocabulary source — see entry for why

### Framework Categories

Entries appear in this order — category signals conceptual grouping:

1. **Team Role** — Belbin Team Roles (9 roles), Margerison-McCann Team Management Wheel
2. **Personality / Behavioral** — Big Five (OCEAN), DISC, MBTI
3. **Cognitive / Work Style** — Thomas-Kilmann Conflict Modes, Situational Leadership
   (Hersey & Blanchard), Kirton Adaption-Innovation (KAI)
4. **Occupational** — O*NET, SFIA
5. **Agent Theory** — BDI (appendix only — not in mapping tables; see appendix)

### Per-Framework Entry Structure

Each entry contains exactly:

1. **What it models** — 2–3 sentences; covers the framework's scope and core claim
2. **Scientific validity** — High / Medium / Low + one-line justification
3. **Workplace adoption** — Widespread / Moderate / Niche
4. **Vocabulary role** — Slot vocabulary / Disposition vocabulary / Reference only
5. **Mapping table** — framework dimensions → AgentDescriptor fields

#### Mapping table rules

Column headers: `slot` / `capabilities` / `socialOrient` / `ruleFollowing` /
`riskAppetite` / `autonomy` / `delegation`

Values in disposition columns (`socialOrient`, `ruleFollowing`, `riskAppetite`,
`autonomy`) are exact Conscientiousness vocabulary term keys:
`collaborative`, `independent`, `facilitative`, `strict`, `principled`, `flexible`,
`conservative`, `measured`, `bold`, `directed`, `semi-autonomous`, `autonomous`.

- `slot`: the vocabulary key used as a slot value (Belbin and occupational frameworks
  only); `—` for personality/behavioral frameworks — these are not role assignments
- `capabilities`: domain keywords or `—`; not a Conscientiousness term
- `delegation`: `true`, `false`, or `—` if the framework makes no claim
- A dimension that maps strongly to a field gets a concrete term; weak mappings
  get `(partial)` with a note
- Belbin and DISC have no canonical external URIs published by their respective
  owners — `exactMatches` is `Map.of()` for all terms in both vocabularies

### Architecture Note: DISC as Disposition Vocabulary

DISC types (Dominance, Influence, Steadiness, Conscientiousness) describe behavioral
patterns that an agent brings to every context — not roles assigned by a team. This
makes DISC a **disposition vocabulary**, not a slot vocabulary. The distinction matters:

- Belbin defines roles a team assigns: `slot = "shaper"`, `slotVocabulary = "urn:casehub:vocab:belbin"`
- DISC defines personality styles the agent always exhibits: `dispositionVocabulary = "urn:casehub:vocab:disc"`

An agent holds both simultaneously — Belbin slot for the team role, DISC type for the
behavioral style. This is the **Belbin + DISC Profile** (see combination patterns).

**API gap — eidos#40:** The intended resolution mechanism is
`VocabularyRegistry.equivalentValues(fromVocab, value, toVocab)`. However, a single
DISC type maps to four *different* Conscientiousness terms depending on which axis is
being resolved (`dominance → independent` on socialOrient, `bold` on riskAppetite,
`flexible` on ruleFollowing, `autonomous` on autonomy). The current signature has no
axis parameter and cannot disambiguate. The Belbin + DISC Profile cannot be fully
implemented until eidos#40 resolves this — either by extending the API signature or
by choosing an alternative resolution strategy (per-axis sub-terms, exactMatches
encoding, or axis-level term design). **#26 must resolve eidos#40 before writing
DiscVocabularyProducer.**

### Vocabulary URI Field Interaction

AgentDescriptor has three vocabulary URI fields:

| Field | Scope | Example |
|-------|-------|---------|
| `domainVocabulary` | Default for ALL fields (slot, capabilities, all disposition axes) | `"urn:casehub:vocab:conscientiousness"` |
| `slotVocabulary` | Override for `slot` field only | `"urn:casehub:vocab:belbin"` |
| `dispositionVocabulary` | Override for all disposition axis fields | `"urn:casehub:vocab:disc"` |

Resolution precedence (most specific wins):
1. Field-specific vocabulary (`slotVocabulary` for `slot`, `dispositionVocabulary` for disposition)
2. `domainVocabulary` (default for any field without a specific override)
3. No vocabulary (raw string — no resolution available)

**Worked examples by combination pattern:**

*Belbin Profile* (Belbin slot + Conscientiousness disposition):
```
slotVocabulary        = "urn:casehub:vocab:belbin"
dispositionVocabulary = "urn:casehub:vocab:conscientiousness"
slot                  = "co-ordinator"
disposition.socialOrient  = "facilitative"
disposition.ruleFollowing = "principled"
disposition.riskAppetite  = "measured"
disposition.autonomy      = "semi-autonomous"
disposition.delegation    = true
```

*Belbin + DISC Profile* (Belbin slot + DISC disposition — pending eidos#40):
```
slotVocabulary        = "urn:casehub:vocab:belbin"
dispositionVocabulary = "urn:casehub:vocab:disc"
slot                  = "co-ordinator"
disposition.socialOrient  = "dominance"   ← DISC type key; axis-aware resolution needed
disposition.ruleFollowing = "dominance"   ← same type, different resolved Conscientiousness term
disposition.riskAppetite  = "dominance"
disposition.autonomy      = "dominance"
disposition.delegation    = true          ← from Belbin role (Co-ordinator), not from DISC
```
Co-ordinator (Belbin: facilitative, measured) + Dominance (DISC: assertive, bold) diverge
on socialOrient and riskAppetite — this combination is additive, not redundant. Populate
`delegation` from the Belbin role's implied value in the draft table; DISC types make no
delegation claim. The registry must resolve `"dominance"` differently per axis — this
requires the API extension tracked in eidos#40.

*Occupational Profile* (O*NET capabilities + Conscientiousness disposition):
```
slotVocabulary        = null
dispositionVocabulary = "urn:casehub:vocab:conscientiousness"
capabilities          = [<o*net or sfia skill names>]
disposition.*         = <conscientiousness terms>
```

Note: Big Five does not appear in these examples because it is **conceptual
grounding** for the disposition axis design, not a vocabulary source. The
Conscientiousness vocabulary is Big Five-grounded; an agent using Conscientiousness
terms is implicitly using Big Five semantics.

### Cross-Reference Summary Table

After all per-framework entries: one table — rows = AgentDescriptor fields, columns
= all 10 frameworks (BDI excluded — appendix only), cells = `slot` / `disposition` /
`capabilities` / `reference only` / `—`.

This is a navigation aid — no new content.

### Framework Compatibility (Curated Pairs)

~10 pairs. Each pair: **Rating** (Additive / Redundant / Contradictory) +
one-sentence reasoning.

Required pairs:

| Pair | Rating | Reasoning |
|------|--------|-----------|
| Belbin + Big Five | Additive | Role (Belbin slot) and stable trait (Conscientiousness vocabulary, which is Big Five-grounded) are orthogonal — this is the natural Belbin Profile |
| Belbin + DISC | Additive | Role assignment (slot) and behavioral style (disposition) are orthogonal; an agent holds both simultaneously — pending eidos#40 for full resolution |
| Belbin + Margerison-McCann | Redundant | Same conceptual space; contradictory terminology; pick Belbin |
| DISC + Big Five (Conscientiousness) | Redundant | DISC is a quadrant simplification of Big Five Extraversion × Agreeableness; no new signal |
| Big Five + Thomas-Kilmann | Additive | Stable trait + conflict strategy — different constructs |
| Big Five + Situational Leadership | Reference | SL describes leader response to follower readiness, not agent trait; use as autonomy axis framing only |
| O*NET + Big Five | Additive | Occupational competence + behavioral trait — orthogonal |
| SFIA + O*NET | Redundant | Both are occupational competence frameworks; SFIA is IT-specific subset |
| MBTI + anything | Contradictory | Low test-retest reliability makes any vocabulary built on MBTI types unstable |
| KAI + DISC | Partial | Adaptor/Innovator partially captured by DISC C/D; KAI adds precision on the innovation axis but not enough to justify two vocabularies |

---

## Part II — Design Guide

### Encoding Guidance

Separate from framework selection, these rules govern how to represent information
in the descriptor:

1. **Slot vocabulary vs. disposition vocabulary**: Belbin roles go in `slot` via
   `slotVocabulary`. Explicit population of disposition axes alongside a Belbin slot
   is correct and expected — the combination patterns show this. The axes are independent
   fields; populating them explicitly makes descriptors queryable without vocabulary
   resolution.

2. **DISC type as disposition vocabulary**: Do not place DISC type names in `slot`.
   DISC describes how an agent behaves (disposition), not what role the team assigned it.
   Use `dispositionVocabulary="urn:casehub:vocab:disc"`.

3. **Thomas-Kilmann conflict modes**: Map TK modes to `socialOrient` only for
   Collaborating (→ `collaborative`). Do not map Competing to `riskAppetite` — Competing
   describes assertiveness in conflict, not risk tolerance. TK Avoiding and Accommodating
   have no clean mapping to current axes; see eidos#38 for a potential `conflictMode` axis.

4. **Autonomy axis authority**: The `directed / semi-autonomous / autonomous` progression
   is conceptually inspired by Situational Leadership's S1→S4 maturity arc, but SL
   describes leader behavior, not agent traits. The axis stands on its own terms.

### Axis-by-Axis Recommendations

For each AgentDescriptor field, which framework to reach for and why:

- **`slot`**: Belbin for team composition roles; O*NET occupation codes for technical roles.
  Never use personality framework type names (DISC, MBTI) as slot values — these are
  behavioral patterns, not role assignments.

- **`socialOrient`**: Conscientiousness vocabulary (`collaborative`, `independent`,
  `facilitative`). TK's Collaborating mode aligns with `collaborative`; the other four
  TK modes have no current equivalent. See eidos#38.

- **`ruleFollowing`**: Conscientiousness vocabulary already covers this fully
  (`strict`, `principled`, `flexible`). KAI (Adaptor → `strict`, Innovator → `flexible`)
  and DISC-C (→ `strict`) provide supporting evidence.

- **`riskAppetite`**: Conscientiousness vocabulary (`conservative`, `measured`, `bold`).
  KAI and Big Five Openness provide supporting evidence. Do not use TK modes here.

- **`autonomy`**: Conscientiousness vocabulary (`directed`, `semi-autonomous`, `autonomous`).
  The S1→S4 arc from Situational Leadership is a useful framing metaphor; do not cite it
  as authority.

- **`delegation`**: Belbin Co-ordinator signals `true`. DISC types make no claim about
  delegation (sub-agent spawning is platform-semantic, not personality-semantic). Default
  to `false` unless a role explicitly involves empowering others.

- **`capabilities`**: O*NET knowledge/skill categories and SFIA competence levels are
  the primary sources for capability name vocabulary. Big Five Openness predicts breadth
  of capability domains but does not define capability names.

### Three Named Combination Patterns

Each pattern shows exactly which AgentDescriptor fields are set:

---

**Belbin Profile** — Belbin slot + Conscientiousness disposition

```
slotVocabulary        = "urn:casehub:vocab:belbin"
dispositionVocabulary = "urn:casehub:vocab:conscientiousness"
slot                  = "<belbin-role-key>"
disposition.socialOrient  = "<conscientiousness term>"
disposition.ruleFollowing = "<conscientiousness term>"
disposition.riskAppetite  = "<conscientiousness term>"
disposition.autonomy      = "<conscientiousness term>"
disposition.delegation    = <bool>
```

What it expresses: team contribution role + full behavioral profile.
What it leaves unspecified: occupational domain; conflict mode (eidos#38).
When to use: agents defined primarily by team function; all four disposition axes
should be populated explicitly — the Belbin draft table shows typical implied values
per role; actual behavioral assessment may warrant deviation.

---

**Belbin + DISC Profile** — Belbin slot + DISC disposition *(pending eidos#40)*

```
slotVocabulary        = "urn:casehub:vocab:belbin"
dispositionVocabulary = "urn:casehub:vocab:disc"
slot                  = "<belbin-role-key>"
disposition.socialOrient  = "<disc-type-key>"
disposition.ruleFollowing = "<disc-type-key>"
disposition.riskAppetite  = "<disc-type-key>"
disposition.autonomy      = "<disc-type-key>"
disposition.delegation    = <bool from Belbin role — not from DISC type>
```

What it expresses: team role (Belbin) + independently-measured personality style (DISC).
The DISC type captures behavioral patterns that may differ from what the Belbin role
implies — a Co-ordinator (Belbin: facilitative, measured) who is also a D-type (DISC:
assertive, bold) diverges on socialOrient and riskAppetite; the combination is additive.
Always populate `delegation` from the Belbin draft table — DISC types make no delegation
claim.

**Implementation blocked on eidos#40**: axis-aware resolution of DISC types to
Conscientiousness terms requires an API extension to `VocabularyRegistry.equivalentValues()`.
Do not implement `DiscVocabularyProducer` until eidos#40 is resolved.

---

**Occupational Profile** — O*NET or SFIA capabilities + Conscientiousness disposition

```
slotVocabulary        = null   ← or domain-specific slot vocabulary
dispositionVocabulary = "urn:casehub:vocab:conscientiousness"
capabilities          = [<o*net or sfia skill names>]
disposition.*         = <conscientiousness terms>
```

What it expresses: technical competence domain + behavioral traits.
What it leaves unspecified: team dynamic; conflict mode (eidos#38).
When to use: agents defined primarily by technical skill (code review, data analysis).

### Anti-Patterns

**Framework selection errors:**

1. **MBTI as vocabulary basis** — poor test-retest reliability; use Big Five instead.
   MBTI types can be approximately mapped to Big Five for legacy compatibility but should
   not be a primary vocabulary source.

2. **DISC + Big Five (Conscientiousness) simultaneously** — DISC is a quadrant
   simplification of Big Five dimensions; using both adds no signal and creates redundant
   encodings. Choose one.

3. **Belbin + Margerison-McCann together** — same conceptual space, contradictory
   terminology. Pick Belbin.

**Encoding errors:**

4. **DISC type names in `slot`** — Dominance, Influence, Steadiness, and
   Conscientiousness describe how an agent behaves everywhere, not what role a team
   assigned it. Use `dispositionVocabulary=urn:casehub:vocab:disc`, not `slotVocabulary`.

5. **Using Belbin role keys as disposition field values** — e.g., `socialOrient="shaper"`
   treats a Belbin slot key (a team role name) as a disposition term. This is a vocabulary
   category error. `socialOrient` must contain a disposition vocabulary term
   (`collaborative`, `independent`, etc.), not a slot key. Explicitly populating
   disposition axes alongside a Belbin slot is correct; the mistake is using the
   slot key itself as a disposition value.

**Axis assignment errors:**

6. **Thomas-Kilmann Competing mode → `riskAppetite`** — Competing is assertiveness
   in conflict, not risk tolerance. Map only TK Collaborating to `socialOrient=collaborative`;
   leave other TK modes unmapped until eidos#38 resolves the `conflictMode` axis question.

7. **DISC Dominance → `delegation=true`** — `delegation` means "can spawn sub-agents"
   (platform-semantic). DISC D-types assign tasks but may maintain tight oversight — this
   does not predict sub-agent spawning. DISC types make no `delegation` claim.

### Vocabulary Draft Tables (normative for eidos#26)

These tables are the primary artefact for #26 implementation. The implementor writes
one `Map.entry(key, new VocabularyTerm(...))` per row.

`exactMatches` is `Map.of()` for all Belbin and DISC terms — neither Belbin Associates
nor DISC framework publishers have released canonical semantic web URIs for their terms.

#### Belbin Team Roles

**Framework source:** Belbin, *Team Roles at Work*, 1993 edition (9-role model)
**Vocabulary version:** `"1.0"` (eidos vocabulary; not a Belbin publication version)
**URI:** `"urn:casehub:vocab:belbin"`

**Note on disposition columns:** The `socialOrient`, `ruleFollowing`, `riskAppetite`,
`autonomy`, and `delegation` columns are annotation — they show the Conscientiousness
values implied by each role for reference and cross-mapping. They are NOT fields in
`VocabularyTerm`. The `BelbinVocabularyProducer` writes only: `value`, `label`,
`description`, `aliases`, `exactMatches = Map.of()`.

| Role | Key | Label | Description | Aliases | socialOrient | ruleFollowing | riskAppetite | autonomy | delegation |
|------|-----|-------|-------------|---------|--------------|---------------|--------------|----------|------------|
| Plant | `plant` | Plant | Creative, unorthodox problem-solver; generates novel ideas independently | `["pl"]` | `independent` | `flexible` | `bold` | `autonomous` | `false` |
| Resource Investigator | `resource-investigator` | Resource Investigator | Extrovert who explores external opportunities and develops contacts | `["ri"]` | `collaborative` | `flexible` | `measured` | `semi-autonomous` | `false` |
| Co-ordinator | `co-ordinator` | Co-ordinator | Clarifies goals, promotes team decision-making, delegates effectively | `["co"]` | `facilitative` | `principled` | `measured` | `semi-autonomous` | `true` |
| Shaper | `shaper` | Shaper | Challenges the team to improve; driven, dynamic, thrives under pressure | `["sh"]` | `independent` | `flexible` | `bold` | `autonomous` | `false` |
| Monitor Evaluator | `monitor-evaluator` | Monitor Evaluator | Sober, strategic, discerning; sees all options and judges accurately | `["me"]` | `independent` | `strict` | `conservative` | `semi-autonomous` | `false` |
| Teamworker | `teamworker` | Teamworker | Cooperative, perceptive, diplomatic; averts friction and builds cohesion | `["tw"]` | `collaborative` | `principled` | `conservative` | `directed` | `false` |
| Implementer | `implementer` | Implementer | Disciplined, reliable, efficient; turns ideas into practical actions | `["imp"]` | `collaborative` | `strict` | `conservative` | `directed` | `false` |
| Completer Finisher | `completer-finisher` | Completer Finisher | Painstaking, conscientious, anxious; ensures delivery to standard | `["cf"]` | `independent` | `strict` | `conservative` | `directed` | `false` |
| Specialist | `specialist` | Specialist | Dedicated, self-starting, single-minded; provides rare knowledge | `["sp"]` | `independent` | `principled` | `measured` | `autonomous` | `false` |

#### DISC Types

**Framework source:** Conceptual DiSC quadrant model, Marston (1928); framework-neutral —
not tied to any specific vendor assessment
**Vocabulary version:** `"1.0"` (eidos vocabulary; not a vendor or framework version)
**URI:** `"urn:casehub:vocab:disc"`

**Architecture:** DISC types are disposition vocabulary terms, not slot terms. The
`→ socialOrient`, `→ ruleFollowing`, etc. columns show the implied Conscientiousness
mapping per axis — this is the mapping `#26`'s `DiscVocabularyProducer` must implement.
These are NOT fields in `VocabularyTerm`; they describe the axis-aware resolution that
`equivalentValues()` must return.

**⚠ Blocked on eidos#40:** The current `equivalentValues(fromVocab, value, toVocab)`
signature has no axis parameter. A DISC type resolves to a *different* Conscientiousness
term on each axis (e.g., `dominance → independent` on socialOrient but `bold` on
riskAppetite). The current API cannot disambiguate. Resolve eidos#40 before writing
`DiscVocabularyProducer`.

| Type | Key | Label | Description | Aliases | → socialOrient | → ruleFollowing | → riskAppetite | → autonomy |
|------|-----|-------|-------------|---------|----------------|-----------------|----------------|------------|
| Dominance | `dominance` | Dominance | Results-driven, direct, decisive; prioritises outcomes over relationships | `["D"]` | `independent` | `flexible` | `bold` | `autonomous` |
| Influence | `influence` | Influence | Enthusiastic, optimistic, collaborative; motivates and involves others | `["i"]` | `collaborative` | `flexible` | `measured` | `semi-autonomous` |
| Steadiness | `steadiness` | Steadiness | Patient, reliable, supportive; values stability and consistency | `["S"]` | `facilitative` | `principled` | `conservative` | `directed` |
| Conscientiousness | `conscientiousness-disc` | Conscientiousness | Analytical, systematic, quality-focused; emphasises accuracy | `["C"]` | `independent` | `strict` | `conservative` | `semi-autonomous` |

Note: `delegation` absent — DISC types make no claim about sub-agent spawning.
`conscientiousness-disc` key avoids collision with `urn:casehub:vocab:conscientiousness`
(a disposition term vocabulary); this DISC entry names a personality type.

### Vocabulary Gap Notes

| Gap | Source | What is missing | Note |
|-----|--------|-----------------|------|
| Co-ordinator autonomy | Belbin Co-ordinator | Orchestrates team decision-making; higher coordination intent than `semi-autonomous` but not independently agenda-driven like `autonomous` | Mapped to `semi-autonomous + delegation=true` — the combination may be sufficient without a new term; decision for #26 |
| Conflict modes | Thomas-Kilmann (all 5 modes) | No `conflictMode` axis exists; TK Avoiding, Accommodating, Competing, Compromising have no home | Tracked in eidos#38 — do not add to `socialOrient` |

The three gap terms proposed in an earlier draft (accommodating, deferring, compromising)
are withdrawn — adding them to `socialOrient` would mix conflict strategy with social
preference, making the axis semantically incoherent.

---

## Implementation Notes for #26

- Vocabulary URIs: `urn:casehub:vocab:<name>`
- Each producer: `@ApplicationScoped` with `@Produces` returning a `Vocabulary`
- `Vocabulary(uri, name, version, Map<String, VocabularyTerm> terms)`
- `VocabularyTerm(value, label, description, List<String> aliases, Map<String, String> exactMatches)`
  — `exactMatches`: external URI → equivalent term value in that external vocabulary
  — for Belbin and DISC: `Map.of()` (no published canonical URIs exist)
- `CdiVocabularyRegistry` discovers all `Instance<Vocabulary>` CDI beans automatically
- Keep Belbin and DISC as separate producers (`BelbinVocabularyProducer`,
  `DiscVocabularyProducer`) in the same `casehub-eidos-vocab` module
- `ConscientiousnessVocabularyProducer` is the model for `BelbinVocabularyProducer`
- **`DiscVocabularyProducer` is blocked on eidos#40** — the axis-aware resolution
  mechanism must be decided before the producer can be designed

---

## Appendix: BDI Agent Architecture Model

BDI (Belief-Desire-Intention) is a formal computational architecture for rational agents
(Rao & Georgeff, 1991). It is included here for architectural context, not as a vocabulary
source — it contributes no `slot`, `capabilities`, or disposition vocabulary terms and is
excluded from the cross-reference summary table.

The following mapping is **illustrative only, not a precise correspondence**:
- **Beliefs** — what the agent knows about the world ≈ `AgentCapability` (declared knowledge)
- **Desires** — what the agent wants to achieve ≈ `AgentPromptContext.GoalContext`
- **Intentions** — committed action plans in BDI; `AgentDisposition` captures stable
  character rather than dynamic commitments — the analogy breaks down here

The value of this mapping is recognising that AgentDescriptor's overall structure has
BDI lineage, not that BDI dimensions become vocabulary terms.

---

## Out of Scope for This Document

- Margerison-McCann detailed mapping — covered only at compare/contrast level
- BDI as a vocabulary module — reference architecture only
- O*NET and SFIA as vocabulary modules — large external schemas; warrant separate issues
- Thomas-Kilmann vocabulary module — blocked on eidos#38 (`conflictMode` axis decision)
- Big Five as a vocabulary module — the Conscientiousness vocabulary already provides
  Big Five-grounded disposition terms; a separate Big Five module would be redundant
  unless future axes require additional OCEAN dimensions
- `conflictMode` as a 5th disposition axis — tracked in eidos#38
- `AgentDisposition` as `Map<String, String>` — tracked in eidos#39
- `VocabularyRegistry.equivalentValues()` axis-aware extension — tracked in eidos#40
- Belbin phase-composition routing in casehub-engine — eidos#28; depends on this doc and #26
