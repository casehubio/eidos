package io.casehub.eidos.vocab;

import io.casehub.eidos.api.Vocabulary;
import io.casehub.eidos.api.VocabularyTerm;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class SvoVocabularyProducer {

    public static final String URI = "urn:casehub:vocab:svo";

    @Produces
    @ApplicationScoped
    public Vocabulary svoVocabulary() {
        return new Vocabulary(URI, "SVO Roles", "1.0", Map.of(
            "performer", new VocabularyTerm("performer", "Performer",
                "Executes the assigned work",
                List.of("actor", "executor"),
                Map.of(CasehubSlotVocabularyProducer.URI, "executor")),
            "evaluator", new VocabularyTerm("evaluator", "Evaluator",
                "Assesses quality of work",
                List.of("reviewer", "judge"),
                Map.of(CasehubSlotVocabularyProducer.URI, "reviewer")),
            "coordinator", new VocabularyTerm("coordinator", "Coordinator",
                "Orchestrates other agents",
                List.of("planner", "orchestrator"),
                Map.of(CasehubSlotVocabularyProducer.URI, "planner"))
        ));
    }
}
