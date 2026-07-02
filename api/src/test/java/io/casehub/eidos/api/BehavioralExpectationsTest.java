package io.casehub.eidos.api;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BehavioralExpectationsTest {

    @Test
    void latencyBound_returns_hint_when_present() {
        var cap = AgentCapability.builder().name("code-review")
                .latencyHintP50Ms(5000L).build();
        assertThat(BehavioralExpectations.latencyBound(cap)).hasValue(5000L);
    }

    @Test
    void latencyBound_empty_when_no_hint() {
        var cap = AgentCapability.builder().name("code-review").build();
        assertThat(BehavioralExpectations.latencyBound(cap)).isEmpty();
    }

    @Test
    void delegationExpected_true_when_delegation_flag_set() {
        var disp = AgentDisposition.builder().delegation(true).build();
        assertThat(BehavioralExpectations.delegationExpected(disp)).isTrue();
    }

    @Test
    void delegationExpected_false_when_delegation_not_set() {
        var disp = AgentDisposition.builder().build();
        assertThat(BehavioralExpectations.delegationExpected(disp)).isFalse();
    }

    @Test
    void delegationExpected_false_when_null_disposition() {
        assertThat(BehavioralExpectations.delegationExpected(null)).isFalse();
    }
}
