# Personality and Role Frameworks — Mapping to AgentDescriptor

> **Status:** Reference document for eidos vocabulary design.
> **Issue:** eidos#29 · **Downstream:** eidos#26 (Belbin/DISC vocabulary module)

This document maps established personality, team-role, and occupational frameworks to
the fields of `AgentDescriptor`. It serves two purposes:

- **Part I — Reference:** What each framework models, its scientific validity, and how
  its dimensions correspond to AgentDescriptor fields. Encyclopedic; does not make
  recommendations.
- **Part II — Design Guide:** Opinionated guidance for vocabulary designers on which
  frameworks to use, how to combine them, what to avoid, and normative vocabulary draft
  tables for eidos#26.

## How to read mapping tables

Disposition column values for `socialOrient`, `ruleFollowing`, `riskAppetite`, and `autonomy`
are exact keys from `urn:casehub:vocab:conscientiousness`. Values for `conflictMode` are
exact keys from `urn:casehub:vocab:thomas-kilmann`:

| Term | Axis | Meaning |
|------|------|---------|
| `collaborative` | socialOrient | Works with others by default |
| `independent` | socialOrient | Works alone by preference |
| `facilitative` | socialOrient | Enables others to work |
| `strict` | ruleFollowing | Follows rules rigidly |
| `principled` | ruleFollowing | Follows intent of rules |
| `flexible` | ruleFollowing | Adapts rules to context |
| `conservative` | riskAppetite | Avoids uncertainty |
| `measured` | riskAppetite | Balances risk and reward |
| `bold` | riskAppetite | Accepts high uncertainty for reward |
| `directed` | autonomy | Follows explicit instructions |
| `semi-autonomous` | autonomy | Acts within defined boundaries |
| `autonomous` | autonomy | Acts on own judgment |
| `competing` | conflictMode | Assertive, uncooperative — pursues own concerns at others' expense |
| `collaborating` | conflictMode | Assertive and cooperative — seeks a solution that fully satisfies both parties |
| `compromising` | conflictMode | Intermediate assertiveness and cooperativeness — partial satisfaction for both |
| `avoiding` | conflictMode | Unassertive and uncooperative — sidesteps or postpones the conflict |
| `accommodating` | conflictMode | Unassertive, cooperative — concedes own concerns to satisfy the other party |

A dimension that maps strongly to a field gets a concrete term. Weak mappings are marked
`(partial)` with a note. `—` means the framework makes no claim for that field.

## Preamble Notes

- **MBTI:** Included for completeness. Unsuitable for vocabulary design — ~50% of people
  receive a different type one month later (poor test-retest reliability). Use Big Five instead.
- **DISC:** Included as a disposition vocabulary source despite Low scientific validity —
  its imprecision is bounded (correlates with Big Five Extraversion × Agreeableness), making
  it usable as shorthand in practice. Implemented as `DiscTerm` + `DiscVocabRegistrar` in
  `casehub-eidos-vocab`; axis-aware resolution via `axisExactMatch` was enabled by eidos#40 (CLOSED).
- **Situational Leadership:** Describes how a *leader* adapts to a follower's development
  stage, not agent traits. Included as conceptual framing for the autonomy axis only; not
  a vocabulary source.

---

# Part I — Reference

## 1. Team Role Frameworks

### 1.1 Belbin Team Roles

**What it models:** Nine roles describing what each person contributes to a team's
function. Roles are complementary and a balanced team needs all nine. Based on
research by Meredith Belbin at Henley Management College, observing real management
teams over nine years.

**Scientific validity:** Medium — widely cited and used in practice; some empirical
support from the original research; critics note reliance on self-report and limited
independent replication.

**Workplace adoption:** Widespread — standard in UK and EU management development.

**Vocabulary role:** Slot vocabulary (`urn:casehub:vocab:belbin`). Belbin roles answer
"what do you contribute to a team?" → `slot` field.

| Dimension | slot | socialOrient | ruleFollowing | riskAppetite | autonomy | delegation |
|-----------|------|--------------|---------------|--------------|----------|------------|
| Plant | `plant` | `independent` | `flexible` | `bold` | `autonomous` | `false` |
| Resource Investigator | `resource-investigator` | `collaborative` | `flexible` | `measured` | `semi-autonomous` | `false` |
| Co-ordinator | `co-ordinator` | `facilitative` | `principled` | `measured` | `semi-autonomous` | `true` |
| Shaper | `shaper` | `independent` | `flexible` | `bold` | `autonomous` | `false` |
| Monitor Evaluator | `monitor-evaluator` | `independent` | `strict` | `conservative` | `semi-autonomous` | `false` |
| Teamworker | `teamworker` | `collaborative` | `principled` | `conservative` | `directed` | `false` |
| Implementer | `implementer` | `collaborative` | `strict` | `conservative` | `directed` | `false` |
| Completer Finisher | `completer-finisher` | `independent` | `strict` | `conservative` | `semi-autonomous` | `false` |
| Specialist | `specialist` | `independent` | `principled` | `measured` | `autonomous` | `false` |

Note: disposition columns show values *implied* by each role. They are reference
annotations — not fields in `BelbinVocabularyProducer`'s `VocabularyTerm` entries.

---

### 1.2 Margerison-McCann Team Management Wheel

**What it models:** Eight role preferences describing how people prefer to work in
teams. Roles are arranged on a wheel showing related preferences. Developed by Charles
Margerison and Dick McCann; similar motivations to Belbin but different theoretical basis.

**Scientific validity:** Low-Medium — less independently validated than Belbin; primarily
practitioner-adopted.

**Workplace adoption:** Moderate — more common in Australia/New Zealand; less globally
adopted than Belbin.

**Vocabulary role:** Reference only. Covers the same conceptual space as Belbin (team
contribution roles) with incompatible terminology. Not recommended for eidos vocabulary.

**Compare/contrast with Belbin:**

| MM Role | Closest Belbin | Where they diverge |
|---------|---------------|-------------------|
| Reporter-Adviser | Monitor Evaluator | MM emphasises information gathering; Belbin emphasises judgement |
| Creator-Innovator | Plant | High overlap; both generate novel ideas independently |
| Explorer-Promoter | Resource Investigator | High overlap; both explore external opportunities |
| Assessor-Developer | Monitor Evaluator / Implementer | MM combines analysis with development; Belbin separates them |
| Thruster-Organiser | Shaper / Implementer | MM combines drive with organisation; Belbin separates them |
| Concluder-Producer | Completer Finisher / Implementer | High overlap; both focus on delivery |
| Controller-Inspector | Monitor Evaluator / Completer Finisher | MM emphasises control; Belbin emphasises quality and judgement |
| Upholder-Maintainer | Teamworker | High overlap; both provide support and stability |

