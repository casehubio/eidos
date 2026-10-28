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

    @Override
    public double axisSignalWeight(DispositionAxis axis) {
        return switch (this) {
            case PLANT -> switch (axis) {
                case RISK_APPETITE -> 0.4;
                case AUTONOMY -> 0.6;
                default -> 0.0;
            };
            case SHAPER -> switch (axis) {
                case RISK_APPETITE -> 0.5;
                case CONFLICT_MODE -> 0.6;
                default -> 0.0;
            };
            case MONITOR_EVALUATOR -> switch (axis) {
                case RULE_FOLLOWING -> 0.5;
                case RISK_APPETITE -> -0.3;
                default -> 0.0;
            };
            case CO_ORDINATOR -> switch (axis) {
                case SOCIAL_ORIENTATION -> 0.6;
                case RULE_FOLLOWING -> 0.3;
                default -> 0.0;
            };
            case TEAMWORKER -> switch (axis) {
                case SOCIAL_ORIENTATION -> 0.5;
                case CONFLICT_MODE -> -0.5;
                default -> 0.0;
            };
            case IMPLEMENTER -> switch (axis) {
                case RULE_FOLLOWING -> 0.6;
                case RISK_APPETITE -> -0.3;
                default -> 0.0;
            };
            case COMPLETER_FINISHER -> switch (axis) {
                case RULE_FOLLOWING -> 0.7;
                case RISK_APPETITE -> -0.5;
                default -> 0.0;
            };
            case SPECIALIST -> switch (axis) {
                case RULE_FOLLOWING -> 0.3;
                case AUTONOMY -> 0.6;
                default -> 0.0;
            };
            case RESOURCE_INVESTIGATOR -> switch (axis) {
                case SOCIAL_ORIENTATION -> 0.7;
                case RISK_APPETITE -> 0.4;
                default -> 0.0;
            };
        };
    }

    @Override
    public String detailedDescription() {
        return switch (this) {
            case PLANT -> "Plants are the team's source of original ideas. They think unconventionally, make unexpected connections, and propose creative solutions to difficult problems. Their weakness is that they may ignore practical constraints or be too absorbed in their ideas to communicate them effectively. They work best when given freedom to think and their ideas are filtered by others for feasibility.";
            case SHAPER -> "Shapers are dynamic, driven individuals who thrive under pressure and push the team to deliver. They challenge complacency, find ways around obstacles, and maintain momentum. Their weakness is that they can be provocative, impatient, and prone to offending others. They are essential when deadlines are tight and the team needs urgency.";
            case MONITOR_EVALUATOR -> "Monitor Evaluators bring sober, impartial analysis to team decisions. They weigh options carefully, spot flaws in plans, and make accurate judgements. Their weakness is that they can be overly critical and slow to commit. They are essential when the team faces complex decisions with significant consequences and needs someone who won't be swayed by enthusiasm alone.";
            case CO_ORDINATOR -> "Co-ordinators draw out contributions from all team members, clarify objectives, and delegate work based on individual strengths. They are calm, confident, and focused on the big picture. Their weakness is that they may be perceived as manipulative or as offloading their own work. They are essential in diverse teams that need structure without autocracy.";
            case TEAMWORKER -> "Teamworkers are perceptive, diplomatic individuals who listen well and smooth over interpersonal friction. They build team spirit, support others, and are flexible about their own role. Their weakness is indecisiveness under pressure and a tendency to avoid confrontation. They are essential when team morale is low or interpersonal tensions threaten progress.";
            case IMPLEMENTER -> "Implementers convert concepts and plans into practical, working procedures. They are disciplined, reliable, and efficient at systematic work. Their weakness is inflexibility and resistance to unproven ideas. They are essential when the team has good ideas but struggles to execute — Implementers bridge the gap between thinking and doing.";
            case COMPLETER_FINISHER -> "Completer-Finishers have an exceptional eye for detail and a drive to deliver polished, error-free work. They maintain high standards, check for mistakes, and ensure nothing is overlooked. Their weakness is that they worry excessively, find it hard to delegate, and may slow the team down. They are essential in quality-critical work where errors have serious consequences.";
            case SPECIALIST -> "Specialists provide expert knowledge and technical skills in a focused area. They are self-starting, dedicated, and committed to maintaining professional standards in their domain. Their weakness is a narrow focus — they contribute on a limited front and may dwell on technicalities. They are essential when the team needs deep expertise that no generalist can provide.";
            case RESOURCE_INVESTIGATOR -> "Resource Investigators are enthusiastic, outgoing team members who explore opportunities, develop contacts, and negotiate for resources. They bring back ideas and information from outside the team. Their weakness is that they lose interest quickly after the initial enthusiasm fades. They are essential in the early stages of a project when the team needs to explore possibilities and build external relationships.";
        };
    }


}
