package io.casehub.eidos.core.health;

import io.casehub.eidos.api.DispositionProfileListener;
import io.casehub.eidos.api.DispositionValue;

import java.util.List;

public class NoOpDispositionProfileListener implements DispositionProfileListener {
    @Override
    public void onProfileEvolved(String agentId, String tenancyId, List<DispositionValue> newProfile) {}
}
