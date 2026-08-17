package io.casehub.eidos.vocab;

import io.casehub.eidos.api.DispositionAxis;
import io.casehub.eidos.api.VocabularyMetadata;
import io.casehub.eidos.api.VocabularyTerm;

import java.util.List;
import java.util.Optional;

@VocabularyMetadata(uri = "urn:casehub:vocab:sarc7",
                    name = "Sarc7 Sarcasm Types", version = "1.0",
                    description = "Seven pragmatically defined sarcasm types from the Sarc7 paper (Xiong et al., ACL WiNLP 2025). Each type carries evaluation dimensions (incongruity, shock value, context dependency, emotional tone) and prompt guidance for LLM rendering.")
public enum Sarc7Term implements VocabularyTerm {

    SELF_DEPRECATING("self-deprecating", "Self-Deprecating",
            "Humor at one's own expense; disarming, builds rapport through vulnerability",
            List.of(), 0.5, 0.2, 0.6, 0.7) {
        @Override
        public Optional<VocabularyTerm> axisExactMatch(Class<?> targetVocab, DispositionAxis axis) {
            if (targetVocab == ConscientiousnessTerm.class) {
                return switch (axis) {
                    case SOCIAL_ORIENTATION -> Optional.of(ConscientiousnessTerm.FACILITATIVE);
                    case RULE_FOLLOWING     -> Optional.of(ConscientiousnessTerm.FLEXIBLE);
                    case RISK_APPETITE      -> Optional.of(ConscientiousnessTerm.CONSERVATIVE);
                    case AUTONOMY           -> Optional.of(ConscientiousnessTerm.SEMI_AUTONOMOUS);
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
    },

    BROODING("brooding", "Brooding",
            "Dark, moody sarcasm with pessimistic undertone; inward-directed frustration",
            List.of(), 0.6, 0.3, 0.7, 0.2) {
        @Override
        public Optional<VocabularyTerm> axisExactMatch(Class<?> targetVocab, DispositionAxis axis) {
            if (targetVocab == ConscientiousnessTerm.class) {
                return switch (axis) {
                    case SOCIAL_ORIENTATION -> Optional.of(ConscientiousnessTerm.INDEPENDENT);
                    case RULE_FOLLOWING     -> Optional.of(ConscientiousnessTerm.PRINCIPLED);
                    case RISK_APPETITE      -> Optional.of(ConscientiousnessTerm.CONSERVATIVE);
                    case AUTONOMY           -> Optional.of(ConscientiousnessTerm.AUTONOMOUS);
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
    },

    DEADPAN("deadpan", "Deadpan",
            "Dry, expressionless delivery; no signal that humor is intended",
            List.of(), 0.8, 0.2, 0.6, 0.4) {
        @Override
        public Optional<VocabularyTerm> axisExactMatch(Class<?> targetVocab, DispositionAxis axis) {
            if (targetVocab == ConscientiousnessTerm.class) {
                return switch (axis) {
                    case SOCIAL_ORIENTATION -> Optional.of(ConscientiousnessTerm.INDEPENDENT);
                    case RULE_FOLLOWING     -> Optional.of(ConscientiousnessTerm.FLEXIBLE);
                    case RISK_APPETITE      -> Optional.of(ConscientiousnessTerm.MEASURED);
                    case AUTONOMY           -> Optional.of(ConscientiousnessTerm.AUTONOMOUS);
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
    },

    POLITE("polite", "Polite",
            "Veiled sarcasm disguised as courtesy; surface-level agreeable",
            List.of(), 0.7, 0.3, 0.8, 0.6) {
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
    },

    OBNOXIOUS("obnoxious", "Obnoxious",
            "Aggressive, in-your-face sarcasm designed to provoke",
            List.of(), 0.6, 0.8, 0.4, 0.2) {
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

    RAGING("raging", "Raging",
            "Angry, intense sarcasm driven by frustration",
            List.of(), 0.5, 0.7, 0.3, 0.1) {
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

    MANIC("manic", "Manic",
            "Frenzied, over-the-top sarcasm with chaotic energy",
            List.of(), 0.7, 0.6, 0.4, 0.5) {
        @Override
        public Optional<VocabularyTerm> axisExactMatch(Class<?> targetVocab, DispositionAxis axis) {
            if (targetVocab == ConscientiousnessTerm.class) {
                return switch (axis) {
                    case SOCIAL_ORIENTATION -> Optional.of(ConscientiousnessTerm.COLLABORATIVE);
                    case RULE_FOLLOWING     -> Optional.of(ConscientiousnessTerm.FLEXIBLE);
                    case RISK_APPETITE      -> Optional.of(ConscientiousnessTerm.BOLD);
                    case AUTONOMY           -> Optional.of(ConscientiousnessTerm.AUTONOMOUS);
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
    };

    public static final String URI = "urn:casehub:vocab:sarc7";

    private final String value, label, description;
    private final List<String> aliases;
    private final double incongruity, shockValue, contextDependency, emotionalTone;

    Sarc7Term(String value, String label, String description, List<String> aliases,
              double incongruity, double shockValue, double contextDependency, double emotionalTone) {
        this.value = value;
        this.label = label;
        this.description = description;
        this.aliases = aliases;
        this.incongruity = incongruity;
        this.shockValue = shockValue;
        this.contextDependency = contextDependency;
        this.emotionalTone = emotionalTone;
    }

    @Override public String value()         { return value; }
    @Override public String label()         { return label; }
    @Override public String description()   { return description; }
    @Override public List<String> aliases() { return aliases; }

    public double incongruity()       { return incongruity; }
    public double shockValue()        { return shockValue; }
    public double contextDependency() { return contextDependency; }
    public double emotionalTone()     { return emotionalTone; }

    @Override
    public String responseStyleGuidance() {
        return switch (this) {
            case SELF_DEPRECATING -> "Use humor at your own expense to build rapport. Highlight your own flaws, mistakes, or limitations with affectionate exaggeration. Be self-aware and disarming — never bitter or fishing for sympathy.";
            case BROODING -> "Deploy dark, moody observations with a pessimistic undertone. Express frustration through cynical commentary that implies the world is fundamentally disappointing. Keep the tone inward-directed — sardonic resignation, not outward aggression.";
            case DEADPAN -> "Deliver humor through understatement and matter-of-fact delivery. Never signal that you are being humorous. State absurd things as though they are obvious facts. Let the incongruity between your flat tone and the content do the work.";
            case POLITE -> "Disguise sarcasm as genuine courtesy. Use formal language, pleasantries, and surface-level agreement while the subtext communicates the opposite. The gap between your polite words and their actual meaning is the humor.";
            case OBNOXIOUS -> "Be deliberately provocative and in-your-face with your humor. Push boundaries, challenge assumptions loudly, and use exaggeration for shock effect. Your sarcasm should be impossible to miss — subtlety is not the goal.";
            case RAGING -> "Channel frustration into intense, forceful sarcasm. Express anger through biting rhetorical questions and furious hyperbole. The humor comes from the disproportionate intensity of your response to the situation.";
            case MANIC -> "Deliver sarcasm with frenzied, chaotic energy. Jump between ideas rapidly, use wild tangents, and escalate the absurdity with each sentence. Your enthusiasm should be unsettling in its intensity — the humor is in the unhinged delivery.";
        };
    }

    @Override
    public String antiPatternWarning() {
        return switch (this) {
            case SELF_DEPRECATING -> "Do not use self-deprecation to avoid taking a position or to deflect from the actual topic. Do not become genuinely self-pitying — the humor requires confidence underneath.";
            case BROODING -> "Do not become genuinely hostile or nihilistic. Brooding sarcasm observes darkness with detachment, not malice. Do not direct frustration at specific individuals.";
            case DEADPAN -> "Do not use exclamation marks, emoji, 'lol', or any tonal marker that telegraphs humor. Do not explain the joke. Do not break character by acknowledging the humor.";
            case POLITE -> "Do not be openly rude or drop the courtesy mask. The humor depends on maintaining plausible deniability — if the sarcasm is obvious, it stops being polite sarcasm. Do not use passive-aggression as a weapon.";
            case OBNOXIOUS -> "Do not target personal vulnerabilities or use humor to bully. Obnoxious sarcasm is about volume and provocation, not cruelty. Do not mistake being offensive for being funny.";
            case RAGING -> "Do not sustain the rage — it should spike and subside. Extended raging becomes exhausting and unfunny. Do not direct anger at the user personally.";
            case MANIC -> "Do not become incoherent — the chaos must have an underlying logic that the audience can follow. Do not use manic energy to avoid answering the actual question.";
        };
    }
}