**Recommendation:** Use Belbin. The conceptual territory is the same; Belbin has broader
global adoption, more independent research, and was established earlier. Implementing
both creates contradictory slot vocabulary with no additive signal.

---

## 2. Personality / Behavioral Frameworks

### 2.1 Big Five (OCEAN)

**What it models:** Five broad dimensions of human personality derived from factor
analysis of personality trait language across cultures. Dimensions: Openness to
Experience, Conscientiousness, Extraversion, Agreeableness, Neuroticism (inverted:
Emotional Stability). The dominant scientific model of personality since the 1990s.

**Scientific validity:** High — the most replicated personality model in psychology;
cross-cultural replication across 50+ countries; strong predictive validity for job
performance and life outcomes. Measured definitively by the NEO PI-R (Costa & McCrae, 1992).

**Workplace adoption:** Widespread in research and clinical settings; Moderate in direct
practitioner use (often mediated through DISC or MBTI as accessible shorthand).

**Vocabulary role:** Reference only. Big Five is the *scientific grounding* for the eidos
disposition axes — the Conscientiousness vocabulary (`urn:casehub:vocab:conscientiousness`)
is Big Five-grounded by design. There is no separate Big Five vocabulary module; agents
using Conscientiousness vocabulary terms are implicitly using Big Five semantics.

| Dimension | socialOrient | ruleFollowing | riskAppetite | autonomy |
|-----------|--------------|---------------|--------------|----------|
| Openness (high) | — | `flexible` (partial) | `bold` (partial) | `autonomous` (partial) |
| Openness (low) | — | `strict` (partial) | `conservative` (partial) | `directed` (partial) |
| Conscientiousness (high) | — | `strict` | `conservative` (partial) | — |
| Conscientiousness (low) | — | `flexible` | `bold` (partial) | — |
| Extraversion (high) | `collaborative` (partial) | — | — | — |
| Extraversion (low) | `independent` (partial) | — | — | — |
| Agreeableness (high) | `facilitative` (partial) | — | — | — |
| Agreeableness (low) | `independent` (partial) | — | — | — |
| Neuroticism (high / Stability low) | — | — | `conservative` (partial) | — |

Note: Big Five dimensions are continuous; the table shows the direction of the mapping
at high/low extremes. Partial mappings indicate overlapping but non-identical constructs.

---

### 2.2 DISC

**What it models:** Four behavioral style quadrants describing how people respond to
their environment. Dimensions: Dominance (task-focused, assertive), Influence (people-
focused, assertive), Steadiness (people-focused, unassertive), Conscientiousness (task-
focused, unassertive). Originated in Marston (1928); now offered by many vendors
(Everything DiSC, DiSC Classic, Thomas International, etc.).

**Scientific validity:** Low — correlates with Big Five Extraversion × Agreeableness
quadrants, confirming some construct validity; however, lacks peer-reviewed normative
data and independent factor replication. Wide vendor variation makes standardisation
difficult. Unlike MBTI (poor test-retest stability), DISC types tend to be consistent —
the validity problem is imprecision, not instability.

**Workplace adoption:** Widespread — one of the most-used workplace personality tools
globally, especially in sales, leadership, and team development.

**Vocabulary role:** Disposition vocabulary (`urn:casehub:vocab:disc`). DISC types
answer "how does this agent behave in any context?" → disposition fields.
**eidos#40 resolved.** DISC is implemented as `DiscTerm` + `DiscVocabRegistrar` in `casehub-eidos-vocab`. `DiscTerm` uses `axisExactMatch` for axis-aware cross-vocabulary resolution (→ `ConscientiousnessTerm` for axes 1–4, → `ThomasKilmannTerm` for `CONFLICT_MODE`).

| Dimension | socialOrient | ruleFollowing | riskAppetite | autonomy | conflictMode |
|-----------|--------------|---------------|--------------|----------|--------------|
| Dominance (D) | `independent` | `flexible` | `bold` | `autonomous` | `competing` |
| Influence (i) | `collaborative` | `flexible` | `measured` | `semi-autonomous` | `collaborating` |
| Steadiness (S) | `facilitative` | `principled` | `conservative` | `directed` | `accommodating` |
| Conscientiousness (C) | `independent` | `strict` | `conservative` | `semi-autonomous` | `avoiding` |

COMPROMISING has no DISC equivalent — it occupies the center of TK's assertiveness×cooperativeness space, which DISC's four quadrant types do not cover.

Note: DISC types do not predict `delegation`. Leave `delegation` at its role-specific
default (from Belbin if a Belbin slot is set; `false` otherwise).

---

### 2.3 MBTI (Myers-Briggs Type Indicator)

**What it models:** Sixteen personality types derived from four binary dichotomies
(I/E, S/N, T/F, J/P), loosely based on Jungian typology. Widely used in corporate
development since the 1950s.

**Scientific validity:** Low — poor test-retest reliability (~50% of people receive a
different type when retested one month later); dichotomous scoring ignores continuous
distributions; limited predictive validity for job performance. Not recommended by the
Society for Industrial and Organizational Psychology for personnel decisions.

**Workplace adoption:** Widespread — estimated 2 million assessments per year; deeply
embedded in corporate culture despite validity concerns.

**Vocabulary role:** Reference only. MBTI types are unsuitable as vocabulary terms due
to instability. The approximate Big Five mappings below are provided for legacy
compatibility only — for example, if an existing agent is described in MBTI terms and
must be translated.

| MBTI dichotomy | Approximate Big Five | AgentDescriptor field |
|----------------|---------------------|----------------------|
| Introvert (I) | Low Extraversion | `socialOrient: independent` (partial) |
| Extravert (E) | High Extraversion | `socialOrient: collaborative` (partial) |
| Sensing (S) | Low Openness | `riskAppetite: conservative` (partial) |
| iNtuition (N) | High Openness | `riskAppetite: bold` (partial) |
| Thinking (T) | Low Agreeableness | `socialOrient: independent` (partial) |
| Feeling (F) | High Agreeableness | `socialOrient: facilitative` (partial) |
| Judging (J) | High Conscientiousness | `ruleFollowing: strict` (partial) |
| Perceiving (P) | Low Conscientiousness | `ruleFollowing: flexible` (partial) |

**Do not create a MBTI vocabulary module for human-assessed personality.** The instability
of type assignments derived from human assessment makes any vocabulary built on those
MBTI terms unreliable over time.

