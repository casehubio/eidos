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

    @Override
    public double axisSignalWeight(DispositionAxis axis) {
        return switch (this) {
            case TYPE_1 -> switch (axis) {
                case RULE_FOLLOWING -> 0.8;
                case CONFLICT_MODE -> 0.3;
                default -> 0.0;
            };
            case TYPE_2 -> switch (axis) {
                case SOCIAL_ORIENTATION -> 0.7;
                case CONFLICT_MODE -> -0.6;
                default -> 0.0;
            };
            case TYPE_3 -> switch (axis) {
                case RISK_APPETITE -> 0.5;
                case CONFLICT_MODE -> 0.4;
                default -> 0.0;
            };
            case TYPE_4 -> switch (axis) {
                case AUTONOMY -> 0.5;
                case RISK_APPETITE -> 0.2;
                default -> 0.0;
            };
            case TYPE_5 -> switch (axis) {
                case AUTONOMY -> 0.8;
                case RISK_APPETITE -> -0.4;
                default -> 0.0;
            };
            case TYPE_6 -> switch (axis) {
                case RULE_FOLLOWING -> 0.5;
                case RISK_APPETITE -> -0.6;
                default -> 0.0;
            };
            case TYPE_7 -> switch (axis) {
                case RISK_APPETITE -> 0.8;
                case RULE_FOLLOWING -> -0.7;
                default -> 0.0;
            };
            case TYPE_8 -> switch (axis) {
                case CONFLICT_MODE -> 0.8;
                case RISK_APPETITE -> 0.6;
                default -> 0.0;
            };
            case TYPE_9 -> switch (axis) {
                case CONFLICT_MODE -> -0.7;
                case SOCIAL_ORIENTATION -> 0.3;
                default -> 0.0;
            };
        };
    }

    @Override
    public String adjective() {
        return switch (this) {
            case TYPE_1 -> "principled";
            case TYPE_2 -> "caring";
            case TYPE_3 -> "driven";
            case TYPE_4 -> "expressive";
            case TYPE_5 -> "investigative";
            case TYPE_6 -> "vigilant";
            case TYPE_7 -> "enthusiastic";
            case TYPE_8 -> "assertive";
            case TYPE_9 -> "harmonizing";
        };
    }

    @Override
    public String detailedDescription() {
        return switch (this) {
            case TYPE_1 -> "Type 1s are motivated by a desire to be good, right, and ethical. They hold themselves and others to high standards, and are driven to improve what they see as flawed. At their best, they are wise, discerning, and inspiring moral leaders. Under stress, they become critical, rigid, and resentful. In work settings, they bring integrity, quality standards, and methodical improvement.";
            case TYPE_2 -> "Type 2s are motivated by a need to be loved and needed. They intuitively sense what others need and provide support, sometimes at the expense of their own needs. At their best, they are genuinely altruistic and deeply empathetic. Under stress, they become possessive and resentful when unappreciated. In teams, they build strong relationships and create supportive environments.";
            case TYPE_3 -> "Type 3s are motivated by a desire to be valuable and worthwhile through achievement. They are efficient, goal-oriented, and skilled at presenting themselves well. At their best, they inspire others through authentic accomplishment. Under stress, they become overly competitive and may prioritise image over substance. In work settings, they drive results and model high performance.";
            case TYPE_4 -> "Type 4s are motivated by a desire to find significance and express their authentic identity. They are deeply attuned to beauty, meaning, and emotional truth. At their best, they bring profound creativity and emotional depth. Under stress, they become self-absorbed and envious. In creative work, they contribute originality, aesthetic sensitivity, and willingness to explore difficult territory.";
            case TYPE_5 -> "Type 5s are motivated by a need to understand the world and conserve their energy. They observe carefully, analyse deeply, and build expertise in focused domains. At their best, they are visionary pioneers who see what others cannot. Under stress, they become detached, hoarding knowledge and retreating from demands. In teams, they contribute deep expertise, objective analysis, and innovative thinking.";
            case TYPE_6 -> "Type 6s are motivated by a need for security and support. They anticipate problems, test loyalty, and prepare for worst-case scenarios. At their best, they are courageous, community-minded, and deeply committed. Under stress, they become suspicious and indecisive. In work settings, they bring risk awareness, thoroughness, and steadfast commitment to the team.";
            case TYPE_7 -> "Type 7s are motivated by a desire to maintain freedom and happiness while avoiding pain. They generate ideas rapidly, connect disparate concepts, and bring infectious optimism. At their best, they are joyful, grateful, and deeply present. Under stress, they become scattered and escapist. In teams, they contribute creative energy, adaptability, and the ability to reframe setbacks as opportunities.";
            case TYPE_8 -> "Type 8s are motivated by a need to be strong and avoid vulnerability. They are direct, decisive, and protective of those in their care. At their best, they are magnanimous leaders who empower others. Under stress, they become controlling and confrontational. In work settings, they bring decisive action, strength under pressure, and willingness to make hard calls.";
            case TYPE_9 -> "Type 9s are motivated by a desire for inner and outer peace. They see all perspectives, mediate conflict, and create inclusive environments. At their best, they are deeply connected, accepting, and able to unite diverse groups. Under stress, they become passive, disengaged, and stubborn in their inaction. In teams, they contribute harmony, inclusiveness, and the ability to find common ground.";
        };
    }


}
