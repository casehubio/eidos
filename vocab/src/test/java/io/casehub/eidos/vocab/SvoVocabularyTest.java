package io.casehub.eidos.vocab;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class SvoVocabularyTest {

    @Test
    void uri_constant_is_correct() {
        assertThat(SvoTerm.URI).isEqualTo("urn:casehub:vocab:svo");
    }

    @Test
    void has_three_terms() {
        assertThat(SvoTerm.values()).hasSize(3);
    }

    @Test
    void performer_aliases_preserved() {
        assertThat(SvoTerm.PERFORMER.aliases()).containsExactlyInAnyOrder("actor", "executor");
    }

    @Test
    void evaluator_aliases_preserved() {
        assertThat(SvoTerm.EVALUATOR.aliases()).containsExactlyInAnyOrder("reviewer", "judge");
    }

    @Test
    void coordinator_aliases_preserved() {
        assertThat(SvoTerm.COORDINATOR.aliases()).containsExactlyInAnyOrder("planner", "orchestrator");
    }

    @Test
    void evaluator_maps_to_casehub_slot_reviewer() {
        var match = SvoTerm.EVALUATOR.exactMatch(CasehubSlotTerm.class);
        assertThat(match).contains(CasehubSlotTerm.REVIEWER);
    }

    @Test
    void coordinator_maps_to_casehub_slot_planner() {
        assertThat(SvoTerm.COORDINATOR.exactMatch(CasehubSlotTerm.class))
            .contains(CasehubSlotTerm.PLANNER);
    }

    @Test
    void performer_maps_to_casehub_slot_executor() {
        assertThat(SvoTerm.PERFORMER.exactMatch(CasehubSlotTerm.class))
            .contains(CasehubSlotTerm.EXECUTOR);
    }

    @Test
    void unknown_target_vocab_returns_empty() {
        assertThat(SvoTerm.EVALUATOR.exactMatch(ConscientiousnessTerm.class)).isEmpty();
    }

    @Test
    void descriptions_preserved() {
        assertThat(SvoTerm.PERFORMER.description()).isEqualTo("Executes the assigned work");
        assertThat(SvoTerm.EVALUATOR.description()).isEqualTo("Assesses quality of work");
        assertThat(SvoTerm.COORDINATOR.description()).isEqualTo("Orchestrates other agents");
    }
}
