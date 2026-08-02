package io.casehub.eidos.vocab;

import io.casehub.eidos.api.DispositionAxis;
import io.casehub.eidos.api.VocabularyMetadata;
import io.casehub.eidos.api.VocabularyTerm;

import java.util.List;
import java.util.Optional;

@VocabularyMetadata(uri = "urn:casehub:vocab:big-five",
                    name = "Big Five (OCEAN)", version = "1.0",
                    description = "Five broad personality dimensions from factor analysis across cultures (Costa & McCrae, 1992). The most replicated personality model in psychology. High/Low poles per dimension. Conscientiousness axis terms already exist in the Conscientiousness vocabulary; this vocabulary provides the remaining four factors.")
public enum BigFiveTerm implements VocabularyTerm {

    OPENNESS_HIGH("openness-high", "Openness (High)",
        "Intellectually curious, creative, open to novel experiences and ideas",
        List.of("O+")) {
        @Override public Optional<VocabularyTerm> axisExactMatch(Class<?> tv, DispositionAxis axis) {
            if (tv == ConscientiousnessTerm.class) return switch (axis) {
                case RULE_FOLLOWING -> Optional.of(ConscientiousnessTerm.FLEXIBLE);
                case RISK_APPETITE  -> Optional.of(ConscientiousnessTerm.BOLD);
                case AUTONOMY       -> Optional.of(ConscientiousnessTerm.AUTONOMOUS);
                case SOCIAL_ORIENTATION, CONFLICT_MODE -> Optional.empty();
            };
            return Optional.empty();
        }
    },
    OPENNESS_LOW("openness-low", "Openness (Low)",
        "Conventional, practical, prefers routine and established approaches",
        List.of("O-")) {
        @Override public Optional<VocabularyTerm> axisExactMatch(Class<?> tv, DispositionAxis axis) {
            if (tv == ConscientiousnessTerm.class) return switch (axis) {
                case RULE_FOLLOWING -> Optional.of(ConscientiousnessTerm.STRICT);
                case RISK_APPETITE  -> Optional.of(ConscientiousnessTerm.CONSERVATIVE);
                case AUTONOMY       -> Optional.of(ConscientiousnessTerm.DIRECTED);
                case SOCIAL_ORIENTATION, CONFLICT_MODE -> Optional.empty();
            };
            return Optional.empty();
        }
    },

    EXTRAVERSION_HIGH("extraversion-high", "Extraversion (High)",
        "Energetic, sociable, assertive; draws energy from external interaction",
        List.of("E+")) {
        @Override public Optional<VocabularyTerm> axisExactMatch(Class<?> tv, DispositionAxis axis) {
            if (tv == ConscientiousnessTerm.class) return switch (axis) {
                case SOCIAL_ORIENTATION -> Optional.of(ConscientiousnessTerm.COLLABORATIVE);
                case RULE_FOLLOWING, RISK_APPETITE, AUTONOMY, CONFLICT_MODE -> Optional.empty();
            };
            if (tv == ThomasKilmannTerm.class) return switch (axis) {
                case CONFLICT_MODE -> Optional.of(ThomasKilmannTerm.COLLABORATING);
                default            -> Optional.empty();
            };
            return Optional.empty();
        }
    },
    EXTRAVERSION_LOW("extraversion-low", "Extraversion (Low / Introversion)",
        "Reserved, reflective, independent; draws energy from internal processing",
        List.of("E-")) {
        @Override public Optional<VocabularyTerm> axisExactMatch(Class<?> tv, DispositionAxis axis) {
            if (tv == ConscientiousnessTerm.class) return switch (axis) {
                case SOCIAL_ORIENTATION -> Optional.of(ConscientiousnessTerm.INDEPENDENT);
                case RULE_FOLLOWING, RISK_APPETITE, AUTONOMY, CONFLICT_MODE -> Optional.empty();
            };
            if (tv == ThomasKilmannTerm.class) return switch (axis) {
                case CONFLICT_MODE -> Optional.of(ThomasKilmannTerm.AVOIDING);
                default            -> Optional.empty();
            };
            return Optional.empty();
        }
    },

    AGREEABLENESS_HIGH("agreeableness-high", "Agreeableness (High)",
        "Cooperative, trusting, empathetic; prioritises group harmony over personal gain",
        List.of("A+")) {
        @Override public Optional<VocabularyTerm> axisExactMatch(Class<?> tv, DispositionAxis axis) {
            if (tv == ConscientiousnessTerm.class) return switch (axis) {
                case SOCIAL_ORIENTATION -> Optional.of(ConscientiousnessTerm.FACILITATIVE);
                case RULE_FOLLOWING, RISK_APPETITE, AUTONOMY, CONFLICT_MODE -> Optional.empty();
            };
            if (tv == ThomasKilmannTerm.class) return switch (axis) {
                case CONFLICT_MODE -> Optional.of(ThomasKilmannTerm.ACCOMMODATING);
                default            -> Optional.empty();
            };
            return Optional.empty();
        }
        @Override public boolean impliesSupervision() { return true; }
    },
    AGREEABLENESS_LOW("agreeableness-low", "Agreeableness (Low)",
        "Competitive, skeptical, challenging; prioritises outcomes over social harmony",
        List.of("A-")) {
        @Override public Optional<VocabularyTerm> axisExactMatch(Class<?> tv, DispositionAxis axis) {
            if (tv == ConscientiousnessTerm.class) return switch (axis) {
                case SOCIAL_ORIENTATION -> Optional.of(ConscientiousnessTerm.INDEPENDENT);
                case RULE_FOLLOWING, RISK_APPETITE, AUTONOMY, CONFLICT_MODE -> Optional.empty();
            };
            if (tv == ThomasKilmannTerm.class) return switch (axis) {
                case CONFLICT_MODE -> Optional.of(ThomasKilmannTerm.COMPETING);
                default            -> Optional.empty();
            };
            return Optional.empty();
        }
    },

    NEUROTICISM_HIGH("neuroticism-high", "Neuroticism (High / Low Emotional Stability)",
        "Emotionally reactive, anxiety-prone, sensitive to stressors",
        List.of("N+")) {
        @Override public Optional<VocabularyTerm> axisExactMatch(Class<?> tv, DispositionAxis axis) {
            if (tv == ConscientiousnessTerm.class) return switch (axis) {
                case RISK_APPETITE -> Optional.of(ConscientiousnessTerm.CONSERVATIVE);
                case SOCIAL_ORIENTATION, RULE_FOLLOWING, AUTONOMY, CONFLICT_MODE -> Optional.empty();
            };
            return Optional.empty();
        }
    },
    NEUROTICISM_LOW("neuroticism-low", "Neuroticism (Low / High Emotional Stability)",
        "Emotionally stable, calm under pressure, resilient to stressors",
        List.of("N-")) {
        @Override public Optional<VocabularyTerm> axisExactMatch(Class<?> tv, DispositionAxis axis) {
            if (tv == ConscientiousnessTerm.class) return switch (axis) {
                case RISK_APPETITE -> Optional.of(ConscientiousnessTerm.MEASURED);
                case SOCIAL_ORIENTATION, RULE_FOLLOWING, AUTONOMY, CONFLICT_MODE -> Optional.empty();
            };
            return Optional.empty();
        }
    };

    public static final String URI = "urn:casehub:vocab:big-five";

    private final String value, label, description;
    private final List<String> aliases;

    BigFiveTerm(String value, String label, String description, List<String> aliases) {
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
