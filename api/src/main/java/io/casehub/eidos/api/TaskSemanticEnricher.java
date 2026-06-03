package io.casehub.eidos.api;

import java.util.OptionalInt;
import java.util.Set;

public interface TaskSemanticEnricher {
    Set<String> dispositionAxes(String capabilityTag, String taskDomain);
    boolean semanticallyEquivalent(String domainA, String domainB);
    OptionalInt significance(String capabilityTag, String taskDomain);
}
