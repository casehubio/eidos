package io.casehub.eidos.org.runtime.topology;

import io.casehub.eidos.org.api.TopologyFamily;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class WellKnownTopologyFamilyClassifierTest {

    private final WellKnownTopologyFamilyClassifier classifier = new WellKnownTopologyFamilyClassifier();

    @ParameterizedTest
    @ValueSource(strings = {"swarm", "mesh"})
    void distributedTopologies(String topology) {
        assertThat(classifier.classify(topology)).contains(TopologyFamily.DISTRIBUTED);
    }

    @Test void hiveIsCentralised() {
        assertThat(classifier.classify("hive")).contains(TopologyFamily.CENTRALISED);
    }

    @Test void manyIsIndependent() {
        assertThat(classifier.classify("many")).contains(TopologyFamily.INDEPENDENT);
    }

    @ParameterizedTest
    @ValueSource(strings = {"custom-topo", "unknown", ""})
    void unknownTopologiesReturnEmpty(String topology) {
        assertThat(classifier.classify(topology)).isEmpty();
    }

    @Test void nullTopologyReturnsEmpty() {
        assertThat(classifier.classify(null)).isEmpty();
    }
}
