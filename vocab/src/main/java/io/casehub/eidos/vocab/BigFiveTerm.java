package io.casehub.eidos.vocab;

import io.casehub.eidos.api.DispositionAxis;
import io.casehub.eidos.api.VocabularyMetadata;
import io.casehub.eidos.api.VocabularyTerm;

import java.util.List;
import java.util.Optional;

@VocabularyMetadata(uri = "urn:casehub:vocab:big-five",
                    name = "Big Five (OCEAN)", version = "1.0",
                    description = "Five broad personality dimensions from factor analysis across cultures (Costa & McCrae, 1992). The most replicated personality model in psychology. High/Low poles for all five dimensions. The Conscientiousness vocabulary provides finer-grained axis value terms; this vocabulary models the Big Five dimension poles for personality profile input.")
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
    },

    CONSCIENTIOUSNESS_HIGH("conscientiousness-high", "Conscientiousness (High)",
        "Organised, disciplined, goal-directed; follows through on commitments",
        List.of("C+")) {
        @Override public Optional<VocabularyTerm> axisExactMatch(Class<?> tv, DispositionAxis axis) {
            if (tv == ConscientiousnessTerm.class) return switch (axis) {
                case RULE_FOLLOWING -> Optional.of(ConscientiousnessTerm.STRICT);
                case RISK_APPETITE  -> Optional.of(ConscientiousnessTerm.CONSERVATIVE);
                case AUTONOMY       -> Optional.of(ConscientiousnessTerm.SEMI_AUTONOMOUS);
                case SOCIAL_ORIENTATION, CONFLICT_MODE -> Optional.empty();
            };
            return Optional.empty();
        }
    },
    CONSCIENTIOUSNESS_LOW("conscientiousness-low", "Conscientiousness (Low)",
        "Flexible, spontaneous, adaptable; comfortable with ambiguity",
        List.of("C-")) {
        @Override public Optional<VocabularyTerm> axisExactMatch(Class<?> tv, DispositionAxis axis) {
            if (tv == ConscientiousnessTerm.class) return switch (axis) {
                case RULE_FOLLOWING -> Optional.of(ConscientiousnessTerm.FLEXIBLE);
                case RISK_APPETITE  -> Optional.of(ConscientiousnessTerm.MEASURED);
                case AUTONOMY       -> Optional.of(ConscientiousnessTerm.AUTONOMOUS);
                case SOCIAL_ORIENTATION, CONFLICT_MODE -> Optional.empty();
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

    @Override
    public double axisSignalWeight(DispositionAxis axis) {
        return switch (this) {
            case OPENNESS_HIGH -> switch (axis) {
                case RISK_APPETITE -> 0.6;
                default -> 0.0;
            };
            case OPENNESS_LOW -> switch (axis) {
                case RISK_APPETITE -> -0.5;
                default -> 0.0;
            };
            case EXTRAVERSION_HIGH -> switch (axis) {
                case SOCIAL_ORIENTATION -> 0.7;
                default -> 0.0;
            };
            case EXTRAVERSION_LOW -> switch (axis) {
                case SOCIAL_ORIENTATION -> -0.6;
                default -> 0.0;
            };
            case AGREEABLENESS_HIGH -> switch (axis) {
                case CONFLICT_MODE -> -0.7;
                default -> 0.0;
            };
            case AGREEABLENESS_LOW -> switch (axis) {
                case CONFLICT_MODE -> 0.6;
                default -> 0.0;
            };
            case NEUROTICISM_HIGH -> switch (axis) {
                case AUTONOMY -> -0.4;
                default -> 0.0;
            };
            case NEUROTICISM_LOW -> switch (axis) {
                case AUTONOMY -> 0.3;
                default -> 0.0;
            };
            case CONSCIENTIOUSNESS_HIGH -> switch (axis) {
                case RULE_FOLLOWING -> 0.7;
                default -> 0.0;
            };
            case CONSCIENTIOUSNESS_LOW -> switch (axis) {
                case RULE_FOLLOWING -> -0.6;
                default -> 0.0;
            };
        };
    }

    @Override
    public String detailedDescription() {
        return switch (this) {
            case OPENNESS_HIGH -> "High Openness individuals are drawn to new ideas, creative expression, and unconventional approaches. They enjoy abstract thinking, are intellectually curious, and adapt readily to change. In work settings, they contribute innovation and flexibility but may resist routine or become bored with repetitive tasks.";
            case OPENNESS_LOW -> "Low Openness individuals prefer concrete, practical approaches over abstract theory. They value tradition, consistency, and proven methods. In work settings, they contribute stability and reliability but may resist change or dismiss unconventional ideas too quickly.";
            case EXTRAVERSION_HIGH -> "High Extraversion individuals are energised by social interaction, assertive in groups, and comfortable being the centre of attention. They think by talking, build broad networks, and maintain high energy. In work settings, they contribute enthusiasm and social drive but may dominate conversations or struggle with solitary deep work.";
            case EXTRAVERSION_LOW -> "Low Extraversion individuals are reflective, reserved, and energised by time alone. They think before speaking, build deep rather than broad relationships, and prefer focused work to group activities. In work settings, they contribute thoughtful analysis and deep focus but may be overlooked in group settings or perceived as aloof.";
            case AGREEABLENESS_HIGH -> "High Agreeableness individuals are warm, cooperative, and motivated to maintain positive relationships. They trust others, avoid conflict, and prioritise group harmony. In work settings, they contribute team cohesion and collaboration but may avoid necessary confrontation or defer too readily to others.";
            case AGREEABLENESS_LOW -> "Low Agreeableness individuals are direct, competitive, and willing to challenge others. They prioritise accuracy over harmony and are comfortable with disagreement. In work settings, they contribute honest feedback and rigorous debate but may create friction or be perceived as abrasive.";
            case NEUROTICISM_HIGH -> "High Neuroticism individuals experience emotions intensely and are more sensitive to stress, criticism, and setbacks. They may anticipate problems that others miss. In work settings, they can contribute heightened awareness of risks and emotional nuance but may need more support during high-pressure periods.";
            case NEUROTICISM_LOW -> "Low Neuroticism individuals remain calm and composed under stress. They recover quickly from setbacks and maintain steady emotional equilibrium. In work settings, they contribute stability and resilience under pressure but may underestimate the emotional impact of situations on others.";
            case CONSCIENTIOUSNESS_HIGH -> "High Conscientiousness individuals are systematic, dependable, and driven to achieve. They plan ahead, follow through on commitments, and maintain high standards. In work settings, they contribute reliability and quality but may become rigid or overly focused on details.";
            case CONSCIENTIOUSNESS_LOW -> "Low Conscientiousness individuals are flexible, spontaneous, and comfortable with ambiguity. They adapt quickly and can pivot without distress. In work settings, they contribute adaptability and creative freedom but may struggle with deadlines, follow-through, or detailed planning.";
        };
    }


}
