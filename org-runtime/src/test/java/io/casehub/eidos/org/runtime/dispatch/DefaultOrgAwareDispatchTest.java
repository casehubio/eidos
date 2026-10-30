package io.casehub.eidos.org.runtime.dispatch;

import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentRegistry;
import io.casehub.eidos.api.CapabilityHealth;
import io.casehub.eidos.api.CapabilityHealth.CapabilityStatus;
import io.casehub.eidos.api.CapabilityHealth.ProbeContext;
import io.casehub.eidos.org.api.OrgStructure;
import io.casehub.eidos.org.memory.InMemoryOrgRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultOrgAwareDispatchTest {

    private InMemoryOrgRegistry orgRegistry;
    private AgentRegistry agentRegistry;
    private CapabilityHealth capabilityHealth;
    private DefaultOrgAwareDispatch dispatch;

    @BeforeEach void setUp() {
        orgRegistry = new InMemoryOrgRegistry();
        agentRegistry = mock(AgentRegistry.class);
        capabilityHealth = mock(CapabilityHealth.class);
        dispatch = new DefaultOrgAwareDispatch(orgRegistry, agentRegistry, capabilityHealth);
    }

    // --- escalation ---

    @Test void escalateFindsFirstHealthyAgent() {
        var org = OrgStructure.define("acme")
            .unit("support").name("Support").kind("team")
                .member("l1", "tier-1")
                .member("l2", "tier-2")
                .member("l3", "tier-3")
                .add()
            .escalatesTo("l1", "l2").add()
            .escalatesTo("l2", "l3").add()
            .build();
        org.registerAll(orgRegistry);

        var l2Descriptor = AgentDescriptor.builder()
                .agentId("l2").name("L2").slot("tier-2").tenancyId("acme").build();
        when(agentRegistry.findById("l2", "acme")).thenReturn(Optional.of(l2Descriptor));
        when(capabilityHealth.probe(eq(l2Descriptor), eq("issue-resolution"), any(ProbeContext.class)))
                .thenReturn(new CapabilityStatus.Ready());

        var result = dispatch.escalate("l1", "issue-resolution", "acme");

        assertThat(result).isPresent();
        assertThat(result.get().descriptor().agentId()).isEqualTo("l2");
    }

    @Test void escalateSkipsUnhealthyAgent() {
        var org = OrgStructure.define("acme")
            .unit("support").name("Support").kind("team")
                .member("l1", "tier-1")
                .member("l2", "tier-2")
                .member("l3", "tier-3")
                .add()
            .escalatesTo("l1", "l2").add()
            .escalatesTo("l2", "l3").add()
            .build();
        org.registerAll(orgRegistry);

        var l2Descriptor = AgentDescriptor.builder()
                .agentId("l2").name("L2").slot("tier-2").tenancyId("acme").build();
        var l3Descriptor = AgentDescriptor.builder()
                .agentId("l3").name("L3").slot("tier-3").tenancyId("acme").build();

        when(agentRegistry.findById("l2", "acme")).thenReturn(Optional.of(l2Descriptor));
        when(agentRegistry.findById("l3", "acme")).thenReturn(Optional.of(l3Descriptor));

        when(capabilityHealth.probe(eq(l2Descriptor), eq("issue-resolution"), any(ProbeContext.class)))
                .thenReturn(new CapabilityStatus.Unavailable("offline"));
        when(capabilityHealth.probe(eq(l3Descriptor), eq("issue-resolution"), any(ProbeContext.class)))
                .thenReturn(new CapabilityStatus.Ready());

        var result = dispatch.escalate("l1", "issue-resolution", "acme");

        assertThat(result).isPresent();
        assertThat(result.get().descriptor().agentId()).isEqualTo("l3");
    }

    @Test void escalateReturnsEmptyWhenAllUnhealthy() {
        var org = OrgStructure.define("acme")
            .unit("support").name("Support").kind("team")
                .member("l1", "tier-1")
                .member("l2", "tier-2")
                .add()
            .escalatesTo("l1", "l2").add()
            .build();
        org.registerAll(orgRegistry);

        var l2Descriptor = AgentDescriptor.builder()
                .agentId("l2").name("L2").slot("tier-2").tenancyId("acme").build();
        when(agentRegistry.findById("l2", "acme")).thenReturn(Optional.of(l2Descriptor));
        when(capabilityHealth.probe(eq(l2Descriptor), eq("review"), any(ProbeContext.class)))
                .thenReturn(new CapabilityStatus.Unavailable("down"));

        assertThat(dispatch.escalate("l1", "review", "acme")).isEmpty();
    }

    @Test void escalateWithNoPathReturnsEmpty() {
        assertThat(dispatch.escalate("lone-agent", "review", "acme")).isEmpty();
    }

    // --- delegation ---

    @Test void canDelegateToSubordinate() {
        var org = OrgStructure.define("acme")
            .unit("eng").name("Engineering").kind("team")
                .member("alice", "lead")
                .member("bob", "dev")
                .add()
            .supervises("alice", "bob").add()
            .build();
        org.registerAll(orgRegistry);

        assertThat(dispatch.canDelegateTo("alice", "bob", "acme")).isTrue();
    }

    @Test void canDelegateToSameUnitPeer() {
        var org = OrgStructure.define("acme")
            .unit("eng").name("Engineering").kind("team")
                .member("alice", "dev")
                .member("bob", "dev")
                .add()
            .build();
        org.registerAll(orgRegistry);

        assertThat(dispatch.canDelegateTo("alice", "bob", "acme")).isTrue();
    }

    @Test void cannotDelegateToUnrelatedAgent() {
        var org = OrgStructure.define("acme")
            .unit("eng").name("Engineering").kind("team")
                .member("alice", "dev")
                .add()
            .unit("sales").name("Sales").kind("team")
                .member("bob", "rep")
                .add()
            .build();
        org.registerAll(orgRegistry);

        assertThat(dispatch.canDelegateTo("alice", "bob", "acme")).isFalse();
    }

    // --- team members ---

    @Test void teamMembersExcludesSelf() {
        var org = OrgStructure.define("acme")
            .unit("eng").name("Engineering").kind("team")
                .member("alice", "lead")
                .member("bob", "dev")
                .member("carol", "dev")
                .add()
            .build();
        org.registerAll(orgRegistry);

        var members = dispatch.teamMembers("bob", "acme");
        assertThat(members).containsExactlyInAnyOrder("alice", "carol");
        assertThat(members).doesNotContain("bob");
    }

    @Test void teamMembersAcrossMultipleUnits() {
        var org = OrgStructure.define("acme")
            .unit("eng").name("Engineering").kind("team")
                .member("alice", "dev")
                .member("bob", "dev")
                .add()
            .unit("review").name("Review Board").kind("committee")
                .member("alice", "reviewer")
                .member("carol", "reviewer")
                .add()
            .build();
        org.registerAll(orgRegistry);

        var members = dispatch.teamMembers("alice", "acme");
        assertThat(members).containsExactlyInAnyOrder("bob", "carol");
    }
}