#### Jungian Rehabilitation

The test-retest critique above assumes personality is *measured* from observed behavior.
For LLM agents, personality is *specified* — declared and injected via structured
prompting. No measurement error because no measurement. The instability is in the
assessment instrument, not the type system itself.

MBTI types are now supported via `MbtiTypeTerm` in `casehub-eidos-vocab`, grounded in
`JungianFunctionTerm` via `specializes()`. Each of the 16 types decomposes into its
dominant + auxiliary cognitive functions. The type label is an emergent property of the
weighted function stack — not an injected identity. The dichotomous scoring problem is
resolved: function weights are continuous [0.0–1.0].

This distinction matters: "INTP" as a human-assessed label is unstable; "Ti-dominant,
Ne-auxiliary with weights [0.35, 0.20, ...]" as a specified cognitive profile is
deterministic and reproducible. See §2.4 Jungian Cognitive Functions.

---

### 2.4 Jungian Cognitive Functions

**What it models:** Eight cognitive functions describing how agents process information
and make decisions. Based on Carl Jung's typological model, operationalized for LLM
agents by the JPAF paper (arXiv:2601.10025). Functions are arranged in two dimensions:
category (Judging vs. Perceiving) and attitude (Introverted vs. Extraverted).

| Function | Label | Category | Attitude | Description |
|----------|-------|----------|----------|-------------|
| Ti | Introverted Thinking | Judging | Introverted | Builds internal logical frameworks; analytical, precision-focused |
| Te | Extraverted Thinking | Judging | Extraverted | Applies logical organization externally; systematic, efficiency-oriented |
| Fi | Introverted Feeling | Judging | Introverted | Evaluates through deeply held personal values; authentic, principled |
| Fe | Extraverted Feeling | Judging | Extraverted | Harmonizes group values and social dynamics; attentive to others |
| Si | Introverted Sensation | Perceiving | Introverted | Draws on internalized sensory impressions and past experience |
| Se | Extraverted Sensation | Perceiving | Extraverted | Focuses on immediate sensory data; concrete, present-moment |
| Ni | Introverted Intuition | Perceiving | Introverted | Synthesizes internal patterns into singular insights; foresight |
| Ne | Extraverted Intuition | Perceiving | Extraverted | Explores external patterns, possibilities, and connections |

**Scientific validity:** Medium-High — Jung's cognitive function theory is the most
enduring part of his typological work. The JPAF paper demonstrates 100% MBTI alignment
across GPT-4, Llama, and Qwen using function-level specification, with TAA (trait
activation accuracy) >90% and PSA (personality shift accuracy) 100% for capable models.
Independent validation via activation steering (arXiv:2607.20803, July 2026) confirms
function-level personality control in LLMs.

**Workplace adoption:** Growing — Jungian cognitive functions underpin MBTI (§2.3) but
are increasingly used directly, bypassing the dichotomous type system.

**Vocabulary role:** Disposition vocabulary (`urn:casehub:vocab:jungian`). Agents declare
a weighted cognitive function profile via `dispositionProfile` on `AgentDisposition`.
Functions resolve to all five disposition axes via `axisExactMatch` (cross-vocabulary
projection to Conscientiousness and Thomas-Kilmann terms).

**Why function-level specification works for LLM agents:**
- **Weighted profiles, not dichotomies:** Each function carries a continuous weight
  [0.0–1.0], avoiding the MBTI dichotomous scoring problem
- **Compensation mechanism:** Dominant functions are balanced by auxiliary functions
  from the opposite category (Judging ↔ Perceiving), creating nuanced behavioral profiles
- **Personality is specified, not measured:** No assessment instrument error — the function
  stack is declared and injected via structured prompting

**Structural rules:**
- **Shadow:** Each function has an opposite-attitude counterpart (Ti↔Te, Fi↔Fe, Si↔Se,
  Ni↔Ne). Shadow activation signals personality evolution
- **Dominant-auxiliary pairing:** Valid pairs use opposite categories (a Judging dominant
  requires a Perceiving auxiliary and vice versa)
- **Weight tiers:** Dominant [0.31–1.0], auxiliary [0.06–0.30], undifferentiated [0–0.06]

**Implementation:** `JungianFunctionTerm` enum in `casehub-eidos-vocab` with `shadow()`,
`opposite()`, `category()`, `attitude()`, `compatibleAuxiliaries()`. `MbtiTypeTerm`
provides 16 MBTI types grounded via `specializes()` to their dominant + auxiliary
JungianFunctionTerms.

---

## 3. Cognitive / Work Style Frameworks

### 3.1 Thomas-Kilmann Conflict Modes

