package io.casehub.eidos.examples;

import io.casehub.eidos.api.*;
import io.casehub.eidos.api.CapabilityHealth.ProbeContext;
import io.casehub.eidos.api.RoleHealth.RoleStatus;
import io.casehub.eidos.api.RoleTransitionAdvisor.TransitionAdvice;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class RoleHealthScenarioTest {

    @Inject RoleHealth roleHealth;
    @Inject RoleTransitionAdvisor transitionAdvisor;
    @Inject RoleHoldCycleStore holdCycleStore;

    @Test
    void agent_with_declared_role_and_no_history_is_declared_fit() {
        var descriptor = buildDescriptorWithRole("coordinator",
                new RoleChangePolicy(RoleChangePolicy.Mode.AUTO_ABOVE_THRESHOLD,
                        0.7, 0.4, 2));
        var status = roleHealth.probe(descriptor, "coordinator", ProbeContext.of(null));
        assertThat(status).isInstanceOf(RoleStatus.DeclaredFit.class);
        assertThat(status.roleName()).isEqualTo("coordinator");
    }

    @Test
    void agent_without_role_profile_is_undeclared() {
        var descriptor = AgentDescriptor.builder()
                .agentId("no-role-agent").name("Agent").tenancyId("role-health-test")
                .slot("worker")
                .disposition(AgentDisposition.builder().build())
                .build();
        var status = roleHealth.probe(descriptor, "coordinator", ProbeContext.of(null));
        assertThat(status).isInstanceOf(RoleStatus.Undeclared.class);
    }

    @Test
    void transition_advisor_returns_policy_disabled_for_disabled_policy() {
        var descriptor = buildDescriptorWithRole("coordinator", RoleChangePolicy.DISABLED);
        var status = new RoleStatus.Fit("coordinator", 0.9);
        var advice = transitionAdvisor.evaluate(descriptor, status);
        assertThat(advice).isInstanceOf(TransitionAdvice.PolicyDisabled.class);
    }

    @Test
    void transition_advisor_accumulates_hold_cycles_for_unfit_role() {
        var policy = new RoleChangePolicy(RoleChangePolicy.Mode.AUTO_ABOVE_THRESHOLD,
                0.7, 0.4, 2);
        var descriptor = buildDescriptorWithRole("hold-cycle-role", policy);
        var unfit = new RoleStatus.Unfit("hold-cycle-role", 0.25, 10, 15);

        var advice1 = transitionAdvisor.evaluate(descriptor, unfit);
        assertThat(advice1).isInstanceOf(TransitionAdvice.Hold.class);

        var advice2 = transitionAdvisor.evaluate(descriptor, unfit);
        assertThat(advice2).isInstanceOf(TransitionAdvice.DemotionAdvised.class);
        var demotion = (TransitionAdvice.DemotionAdvised) advice2;
        assertThat(demotion.holdCyclesElapsed()).isEqualTo(2);
        assertThat(demotion.holdCyclesRequired()).isEqualTo(2);
    }

    @Test
    void fit_status_resets_accumulated_hold_cycles() {
        var policy = new RoleChangePolicy(RoleChangePolicy.Mode.AUTO_ABOVE_THRESHOLD,
                0.7, 0.4, 3);
        var descriptor = buildDescriptorWithRole("reset-role", policy);
        var unfit = new RoleStatus.Unfit("reset-role", 0.25, 10, 15);

        transitionAdvisor.evaluate(descriptor, unfit);
        transitionAdvisor.evaluate(descriptor, unfit);
        assertThat(holdCycleStore.get("role-health-agent", "reset-role",
                "role-health-test").consecutiveCycles()).isEqualTo(2);

        var fit = new RoleStatus.Fit("reset-role", 0.85);
        var advice = transitionAdvisor.evaluate(descriptor, fit);
        assertThat(advice).isInstanceOf(TransitionAdvice.PromotionWarranted.class);
        assertThat(holdCycleStore.get("role-health-agent", "reset-role",
                "role-health-test").consecutiveCycles()).isEqualTo(0);
    }

    private static AgentDescriptor buildDescriptorWithRole(String roleName,
                                                            RoleChangePolicy policy) {
        return AgentDescriptor.builder()
                .agentId("role-health-agent").name("Agent").tenancyId("role-health-test")
                .slot("worker")
                .disposition(AgentDisposition.builder().build())
                .roleProfile(new RoleProfile(
                        List.of(new RoleDeclaration(roleName, null)), policy))
                .build();
    }
}
