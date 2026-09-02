package io.casehub.eidos.api;

import java.util.Locale;

public final class PersonalityTypeDeriver {

    private PersonalityTypeDeriver() {}

    public static void derive(PersonalityInput input, VocabularyRegistry vocabRegistry,
                              AgentDisposition.Builder builder) {
        if (vocabRegistry == null) return;

        if (!input.mbtiType().isEmpty() && !input.hasExplicitProfile()) {
            String mbtiType = input.mbtiType().toLowerCase(Locale.ROOT);
            vocabRegistry.resolve("urn:casehub:vocab:mbti", mbtiType)
                .ifPresent(term -> builder.dispositionProfile(term.defaultProfile()));
        }

        if (!input.enneagramType().isEmpty()) {
            String enneaValue = input.enneagramType().toLowerCase(Locale.ROOT);
            if (vocabRegistry.resolve("urn:casehub:vocab:enneagram", enneaValue).isPresent()) {
                for (var axis : DispositionAxis.values()) {
                    if (input.explicitAxes().containsKey(axis)) continue;
                    if (axis == DispositionAxis.CONFLICT_MODE) {
                        vocabRegistry.equivalentValues(
                            "urn:casehub:vocab:enneagram", enneaValue,
                            "urn:casehub:vocab:thomas-kilmann", axis)
                            .ifPresent(builder::conflictMode);
                    } else {
                        vocabRegistry.equivalentValues(
                            "urn:casehub:vocab:enneagram", enneaValue,
                            "urn:casehub:vocab:conscientiousness", axis)
                            .ifPresent(val -> {
                                switch (axis) {
                                    case SOCIAL_ORIENTATION -> builder.socialOrient(val);
                                    case RULE_FOLLOWING -> builder.ruleFollowing(val);
                                    case RISK_APPETITE -> builder.riskAppetite(val);
                                    case AUTONOMY -> builder.autonomy(val);
                                    default -> {}
                                }
                            });
                    }
                }
            }
        }
    }
}
