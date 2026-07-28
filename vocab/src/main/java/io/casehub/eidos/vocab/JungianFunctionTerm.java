package io.casehub.eidos.vocab;

import io.casehub.eidos.api.DispositionAxis;
import io.casehub.eidos.api.VocabularyMetadata;
import io.casehub.eidos.api.VocabularyTerm;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@VocabularyMetadata(uri = "urn:casehub:vocab:jungian",
                    name = "Jungian Cognitive Functions", version = "1.0",
                    description = "Eight Jungian cognitive functions — the foundational model for structured personality control in LLM agents. Based on JPAF (arXiv:2601.10025) which demonstrates 100% MBTI alignment via function-level specification.")
public enum JungianFunctionTerm implements VocabularyTerm {

    TI("ti", "Introverted Thinking",
       "Builds internal logical frameworks; analytical, precision-focused, seeking internal consistency",
       FunctionCategory.JUDGING, FunctionAttitude.INTROVERTED) {
        @Override public Optional<VocabularyTerm> axisExactMatch(Class<?> tv, DispositionAxis axis) {
            if (tv == ConscientiousnessTerm.class) return switch (axis) {
                case SOCIAL_ORIENTATION -> Optional.of(ConscientiousnessTerm.INDEPENDENT);
                case RULE_FOLLOWING     -> Optional.of(ConscientiousnessTerm.PRINCIPLED);
                case RISK_APPETITE      -> Optional.of(ConscientiousnessTerm.MEASURED);
                case AUTONOMY           -> Optional.of(ConscientiousnessTerm.AUTONOMOUS);
                case CONFLICT_MODE      -> Optional.empty();
            };
            if (tv == ThomasKilmannTerm.class) return switch (axis) {
                case CONFLICT_MODE -> Optional.of(ThomasKilmannTerm.AVOIDING);
                default            -> Optional.empty();
            };
            return Optional.empty();
        }
    },

    TE("te", "Extraverted Thinking",
       "Applies logical organization externally; systematic, efficiency-oriented, objective decision-making",
       FunctionCategory.JUDGING, FunctionAttitude.EXTRAVERTED) {
        @Override public Optional<VocabularyTerm> axisExactMatch(Class<?> tv, DispositionAxis axis) {
            if (tv == ConscientiousnessTerm.class) return switch (axis) {
                case SOCIAL_ORIENTATION -> Optional.of(ConscientiousnessTerm.INDEPENDENT);
                case RULE_FOLLOWING     -> Optional.of(ConscientiousnessTerm.STRICT);
                case RISK_APPETITE      -> Optional.of(ConscientiousnessTerm.MEASURED);
                case AUTONOMY           -> Optional.of(ConscientiousnessTerm.SEMI_AUTONOMOUS);
                case CONFLICT_MODE      -> Optional.empty();
            };
            if (tv == ThomasKilmannTerm.class) return switch (axis) {
                case CONFLICT_MODE -> Optional.of(ThomasKilmannTerm.COMPETING);
                default            -> Optional.empty();
            };
            return Optional.empty();
        }
    },

    FI("fi", "Introverted Feeling",
       "Evaluates through deeply held personal values; authentic, individually principled moral reasoning",
       FunctionCategory.JUDGING, FunctionAttitude.INTROVERTED) {
        @Override public Optional<VocabularyTerm> axisExactMatch(Class<?> tv, DispositionAxis axis) {
            if (tv == ConscientiousnessTerm.class) return switch (axis) {
                case SOCIAL_ORIENTATION -> Optional.of(ConscientiousnessTerm.INDEPENDENT);
                case RULE_FOLLOWING     -> Optional.of(ConscientiousnessTerm.PRINCIPLED);
                case RISK_APPETITE      -> Optional.of(ConscientiousnessTerm.CONSERVATIVE);
                case AUTONOMY           -> Optional.of(ConscientiousnessTerm.AUTONOMOUS);
                case CONFLICT_MODE      -> Optional.empty();
            };
            if (tv == ThomasKilmannTerm.class) return switch (axis) {
                case CONFLICT_MODE -> Optional.of(ThomasKilmannTerm.ACCOMMODATING);
                default            -> Optional.empty();
            };
            return Optional.empty();
        }
    },

