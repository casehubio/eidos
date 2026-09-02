package io.casehub.eidos.runtime.yaml;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.casehub.eidos.api.AgentDisposition;
import io.casehub.eidos.api.DispositionAxis;
import io.casehub.eidos.api.DispositionValue;
import io.casehub.eidos.api.PersonalityInput;
import io.casehub.eidos.api.PersonalityTypeDeriver;
import io.casehub.eidos.api.VocabularyRegistry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DispositionDeserializer extends JsonDeserializer<AgentDisposition> {

    private final VocabularyRegistry vocabRegistry;

    public DispositionDeserializer(VocabularyRegistry vocabRegistry) {
        this.vocabRegistry = vocabRegistry;
    }

    @Override
    public AgentDisposition deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        ObjectNode root    = p.readValueAsTree();
        var        builder = AgentDisposition.builder();

        String explicitSocialOrient  = stringField(root, "socialOrient");
        String explicitRuleFollowing = stringField(root, "ruleFollowing");
        String explicitRiskAppetite  = stringField(root, "riskAppetite");
        String explicitAutonomy      = stringField(root, "autonomy");
        String explicitConflictMode  = stringField(root, "conflictMode");

        if (explicitSocialOrient != null) {builder.socialOrient(explicitSocialOrient);}
        if (explicitRuleFollowing != null) {builder.ruleFollowing(explicitRuleFollowing);}
        if (explicitRiskAppetite != null) {builder.riskAppetite(explicitRiskAppetite);}
        if (explicitAutonomy != null) {builder.autonomy(explicitAutonomy);}
        if (explicitConflictMode != null) {builder.conflictMode(explicitConflictMode);}

        if (root.has("delegation")) {builder.delegation(root.get("delegation").asBoolean());}

        List<DispositionValue> explicitProfile = parseWeightedList(root, "dispositionProfile");
        if (explicitProfile != null) {builder.dispositionProfile(explicitProfile);}

        List<DispositionValue> styleProfile = parseWeightedList(root, "styleProfile");
        if (styleProfile != null) {builder.styleProfile(styleProfile);}

        var explicitAxes = new java.util.EnumMap<DispositionAxis, String>(DispositionAxis.class);
        if (explicitSocialOrient != null) {explicitAxes.put(DispositionAxis.SOCIAL_ORIENTATION, explicitSocialOrient);}
        if (explicitRuleFollowing != null) {explicitAxes.put(DispositionAxis.RULE_FOLLOWING, explicitRuleFollowing);}
        if (explicitRiskAppetite != null) {explicitAxes.put(DispositionAxis.RISK_APPETITE, explicitRiskAppetite);}
        if (explicitAutonomy != null) {explicitAxes.put(DispositionAxis.AUTONOMY, explicitAutonomy);}
        if (explicitConflictMode != null) {explicitAxes.put(DispositionAxis.CONFLICT_MODE, explicitConflictMode);}

        String mbtiType      = root.has("mbtiType") ? root.get("mbtiType").asText() : "";
        String enneagramType = root.has("enneagramType") ? root.get("enneagramType").asText() : "";

        PersonalityTypeDeriver.derive(
                new PersonalityInput(mbtiType, enneagramType, explicitProfile != null, explicitAxes),
                vocabRegistry, builder);

        return builder.build();}

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
