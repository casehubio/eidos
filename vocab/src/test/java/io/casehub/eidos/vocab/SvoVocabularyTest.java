package io.casehub.eidos.vocab;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class SvoVocabularyTest {

    final SvoVocabularyProducer producer = new SvoVocabularyProducer();

    @Test
    void uri_is_correct() {
        assertThat(producer.svoVocabulary().uri()).isEqualTo("urn:casehub:vocab:svo");
    }

    @Test
    void has_performer_evaluator_coordinator_terms() {
        var terms = producer.svoVocabulary().terms();
        assertThat(terms).containsKeys("performer", "evaluator", "coordinator");
    }

    @Test
    void performer_has_expected_aliases() {
        var term = producer.svoVocabulary().terms().get("performer");
        assertThat(term.aliases()).contains("actor", "executor");
    }

    @Test
    void evaluator_maps_to_casehub_slot_reviewer() {
        var term = producer.svoVocabulary().terms().get("evaluator");
        assertThat(term.exactMatches().get(CasehubSlotVocabularyProducer.URI)).isEqualTo("reviewer");
    }

    @Test
    void coordinator_maps_to_casehub_slot_planner() {
        var term = producer.svoVocabulary().terms().get("coordinator");
        assertThat(term.exactMatches().get(CasehubSlotVocabularyProducer.URI)).isEqualTo("planner");
    }

    @Test
    void performer_maps_to_casehub_slot_executor() {
        var term = producer.svoVocabulary().terms().get("performer");
        assertThat(term.exactMatches().get(CasehubSlotVocabularyProducer.URI)).isEqualTo("executor");
    }
}
