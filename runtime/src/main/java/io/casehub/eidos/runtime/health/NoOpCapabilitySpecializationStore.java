package io.casehub.eidos.runtime.health;

import io.casehub.eidos.api.CapabilitySpecializationStore;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;

@DefaultBean
@ApplicationScoped
public class NoOpCapabilitySpecializationStore implements CapabilitySpecializationStore {

    @Override
    public void recordDecline(String agentId, String tenancyId,
                               String capabilityName, String domain) {}

    @Override
    public void clearDeclines(String agentId, String tenancyId, String capabilityName) {}

    @Override
    public Map<String, Integer> learnedExclusions(String agentId, String tenancyId,
                                                   String capabilityName) {
        return Map.of();
    }

    @Override
    public int declineCount(String agentId, String tenancyId,
                             String capabilityName, String domain) {
        return 0;
    }
}
