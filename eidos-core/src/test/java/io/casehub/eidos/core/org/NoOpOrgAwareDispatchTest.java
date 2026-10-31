package io.casehub.eidos.core.org;

import io.casehub.eidos.api.OrgAwareDispatch;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NoOpOrgAwareDispatchTest {

    private final OrgAwareDispatch dispatch = new NoOpOrgAwareDispatch();

    @Test void escalateReturnsEmpty() {
        assertThat(dispatch.escalate("agent-1", "code-review", "t1")).isEmpty();
    }

    @Test void canDelegateToReturnsTrue() {
        assertThat(dispatch.canDelegateTo("from", "to", "t1")).isTrue();
    }

    @Test void teamMembersReturnsEmpty() {
        assertThat(dispatch.teamMembers("agent-1", "t1")).isEmpty();
    }

    @Test
    void orgDistanceReturnsNegativeOne() {
        assertThat(dispatch.orgDistance("a", "b", "t1")).isEqualTo(-1);
    }

}
