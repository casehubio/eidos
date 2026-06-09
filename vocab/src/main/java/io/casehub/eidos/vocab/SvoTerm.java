package io.casehub.eidos.vocab;

import io.casehub.eidos.api.VocabularyMetadata;
import io.casehub.eidos.api.VocabularyTerm;

import java.util.List;
import java.util.Optional;

@VocabularyMetadata(uri = "urn:casehub:vocab:svo", name = "SVO Roles", version = "1.0",
                    description = "A simplified three-role model (Coordinator, Performer, Evaluator) for agent function in multi-agent workflows. Derived from Subject-Verb-Object role theory. Intended as a lightweight slot vocabulary.")
public enum SvoTerm implements VocabularyTerm {

    COORDINATOR("coordinator", "Coordinator", "Orchestrates other agents",
                List.of("planner", "orchestrator")) {
        @Override public Optional<VocabularyTerm> exactMatch(Class<?> t) {
            // Class identity is correct — Class instances are singletons per class loader
            return t == CasehubSlotTerm.class
                ? Optional.of(CasehubSlotTerm.PLANNER)
                : Optional.empty();
        }
    },

    PERFORMER("performer", "Performer", "Executes the assigned work",
              List.of("actor", "executor")) {
        @Override public Optional<VocabularyTerm> exactMatch(Class<?> t) {
            return t == CasehubSlotTerm.class
                ? Optional.of(CasehubSlotTerm.EXECUTOR)
                : Optional.empty();
        }
    },

    EVALUATOR("evaluator", "Evaluator", "Assesses quality of work",
              List.of("reviewer", "judge")) {
        @Override public Optional<VocabularyTerm> exactMatch(Class<?> t) {
            return t == CasehubSlotTerm.class
                ? Optional.of(CasehubSlotTerm.REVIEWER)
                : Optional.empty();
        }
    };

    public static final String URI = "urn:casehub:vocab:svo";

    private final String value, label, description;
    private final List<String> aliases;

    SvoTerm(String value, String label, String description, List<String> aliases) {
        this.value = value;
        this.label = label;
        this.description = description;
        this.aliases = aliases;
    }

    @Override public String value()         { return value; }
    @Override public String label()         { return label; }
    @Override public String description()   { return description; }
    @Override public List<String> aliases() { return aliases; }
}
