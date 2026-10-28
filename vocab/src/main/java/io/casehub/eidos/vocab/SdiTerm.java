package io.casehub.eidos.vocab;

import io.casehub.eidos.api.DispositionAxis;
import io.casehub.eidos.api.VocabularyMetadata;
import io.casehub.eidos.api.VocabularyTerm;

import java.util.List;
import java.util.Optional;

@VocabularyMetadata(uri = "urn:casehub:vocab:sdi",
                    name = "Strength Deployment Inventory", version = "1.0",
                    description = "Four motivational value systems describing relationship-focused conflict behavior. Complements Thomas-Kilmann (strategy vs motivation). SDI describes WHY people engage in conflict; TK describes HOW.")
public enum SdiTerm implements VocabularyTerm {

    BLUE("blue", "Altruistic-Nurturing",
        "Motivated by helping others and protecting people; concerned with others' welfare",
        List.of("altruistic", "nurturing")) {
        @Override public Optional<VocabularyTerm> axisExactMatch(Class<?> tv, DispositionAxis axis) {
            if (tv == ConscientiousnessTerm.class) return switch (axis) {
                case SOCIAL_ORIENTATION -> Optional.of(ConscientiousnessTerm.FACILITATIVE);
                case RULE_FOLLOWING     -> Optional.of(ConscientiousnessTerm.PRINCIPLED);
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

    RED("red", "Assertive-Directing",
        "Motivated by achieving results and accomplishing tasks; action-oriented and decisive",
        List.of("assertive", "directing")) {
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

    GREEN("green", "Analytic-Autonomizing",
        "Motivated by establishing order and making sense of things; methodical and self-reliant",
        List.of("analytic", "autonomizing")) {
        @Override public Optional<VocabularyTerm> axisExactMatch(Class<?> tv, DispositionAxis axis) {
            if (tv == ConscientiousnessTerm.class) return switch (axis) {
                case SOCIAL_ORIENTATION -> Optional.of(ConscientiousnessTerm.INDEPENDENT);
                case RULE_FOLLOWING     -> Optional.of(ConscientiousnessTerm.STRICT);
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

    HUB("hub", "Flexible-Cohering",
        "Motivated by group flexibility and adaptability; seeks to maintain group cohesion through compromise",
        List.of("flexible", "cohering")) {
        @Override public Optional<VocabularyTerm> axisExactMatch(Class<?> tv, DispositionAxis axis) {
            if (tv == ConscientiousnessTerm.class) return switch (axis) {
                case SOCIAL_ORIENTATION -> Optional.of(ConscientiousnessTerm.COLLABORATIVE);
                case RULE_FOLLOWING     -> Optional.of(ConscientiousnessTerm.FLEXIBLE);
                case RISK_APPETITE      -> Optional.of(ConscientiousnessTerm.MEASURED);
                case AUTONOMY           -> Optional.of(ConscientiousnessTerm.SEMI_AUTONOMOUS);
                case CONFLICT_MODE      -> Optional.empty();
            };
            if (tv == ThomasKilmannTerm.class) return switch (axis) {
                case CONFLICT_MODE -> Optional.of(ThomasKilmannTerm.COMPROMISING);
                default            -> Optional.empty();
            };
            return Optional.empty();
        }
    };

    public static final String URI = "urn:casehub:vocab:sdi";

    private final String value, label, description;
    private final List<String> aliases;

    SdiTerm(String value, String label, String description, List<String> aliases) {
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
            case BLUE -> switch (axis) {
                case SOCIAL_ORIENTATION -> 0.6;
                case CONFLICT_MODE -> -0.7;
                default -> 0.0;
            };
            case RED -> switch (axis) {
                case RISK_APPETITE -> 0.6;
                case AUTONOMY -> 0.3;
                case CONFLICT_MODE -> 0.7;
                default -> 0.0;
            };
            case GREEN -> switch (axis) {
                case RULE_FOLLOWING -> 0.4;
                case RISK_APPETITE -> -0.3;
                case AUTONOMY -> 0.7;
                default -> 0.0;
            };
            case HUB -> switch (axis) {
                case SOCIAL_ORIENTATION -> 0.3;
                case RULE_FOLLOWING -> -0.3;
                default -> 0.0;
            };
        };
    }

    @Override
    public String adjective() {
        return switch (this) {
            case BLUE -> "altruistic";
            case RED -> "assertive";
            case GREEN -> "analytical";
            case HUB -> "flexible";
        };
    }

    @Override
    public String detailedDescription() {
        return switch (this) {
            case BLUE -> "Blue SDI individuals are driven by concern for the welfare of others. They seek to protect, nurture, and develop people. They are most fulfilled when their work helps others grow or when they can create trusting relationships. Under conflict, they may initially accommodate, but can become fiercely assertive when core values around care and fairness are violated.";
            case RED -> "Red SDI individuals are driven by performance, achievement, and taking charge. They seek challenges, set ambitious goals, and act decisively. They are most fulfilled when they can influence outcomes directly. Under conflict, they confront issues head-on and may become competitive or domineering. They respect competence and directness in others.";
            case GREEN -> "Green SDI individuals are driven by a need to understand, organise, and ensure things are done correctly. They value thoughtful analysis, fair processes, and self-sufficiency. They are most fulfilled when they can work methodically toward well-reasoned outcomes. Under conflict, they may withdraw to analyse the situation, and can become stubbornly principled.";
            case HUB -> "Hub SDI individuals are motivated by flexibility and cohesion. They draw on altruistic (Blue), assertive (Red), and analytical (Green) motives as the situation demands. They are natural mediators who see multiple perspectives. They are most fulfilled in roles that require versatility. Under conflict, they may initially try to find a flexible solution but can feel pulled in multiple directions.";
        };
    }


}
