package io.casehub.eidos.api;

import java.util.*;
import java.util.stream.Collectors;

public final class AgentDescriptorComparator {

    static final int COMPARED_FIELD_COUNT = 16;
    static final int COMPARED_CAPABILITY_FIELD_COUNT = 8;
    static final int COMPARED_DISPOSITION_FIELD_COUNT = 6;

    public record ComparisonResult(List<FieldDrift> drifts) {
        public boolean matches() { return drifts.isEmpty(); }
    }

    public record FieldDrift(String field, String desiredValue, String actualValue) {}

    private AgentDescriptorComparator() {}

    public static ComparisonResult compare(AgentDescriptor desired, AgentDescriptor actual) {
        List<FieldDrift> drifts = new ArrayList<>();

        compareSimpleFields(drifts, desired, actual);
        compareAxisVocabularies(drifts, desired.axisVocabularies(), actual.axisVocabularies());
        compareDisposition(drifts, desired.disposition(), actual.disposition());
        compareCapabilities(drifts, desired.capabilities(), actual.capabilities());

        return new ComparisonResult(List.copyOf(drifts));
    }

    private static void compareSimpleFields(List<FieldDrift> drifts, AgentDescriptor desired, AgentDescriptor actual) {
        compareField(drifts, "name", desired.name(), actual.name());
        compareField(drifts, "slot", desired.slot(), actual.slot());
        compareField(drifts, "version", desired.version(), actual.version());
        compareField(drifts, "provider", desired.provider(), actual.provider());
        compareField(drifts, "modelFamily", desired.modelFamily(), actual.modelFamily());
        compareField(drifts, "modelVersion", desired.modelVersion(), actual.modelVersion());
        compareField(drifts, "weightsFingerprint", desired.weightsFingerprint(), actual.weightsFingerprint());
        compareField(drifts, "domainVocabulary", desired.domainVocabulary(), actual.domainVocabulary());
        compareField(drifts, "slotVocabulary", desired.slotVocabulary(), actual.slotVocabulary());
        compareField(drifts, "dispositionVocabulary", desired.dispositionVocabulary(), actual.dispositionVocabulary());
        compareField(drifts, "jurisdiction", desired.jurisdiction(), actual.jurisdiction());
        compareField(drifts, "dataHandlingPolicy", desired.dataHandlingPolicy(), actual.dataHandlingPolicy());
        compareField(drifts, "briefing", desired.briefing(), actual.briefing());
    }

    private static void compareAxisVocabularies(List<FieldDrift> drifts,
                                                 Map<DispositionAxis, String> desired,
                                                 Map<DispositionAxis, String> actual) {
        Map<DispositionAxis, String> d = desired != null ? desired : Map.of();
        Map<DispositionAxis, String> a = actual != null ? actual : Map.of();
        Set<DispositionAxis> allKeys = new TreeSet<>(Comparator.comparing(Enum::name));
        allKeys.addAll(d.keySet());
        allKeys.addAll(a.keySet());
        for (DispositionAxis key : allKeys) {
            compareField(drifts, "axisVocabularies[" + key.name() + "]", d.get(key), a.get(key));
        }
    }

    private static void compareDisposition(List<FieldDrift> drifts,
                                            AgentDisposition desired,
                                            AgentDisposition actual) {
        if (desired == null && actual == null) return;
        if (desired == null || actual == null) {
            drifts.add(new FieldDrift("disposition", String.valueOf(desired), String.valueOf(actual)));
            return;
        }
        compareField(drifts, "disposition.socialOrient", desired.socialOrient(), actual.socialOrient());
        compareField(drifts, "disposition.ruleFollowing", desired.ruleFollowing(), actual.ruleFollowing());
        compareField(drifts, "disposition.riskAppetite", desired.riskAppetite(), actual.riskAppetite());
        compareField(drifts, "disposition.autonomy", desired.autonomy(), actual.autonomy());
        compareField(drifts, "disposition.conflictMode", desired.conflictMode(), actual.conflictMode());
        if (desired.delegation() != actual.delegation()) {
            drifts.add(new FieldDrift("disposition.delegation",
                    String.valueOf(desired.delegation()), String.valueOf(actual.delegation())));
        }
    }

    private static void compareCapabilities(List<FieldDrift> drifts,
                                             List<AgentCapability> desired,
                                             List<AgentCapability> actual) {
        Map<String, AgentCapability> desiredByName = desired.stream()
                .collect(Collectors.toMap(AgentCapability::name, c -> c));
        Map<String, AgentCapability> actualByName = actual.stream()
                .collect(Collectors.toMap(AgentCapability::name, c -> c));

        for (String name : new TreeSet<>(desiredByName.keySet())) {
            if (!actualByName.containsKey(name)) {
                drifts.add(new FieldDrift("capabilities[" + name + "]", "(present)", "(absent)"));
            }
        }
        for (String name : new TreeSet<>(actualByName.keySet())) {
            if (!desiredByName.containsKey(name)) {
                drifts.add(new FieldDrift("capabilities[" + name + "]", "(absent)", "(present)"));
            }
        }
        for (var entry : new TreeMap<>(desiredByName).entrySet()) {
            AgentCapability actualCap = actualByName.get(entry.getKey());
            if (actualCap != null) {
                compareCapability(drifts, entry.getKey(), entry.getValue(), actualCap);
            }
        }
    }

    private static void compareCapability(List<FieldDrift> drifts, String capName,
                                           AgentCapability desired, AgentCapability actual) {
        String prefix = "capabilities[" + capName + "].";
        compareField(drifts, prefix + "qualityHint", desired.qualityHint(), actual.qualityHint());
        compareField(drifts, prefix + "latencyHintP50Ms", desired.latencyHintP50Ms(), actual.latencyHintP50Ms());
        compareField(drifts, prefix + "costHint", desired.costHint(), actual.costHint());
        compareField(drifts, prefix + "inputTypes", desired.inputTypes(), actual.inputTypes());
        compareField(drifts, prefix + "outputTypes", desired.outputTypes(), actual.outputTypes());
        compareField(drifts, prefix + "tags", desired.tags(), actual.tags());
        compareField(drifts, prefix + "epistemicDomains", desired.epistemicDomains(), actual.epistemicDomains());
        compareField(drifts, prefix + "excludedDomains", desired.excludedDomains(), actual.excludedDomains());
    }

    private static void compareField(List<FieldDrift> drifts, String field, Object desired, Object actual) {
        if (!Objects.equals(desired, actual)) {
            drifts.add(new FieldDrift(field, String.valueOf(desired), String.valueOf(actual)));
        }
    }
}
