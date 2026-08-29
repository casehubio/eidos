package io.casehub.eidos.runtime.yaml;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.casehub.eidos.api.AgentDisposition;
import io.casehub.eidos.api.DispositionAxis;
import io.casehub.eidos.api.DispositionValue;
import io.casehub.eidos.api.VocabularyRegistry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DispositionDeserializer extends JsonDeserializer<AgentDisposition> {

    private final VocabularyRegistry vocabRegistry;

    public DispositionDeserializer(VocabularyRegistry vocabRegistry) {
        this.vocabRegistry = vocabRegistry;
    }

    @Override
    public AgentDisposition deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        ObjectNode root = p.readValueAsTree();
        var builder = AgentDisposition.builder();

        String explicitSocialOrient = stringField(root, "socialOrient");
        String explicitRuleFollowing = stringField(root, "ruleFollowing");
        String explicitRiskAppetite = stringField(root, "riskAppetite");
        String explicitAutonomy = stringField(root, "autonomy");
        String explicitConflictMode = stringField(root, "conflictMode");

        if (explicitSocialOrient != null) builder.socialOrient(explicitSocialOrient);
        if (explicitRuleFollowing != null) builder.ruleFollowing(explicitRuleFollowing);
        if (explicitRiskAppetite != null) builder.riskAppetite(explicitRiskAppetite);
        if (explicitAutonomy != null) builder.autonomy(explicitAutonomy);
        if (explicitConflictMode != null) builder.conflictMode(explicitConflictMode);

        if (root.has("delegation")) builder.delegation(root.get("delegation").asBoolean());

        List<DispositionValue> explicitProfile = parseWeightedList(root, "dispositionProfile");
        if (explicitProfile != null) builder.dispositionProfile(explicitProfile);

        List<DispositionValue> styleProfile = parseWeightedList(root, "styleProfile");
        if (styleProfile != null) builder.styleProfile(styleProfile);

        if (root.has("mbtiType") && explicitProfile == null && vocabRegistry != null) {
            String mbtiType = root.get("mbtiType").asText().toLowerCase(Locale.ROOT);
            vocabRegistry.resolve("urn:casehub:vocab:mbti", mbtiType)
                .ifPresent(term -> builder.dispositionProfile(term.defaultProfile()));
        }

        if (root.has("enneagramType") && vocabRegistry != null) {
            String enneaValue = root.get("enneagramType").asText().toLowerCase(Locale.ROOT);
            if (vocabRegistry.resolve("urn:casehub:vocab:enneagram", enneaValue).isPresent()) {
                for (var axis : DispositionAxis.values()) {
                    if (axis == DispositionAxis.CONFLICT_MODE) {
                        if (explicitConflictMode == null) {
                            vocabRegistry.equivalentValues(
                                "urn:casehub:vocab:enneagram", enneaValue,
                                "urn:casehub:vocab:thomas-kilmann", axis)
                                .ifPresent(builder::conflictMode);
                        }
                    } else {
                        String explicit = switch (axis) {
                            case SOCIAL_ORIENTATION -> explicitSocialOrient;
                            case RULE_FOLLOWING -> explicitRuleFollowing;
                            case RISK_APPETITE -> explicitRiskAppetite;
                            case AUTONOMY -> explicitAutonomy;
                            default -> null;
                        };
                        if (explicit == null) {
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

        return builder.build();
    }

    private static String stringField(ObjectNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : null;
    }

    private static List<DispositionValue> parseWeightedList(ObjectNode node, String field) {
        if (!node.has(field) || !node.get(field).isArray()) return null;
        var values = new ArrayList<DispositionValue>();
        for (JsonNode item : node.get(field)) {
            values.add(new DispositionValue(item.get("term").asText(), item.get("weight").asDouble()));
        }
        return values;
    }
}