**What it models:** Five strategies for handling interpersonal conflict, defined by two
dimensions: assertiveness (pursuing own concerns) and cooperativeness (attending to
others' concerns). Modes: Competing (high assert, low coop), Collaborating (high/high),
Compromising (mid/mid), Avoiding (low/low), Accommodating (low assert, high coop).
Developed by Thomas and Kilmann (1974) from the Blake-Mouton Managerial Grid.

**Scientific validity:** Medium — empirically grounded in the Blake-Mouton framework;
reasonable construct validity for conflict behavior; not predictive of stable trait.

**Workplace adoption:** Widespread in conflict resolution, negotiation, and team dynamics.

**Vocabulary role:** Conflict mode disposition vocabulary (`urn:casehub:vocab:thomas-kilmann`). An agent's default conflict approach is treated as a stable prior — the same epistemological status as the other four disposition axes. Use the `conflictMode` field in `AgentDisposition`; see `ThomasKilmannTerm` in `casehub-eidos-vocab`.

Implemented in eidos#38.

---

### 3.2 Situational Leadership (Hersey & Blanchard)

**What it models:** How a *leader* should adapt their management style to a follower's
development level for a specific task. Four leader styles: S1 Directing (high task, low
relationship), S2 Coaching (high/high), S3 Supporting (low task, high relationship),
S4 Delegating (low/low). Developed by Hersey and Blanchard (1969).

**Scientific validity:** Low — widely adopted in corporate leadership training; weak
empirical support as a predictive model; the contingency relationship between follower
readiness and recommended style has not replicated reliably.

**Workplace adoption:** Widespread in corporate management development.

**Vocabulary role:** Reference only — conceptual framing for the autonomy axis, not
a vocabulary source. **Important:** SL describes LEADER behavior adapting to follower
readiness, not follower (or agent) traits. Mapping SL styles to agent autonomy uses the
model backwards. The value is the intuitive framing: the S1→S4 arc provides a memorable
image of the directed→autonomous progression, not an authoritative definition.

| SL style (leader) | Follower readiness implied | Autonomy axis framing |
|--------------------|--------------------------|----------------------|
| S1 Directing | Low competence, low commitment | `directed` — follows explicit instructions |
| S2 Coaching | Low-moderate competence, high commitment | (between `directed` and `semi-autonomous` — no distinct term) |
| S3 Supporting | High competence, variable commitment | `semi-autonomous` — acts within defined boundaries |
| S4 Delegating | High competence, high commitment | `autonomous` — acts on own judgment |

---

### 3.3 Kirton Adaption-Innovation (KAI)

**What it models:** A single cognitive style dimension ranging from Adaptor (prefers
proven methods, works within structure, incrementally improves) to Innovator (prefers
novel approaches, challenges assumptions, reconstructs problems). Developed by Michael
Kirton (1976); measured by the KAI inventory (32–160).

**Scientific validity:** Medium — the KAI inventory has good test-retest reliability
and convergent validity with related constructs; the single-axis model has been
challenged but holds reasonably well empirically.

**Workplace adoption:** Moderate — used in innovation management and team composition.

**Vocabulary role:** Reference only. Supports the Conscientiousness vocabulary — KAI
Adaptor and Innovator endpoints align with existing terms.

| Dimension | ruleFollowing | riskAppetite |
|-----------|---------------|--------------|
| Adaptor (low KAI score) | `strict` | `conservative` |
| Innovator (high KAI score) | `flexible` | `bold` |

The KAI dimension provides supporting evidence for the `ruleFollowing` and `riskAppetite`
axes — not additional vocabulary. If an agent's KAI score is known, use the corresponding
Conscientiousness terms directly.

---

## 4. Occupational Frameworks

### 4.1 O*NET

**What it models:** The US Occupational Information Network — a comprehensive database
of ~1,000 occupations organised by Knowledge, Skills, Abilities, Work Activities, Work
Context, and Work Styles. Maintained by the US Department of Labor. Provides standardised
vocabulary for describing what workers need to know and be able to do.

**Scientific validity:** High — government-maintained; empirically derived from job
analysis across thousands of occupations; updated continuously.

**Workplace adoption:** Widespread — the US standard for occupational classification;
widely referenced internationally.

**Vocabulary role:** Capabilities vocabulary source. O*NET Knowledge and Skill category
names are the primary source for `AgentCapability.name` values. Occupation codes
(e.g., `15-1252.00` for Software Developers) can be used as `slot` values for technically
precise occupational roles.

| O*NET component | AgentDescriptor field |
|----------------|----------------------|
| Occupation code | `slot` (when technical precision needed) |
| Knowledge categories | `capabilities[].name` |
| Skill categories | `capabilities[].name` |
| Abilities | `capabilities[].name` (partial — some abilities → disposition) |
| Work Styles | disposition (partial — Achievement/Effort → `autonomy`; Concern for Others → `socialOrient`) |
| Work Activities | `capabilities[].name` |

**Example capability names from O*NET:** `programming`, `systems-analysis`,
`critical-thinking`, `active-listening`, `judgment-and-decision-making`.

---

### 4.2 SFIA (Skills Framework for the Information Age)

**What it models:** A competence framework for IT and digital professionals, defining
~120 skills across seven categories and seven responsibility levels (1 = Follow to
7 = Set strategy). Maintained by the SFIA Foundation. IT-specific — not general purpose.

**Scientific validity:** Medium — industry-maintained standard; widely validated through
industry use; not peer-reviewed research.

**Workplace adoption:** Widespread in IT sector — standard in UK public sector and many
enterprise IT organisations.

**Vocabulary role:** Capabilities vocabulary source for IT-domain agents. SFIA skill
names provide precise `AgentCapability.name` values for technical roles. Responsibility
levels offer a partial `autonomy` mapping.

| SFIA component | AgentDescriptor field |
|---------------|----------------------|
| Skill names | `capabilities[].name` |
| Skill categories | `capabilities[].tags` |
| Responsibility levels 1–2 | `autonomy: directed` (partial) |
| Responsibility levels 3–4 | `autonomy: semi-autonomous` (partial) |
| Responsibility levels 5–7 | `autonomy: autonomous` (partial) |

**Example SFIA skill names:** `software-development`, `systems-architecture`,
`data-management`, `security-administration`, `user-experience-design`.

**Note:** Use O*NET for non-IT agents; use SFIA for IT/digital agents. Using both
simultaneously adds no signal — SFIA is an IT-specific subset of the O*NET knowledge space.

---

## 5. Cross-Reference Summary Table

Rows = AgentDescriptor fields. Columns = all frameworks except BDI (appendix only).

| Field | Belbin | MM | Big Five | DISC | MBTI | TK | Jungian | SL | KAI | O\*NET | SFIA |
|-------|--------|----|----------|------|------|----|---------|----|-----|--------|------|
| `slot` | **slot** | — | — | — | — | — | — | — | — | capabilities* | — |
| `capabilities` | — | — | — | — | — | — | — | — | — | **capabilities** | **capabilities** |
| `socialOrient` | **disposition** | reference | partial | **disposition** | partial | partial | **disposition** | — | — | partial | — |
| `ruleFollowing` | **disposition** | reference | partial | **disposition** | partial | — | **disposition** | — | partial | — | — |
| `riskAppetite` | **disposition** | reference | partial | **disposition** | partial | — | **disposition** | — | partial | — | — |
| `autonomy` | **disposition** | reference | partial | **disposition** | — | — | **disposition** | reference | — | partial | partial |
| `conflictMode` | — | — | — | **disposition** | — | **disposition** | **disposition** | — | — | — | — |
| `delegation` | **disposition** | — | — | — | — | — | — | — | — | — | — |

Key: **bold** = primary vocabulary source for this field · `partial` = partial/approximate mapping ·
`reference` = conceptual grounding only, no vocabulary terms · `Partial` (compatibility) = meaningful overlap but insufficient to justify dual vocabulary · `—` = no claim

Note: Jungian maps to all 5 disposition axes via `axisExactMatch` cross-vocabulary projection
(→ Conscientiousness for socialOrient, ruleFollowing, riskAppetite, autonomy; → Thomas-Kilmann
for conflictMode). It does not map to slot, capabilities, or delegation.

Note on DISC × conflictMode: DISC types resolve to TK conflict modes via `axisExactMatch` (D→competing, i→collaborating, S→accommodating, C→avoiding). They appear as `**disposition**` here because an agent using `dispositionVocabulary="urn:casehub:vocab:disc"` will have its `conflictMode` value axis-resolved to TK terms automatically — DISC is a usable vocabulary source for this field even though TK defines the underlying terms. COMPROMISING has no DISC equivalent.

*O\*NET provides occupation codes that may be used as `slot` values when technical
precision is needed over team-role vocabulary.

---

## 6. Framework Compatibility

Curated pairs only — combinations that are meaningfully Additive, Redundant, Reference, Inadvisable, or Partial. Full N×N matrix omitted; most cross-category pairings are simply
orthogonal.

| Pair | Rating | Reasoning |
|------|--------|-----------|
| Belbin + Big Five | Additive | Role (Belbin slot) and stable trait (Conscientiousness vocabulary, which is Big Five-grounded) are orthogonal — this is the natural Belbin Profile |
| Belbin + DISC | Additive | Role assignment (slot) and behavioral style (disposition) are orthogonal; an agent holds both simultaneously. Implemented in eidos#26 (eidos#40 CLOSED). |
| Belbin + Margerison-McCann | Redundant | Same conceptual territory; contradictory terminology; pick Belbin |
| DISC + Big Five (Conscientiousness vocabulary) | Redundant | DISC is a quadrant simplification of Big Five Extraversion × Agreeableness; no new signal; choose one |
| Big Five + Thomas-Kilmann | Additive | Stable personality trait + situational conflict strategy are different constructs |
| Big Five + Situational Leadership | Reference | SL describes leader response to follower readiness, not agent trait; useful as autonomy axis framing only |
| O*NET + Big Five | Additive | Occupational competence (capabilities) + behavioral trait (disposition) are orthogonal |
| SFIA + O*NET | Redundant | Both are occupational competence frameworks; SFIA is an IT-specific subset |
| MBTI (human-assessed) + anything | Inadvisable | Low test-retest reliability makes any vocabulary built on human-assessed MBTI types unstable; not a conceptual contradiction but any combination produces unreliable encodings |
| MBTI (agent-specified) + Jungian | Hierarchical | MBTI types emerge from Jungian function stacks via `specializes()`. The type label is an emergent property — see §2.3 Jungian Rehabilitation |
| Jungian + Belbin | Additive | Cognitive style (Jungian functions) and team role (Belbin slot) are orthogonal — both contribute independent signal |
| Jungian + DISC | Redundant | Both describe behavioral style; Jungian is deeper (8 functions with structural rules vs. 4 quadrants). Jungian projects onto the same disposition axes as DISC |
| Jungian + Conscientiousness | Redundant | Jungian functions project onto all Conscientiousness axes via `axisExactMatch` — using both creates contradictory encodings |
| KAI + DISC | Partial | Adaptor/Innovator overlaps with DISC C/D on ruleFollowing and riskAppetite; KAI adds precision but not enough to justify two vocabularies |

---

# Part II — Design Guide

## Architecture: DISC as Disposition Vocabulary

DISC types describe behavioral patterns that an agent brings to every context — they are
not roles assigned by a team. This makes DISC a **disposition vocabulary**, not a slot
vocabulary.

- **Belbin** answers "what role do you play in this team?" → `slot` field
- **DISC** answers "how do you behave in any context?" → disposition fields

An agent holds both simultaneously:

```
slotVocabulary        = "urn:casehub:vocab:belbin"     ← team role
dispositionVocabulary = "urn:casehub:vocab:disc"        ← behavioral style
slot                  = "co-ordinator"
disposition.socialOrient  = "dominance"                 ← DISC type; resolved per axis
```

This combination is additive: a Co-ordinator (Belbin: `facilitative`, `measured`) who is
also a D-type (DISC: `independent` on socialOrient, `bold` on riskAppetite) diverges on
both axes.
The DISC type reveals behavioral style that the Belbin role does not predict.

**eidos#40 resolved.** `VocabularyRegistry.equivalentValues(fromVocab, value, toVocab, axis)` provides per-axis disambiguation. A DISC type correctly resolves to different Conscientiousness terms per axis (`dominance → independent` on socialOrient, `bold` on riskAppetite, etc.). Implemented in `DiscTerm` via `axisExactMatch`.

---

## Vocabulary URI Field Interaction

`AgentDescriptor` has three vocabulary URI fields:

| Field | Scope | Purpose |
|-------|-------|---------|
| `domainVocabulary` | ALL fields (default) | Sets the default vocabulary for slot, capabilities, and all disposition axes |
| `slotVocabulary` | `slot` only | Overrides `domainVocabulary` for slot resolution |
| `dispositionVocabulary` | All disposition axes | Overrides `domainVocabulary` for all disposition field values |

`domainVocabulary` is useful when a single custom vocabulary covers all fields in a descriptor — for example, a domain-specific vocabulary that defines both slot terms and disposition terms under one URI. When using separate Belbin and Conscientiousness vocabularies, use the more specific `slotVocabulary` and `dispositionVocabulary` fields instead.

**Resolution precedence (most specific wins):**
1. `slotVocabulary` for `slot`; `dispositionVocabulary` for disposition fields
2. `domainVocabulary` for any field without a specific override
3. No vocabulary — raw string, no resolution

**Worked examples:**

*Belbin Profile* — Belbin slot + Conscientiousness disposition:
```
slotVocabulary        = "urn:casehub:vocab:belbin"
dispositionVocabulary = "urn:casehub:vocab:conscientiousness"
axisVocabularies      = {CONFLICT_MODE: "urn:casehub:vocab:thomas-kilmann"}
slot                  = "co-ordinator"
disposition.socialOrient  = "facilitative"
disposition.ruleFollowing = "principled"
disposition.riskAppetite  = "measured"
disposition.autonomy      = "semi-autonomous"
disposition.conflictMode  = "collaborating"   ← TK term, axis-overridden
disposition.delegation    = true
```

*Belbin + DISC Profile*:
```
slotVocabulary        = "urn:casehub:vocab:belbin"
dispositionVocabulary = "urn:casehub:vocab:disc"
slot                  = "co-ordinator"
disposition.socialOrient  = "dominance"   ← DISC type; axis-aware resolution needed
disposition.ruleFollowing = "dominance"   ← same DISC type → different Conscientiousness term
disposition.riskAppetite  = "dominance"
disposition.autonomy      = "dominance"
disposition.delegation    = true          ← from Belbin role (Co-ordinator), not from DISC
```

*Occupational Profile* — O*NET capabilities + Conscientiousness disposition:
```
slotVocabulary        = null
dispositionVocabulary = "urn:casehub:vocab:conscientiousness"
capabilities          = [AgentCapability(name="software-development", ...), ...]
disposition.socialOrient  = "independent"
disposition.ruleFollowing = "strict"
disposition.riskAppetite  = "conservative"
disposition.autonomy      = "semi-autonomous"
```

---

## Encoding Guidance

Rules that apply regardless of which framework is used:

1. **Slot vocabulary vs. disposition vocabulary:** Belbin roles go in `slot` via
   `slotVocabulary`. Explicitly populating disposition axes alongside a Belbin slot is
   correct and expected — the axes are independent fields and explicit values are
   directly queryable without vocabulary resolution.

2. **DISC as disposition vocabulary:** Do not place DISC type names in `slot`. DISC
   describes behavioral pattern (disposition), not team assignment. Use
   `dispositionVocabulary="urn:casehub:vocab:disc"`.

3. **Thomas-Kilmann conflict modes:** Map only TK Collaborating to
   `socialOrient=collaborative`. Do not map TK Competing to `riskAppetite` — Competing
   is conflict assertiveness, not risk tolerance. Avoiding and Accommodating have no
   current axis mapping; see eidos#38.

4. **Autonomy axis authority:** The `directed / semi-autonomous / autonomous` progression
   is conceptually inspired by SL's S1→S4 arc. SL describes leader behavior, not agent
   traits. The axis stands on its own — do not cite SL as the authority for autonomy values.

---

## Axis-by-Axis Recommendations

For each `AgentDescriptor` field, the primary framework to reach for and why:

**`slot`:** Belbin for team composition roles; O*NET occupation codes for technical
roles; SFIA skill categories for IT roles. Never use DISC or MBTI type names as slot
values — they describe behavioral patterns, not role assignments.

**`socialOrient`:** Conscientiousness vocabulary directly (`collaborative`, `independent`,
`facilitative`). TK Collaborating aligns with `collaborative`; no other TK modes map
cleanly. Big Five Extraversion and Agreeableness provide supporting evidence but not
additional terms. See eidos#38 for conflict-mode coverage.

**`ruleFollowing`:** Conscientiousness vocabulary (`strict`, `principled`, `flexible`)
is complete. KAI and DISC-C provide supporting evidence — no additional terms needed.

**`riskAppetite`:** Conscientiousness vocabulary (`conservative`, `measured`, `bold`)
is complete. KAI and Big Five Openness support it. Do not use TK modes here.

**`autonomy`:** Conscientiousness vocabulary (`directed`, `semi-autonomous`, `autonomous`)
is complete. SL S1→S4 provides an intuitive mental model. Do not cite SL as authority.

**`conflictMode`:** Thomas-Kilmann is the only primary vocabulary source — use
`urn:casehub:vocab:thomas-kilmann` and its five terms (`competing`, `collaborating`,
`compromising`, `avoiding`, `accommodating`). DISC types resolve to TK terms automatically
via `axisExactMatch` when `dispositionVocabulary="urn:casehub:vocab:disc"` is set — no
explicit TK vocabulary URI is needed in that case. Belbin roles have no defined TK mapping;
do not infer conflict mode from a Belbin slot.

**`delegation`:** Set to `true` for Belbin Co-ordinator. DISC types make no delegation
claim (sub-agent spawning is platform-semantic, not personality-semantic). Default `false`
unless the role explicitly involves empowering others.

**`capabilities`:** O*NET Knowledge and Skill category names for general roles; SFIA
skill names for IT roles. Big Five Openness predicts breadth of capability domains but
does not define capability names.

---

## Combination Patterns

Three named patterns covering the most common agent descriptor compositions:

---

### Belbin Profile

*Belbin slot + Conscientiousness disposition*

```
slotVocabulary        = "urn:casehub:vocab:belbin"
dispositionVocabulary = "urn:casehub:vocab:conscientiousness"
slot                  = "<belbin-role-key>"
disposition.socialOrient  = "<conscientiousness term>"
disposition.ruleFollowing = "<conscientiousness term>"
disposition.riskAppetite  = "<conscientiousness term>"
disposition.autonomy      = "<conscientiousness term>"
disposition.conflictMode  = "<thomas-kilmann term>"   ← requires axisVocabularies override (see note)
disposition.delegation    = <bool from Belbin draft table>
```

Note: `conflictMode` uses TK terms, not Conscientiousness terms. When `dispositionVocabulary`
is set to Conscientiousness, set `axisVocabularies = {CONFLICT_MODE: "urn:casehub:vocab:thomas-kilmann"}`
to override for that axis. Alternatively, omit `conflictMode` — it is not required.

**What it expresses:** Team contribution role + full behavioral profile.
**What it leaves unspecified:** Occupational domain.
**When to use:** Agents defined primarily by team function. Populate all four Conscientiousness
axes — the Belbin vocabulary draft table shows typical implied values per role; actual
behavioral assessment may warrant deviation. Add `conflictMode` when conflict style is known.

---

### Belbin + DISC Profile

*Belbin slot + DISC disposition*

```
slotVocabulary        = "urn:casehub:vocab:belbin"
dispositionVocabulary = "urn:casehub:vocab:disc"
slot                  = "<belbin-role-key>"
disposition.socialOrient  = "<disc-type-key>"
disposition.ruleFollowing = "<disc-type-key>"
disposition.riskAppetite  = "<disc-type-key>"
disposition.autonomy      = "<disc-type-key>"
disposition.conflictMode  = "<disc-type-key>"   ← axis-resolved to TK term via axisExactMatch
disposition.delegation    = <bool from Belbin draft table — not from DISC>
```

**What it expresses:** Team role (Belbin) + independently-measured personality style
(DISC), including conflict mode via DISC's axis-aware TK resolution.
Additive when the DISC type diverges from the Belbin role's implied disposition
— a Co-ordinator (`facilitative`, `measured`) who is also a D-type (`independent` on
socialOrient, `bold` on riskAppetite) diverges on both axes.
Always populate `delegation` from the Belbin draft table; DISC types make no delegation claim.
**Implemented** in `casehub-eidos-vocab` (eidos#26). Axis-aware resolution via `axisExactMatch` enabled by eidos#40 (CLOSED).

---

### Occupational Profile

*O*NET or SFIA capabilities + Conscientiousness disposition*

```
slotVocabulary        = null   ← or domain-specific slot vocabulary
dispositionVocabulary = "urn:casehub:vocab:conscientiousness"
capabilities          = [<o*net or sfia skill names as AgentCapability entries>]
disposition.socialOrient  = "<conscientiousness term>"
disposition.ruleFollowing = "<conscientiousness term>"
disposition.riskAppetite  = "<conscientiousness term>"
disposition.autonomy      = "<conscientiousness term>"
disposition.conflictMode  = "<thomas-kilmann term>"   ← requires axisVocabularies override (see Belbin Profile note)
```

**What it expresses:** Technical competence domain + behavioral traits.
**What it leaves unspecified:** Team dynamic.
**When to use:** Agents defined primarily by technical skill (code review, data analysis,
security analysis). Use O*NET for general roles; SFIA for IT-specific roles. Add
`conflictMode` when the agent's conflict handling style is a meaningful signal.

---

### Jungian Profile

*Jungian cognitive function disposition profile with auto-derived axes*

```
dispositionVocabulary = "urn:casehub:vocab:jungian"
dispositionProfile      = [{ti, 0.45}, {ne, 0.20}, {si, 0.10}, {fe, 0.08}, ...]
// axes auto-derived via cross-vocabulary projection
```

**What it expresses:** Complete cognitive style via weighted function stack. All five
disposition axes are auto-derived from the function profile via `axisExactMatch`
cross-vocabulary projection (Jungian → Conscientiousness for axes 1–4, Jungian →
Thomas-Kilmann for conflictMode). No explicit axis values needed.

**What it leaves unspecified:** Team role (add Belbin `slotVocabulary` if needed),
occupational domain (add capabilities separately).

**When to use:** Agents whose behavioral profile should emerge from cognitive style
rather than being directly specified per axis. Particularly suited for agents that need
personality evolution via `DispositionHealth` / `DispositionEvolution` — the Jungian
function stack provides the structural rules for valid transitions.

**MBTI convenience:** For agents with a known MBTI type, use
`MbtiTypeTerm.INTP.defaultProfile()` to generate the 8-function weight distribution
automatically. The MBTI type label is emergent — never inject it directly.

---

## Anti-Patterns

### Framework Selection Errors

**1. MBTI as vocabulary basis (human-measured)**
When MBTI types are derived from human personality assessment, ~50% type-change one month
later makes any vocabulary built on MBTI terms unreliable. Use Big Five /
Conscientiousness vocabulary instead for assessment-derived personality. MBTI→Big Five
approximate mappings (§2.3) are provided for legacy translation only.
**Exception:** For LLM agents, personality is *specified* not *measured* —
`MbtiTypeTerm` provides MBTI types grounded through Jungian cognitive functions
(`specializes()` → `JungianFunctionTerm`). See §2.4 Jungian Cognitive Functions.

**2. DISC + Conscientiousness vocabulary simultaneously**
DISC is a quadrant simplification of Big Five Extraversion × Agreeableness. Using
`dispositionVocabulary=disc` alongside explicit Conscientiousness terms adds no signal
and creates contradictory encodings. Choose one vocabulary per descriptor.

**3. Belbin + Margerison-McCann together**
Both model team contribution roles with overlapping but inconsistently-named dimensions.
Combining them produces contradictory slot vocabulary. Pick Belbin (broader global
adoption, more research).

### Encoding Errors

**4. DISC type names in `slot`**
`slot="dominance"` treats a behavioral personality style as a team role assignment. DISC
types describe how an agent behaves everywhere, not what role a team assigned it. Use
`dispositionVocabulary="urn:casehub:vocab:disc"` and leave `slotVocabulary` for Belbin
or an occupational vocabulary.

**5. Using a Belbin role key as a disposition field value**
`socialOrient="shaper"` treats a Belbin slot key as a disposition term. This is a
vocabulary category error — `socialOrient` must contain a disposition vocabulary term
(`collaborative`, `independent`, etc.), not a slot key. Explicitly populating disposition
axes alongside a Belbin slot is correct; the mistake is using the slot key itself as a
disposition value.

### Axis Assignment Errors

**6. Thomas-Kilmann modes mapped to non-conflict axes**
TK modes describe conflict strategy. Map TK Collaborating to `socialOrient=collaborative` only when no dedicated `conflictMode` is set. Once `conflictMode` is set via `urn:casehub:vocab:thomas-kilmann`, prefer the dedicated axis.

Do not map TK Competing, Avoiding, Accommodating, or Compromising to any existing axis. All five modes map cleanly to `conflictMode`.

**7. DISC Dominance → `delegation=true`**
`delegation` means "can spawn sub-agents" — a platform-semantic boolean. DISC D-types
assign tasks but often maintain tight oversight; this does not predict sub-agent spawning
capability. DISC types make no delegation claim; leave it at its role-specific default.

---

## Vocabulary Draft Tables

These tables are the normative source for eidos#26 implementation. Both `BelbinTerm` and
`DiscTerm` are implemented in `casehub-eidos-vocab`. `DiscTerm` uses `axisExactMatch` for
per-axis resolution; eidos#40 (axis-aware `equivalentValues()`) is CLOSED.

`exactMatches` is `Map.of()` for all entries — neither Belbin Associates nor DISC
framework publishers have released canonical semantic web URIs for their terms.

### Belbin Team Roles

**Framework source:** Belbin, *Team Roles at Work*, 1993 edition (9-role model)
**Vocabulary version:** `"1.0"` (eidos vocabulary; not a Belbin publication version)
**URI:** `"urn:casehub:vocab:belbin"`

**Implementation note:** The disposition columns in this section (`socialOrient`,
`ruleFollowing`, `riskAppetite`, `autonomy`, `delegation`) are reference annotation —
they show Conscientiousness values implied by each role for cross-mapping guidance.
They are **not** fields in `VocabularyTerm`. `BelbinVocabularyProducer` writes only:
`value`, `label`, `description`, `aliases`, `exactMatches = Map.of()`.

| Role | value (key) | label | description | aliases |
|------|-------------|-------|-------------|---------|
| Plant | `plant` | Plant | Creative, unorthodox problem-solver; generates novel ideas independently | `["pl"]` |
| Resource Investigator | `resource-investigator` | Resource Investigator | Extrovert who explores external opportunities and develops contacts | `["ri"]` |
| Co-ordinator | `co-ordinator` | Co-ordinator | Clarifies goals, promotes team decision-making, delegates effectively | `["co"]` |
| Shaper | `shaper` | Shaper | Challenges the team to improve; driven, dynamic, thrives under pressure | `["sh"]` |
| Monitor Evaluator | `monitor-evaluator` | Monitor Evaluator | Sober, strategic, discerning; sees all options and judges accurately | `["me"]` |
| Teamworker | `teamworker` | Teamworker | Cooperative, perceptive, diplomatic; averts friction and builds cohesion | `["tw"]` |
| Implementer | `implementer` | Implementer | Disciplined, reliable, efficient; turns ideas into practical actions | `["imp"]` |
| Completer Finisher | `completer-finisher` | Completer Finisher | Painstaking, conscientious, anxious; ensures delivery to standard | `["cf"]` |
| Specialist | `specialist` | Specialist | Dedicated, self-starting, single-minded; provides rare knowledge | `["sp"]` |

Implied disposition values per role (reference — use in combination pattern selection,
not in VocabularyTerm entries):

| Key | socialOrient | ruleFollowing | riskAppetite | autonomy | delegation | conflictMode |
|-----|--------------|---------------|--------------|----------|------------|--------------|
| `plant` | `independent` | `flexible` | `bold` | `autonomous` | `false` | — |
| `resource-investigator` | `collaborative` | `flexible` | `measured` | `semi-autonomous` | `false` | — |
| `co-ordinator` | `facilitative` | `principled` | `measured` | `semi-autonomous` | `true` | — |
| `shaper` | `independent` | `flexible` | `bold` | `autonomous` | `false` | — |
| `monitor-evaluator` | `independent` | `strict` | `conservative` | `semi-autonomous` | `false` | — |
| `teamworker` | `collaborative` | `principled` | `conservative` | `directed` | `false` | — |
| `implementer` | `collaborative` | `strict` | `conservative` | `directed` | `false` | — |
| `completer-finisher` | `independent` | `strict` | `conservative` | `semi-autonomous` | `false` | — |
| `specialist` | `independent` | `principled` | `measured` | `autonomous` | `false` | — |

Note: Belbin roles describe team contribution; Belbin→TK conflict-mode cross-vocabulary mappings would conflate role semantics with conflict-mode semantics. No Belbin→TK mapping is defined.

---

### DISC Types

**Framework source:** Conceptual DiSC quadrant model, Marston (1928); framework-neutral —
not tied to any vendor assessment
**Vocabulary version:** `"1.0"` (eidos vocabulary; not a vendor or framework version)
**URI:** `"urn:casehub:vocab:disc"`

**eidos#40 resolved.** The `→` columns show the Conscientiousness term returned when resolving this DISC type on each specific axis via `axisExactMatch`. `DiscTerm` implements this mapping in `casehub-eidos-vocab`; registered automatically via `DiscVocabRegistrar`. See eidos#26 for the implementation.

DISC types are disposition vocabulary terms, not slot terms. An agent declares
`dispositionVocabulary="urn:casehub:vocab:disc"` and uses DISC type keys as disposition
field values.

| Type | value (key) | label | description | aliases | → socialOrient | → ruleFollowing | → riskAppetite | → autonomy |
|------|-------------|-------|-------------|---------|----------------|-----------------|----------------|------------|
| Dominance | `dominance` | Dominance | Results-driven, direct, decisive; prioritises outcomes over relationships | `["D"]` | `independent` | `flexible` | `bold` | `autonomous` |
| Influence | `influence` | Influence | Enthusiastic, optimistic, collaborative; motivates and involves others | `["i"]` | `collaborative` | `flexible` | `measured` | `semi-autonomous` |
| Steadiness | `steadiness` | Steadiness | Patient, reliable, supportive; values stability and consistency | `["S"]` | `facilitative` | `principled` | `conservative` | `directed` |
| Conscientiousness | `conscientiousness-disc` | Conscientiousness | Analytical, systematic, quality-focused; emphasises accuracy | `["C"]` | `independent` | `strict` | `conservative` | `semi-autonomous` |

Note: `delegation` absent — DISC types make no claim about sub-agent spawning.
`conscientiousness-disc` key avoids collision with `urn:casehub:vocab:conscientiousness`
(a disposition term vocabulary); this DISC entry names a personality type.

---

## Vocabulary Gap Notes

| Gap | Source | What is missing | Note |
|-----|--------|-----------------|------|
| Co-ordinator autonomy | Belbin Co-ordinator | Orchestrates team decision-making; higher coordination intent than `semi-autonomous` but not independently agenda-driven like `autonomous` | Mapped to `semi-autonomous + delegation=true` — the combination may be sufficient; decision for eidos#26 |

The `conflictMode` axis and `urn:casehub:vocab:thomas-kilmann` vocabulary resolve all five TK modes (eidos#38, eidos#26). COMPROMISING has no DISC equivalent — it occupies the center of TK's two-dimensional space, which DISC's four quadrant types do not cover.

Previously proposed gap terms (accommodating, deferring, compromising) are withdrawn.
Adding them to `socialOrient` would mix conflict strategy with social preference, making
the axis semantically incoherent.

---

## Implementation Notes for eidos#26

Each vocabulary is a Java enum implementing `VocabularyTerm`, annotated with
`@VocabularyMetadata(uri = "...", name = "...", version = "...")`. A companion
`@ApplicationScoped` class implementing `VocabularyRegistrar` returns the enum class
via `vocabulary()`. `CdiVocabularyRegistry` discovers all registrars at startup via
`Instance<VocabularyRegistrar>`.

See `ConscientiousnessTerm` + `ConscientiousnessVocabRegistrar` as the canonical reference
implementation. For vocabularies using `axisExactMatch`, see `DiscTerm` (anonymous subclass
pattern, same as `SvoTerm`).

---

## Appendix: BDI Agent Architecture Model

BDI (Belief-Desire-Intention) is a formal computational architecture for rational agents
(Rao & Georgeff, 1991). It describes how an agent's reasoning cycle should be implemented,
not how to characterise behavioral disposition or team contribution. It is included here
for architectural context only — it contributes no slot, capabilities, or disposition
vocabulary terms and is excluded from the cross-reference table and all combination patterns.

The following mapping is **illustrative only, not a precise correspondence:**

| BDI component | Approximate AgentDescriptor analogue | Where the analogy breaks |
|---------------|--------------------------------------|--------------------------|
| Beliefs — what the agent knows | `AgentCapability` — declared knowledge and skills | Beliefs are dynamic (updated by perception); capabilities are static declarations |
| Desires — what the agent wants to achieve | `AgentPromptContext.GoalContext` — render-time goal context | Desires are persistent motivations; GoalContext is session-scoped |
| Intentions — committed action plans | `AgentDisposition` — stable behavioral character | Intentions are dynamic and context-specific; AgentDisposition is a static prior |

The value of this mapping is recognising that AgentDescriptor's overall structure has
BDI lineage. The descriptor captures static priors (what the agent is, what it can do,
how it tends to behave); BDI describes the runtime loop that uses those priors. They are
complementary, not equivalent.
