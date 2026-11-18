package io.casehub.eidos.memory;

import io.casehub.eidos.api.DispositionSignalStore;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Alternative
@Priority(1)
@ApplicationScoped
public class InMemoryDispositionSignalStore implements DispositionSignalStore {

    private record StoreKey(String agentId, String tenancyId) {}

    private final ConcurrentHashMap<StoreKey, ConcurrentHashMap<String, Double>> store =
            new ConcurrentHashMap<>();

    @Override
    public void recordActivation(final String agentId, final String tenancyId,
                                 final String functionTerm) {
        store.computeIfAbsent(new StoreKey(agentId, tenancyId), k -> new ConcurrentHashMap<>())
             .merge(functionTerm, 1.0, Double::sum);
    }

    @Override
    public Map<String, Double> activationCounts(final String agentId,
                                                final String tenancyId) {
        final var functions = store.get(new StoreKey(agentId, tenancyId));
        if (functions == null) {return Map.of();}
        final var result = new HashMap<String, Double>();
        functions.forEach((term, weight) -> {
            if (weight > 0.0) {result.put(term, weight);}
        });
        return Map.copyOf(result);
    }

    @Override
    public void decay(final String agentId, final String tenancyId,
                      final double decayFactor) {
        final var functions = store.get(new StoreKey(agentId, tenancyId));
        if (functions == null) {return;}
        for (final var term : functions.keySet()) {
            functions.compute(term, (k, w) -> {
                if (w == null) {return null;}
                double decayed = w * decayFactor;
                return decayed > 0.0 ? decayed : null;
            });
        }
    }

    @Override
    public void clear(final String agentId, final String tenancyId) {
        store.computeIfPresent(new StoreKey(agentId, tenancyId), (k, v) -> null);
    }
}
