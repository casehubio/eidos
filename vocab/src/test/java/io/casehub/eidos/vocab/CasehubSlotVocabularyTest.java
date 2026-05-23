package io.casehub.eidos.vocab;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class CasehubSlotVocabularyTest {

    final CasehubSlotVocabularyProducer producer = new CasehubSlotVocabularyProducer();

    @Test
    void uri_is_correct() {
        assertThat(producer.casehubSlotVocabulary().uri())
            .isEqualTo("urn:casehub:vocab:casehub-slot");
    }

    @Test
    void has_four_slots() {
        assertThat(producer.casehubSlotVocabulary().terms())
            .containsKeys("planner", "reviewer", "executor", "supervisor");
    }

    @Test
    void reviewer_maps_to_svo_evaluator() {
        var term = producer.casehubSlotVocabulary().terms().get("reviewer");
        assertThat(term.exactMatches().get(SvoVocabularyProducer.URI)).isEqualTo("evaluator");
    }

    @Test
    void planner_maps_to_svo_coordinator() {
        var term = producer.casehubSlotVocabulary().terms().get("planner");
        assertThat(term.exactMatches().get(SvoVocabularyProducer.URI)).isEqualTo("coordinator");
    }

    @Test
    void executor_maps_to_svo_performer() {
        var term = producer.casehubSlotVocabulary().terms().get("executor");
        assertThat(term.exactMatches().get(SvoVocabularyProducer.URI)).isEqualTo("performer");
    }

    @Test
    void supervisor_has_no_svo_exact_match() {
        var term = producer.casehubSlotVocabulary().terms().get("supervisor");
        assertThat(term.exactMatches()).doesNotContainKey(SvoVocabularyProducer.URI);
    }

    @Test
    void cross_reference_consistency_svo_to_slot_and_back() {
        var svo = new SvoVocabularyProducer().svoVocabulary();
        var slot = producer.casehubSlotVocabulary();

        String svoEvaluatorMapsToSlot = svo.terms().get("evaluator")
            .exactMatches().get(CasehubSlotVocabularyProducer.URI);
        String slotReviewerMapsToSvo = slot.terms().get(svoEvaluatorMapsToSlot)
            .exactMatches().get(SvoVocabularyProducer.URI);

        assertThat(slotReviewerMapsToSvo).isEqualTo("evaluator");
    }
}
