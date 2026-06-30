package io.casehub.eidos.memory;

import io.casehub.eidos.api.CapabilitySpecializationStore;
import io.casehub.eidos.api.SpecializationSignal;
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
    int declineTtlDays;

    @ConfigProperty(name = "casehub.eidos.specialization.success-ttl-days", defaultValue = "30")
    int successTtlDays;

    private final ConcurrentHashMap<StoreKey, ConcurrentHashMap<String, ConcurrentLinkedQueue<Instant>>>
        store = new ConcurrentHashMap<>();

    @Override
    public void record(final String agentId, final String tenancyId,
                       final String capabilityName, final String domain,
                       final SpecializationSignal signal) {
        final var key = new StoreKey(agentId, tenancyId, capabilityName, signal);
        final var domainQueues = store.computeIfAbsent(key, k -> new ConcurrentHashMap<>());
        final var queue = domainQueues.computeIfAbsent(domain, d -> new ConcurrentLinkedQueue<>());
        final Instant now = Instant.now();
        queue.removeIf(ts -> !now.isBefore(ts));
        queue.offer(now.plusSeconds((long) ttlDaysFor(signal) * 86400));
    }

    @Override
    public void clear(final String agentId, final String tenancyId,
                      final String capabilityName, final SpecializationSignal signal) {
        store.remove(new StoreKey(agentId, tenancyId, capabilityName, signal));
    }

    @Override
    public Map<String, Integer> learned(final String agentId, final String tenancyId,
                                         final String capabilityName,
                                         final SpecializationSignal signal) {
        final var domainQueues = store.get(new StoreKey(agentId, tenancyId, capabilityName, signal));
        if (domainQueues == null) return Map.of();
        final Instant now = Instant.now();
        final var result = new HashMap<String, Integer>();
        domainQueues.forEach((domain, queue) -> {
            final int cnt = (int) queue.stream().filter(ts -> now.isBefore(ts)).count();
            if (cnt > 0) result.put(domain, cnt);
        });
        return Map.copyOf(result);
    }

    @Override
    public int count(final String agentId, final String tenancyId,
                     final String capabilityName, final String domain,
                     final SpecializationSignal signal) {
        final var domainQueues = store.get(new StoreKey(agentId, tenancyId, capabilityName, signal));
        if (domainQueues == null) return 0;
        final var queue = domainQueues.get(domain);
        if (queue == null) return 0;
        final Instant now = Instant.now();
        return (int) queue.stream().filter(ts -> now.isBefore(ts)).count();
    }

    private int ttlDaysFor(final SpecializationSignal signal) {
        return signal == SpecializationSignal.DECLINE ? declineTtlDays : successTtlDays;
    }

    private record StoreKey(String agentId, String tenancyId,
                             String capabilityName, SpecializationSignal signal) {}
}
