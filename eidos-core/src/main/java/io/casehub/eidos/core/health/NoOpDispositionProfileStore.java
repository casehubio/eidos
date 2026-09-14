package io.casehub.eidos.core.health;

import io.casehub.eidos.api.DispositionProfileStore;
import io.casehub.eidos.api.DispositionValue;

import java.util.List;

public class NoOpDispositionProfileStore implements DispositionProfileStore {
    @Override
    public void update(String agentId, String tenancyId, List<DispositionValue> newProfile) {}
}
