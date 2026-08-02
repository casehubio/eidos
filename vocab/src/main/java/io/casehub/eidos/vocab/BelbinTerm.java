package io.casehub.eidos.vocab;

import io.casehub.eidos.api.DispositionAxis;
import io.casehub.eidos.api.VocabularyMetadata;
import io.casehub.eidos.api.VocabularyTerm;

import java.util.List;
import java.util.Optional;

@VocabularyMetadata(uri = "urn:casehub:vocab:belbin",
                    name = "Belbin Team Roles", version = "1.0",
                    description = "Nine complementary team-role archetypes developed by Meredith Belbin from observational research at Henley Management College (1981). Roles describe what a person contributes to a team's function. Medium scientific validity; widely adopted in UK and EU management development.")
public enum BelbinTerm implements VocabularyTerm {

    PLANT("plant", "Plant",
          "Creative, unorthodox problem-solver; generates novel ideas independently",
          List.of("pl")) {
        @Override
        public Optional<VocabularyTerm> axisExactMatch(Class<?> tv, DispositionAxis axis) {
            if (tv == ConscientiousnessTerm.class) {
                return switch (axis) {
                    case SOCIAL_ORIENTATION -> Optional.of(ConscientiousnessTerm.INDEPENDENT);
                    case RULE_FOLLOWING -> Optional.of(ConscientiousnessTerm.FLEXIBLE);
                    case RISK_APPETITE -> Optional.of(ConscientiousnessTerm.BOLD);
                    case AUTONOMY -> Optional.of(ConscientiousnessTerm.AUTONOMOUS);
                    case CONFLICT_MODE -> Optional.empty();
                };
            }
            if (tv == ThomasKilmannTerm.class) {
                return switch (axis) {
                    case CONFLICT_MODE -> Optional.of(ThomasKilmannTerm.AVOIDING);
                    default -> Optional.empty();
                };
            }
            return Optional.empty();
        }
    },
    RESOURCE_INVESTIGATOR("resource-investigator", "Resource Investigator",
                          "Extrovert who explores external opportunities and develops contacts",
                          List.of("ri")) {
        @Override
        public Optional<VocabularyTerm> axisExactMatch(Class<?> tv, DispositionAxis axis) {
            if (tv == ConscientiousnessTerm.class) {
                return switch (axis) {
                    case SOCIAL_ORIENTATION -> Optional.of(ConscientiousnessTerm.COLLABORATIVE);
                    case RULE_FOLLOWING -> Optional.of(ConscientiousnessTerm.FLEXIBLE);
                    case RISK_APPETITE -> Optional.of(ConscientiousnessTerm.MEASURED);
                    case AUTONOMY -> Optional.of(ConscientiousnessTerm.SEMI_AUTONOMOUS);
                    case CONFLICT_MODE -> Optional.empty();
                };
            }
            if (tv == ThomasKilmannTerm.class) {
                return switch (axis) {
                    case CONFLICT_MODE -> Optional.of(ThomasKilmannTerm.COLLABORATING);
                    default -> Optional.empty();
                };
            }
            return Optional.empty();
        }
    },
    CO_ORDINATOR("co-ordinator", "Co-ordinator",
                 "Clarifies goals, promotes team decision-making, delegates effectively",
                 List.of("co")) {
        @Override
        public Optional<VocabularyTerm> axisExactMatch(Class<?> tv, DispositionAxis axis) {
            if (tv == ConscientiousnessTerm.class) {
                return switch (axis) {
                    case SOCIAL_ORIENTATION -> Optional.of(ConscientiousnessTerm.FACILITATIVE);
                    case RULE_FOLLOWING -> Optional.of(ConscientiousnessTerm.PRINCIPLED);
                    case RISK_APPETITE -> Optional.of(ConscientiousnessTerm.MEASURED);
                    case AUTONOMY -> Optional.of(ConscientiousnessTerm.SEMI_AUTONOMOUS);
                    case CONFLICT_MODE -> Optional.empty();
                };
            }
            if (tv == ThomasKilmannTerm.class) {
                return switch (axis) {
                    case CONFLICT_MODE -> Optional.of(ThomasKilmannTerm.COMPROMISING);
                    default -> Optional.empty();
                };
            }
            return Optional.empty();
        }

        @Override
        public boolean impliesSupervision() {return true;}
    },
    SHAPER("shaper", "Shaper",
           "Challenges the team to improve; driven, dynamic, thrives under pressure",
           List.of("sh")) {
        @Override
        public Optional<VocabularyTerm> axisExactMatch(Class<?> tv, DispositionAxis axis) {
            if (tv == ConscientiousnessTerm.class) {
                return switch (axis) {
                    case SOCIAL_ORIENTATION -> Optional.of(ConscientiousnessTerm.INDEPENDENT);
                    case RULE_FOLLOWING -> Optional.of(ConscientiousnessTerm.FLEXIBLE);
                    case RISK_APPETITE -> Optional.of(ConscientiousnessTerm.BOLD);
                    case AUTONOMY -> Optional.of(ConscientiousnessTerm.AUTONOMOUS);
                    case CONFLICT_MODE -> Optional.empty();
                };
            }
            if (tv == ThomasKilmannTerm.class) {
                return switch (axis) {
                    case CONFLICT_MODE -> Optional.of(ThomasKilmannTerm.COMPETING);
                    default -> Optional.empty();
                };
            }
            return Optional.empty();
        }
    },
    MONITOR_EVALUATOR("monitor-evaluator", "Monitor Evaluator",
                      "Sober, strategic, discerning; sees all options and judges accurately",
                      List.of("me")) {
        @Override
        public Optional<VocabularyTerm> axisExactMatch(Class<?> tv, DispositionAxis axis) {
            if (tv == ConscientiousnessTerm.class) {
                return switch (axis) {
                    case SOCIAL_ORIENTATION -> Optional.of(ConscientiousnessTerm.INDEPENDENT);
                    case RULE_FOLLOWING -> Optional.of(ConscientiousnessTerm.STRICT);
                    case RISK_APPETITE -> Optional.of(ConscientiousnessTerm.CONSERVATIVE);
                    case AUTONOMY -> Optional.of(ConscientiousnessTerm.SEMI_AUTONOMOUS);
                    case CONFLICT_MODE -> Optional.empty();
                };
            }
            if (tv == ThomasKilmannTerm.class) {
                return switch (axis) {
                    case CONFLICT_MODE -> Optional.of(ThomasKilmannTerm.AVOIDING);
                    default -> Optional.empty();
                };
            }
            return Optional.empty();
        }
    },
    TEAMWORKER("teamworker", "Teamworker",
               "Cooperative, perceptive, diplomatic; averts friction and builds cohesion",
               List.of("tw")) {
        @Override
        public Optional<VocabularyTerm> axisExactMatch(Class<?> tv, DispositionAxis axis) {
            if (tv == ConscientiousnessTerm.class) {
                return switch (axis) {
                    case SOCIAL_ORIENTATION -> Optional.of(ConscientiousnessTerm.COLLABORATIVE);
                    case RULE_FOLLOWING -> Optional.of(ConscientiousnessTerm.PRINCIPLED);
                    case RISK_APPETITE -> Optional.of(ConscientiousnessTerm.CONSERVATIVE);
                    case AUTONOMY -> Optional.of(ConscientiousnessTerm.DIRECTED);
                    case CONFLICT_MODE -> Optional.empty();
                };
            }
            if (tv == ThomasKilmannTerm.class) {
                return switch (axis) {
                    case CONFLICT_MODE -> Optional.of(ThomasKilmannTerm.ACCOMMODATING);
                    default -> Optional.empty();
                };
            }
            return Optional.empty();
        }

        @Override
        public boolean impliesSupervision() {return true;}
    },
    IMPLEMENTER("implementer", "Implementer",
                "Disciplined, reliable, efficient; turns ideas into practical actions",
                List.of("imp")) {
        @Override
        public Optional<VocabularyTerm> axisExactMatch(Class<?> tv, DispositionAxis axis) {
            if (tv == ConscientiousnessTerm.class) {
                return switch (axis) {
                    case SOCIAL_ORIENTATION -> Optional.of(ConscientiousnessTerm.COLLABORATIVE);
                    case RULE_FOLLOWING -> Optional.of(ConscientiousnessTerm.STRICT);
                    case RISK_APPETITE -> Optional.of(ConscientiousnessTerm.CONSERVATIVE);
                    case AUTONOMY -> Optional.of(ConscientiousnessTerm.DIRECTED);
                    case CONFLICT_MODE -> Optional.empty();
                };
            }
            if (tv == ThomasKilmannTerm.class) {
                return switch (axis) {
                    case CONFLICT_MODE -> Optional.of(ThomasKilmannTerm.COMPROMISING);
                    default -> Optional.empty();
                };
            }
            return Optional.empty();
        }
    },
    COMPLETER_FINISHER("completer-finisher", "Completer Finisher",
                       "Painstaking, conscientious, anxious; ensures delivery to standard",
                       List.of("cf")) {
        @Override
        public Optional<VocabularyTerm> axisExactMatch(Class<?> tv, DispositionAxis axis) {
            if (tv == ConscientiousnessTerm.class) {
                return switch (axis) {
                    case SOCIAL_ORIENTATION -> Optional.of(ConscientiousnessTerm.INDEPENDENT);
                    case RULE_FOLLOWING -> Optional.of(ConscientiousnessTerm.STRICT);
                    case RISK_APPETITE -> Optional.of(ConscientiousnessTerm.CONSERVATIVE);
                    case AUTONOMY -> Optional.of(ConscientiousnessTerm.SEMI_AUTONOMOUS);
                    case CONFLICT_MODE -> Optional.empty();
                };
            }
            if (tv == ThomasKilmannTerm.class) {
                return switch (axis) {
                    case CONFLICT_MODE -> Optional.of(ThomasKilmannTerm.AVOIDING);
                    default -> Optional.empty();
                };
            }
            return Optional.empty();
        }
    },
    SPECIALIST("specialist", "Specialist",
               "Dedicated, self-starting, single-minded; provides rare knowledge",
               List.of("sp")) {
        @Override
        public Optional<VocabularyTerm> axisExactMatch(Class<?> tv, DispositionAxis axis) {
            if (tv == ConscientiousnessTerm.class) {
                return switch (axis) {
                    case SOCIAL_ORIENTATION -> Optional.of(ConscientiousnessTerm.INDEPENDENT);
                    case RULE_FOLLOWING -> Optional.of(ConscientiousnessTerm.PRINCIPLED);
                    case RISK_APPETITE -> Optional.of(ConscientiousnessTerm.MEASURED);
                    case AUTONOMY -> Optional.of(ConscientiousnessTerm.AUTONOMOUS);
                    case CONFLICT_MODE -> Optional.empty();
                };
            }
            if (tv == ThomasKilmannTerm.class) {
                return switch (axis) {
                    case CONFLICT_MODE -> Optional.of(ThomasKilmannTerm.COMPETING);
                    default -> Optional.empty();
                };
            }
            return Optional.empty();
        }
    };

    public static final String URI = "urn:casehub:vocab:belbin";

    private final String value, label, description;
    private final List<String> aliases;

    BelbinTerm(String value, String label, String description, List<String> aliases) {
        this.value       = value;
        this.label       = label;
        this.description = description;
        this.aliases     = aliases;
    }

    @Override
    public String value()         {return value;}

    @Override
    public String label()         {return label;}

    @Override
    public String description()   {return description;}

    @Override
    public List<String> aliases() {return aliases;}
}
