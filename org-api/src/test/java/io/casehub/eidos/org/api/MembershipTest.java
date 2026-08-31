package io.casehub.eidos.org.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MembershipTest {

    @Test void requiresAgentId() {
        assertThatThrownBy(() -> new Membership(null, "witness", null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test void roleIsOptional() {
        var m = new Membership("agent-1", null, null);
        assertThat(m.role()).isNull();
    }

    @Test void fullMembership() {
        var m = new Membership("agent-1", "witness", "urn:gastown:vocab:org");
        assertThat(m.agentId()).isEqualTo("agent-1");
        assertThat(m.role()).isEqualTo("witness");
        assertThat(m.roleVocabulary()).isEqualTo("urn:gastown:vocab:org");
    }
}
