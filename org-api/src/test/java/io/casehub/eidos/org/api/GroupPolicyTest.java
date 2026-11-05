package io.casehub.eidos.org.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GroupPolicyTest {

    @Test void defaultDrainTimeout() {
        var policy = new GroupPolicy(null, 0, null, 0);
        assertThat(policy.dissolutionDrainTimeoutMinutes())
            .isEqualTo(GroupPolicy.DEFAULT_DISSOLUTION_DRAIN_TIMEOUT_MINUTES);
    }

    @Test void customDrainTimeout() {
        var policy = new GroupPolicy(null, 0, null, 60);
        assertThat(policy.dissolutionDrainTimeoutMinutes()).isEqualTo(60);
    }

    @Test void rejectsNegativeDrainTimeout() {
        assertThatThrownBy(() -> new GroupPolicy(null, 0, null, -5))
            .isInstanceOf(OrgValidationException.class)
            .hasMessageContaining("dissolutionDrainTimeoutMinutes");
    }

    @Test void rejectsNegativeNormativePriority() {
        assertThatThrownBy(() -> new GroupPolicy(null, -1, null, 0))
            .isInstanceOf(OrgValidationException.class)
            .hasMessageContaining("normativePriority");
    }
}
