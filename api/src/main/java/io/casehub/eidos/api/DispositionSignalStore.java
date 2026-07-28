package io.casehub.eidos.api;

import java.util.Map;

public interface DispositionSignalStore {

    void recordActivation(String agentId, String tenancyId, String functionTerm);

    Map<String, Integer> activationCounts(String agentId, String tenancyId);

    void decay(String agentId, String tenancyId, double decayFactor);

    void clear(String agentId, String tenancyId);
}
