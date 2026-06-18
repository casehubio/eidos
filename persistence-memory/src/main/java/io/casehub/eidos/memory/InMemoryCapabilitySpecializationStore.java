package io.casehub.eidos.memory;

import io.casehub.eidos.api.CapabilitySpecializationStore;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Alternative
@Priority(1)
@ApplicationScoped
public class InMemoryCapabilitySpecializationStore implements CapabilitySpecializationStore {

    @ConfigProperty(name = "casehub.eidos.specialization.decline-ttl-days", defaultValue = "30")
    private int declineTtlDays;

    private final ConcurrentHashMap<AgentCapKey, ConcurrentHashMap<String, ConcurrentLinkedQueue<Instant>>>
        store = new ConcurrentHashMap<>();

    @Override
    public void recordDecline(final String agentId, final String tenancyId,
                               final String capabilityName, final String domain) {
        final var key = new AgentCapKey(agentId, tenancyId, capabilityName);
        final var domainQueues = store.computeIfAbsent(key, k -> new ConcurrentHashMap<>());
        final var queue = domainQueues.computeIfAbsent(domain, d -> new ConcurrentLinkedQueue<>());
        final Instant now = Instant.now();
        queue.removeIf(ts -> !now.isBefore(ts));
        queue.offer(now.plusSeconds((long) declineTtlDays * 86400));
    }

    @Override
    public void clearDeclines(final String agentId, final String tenancyId,
                               final String capabilityName) {
        store.remove(new AgentCapKey(agentId, tenancyId, capabilityName));
    }

    @Override
    public Map<String, Integer> learnedExclusions(final String agentId, final String tenancyId,
                                                   final String capabilityName) {
        final var domainQueues = store.get(new AgentCapKey(agentId, tenancyId, capabilityName));
        if (domainQueues == null) return Map.of();
        final Instant now = Instant.now();
        final var result = new HashMap<String, Integer>();
        domainQueues.forEach((domain, queue) -> {
            final int count = (int) queue.stream().filter(ts -> now.isBefore(ts)).count();
            if (count > 0) result.put(domain, count);
        });
        return Map.copyOf(result);
    }

    @Override
    public int declineCount(final String agentId, final String tenancyId,
                             final String capabilityName, final String domain) {
        final var domainQueues = store.get(new AgentCapKey(agentId, tenancyId, capabilityName));
        if (domainQueues == null) return 0;
        final var queue = domainQueues.get(domain);
        if (queue == null) return 0;
        final Instant now = Instant.now();
        return (int) queue.stream().filter(ts -> now.isBefore(ts)).count();
    }

    private record AgentCapKey(String agentId, String tenancyId, String capabilityName) {}
}
