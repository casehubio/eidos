package io.casehub.eidos.vocab;

import io.casehub.eidos.api.DispositionAxis;
import io.casehub.eidos.api.VocabularyMetadata;
import io.casehub.eidos.api.VocabularyTerm;

import java.util.List;
import java.util.Optional;

@VocabularyMetadata(uri = "urn:casehub:vocab:disc",
                    name = "DISC Behavioral Styles", version = "1.0",
                    description = "A four-quadrant behavioral style model (Dominance, Influence, Steadiness, Conscientiousness-DISC) used as a disposition shorthand. Correlates with Big Five Extraversion × Agreeableness. Low independent scientific validity, but bounded imprecision makes it usable in practice.")
public enum DiscTerm implements VocabularyTerm {

    DOMINANCE("dominance", "Dominance",
              "Results-driven, direct, decisive; prioritises outcomes over relationships",
              List.of("D")) {
        @Override
        public Optional<VocabularyTerm> axisExactMatch(Class<?> targetVocab, DispositionAxis axis) {
            if (targetVocab == ConscientiousnessTerm.class) {
                return switch (axis) {
                    case SOCIAL_ORIENTATION -> Optional.of(ConscientiousnessTerm.INDEPENDENT);
                    case RULE_FOLLOWING     -> Optional.of(ConscientiousnessTerm.FLEXIBLE);
                    case RISK_APPETITE      -> Optional.of(ConscientiousnessTerm.BOLD);
                    case AUTONOMY           -> Optional.of(ConscientiousnessTerm.AUTONOMOUS);
                    case CONFLICT_MODE      -> Optional.empty();
                };
            }
            if (targetVocab == ThomasKilmannTerm.class) {
                return switch (axis) {
                    case CONFLICT_MODE                                               -> Optional.of(ThomasKilmannTerm.COMPETING);
                    case SOCIAL_ORIENTATION, RULE_FOLLOWING, RISK_APPETITE, AUTONOMY -> Optional.empty();
                };
            }
            return Optional.empty();
        }
    },

    INFLUENCE("influence", "Influence",
              "Enthusiastic, optimistic, collaborative; motivates and involves others",
              List.of("i")) {
        @Override
        public Optional<VocabularyTerm> axisExactMatch(Class<?> targetVocab, DispositionAxis axis) {
            if (targetVocab == ConscientiousnessTerm.class) {
                return switch (axis) {
                    case SOCIAL_ORIENTATION -> Optional.of(ConscientiousnessTerm.COLLABORATIVE);
                    case RULE_FOLLOWING     -> Optional.of(ConscientiousnessTerm.FLEXIBLE);
                    case RISK_APPETITE      -> Optional.of(ConscientiousnessTerm.MEASURED);
                    case AUTONOMY           -> Optional.of(ConscientiousnessTerm.SEMI_AUTONOMOUS);
                    case CONFLICT_MODE      -> Optional.empty();
                };
            }
            if (targetVocab == ThomasKilmannTerm.class) {
                return switch (axis) {
                    case CONFLICT_MODE                                               -> Optional.of(ThomasKilmannTerm.COLLABORATING);
                    case SOCIAL_ORIENTATION, RULE_FOLLOWING, RISK_APPETITE, AUTONOMY -> Optional.empty();
                };
            }
            return Optional.empty();
        }

        @Override public boolean impliesSupervision() { return true; }
    },

    STEADINESS("steadiness", "Steadiness",
               "Patient, reliable, supportive; values stability and consistency",
               List.of("S")) {
        @Override
        public Optional<VocabularyTerm> axisExactMatch(Class<?> targetVocab, DispositionAxis axis) {
            if (targetVocab == ConscientiousnessTerm.class) {
                return switch (axis) {
                    case SOCIAL_ORIENTATION -> Optional.of(ConscientiousnessTerm.FACILITATIVE);
                    case RULE_FOLLOWING     -> Optional.of(ConscientiousnessTerm.PRINCIPLED);
                    case RISK_APPETITE      -> Optional.of(ConscientiousnessTerm.CONSERVATIVE);
                    case AUTONOMY           -> Optional.of(ConscientiousnessTerm.DIRECTED);
                    case CONFLICT_MODE      -> Optional.empty();
                };
            }
            if (targetVocab == ThomasKilmannTerm.class) {
                return switch (axis) {
                    case CONFLICT_MODE                                               -> Optional.of(ThomasKilmannTerm.ACCOMMODATING);
                    case SOCIAL_ORIENTATION, RULE_FOLLOWING, RISK_APPETITE, AUTONOMY -> Optional.empty();
                };
            }
            return Optional.empty();
        }

        @Override public boolean impliesSupervision() { return true; }
    },

    CONSCIENTIOUSNESS_DISC("conscientiousness-disc", "Analytical (DISC-C)",
                           "Analytical, systematic, quality-focused; emphasises accuracy",
                           List.of("C")) {
        @Override
        public Optional<VocabularyTerm> axisExactMatch(Class<?> targetVocab, DispositionAxis axis) {
            if (targetVocab == ConscientiousnessTerm.class) {
                return switch (axis) {
                    case SOCIAL_ORIENTATION -> Optional.of(ConscientiousnessTerm.INDEPENDENT);
                    case RULE_FOLLOWING     -> Optional.of(ConscientiousnessTerm.STRICT);
                    case RISK_APPETITE      -> Optional.of(ConscientiousnessTerm.CONSERVATIVE);
                    case AUTONOMY           -> Optional.of(ConscientiousnessTerm.SEMI_AUTONOMOUS);
                    case CONFLICT_MODE      -> Optional.empty();
                };
            }
            if (targetVocab == ThomasKilmannTerm.class) {
                return switch (axis) {
                    case CONFLICT_MODE                                               -> Optional.of(ThomasKilmannTerm.AVOIDING);
                    case SOCIAL_ORIENTATION, RULE_FOLLOWING, RISK_APPETITE, AUTONOMY -> Optional.empty();
                };
            }
            return Optional.empty();
        }

        @Override public boolean impliesSupervision() { return true; }
    };

    public static final String URI = "urn:casehub:vocab:disc";

    private final String value, label, description;
    private final List<String> aliases;

    DiscTerm(String value, String label, String description, List<String> aliases) {
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
