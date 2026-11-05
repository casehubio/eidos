package io.casehub.eidos.org.api;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class TopologyFamilyTest {

    @Test void hasExpectedValues() {
        assertThat(TopologyFamily.values()).containsExactly(
            TopologyFamily.DISTRIBUTED,
            TopologyFamily.CENTRALISED,
            TopologyFamily.INDEPENDENT
        );
    }

    @Test void classifierContractReturnsOptional() {
        TopologyFamilyClassifier classifier = t -> Optional.of(TopologyFamily.DISTRIBUTED);
        assertThat(classifier.classify("swarm")).contains(TopologyFamily.DISTRIBUTED);
    }

    @Test void classifierCanReturnEmpty() {
        TopologyFamilyClassifier classifier = t -> Optional.empty();
        assertThat(classifier.classify("custom-topo")).isEmpty();
    }
}
