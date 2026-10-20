package io.casehub.eidos.core.yaml;

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

        var explicitAxes = new java.util.EnumMap<DispositionAxis, String>(DispositionAxis.class);

        for (var axis : DispositionAxis.values()) {
            String key = axis.jsonKey();
            if (!root.has(key) || root.get(key).isNull()) {continue;}

            JsonNode axisNode = root.get(key);
            if (axisNode.isArray()) {
                List<DispositionValue> values = parseWeightedList(root, key);
                if (values != null && !values.isEmpty()) {
                    setAxisValues(builder, axis, values);
                    explicitAxes.put(axis, values.get(0).term());
                }
            } else {
                String term = axisNode.asText();
                setAxisTerm(builder, axis, term);
                explicitAxes.put(axis, term);
            }
        }

        if (root.has("delegation")) {builder.delegation(root.get("delegation").asBoolean());}

        List<DispositionValue> explicitProfile = parseWeightedList(root, "dispositionProfile");
        if (explicitProfile != null) {builder.dispositionProfile(explicitProfile);}

        List<DispositionValue> styleProfile = parseWeightedList(root, "styleProfile");
        if (styleProfile != null) {builder.styleProfile(styleProfile);}

        String mbtiType      = root.has("mbtiType") ? root.get("mbtiType").asText() : "";
        String enneagramType = root.has("enneagramType") ? root.get("enneagramType").asText() : "";

        PersonalityTypeDeriver.derive(
                new PersonalityInput(mbtiType, enneagramType, explicitProfile != null, explicitAxes),
                vocabRegistry, builder);

        builder.mbtiType(mbtiType.isEmpty() ? null : mbtiType);
        builder.enneagramType(enneagramType.isEmpty() ? null : enneagramType);

        return builder.build();
    }

    private static List<DispositionValue> parseWeightedList(ObjectNode node, String field) {
        if (!node.has(field) || !node.get(field).isArray()) return null;
        var values = new ArrayList<DispositionValue>();
        for (JsonNode item : node.get(field)) {
            values.add(new DispositionValue(item.get("term").asText(), item.get("weight").asDouble()));
        }
        return values;
    }

    private static void setAxisTerm(AgentDisposition.Builder builder, DispositionAxis axis, String term) {
        switch (axis) {
            case SOCIAL_ORIENTATION -> builder.socialOrient(term);
            case RULE_FOLLOWING -> builder.ruleFollowing(term);
            case RISK_APPETITE -> builder.riskAppetite(term);
            case AUTONOMY -> builder.autonomy(term);
            case CONFLICT_MODE -> builder.conflictMode(term);
        }
    }

    private static void setAxisValues(AgentDisposition.Builder builder, DispositionAxis axis, List<DispositionValue> values) {
        switch (axis) {
            case SOCIAL_ORIENTATION -> builder.socialOrient(values);
            case RULE_FOLLOWING -> builder.ruleFollowing(values);
            case RISK_APPETITE -> builder.riskAppetite(values);
            case AUTONOMY -> builder.autonomy(values);
            case CONFLICT_MODE -> builder.conflictMode(values);
        }
    }

}
