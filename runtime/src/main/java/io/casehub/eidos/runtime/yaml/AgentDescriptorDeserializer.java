package io.casehub.eidos.runtime.yaml;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.casehub.eidos.api.AgentCapability;
import io.casehub.eidos.api.AgentConstraint;
import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentDisposition;
import io.casehub.eidos.api.AgentGoal;
import io.casehub.eidos.api.ConstraintSeverity;
import io.casehub.eidos.api.DispositionAxis;
import io.casehub.eidos.api.GoalPriority;
import io.casehub.eidos.api.TemplateRef;
import io.casehub.eidos.api.Visibility;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class AgentDescriptorDeserializer extends JsonDeserializer<AgentDescriptor> {

    @Override
    public AgentDescriptor deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        ObjectNode root = p.readValueAsTree();
        var builder = AgentDescriptor.builder();

        builder.agentId(stringField(root, "agentId"));
        builder.name(stringField(root, "name"));
        builder.slot(stringField(root, "slot"));
        builder.tenancyId(root.has("tenancyId") ? root.get("tenancyId").asText() : "default");

        ifString(root, "version", builder::version);
        ifString(root, "provider", builder::provider);
        ifString(root, "modelFamily", builder::modelFamily);
        ifString(root, "modelVersion", builder::modelVersion);
        ifString(root, "weightsFingerprint", builder::weightsFingerprint);
        ifString(root, "domainVocabulary", builder::domainVocabulary);
        ifString(root, "slotVocabulary", builder::slotVocabulary);
        ifString(root, "dispositionVocabulary", builder::dispositionVocabulary);
        ifString(root, "styleVocabulary", builder::styleVocabulary);
        ifString(root, "jurisdiction", builder::jurisdiction);
        ifString(root, "dataHandlingPolicy", builder::dataHandlingPolicy);
        ifString(root, "briefing", builder::briefing);

        if (root.has("axisVocabularies")) {
            var axisMap = new LinkedHashMap<DispositionAxis, String>();
            root.get("axisVocabularies").fields().forEachRemaining(e ->
                axisMap.put(DispositionAxis.valueOf(e.getKey()), e.getValue().asText()));
            builder.axisVocabularies(axisMap);
        }

        if (root.has("disposition")) {
            builder.disposition(ctxt.readTreeAsValue(root.get("disposition"), AgentDisposition.class));
        }

        if (root.has("capabilities") && root.get("capabilities").isArray()) {
            var caps = new ArrayList<AgentCapability>();
            for (JsonNode capNode : root.get("capabilities")) {
                caps.add(deserializeCapability(capNode));
            }
            builder.capabilities(caps);
        }

        if (root.has("goals") && root.get("goals").isArray()) {
            var goals = new ArrayList<AgentGoal>();
            for (JsonNode goalNode : root.get("goals")) {
                goals.add(deserializeGoal(goalNode));
            }
            builder.goals(goals);
        }

        if (root.has("constraints") && root.get("constraints").isArray()) {
            var constraints = new ArrayList<AgentConstraint>();
            for (JsonNode cNode : root.get("constraints")) {
                constraints.add(deserializeConstraint(cNode));
            }
            builder.constraints(constraints);
        }

        if (root.has("templates") && root.get("templates").isArray()) {
            var templates = new ArrayList<TemplateRef>();
            for (JsonNode tNode : root.get("templates")) {
                String ref = tNode.get("ref").asText();
                Map<String, String> args = new LinkedHashMap<>();
                if (tNode.has("args")) {
                    tNode.get("args").fields().forEachRemaining(e ->
                        args.put(e.getKey(), e.getValue().asText()));
                }
                templates.add(new TemplateRef(ref, args));
            }
            builder.templates(templates);
        }

        return builder.build();
    }

    private AgentCapability deserializeCapability(JsonNode node) {
        var b = new AgentCapability.Builder().name(node.get("name").asText());
        if (node.has("description")) b.description(node.get("description").asText());
        if (node.has("capabilityVocabulary")) b.capabilityVocabulary(node.get("capabilityVocabulary").asText());
        if (node.has("qualityHint")) b.qualityHint(node.get("qualityHint").asDouble());
        if (node.has("latencyHintP50Ms")) b.latencyHintP50Ms(node.get("latencyHintP50Ms").asLong());
        if (node.has("costHint")) b.costHint(node.get("costHint").asText());
        if (node.has("inputTypes")) b.inputTypes(stringList(node.get("inputTypes")));
        if (node.has("outputTypes")) b.outputTypes(stringList(node.get("outputTypes")));
        if (node.has("tags")) b.tags(stringList(node.get("tags")));
        if (node.has("epistemicDomains")) {
            var map = new LinkedHashMap<String, Double>();
            node.get("epistemicDomains").fields().forEachRemaining(e ->
                map.put(e.getKey(), e.getValue().asDouble()));
            b.epistemicDomains(map);
        }
        if (node.has("excludedDomains")) b.excludedDomains(new LinkedHashSet<>(stringList(node.get("excludedDomains"))));
        return b.build();
    }

    private AgentGoal deserializeGoal(JsonNode node) {
        Map<String, String> attributes = null;
        if (node.has("attributes") && node.get("attributes").isObject()) {
            attributes = new LinkedHashMap<>();
            var attrs = attributes;
            node.get("attributes").fields().forEachRemaining(e ->
                                                                     attrs.put(e.getKey(), e.getValue().asText()));
        }
        return new AgentGoal(
                node.get("name").asText(),
                node.get("description").asText(),
                node.has("priority") ? GoalPriority.valueOf(node.get("priority").asText()) : GoalPriority.PRIMARY,
                node.has("visibility") ? Visibility.valueOf(node.get("visibility").asText()) : Visibility.PUBLIC,
                node.has("capabilities") ? stringList(node.get("capabilities")) : List.of(),
                attributes);
    }

    private AgentConstraint deserializeConstraint(JsonNode node) {
        return new AgentConstraint(
                node.get("name").asText(),
                node.get("description").asText(),
                node.has("visibility") ? Visibility.valueOf(node.get("visibility").asText()) : Visibility.PUBLIC,
                node.has("severity") ? ConstraintSeverity.valueOf(node.get("severity").asText()) : ConstraintSeverity.HARD);
    }

    private static String stringField(ObjectNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : null;
    }

    private static void ifString(ObjectNode node, String field, Consumer<String> setter) {
        if (node.has(field) && !node.get(field).isNull()) setter.accept(node.get(field).asText());
    }

    private static List<String> stringList(JsonNode arrayNode) {
        var list = new ArrayList<String>();
        for (JsonNode item : arrayNode) list.add(item.asText());
        return list;
    }
}
