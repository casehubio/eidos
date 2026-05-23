package io.casehub.eidos.examples;

import io.casehub.eidos.api.*;
import io.casehub.eidos.vocab.CasehubSlotVocabularyProducer;
import io.casehub.eidos.vocab.SvoVocabularyProducer;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@QuarkusTest
class CrossVocabularyDiscoveryTest {

    @Inject VocabularyRegistry vocabRegistry;

    @Test
    void svo_and_casehub_slot_vocabularies_are_discoverable() {
        assertThat(vocabRegistry.find(SvoVocabularyProducer.URI)).isPresent();
        assertThat(vocabRegistry.find(CasehubSlotVocabularyProducer.URI)).isPresent();
    }

    @Test
    void svo_evaluator_is_equivalent_to_casehub_slot_reviewer() {
        var equivalents = vocabRegistry.equivalentValues(
            SvoVocabularyProducer.URI, "evaluator", CasehubSlotVocabularyProducer.URI);
        assertThat(equivalents).containsExactly("reviewer");
    }

    @Test
    void casehub_slot_reviewer_is_equivalent_to_svo_evaluator() {
        var equivalents = vocabRegistry.equivalentValues(
            CasehubSlotVocabularyProducer.URI, "reviewer", SvoVocabularyProducer.URI);
        assertThat(equivalents).containsExactly("evaluator");
    }

    @Test
    void cross_reference_is_bidirectional_for_all_pairs() {
        assertThat(vocabRegistry.equivalentValues(
            SvoVocabularyProducer.URI, "coordinator", CasehubSlotVocabularyProducer.URI))
            .containsExactly("planner");
        assertThat(vocabRegistry.equivalentValues(
            CasehubSlotVocabularyProducer.URI, "planner", SvoVocabularyProducer.URI))
            .containsExactly("coordinator");
        assertThat(vocabRegistry.equivalentValues(
            SvoVocabularyProducer.URI, "performer", CasehubSlotVocabularyProducer.URI))
            .containsExactly("executor");
        assertThat(vocabRegistry.equivalentValues(
            CasehubSlotVocabularyProducer.URI, "executor", SvoVocabularyProducer.URI))
            .containsExactly("performer");
    }

    @Test
    void resolve_term_by_alias() {
        var term = vocabRegistry.resolve(SvoVocabularyProducer.URI, "reviewer");
        assertThat(term).isPresent();
        assertThat(term.get().value()).isEqualTo("evaluator");
        assertThat(term.get().label()).isEqualTo("Evaluator");
    }

    @Test
    void supervisor_has_no_svo_equivalent() {
        var equivalents = vocabRegistry.equivalentValues(
            CasehubSlotVocabularyProducer.URI, "supervisor", SvoVocabularyProducer.URI);
        assertThat(equivalents).isEmpty();
    }
}
