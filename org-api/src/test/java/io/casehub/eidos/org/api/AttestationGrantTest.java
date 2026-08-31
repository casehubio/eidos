package io.casehub.eidos.org.api;

import io.casehub.eidos.api.BehavioralSignal;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AttestationGrantTest {

    @Test void requiresDimensions() {
        assertThatThrownBy(() -> new AttestationGrant(Set.of(), Set.of(), Set.of()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void nullDimensionsRejected() {
        assertThatThrownBy(() -> new AttestationGrant(null, Set.of(), Set.of()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void validGrant() {
        var grant = new AttestationGrant(
            Set.of("LATENCY"), Set.of("code-review"),
            Set.of(BehavioralSignal.VIOLATED));
        assertThat(grant.dimensions()).containsExactly("LATENCY");
        assertThat(grant.capabilityScope()).containsExactly("code-review");
        assertThat(grant.signalTypes()).containsExactly(BehavioralSignal.VIOLATED);
    }

    @Test void nullCapabilityScopeDefaultsToEmpty() {
        var grant = new AttestationGrant(Set.of("LATENCY"), null, null);
        assertThat(grant.capabilityScope()).isEmpty();
        assertThat(grant.signalTypes()).isEmpty();
    }

    @Test void setsAreImmutable() {
        var grant = new AttestationGrant(
            Set.of("LATENCY", "ATTESTATION_RATE"), Set.of(), Set.of());
        assertThatThrownBy(() -> grant.dimensions().add("NEW"))
            .isInstanceOf(UnsupportedOperationException.class);
    }
}
