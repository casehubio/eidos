package io.casehub.eidos.api;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class DispositionSignalStoreDefaultsTest {

    @Test
    void valenceAwareRecordDelegatesToOriginal() {
        var recorded = new AtomicReference<String>();
        DispositionSignalStore store = new DispositionSignalStore() {
            @Override public void recordActivation(String agentId, String tenancyId, String functionTerm) {
                recorded.set(functionTerm);
            }
            @Override public Map<String, Integer> activationCounts(String a, String t) { return Map.of(); }
            @Override public void decay(String a, String t, double f) {}
            @Override public void clear(String a, String t) {}
        };

        store.recordActivation("agent1", "tenant1", "ti", SignalValence.NEGATIVE);
        assertThat(recorded.get()).isEqualTo("ti");
    }

    @Test
    void valenceCountsWrapsExistingCountsAsPositive() {
        DispositionSignalStore store = new DispositionSignalStore() {
            @Override public void recordActivation(String a, String t, String f) {}
            @Override public Map<String, Integer> activationCounts(String a, String t) {
                var m = new LinkedHashMap<String, Integer>();
                m.put("ti", 5);
                m.put("ne", 3);
                return m;
            }
            @Override public void decay(String a, String t, double f) {}
            @Override public void clear(String a, String t) {}
        };

        var counts = store.valenceCounts("a", "t");
        assertThat(counts.get("ti")).isEqualTo(new ValenceCounts(5, 0));
        assertThat(counts.get("ne")).isEqualTo(new ValenceCounts(3, 0));
    }
}
