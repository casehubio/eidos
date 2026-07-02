package io.casehub.eidos.memory;

import io.casehub.eidos.api.BehavioralSignal;
import io.casehub.eidos.api.BehavioralSignalStore;
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
public class InMemoryBehavioralSignalStore implements BehavioralSignalStore {

    @ConfigProperty(name = "casehub.eidos.behavioral-signal.decline-ttl-days", defaultValue = "30")
    int declineTtlDays;

    @ConfigProperty(name = "casehub.eidos.behavioral-signal.success-ttl-days", defaultValue = "30")
    int successTtlDays;

    @ConfigProperty(name = "casehub.eidos.behavioral-signal.compliant-ttl-days", defaultValue = "30")
    int compliantTtlDays;

    @ConfigProperty(name = "casehub.eidos.behavioral-signal.violated-ttl-days", defaultValue = "90")
    int violatedTtlDays;

    private final ConcurrentHashMap<StoreKey, ConcurrentHashMap<String, ConcurrentLinkedQueue<Instant>>>
        store = new ConcurrentHashMap<>();

    @Override
    public void record(final String agentId, final String tenancyId,
                       final String capabilityName, final String qualifier,
                       final BehavioralSignal signal) {
        final var key = new StoreKey(agentId, tenancyId, capabilityName, signal);
        final var qualifierQueues = store.computeIfAbsent(key, k -> new ConcurrentHashMap<>());
        final var queue = qualifierQueues.computeIfAbsent(qualifier, q -> new ConcurrentLinkedQueue<>());
        final Instant now = Instant.now();
        queue.removeIf(ts -> !now.isBefore(ts));
        queue.offer(now.plusSeconds((long) ttlDaysFor(signal) * 86400));
    }

    @Override
    public void clear(final String agentId, final String tenancyId,
                      final String capabilityName, final BehavioralSignal signal) {
        store.remove(new StoreKey(agentId, tenancyId, capabilityName, signal));
    }

    @Override
    public Map<String, Integer> learned(final String agentId, final String tenancyId,
                                         final String capabilityName,
                                         final BehavioralSignal signal) {
        final var qualifierQueues = store.get(new StoreKey(agentId, tenancyId, capabilityName, signal));
        if (qualifierQueues == null) return Map.of();
        final Instant now = Instant.now();
        final var result = new HashMap<String, Integer>();
        qualifierQueues.forEach((qualifier, queue) -> {
            final int cnt = (int) queue.stream().filter(ts -> now.isBefore(ts)).count();
            if (cnt > 0) result.put(qualifier, cnt);
        });
        return Map.copyOf(result);
    }

    @Override
    public int count(final String agentId, final String tenancyId,
                     final String capabilityName, final String qualifier,
                     final BehavioralSignal signal) {
        final var qualifierQueues = store.get(new StoreKey(agentId, tenancyId, capabilityName, signal));
        if (qualifierQueues == null) return 0;
        final var queue = qualifierQueues.get(qualifier);
        if (queue == null) return 0;
        final Instant now = Instant.now();
        return (int) queue.stream().filter(ts -> now.isBefore(ts)).count();
    }

    private int ttlDaysFor(final BehavioralSignal signal) {
        return switch (signal) {
            case DECLINE -> declineTtlDays;
            case SUCCESS -> successTtlDays;
            case COMPLIANT -> compliantTtlDays;
            case VIOLATED -> violatedTtlDays;
        };
    }

    private record StoreKey(String agentId, String tenancyId,
                             String capabilityName, BehavioralSignal signal) {}
}