    FE("fe", "Extraverted Feeling",
       "Harmonizes group values and social dynamics; attentive to others' emotions and interpersonal harmony",
       FunctionCategory.JUDGING, FunctionAttitude.EXTRAVERTED) {
        @Override public Optional<VocabularyTerm> axisExactMatch(Class<?> tv, DispositionAxis axis) {
            if (tv == ConscientiousnessTerm.class) return switch (axis) {
                case SOCIAL_ORIENTATION -> Optional.of(ConscientiousnessTerm.FACILITATIVE);
                case RULE_FOLLOWING     -> Optional.of(ConscientiousnessTerm.FLEXIBLE);
                case RISK_APPETITE      -> Optional.of(ConscientiousnessTerm.CONSERVATIVE);
                case AUTONOMY           -> Optional.of(ConscientiousnessTerm.SEMI_AUTONOMOUS);
                case CONFLICT_MODE      -> Optional.empty();
            };
            if (tv == ThomasKilmannTerm.class) return switch (axis) {
                case CONFLICT_MODE -> Optional.of(ThomasKilmannTerm.COLLABORATING);
                default            -> Optional.empty();
            };
            return Optional.empty();
        }
    },

    SI("si", "Introverted Sensation",
       "Draws on internalized sensory impressions and past experience; focused on detailed recall and established patterns",
       FunctionCategory.PERCEIVING, FunctionAttitude.INTROVERTED) {
        @Override public Optional<VocabularyTerm> axisExactMatch(Class<?> tv, DispositionAxis axis) {
            if (tv == ConscientiousnessTerm.class) return switch (axis) {
                case SOCIAL_ORIENTATION -> Optional.of(ConscientiousnessTerm.COLLABORATIVE);
                case RULE_FOLLOWING     -> Optional.of(ConscientiousnessTerm.STRICT);
                case RISK_APPETITE      -> Optional.of(ConscientiousnessTerm.CONSERVATIVE);
                case AUTONOMY           -> Optional.of(ConscientiousnessTerm.DIRECTED);
                case CONFLICT_MODE      -> Optional.empty();
            };
            if (tv == ThomasKilmannTerm.class) return switch (axis) {
                case CONFLICT_MODE -> Optional.of(ThomasKilmannTerm.AVOIDING);
                default            -> Optional.empty();
            };
            return Optional.empty();
        }
    },

    SE("se", "Extraverted Sensation",
       "Focuses on immediate sensory data from the external environment; concrete, present-moment awareness",
       FunctionCategory.PERCEIVING, FunctionAttitude.EXTRAVERTED) {
        @Override public Optional<VocabularyTerm> axisExactMatch(Class<?> tv, DispositionAxis axis) {
            if (tv == ConscientiousnessTerm.class) return switch (axis) {
                case SOCIAL_ORIENTATION -> Optional.of(ConscientiousnessTerm.COLLABORATIVE);
                case RULE_FOLLOWING     -> Optional.of(ConscientiousnessTerm.FLEXIBLE);
                case RISK_APPETITE      -> Optional.of(ConscientiousnessTerm.BOLD);
                case AUTONOMY           -> Optional.of(ConscientiousnessTerm.SEMI_AUTONOMOUS);
                case CONFLICT_MODE      -> Optional.empty();
            };
            if (tv == ThomasKilmannTerm.class) return switch (axis) {
                case CONFLICT_MODE -> Optional.of(ThomasKilmannTerm.COMPETING);
                default            -> Optional.empty();
            };
            return Optional.empty();
        }
    },

