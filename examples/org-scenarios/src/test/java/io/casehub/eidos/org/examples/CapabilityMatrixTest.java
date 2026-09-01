package io.casehub.eidos.org.examples;

import org.junit.jupiter.api.Test;

/**
 * Capability Matrix — which features each example exercises.
 *
 * Use this to find the right example for what you're building.
 *
 * <pre>
 * ┌──────────────────────────┬──────────┬─────────────┬──────────┬──────────┐
 * │ Feature                  │ Gastown  │ Review Team │ Support  │ Clinical │
 * ├──────────────────────────┼──────────┼─────────────┼──────────┼──────────┤
 * │ Organizational units     │    ✓     │      ✓      │    ✓     │    ✓     │
 * │ Unit hierarchy           │    —     │      —      │    —     │    ✓     │
 * │ Membership roles         │    ✓     │      ✓      │    ✓     │    ✓     │
 * │ Collective capabilities  │    ✓     │      ✓      │    ✓     │    ✓     │
 * │ SUPERVISES               │    ✓     │      —      │    ✓     │    ✓     │
 * │ DELEGATES_TO             │    —     │      —      │    —     │    ✓     │
 * │ ESCALATES_TO             │    ✓     │      —      │    ✓     │    ✓     │
 * │ REPORTS_TO               │    —     │      —      │    —     │    ✓     │
 * │ BACKS_UP                 │    ✓     │      —      │    ✓     │    —     │
 * │ EXTENDED                 │    —     │      —      │    —     │    ✓     │
 * │ Scoped relationships     │    ✓     │      —      │    ✓     │    ✓     │
 * │ Attestation grants       │    ✓     │      —      │    —     │    ✓     │
 * │ Escalation chains        │    ✓     │      —      │    ✓     │    ✓     │
 * │ Deep supervision (3+)    │    ✓     │      —      │    ✓     │    —     │
 * │ Flat team (no hierarchy) │    —     │      ✓      │    —     │    —     │
 * │ Cross-dept delegation    │    —     │      —      │    —     │    ✓     │
 * │ Mutual backup            │    —     │      —      │    ✓     │    —     │
 * │ Vocabulary-grounded kind │    ✓     │      —      │    —     │    —     │
 * │ YAML parity              │    ✓     │      —      │    —     │    —     │
 * └──────────────────────────┴──────────┴─────────────┴──────────┴──────────┘
 *
 * Choosing an example:
 *
 * - "I need a supervision hierarchy"        → Start with Gastown
 * - "I need a flat team with shared caps"   → Start with Review Team
 * - "I need tiered escalation"              → Start with Customer Support
 * - "I need cross-department delegation"    → Start with Clinical Triage
 * - "I need YAML-driven org structure"      → See YamlOrgExample
 * - "I need all the features"              → Combine Clinical (most features)
 *                                             with Gastown (deep supervision)
 * </pre>
 */
class CapabilityMatrixTest {

    @Test void matrixIsDocumentation() {
        // This class exists as navigable documentation.
        // The matrix above guides users to the right example.
        // Each example is a runnable test that validates the pattern.
    }
}
