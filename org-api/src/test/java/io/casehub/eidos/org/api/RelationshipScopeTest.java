package io.casehub.eidos.org.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RelationshipScopeTest {

    @Test void emptyScope() {
        var scope = new RelationshipScope(null, null, null);
        assertThat(scope.isEmpty()).isTrue();
    }

    @Test void capabilityScoped() {
        var scope = new RelationshipScope("code-review", null, null);
        assertThat(scope.isEmpty()).isFalse();
        assertThat(scope.capabilityName()).isEqualTo("code-review");
    }

    @Test void domainScoped() {
        var scope = new RelationshipScope(null, "infrastructure", null);
        assertThat(scope.isEmpty()).isFalse();
        assertThat(scope.domain()).isEqualTo("infrastructure");
    }

    @Test void fullyScoped() {
        var scope = new RelationshipScope("monitoring", "infrastructure", "region-eu");
        assertThat(scope.isEmpty()).isFalse();
    }
}