    NI("ni", "Introverted Intuition",
       "Synthesizes internal patterns into singular insights; seeks deep underlying meanings and future implications",
       FunctionCategory.PERCEIVING, FunctionAttitude.INTROVERTED) {
        @Override public Optional<VocabularyTerm> axisExactMatch(Class<?> tv, DispositionAxis axis) {
            if (tv == ConscientiousnessTerm.class) return switch (axis) {
                case SOCIAL_ORIENTATION -> Optional.of(ConscientiousnessTerm.INDEPENDENT);
                case RULE_FOLLOWING     -> Optional.of(ConscientiousnessTerm.PRINCIPLED);
                case RISK_APPETITE      -> Optional.of(ConscientiousnessTerm.BOLD);
                case AUTONOMY           -> Optional.of(ConscientiousnessTerm.AUTONOMOUS);
                case CONFLICT_MODE      -> Optional.empty();
            };
            if (tv == ThomasKilmannTerm.class) return switch (axis) {
                case CONFLICT_MODE -> Optional.of(ThomasKilmannTerm.AVOIDING);
                default            -> Optional.empty();
            };
            return Optional.empty();
        }
    },

    NE("ne", "Extraverted Intuition",
       "Explores external patterns, possibilities, and connections; generates multiple ideas and sees potential",
       FunctionCategory.PERCEIVING, FunctionAttitude.EXTRAVERTED) {
        @Override public Optional<VocabularyTerm> axisExactMatch(Class<?> tv, DispositionAxis axis) {
            if (tv == ConscientiousnessTerm.class) return switch (axis) {
                case SOCIAL_ORIENTATION -> Optional.of(ConscientiousnessTerm.COLLABORATIVE);
                case RULE_FOLLOWING     -> Optional.of(ConscientiousnessTerm.FLEXIBLE);
                case RISK_APPETITE      -> Optional.of(ConscientiousnessTerm.BOLD);
                case AUTONOMY           -> Optional.of(ConscientiousnessTerm.SEMI_AUTONOMOUS);
                case CONFLICT_MODE      -> Optional.empty();
            };
            if (tv == ThomasKilmannTerm.class) return switch (axis) {
                case CONFLICT_MODE -> Optional.of(ThomasKilmannTerm.COLLABORATING);
                default            -> Optional.empty();
            };
            return Optional.empty();
        }
    };

    public static final String URI = "urn:casehub:vocab:jungian";

    public static final double DOMINANT_MIN = 0.31;
    public static final double DOMINANT_MAX = 1.00;
    public static final double AUXILIARY_MIN = 0.06;
    public static final double AUXILIARY_MAX = 0.30;
    public static final double UNDIFFERENTIATED_MAX = 0.06;
    public static final double REINFORCEMENT_DELTA = 0.06;
    public static final double DECAY_FACTOR = 0.20;

    private final String value, label, description;
    private final FunctionCategory category;
    private final FunctionAttitude attitude;

    JungianFunctionTerm(String value, String label, String description,
                        FunctionCategory category, FunctionAttitude attitude) {
        this.value = value;
        this.label = label;
        this.description = description;
        this.category = category;
        this.attitude = attitude;
    }

    @Override public String value()       { return value; }
    @Override public String label()       { return label; }
    @Override public String description() { return description; }

    public FunctionCategory category() { return category; }
    public FunctionAttitude attitude()  { return attitude; }

    public JungianFunctionTerm shadow() {
        return switch (this) {
            case TI -> TE; case TE -> TI;
            case FI -> FE; case FE -> FI;
            case SI -> SE; case SE -> SI;
            case NI -> NE; case NE -> NI;
        };
    }

    @Override
    public Optional<VocabularyTerm> opposite() {
        return Optional.of(shadow());
    }


    public List<JungianFunctionTerm> compatibleAuxiliaries() {
        FunctionCategory opposite = category == FunctionCategory.JUDGING
                ? FunctionCategory.PERCEIVING : FunctionCategory.JUDGING;
        return Arrays.stream(values())
                .filter(f -> f.category == opposite)
                .toList();
    }
}
