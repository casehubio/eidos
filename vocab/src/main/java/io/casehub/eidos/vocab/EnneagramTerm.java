package io.casehub.eidos.vocab;

import io.casehub.eidos.api.DispositionAxis;
import io.casehub.eidos.api.VocabularyMetadata;
import io.casehub.eidos.api.VocabularyTerm;

import java.util.List;
import java.util.Optional;

@VocabularyMetadata(uri = "urn:casehub:vocab:enneagram",
                    name = "Enneagram", version = "1.0",
                    description = "Nine motivation-based personality types with growth/stress dynamics. Orthogonal to Jungian (motivation vs cognition). Each type has a core fear, desire, and characteristic behavioral pattern.")
public enum EnneagramTerm implements VocabularyTerm {

    TYPE_1("type-1", "Reformer",
        "Principled, purposeful, self-controlled; motivated by the desire to be right and improve things",
        List.of("1", "reformer", "perfectionist")) {
        @Override public Optional<VocabularyTerm> axisExactMatch(Class<?> tv, DispositionAxis axis) {
            if (tv == ConscientiousnessTerm.class) return switch (axis) {
                case RULE_FOLLOWING     -> Optional.of(ConscientiousnessTerm.STRICT);
                case RISK_APPETITE      -> Optional.of(ConscientiousnessTerm.CONSERVATIVE);
                case SOCIAL_ORIENTATION -> Optional.of(ConscientiousnessTerm.INDEPENDENT);
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
    TYPE_2("type-2", "Helper",
        "Generous, demonstrative, people-pleasing; motivated by the desire to be loved and needed",
        List.of("2", "helper", "giver")) {
        @Override public Optional<VocabularyTerm> axisExactMatch(Class<?> tv, DispositionAxis axis) {
            if (tv == ConscientiousnessTerm.class) return switch (axis) {
                case SOCIAL_ORIENTATION -> Optional.of(ConscientiousnessTerm.FACILITATIVE);
                case RULE_FOLLOWING     -> Optional.of(ConscientiousnessTerm.FLEXIBLE);
                case RISK_APPETITE      -> Optional.of(ConscientiousnessTerm.CONSERVATIVE);
                case AUTONOMY           -> Optional.of(ConscientiousnessTerm.DIRECTED);
                case CONFLICT_MODE      -> Optional.empty();
            };
            if (tv == ThomasKilmannTerm.class) return switch (axis) {
                case CONFLICT_MODE -> Optional.of(ThomasKilmannTerm.ACCOMMODATING);
                default            -> Optional.empty();
            };
            return Optional.empty();
        }
        @Override public boolean impliesSupervision() { return true; }
    },
    TYPE_3("type-3", "Achiever",
        "Adaptive, excelling, driven; motivated by the desire to be valuable and successful",
        List.of("3", "achiever", "performer")) {
        @Override public Optional<VocabularyTerm> axisExactMatch(Class<?> tv, DispositionAxis axis) {
            if (tv == ConscientiousnessTerm.class) return switch (axis) {
                case SOCIAL_ORIENTATION -> Optional.of(ConscientiousnessTerm.COLLABORATIVE);
                case RULE_FOLLOWING     -> Optional.of(ConscientiousnessTerm.FLEXIBLE);
                case RISK_APPETITE      -> Optional.of(ConscientiousnessTerm.BOLD);
                case AUTONOMY           -> Optional.of(ConscientiousnessTerm.AUTONOMOUS);
                case CONFLICT_MODE      -> Optional.empty();
            };
            if (tv == ThomasKilmannTerm.class) return switch (axis) {
                case CONFLICT_MODE -> Optional.of(ThomasKilmannTerm.COMPETING);
                default            -> Optional.empty();
            };
            return Optional.empty();
        }
    },
    TYPE_4("type-4", "Individualist",
        "Expressive, dramatic, self-absorbed; motivated by the desire to be unique and authentic",
        List.of("4", "individualist", "romantic")) {
        @Override public Optional<VocabularyTerm> axisExactMatch(Class<?> tv, DispositionAxis axis) {
            if (tv == ConscientiousnessTerm.class) return switch (axis) {
                case SOCIAL_ORIENTATION -> Optional.of(ConscientiousnessTerm.INDEPENDENT);
                case RULE_FOLLOWING     -> Optional.of(ConscientiousnessTerm.FLEXIBLE);
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
    TYPE_5("type-5", "Investigator",
        "Perceptive, innovative, secretive; motivated by the desire to understand and be competent",
        List.of("5", "investigator", "observer")) {
        @Override public Optional<VocabularyTerm> axisExactMatch(Class<?> tv, DispositionAxis axis) {
            if (tv == ConscientiousnessTerm.class) return switch (axis) {
                case SOCIAL_ORIENTATION -> Optional.of(ConscientiousnessTerm.INDEPENDENT);
                case RULE_FOLLOWING     -> Optional.of(ConscientiousnessTerm.PRINCIPLED);
                case RISK_APPETITE      -> Optional.of(ConscientiousnessTerm.CONSERVATIVE);
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
    TYPE_6("type-6", "Loyalist",
        "Committed, security-oriented, vigilant; motivated by the desire for safety and support",
        List.of("6", "loyalist", "skeptic")) {
        @Override public Optional<VocabularyTerm> axisExactMatch(Class<?> tv, DispositionAxis axis) {
            if (tv == ConscientiousnessTerm.class) return switch (axis) {
                case SOCIAL_ORIENTATION -> Optional.of(ConscientiousnessTerm.COLLABORATIVE);
                case RULE_FOLLOWING     -> Optional.of(ConscientiousnessTerm.STRICT);
                case RISK_APPETITE      -> Optional.of(ConscientiousnessTerm.CONSERVATIVE);
                case AUTONOMY           -> Optional.of(ConscientiousnessTerm.DIRECTED);
                case CONFLICT_MODE      -> Optional.empty();
            };
            if (tv == ThomasKilmannTerm.class) return switch (axis) {
                case CONFLICT_MODE -> Optional.of(ThomasKilmannTerm.COMPROMISING);
                default            -> Optional.empty();
            };
            return Optional.empty();
        }
        @Override public boolean impliesSupervision() { return true; }
    },
    TYPE_7("type-7", "Enthusiast",
        "Spontaneous, versatile, scattered; motivated by the desire for satisfaction and new experiences",
        List.of("7", "enthusiast", "epicure")) {
        @Override public Optional<VocabularyTerm> axisExactMatch(Class<?> tv, DispositionAxis axis) {
            if (tv == ConscientiousnessTerm.class) return switch (axis) {
                case SOCIAL_ORIENTATION -> Optional.of(ConscientiousnessTerm.COLLABORATIVE);
                case RULE_FOLLOWING     -> Optional.of(ConscientiousnessTerm.FLEXIBLE);
                case RISK_APPETITE      -> Optional.of(ConscientiousnessTerm.BOLD);
                case AUTONOMY           -> Optional.of(ConscientiousnessTerm.AUTONOMOUS);
                case CONFLICT_MODE      -> Optional.empty();
            };
            if (tv == ThomasKilmannTerm.class) return switch (axis) {
                case CONFLICT_MODE -> Optional.of(ThomasKilmannTerm.COLLABORATING);
                default            -> Optional.empty();
            };
            return Optional.empty();
        }
    },
    TYPE_8("type-8", "Challenger",
        "Self-confident, decisive, confrontational; motivated by the desire to control and protect",
        List.of("8", "challenger", "boss")) {
        @Override public Optional<VocabularyTerm> axisExactMatch(Class<?> tv, DispositionAxis axis) {
            if (tv == ConscientiousnessTerm.class) return switch (axis) {
                case SOCIAL_ORIENTATION -> Optional.of(ConscientiousnessTerm.INDEPENDENT);
                case RULE_FOLLOWING     -> Optional.of(ConscientiousnessTerm.FLEXIBLE);
                case RISK_APPETITE      -> Optional.of(ConscientiousnessTerm.BOLD);
                case AUTONOMY           -> Optional.of(ConscientiousnessTerm.AUTONOMOUS);
                case CONFLICT_MODE      -> Optional.empty();
            };
            if (tv == ThomasKilmannTerm.class) return switch (axis) {
                case CONFLICT_MODE -> Optional.of(ThomasKilmannTerm.COMPETING);
                default            -> Optional.empty();
            };
            return Optional.empty();
        }
    },
    TYPE_9("type-9", "Peacemaker",
        "Receptive, reassuring, complacent; motivated by the desire for inner peace and harmony",
        List.of("9", "peacemaker", "mediator")) {
        @Override public Optional<VocabularyTerm> axisExactMatch(Class<?> tv, DispositionAxis axis) {
            if (tv == ConscientiousnessTerm.class) return switch (axis) {
                case SOCIAL_ORIENTATION -> Optional.of(ConscientiousnessTerm.FACILITATIVE);
                case RULE_FOLLOWING     -> Optional.of(ConscientiousnessTerm.PRINCIPLED);
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
        @Override public boolean impliesSupervision() { return true; }
    };

    public static final String URI = "urn:casehub:vocab:enneagram";

    private final String value, label, description;
    private final List<String> aliases;

    EnneagramTerm(String value, String label, String description, List<String> aliases) {
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
