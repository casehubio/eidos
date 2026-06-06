package io.casehub.eidos.vocab;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class CasehubSlotVocabularyTest {

    @Test
    void uri_constant_is_correct() {
        assertThat(CasehubSlotTerm.URI).isEqualTo("urn:casehub:vocab:casehub-slot");
    }

    @Test
    void has_four_terms() {
        assertThat(CasehubSlotTerm.values()).hasSize(4);
    }

    @Test
    void reviewer_maps_to_svo_evaluator() {
        assertThat(CasehubSlotTerm.REVIEWER.exactMatch(SvoTerm.class))
            .contains(SvoTerm.EVALUATOR);
    }

    @Test
    void planner_maps_to_svo_coordinator() {
        assertThat(CasehubSlotTerm.PLANNER.exactMatch(SvoTerm.class))
            .contains(SvoTerm.COORDINATOR);
    }

    @Test
    void executor_maps_to_svo_performer() {
        assertThat(CasehubSlotTerm.EXECUTOR.exactMatch(SvoTerm.class))
            .contains(SvoTerm.PERFORMER);
    }

    @Test
    void supervisor_has_no_svo_match() {
        assertThat(CasehubSlotTerm.SUPERVISOR.exactMatch(SvoTerm.class)).isEmpty();
    }

    @Test
    void aliases_preserved() {
        assertThat(CasehubSlotTerm.PLANNER.aliases()).contains("orchestrator");
        assertThat(CasehubSlotTerm.REVIEWER.aliases()).containsExactlyInAnyOrder("evaluator", "judge");
        assertThat(CasehubSlotTerm.EXECUTOR.aliases()).contains("performer");
        assertThat(CasehubSlotTerm.SUPERVISOR.aliases()).contains("overseer");
    }

    @Test
    void bidirectional_consistency() {
        var reviewerMatch = CasehubSlotTerm.REVIEWER.exactMatch(SvoTerm.class);
        assertThat(reviewerMatch).isPresent();
        var backMatch = reviewerMatch.get().exactMatch(CasehubSlotTerm.class);
        assertThat(backMatch).contains(CasehubSlotTerm.REVIEWER);
    }
}
