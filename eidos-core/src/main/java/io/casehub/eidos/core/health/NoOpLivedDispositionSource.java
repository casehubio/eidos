package io.casehub.eidos.core.health;

import io.casehub.eidos.api.AgentDisposition;
import io.casehub.eidos.api.LivedDispositionSource;

public class NoOpLivedDispositionSource implements LivedDispositionSource {

    @Override
    public AgentDisposition resolveEffective(String agentId, String tenancyId, AgentDisposition authored) {
        return authored;
    }
}
