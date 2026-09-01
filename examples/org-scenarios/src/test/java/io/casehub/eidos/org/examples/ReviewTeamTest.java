package io.casehub.eidos.org.examples;

import io.casehub.eidos.org.api.OrgStructure;
import io.casehub.eidos.org.memory.InMemoryOrgRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Code Review Team — flat team with collective capabilities.
 *
 * Demonstrates: flat team (no hierarchy), collective capabilities,
 * team goals, membership roles, no supervision relationships.
 *
 * Three reviewers form a team whose collective capability
 * "full-stack-review" emerges from the combination — no single
 * member has it alone.
 */
class ReviewTeamTest {

    @Test void flatReviewTeam() {
        var devtown = OrgStructure.define("devtown")

            .unit("review-team").name("Code Review Team").kind("team")
                .member("structural-reviewer", "structural")
                .member("content-reviewer", "content")
                .member("readability-reviewer", "readability")
                .member("completeness-reviewer", "completeness")
                .capability("full-stack-review")
                .capability("document-review")
                .add()

            .build();

        var registry = new InMemoryOrgRegistry();
        devtown.registerAll(registry);

        // Flat team — no relationships needed
        assertThat(devtown.relationships()).isEmpty();

        // Collective capabilities
        var team = registry.findUnit("review-team", "devtown").orElseThrow();
        assertThat(team.capabilities()).hasSize(2);

        // All four reviewers are members
        assertThat(registry.membersOf("review-team", "devtown")).hasSize(4);

        // Each reviewer belongs to exactly one team
        assertThat(registry.unitsFor("structural-reviewer", "devtown")).hasSize(1);
    }
}
