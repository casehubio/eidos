package io.casehub.eidos.vocab;

import io.casehub.eidos.api.VocabularyMetadata;
import io.casehub.eidos.api.VocabularyTerm;

import java.util.List;
import java.util.Optional;

@VocabularyMetadata(uri = "urn:casehub:vocab:casehub-slot",
                    name = "CaseHub Slot Roles", version = "1.0",
                    description = "CaseHub's native slot vocabulary defining four platform-standard roles: Planner, Executor, Reviewer, Supervisor. Use when an external team-role framework is not required.")
public enum CasehubSlotTerm implements VocabularyTerm {

    PLANNER("planner", "Planner", "Plans and coordinates case execution",
            List.of("orchestrator")) {
        @Override public Optional<VocabularyTerm> exactMatch(Class<?> t) {
            // Class identity is correct — Class instances are singletons per class loader
            return t == SvoTerm.class ? Optional.of(SvoTerm.COORDINATOR) : Optional.empty();
        }
    },

    REVIEWER("reviewer", "Reviewer", "Evaluates outputs for quality",
             List.of("evaluator", "judge")) {
        @Override public Optional<VocabularyTerm> exactMatch(Class<?> t) {
            return t == SvoTerm.class ? Optional.of(SvoTerm.EVALUATOR) : Optional.empty();
        }
    },

    EXECUTOR("executor", "Executor", "Executes assigned tasks",
             List.of("performer")) {
        @Override public Optional<VocabularyTerm> exactMatch(Class<?> t) {
            return t == SvoTerm.class ? Optional.of(SvoTerm.PERFORMER) : Optional.empty();
        }
    },

    SUPERVISOR("supervisor", "Supervisor", "Oversees and governs agent behaviour",
               List.of("overseer"));

    public static final String URI = "urn:casehub:vocab:casehub-slot";

    private final String value, label, description;
    private final List<String> aliases;

    CasehubSlotTerm(String value, String label, String description, List<String> aliases) {
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
