package io.casehub.eidos.api;

import java.util.List;

@FunctionalInterface
public interface DispositionProfileStore {
    void update(String agentId, String tenancyId, List<DispositionValue> newProfile);
}
