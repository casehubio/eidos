package io.casehub.eidos.vocab;

import io.casehub.eidos.api.Vocabulary;
import io.casehub.eidos.api.VocabularyTerm;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class CasehubSlotVocabularyProducer {

    public static final String URI = "urn:casehub:vocab:casehub-slot";

    @Produces
    @ApplicationScoped
    public Vocabulary casehubSlotVocabulary() {
        return new Vocabulary(URI, "CaseHub Slot Roles", "1.0", Map.of(
            "planner",    new VocabularyTerm("planner", "Planner",
                "Plans and coordinates case execution",
                List.of("orchestrator"),
                Map.of(SvoVocabularyProducer.URI, "coordinator")),
            "reviewer",   new VocabularyTerm("reviewer", "Reviewer",
                "Evaluates outputs for quality",
                List.of("evaluator", "judge"),
                Map.of(SvoVocabularyProducer.URI, "evaluator")),
            "executor",   new VocabularyTerm("executor", "Executor",
                "Executes assigned tasks",
                List.of("performer"),
                Map.of(SvoVocabularyProducer.URI, "performer")),
            "supervisor", new VocabularyTerm("supervisor", "Supervisor",
                "Oversees and governs agent behaviour",
                List.of("overseer"),
                Map.of())
        ));
    }
}
