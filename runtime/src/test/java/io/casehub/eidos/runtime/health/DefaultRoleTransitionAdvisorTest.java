package io.casehub.eidos.runtime.health;

import io.casehub.eidos.api.*;
import io.casehub.eidos.api.RoleHealth.RoleStatus;
import io.casehub.eidos.api.RoleHoldCycleStore.HoldCycleState;
import io.casehub.eidos.api.RoleTransitionAdvisor.TransitionAdvice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultRoleTransitionAdvisorTest {

    private StubRoleHoldCycleStore holdCycleStore;
    private DefaultRoleTransitionAdvisor advisor;

    @BeforeEach
    void setUp() {
        holdCycleStore = new StubRoleHoldCycleStore();
        advisor = new DefaultRoleTransitionAdvisor(holdCycleStore, 90);
    }

    @Test
    void policy_disabled_returns_policy_disabled() {
        var descriptor = descriptorWithPolicy(RoleChangePolicy.DISABLED);
        var advice = advisor.evaluate(descriptor, new RoleStatus.Fit("coordinator", 0.9));
        assertThat(advice).isInstanceOf(TransitionAdvice.PolicyDisabled.class);
    }

    @Test
    void undeclared_throws_illegal_argument() {
        var descriptor = descriptorWithPolicy(RoleChangePolicy.ADVISORY);
        assertThatThrownBy(() -> advisor.evaluate(descriptor,
                new RoleStatus.Undeclared("coordinator")))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fit_resets_hold_cycles_and_returns_promotion_for_auto() {
        var policy = new RoleChangePolicy(RoleChangePolicy.Mode.AUTO_ABOVE_THRESHOLD,
                0.7, 0.4, 2);
        var descriptor = descriptorWithPolicy(policy);
        holdCycleStore.increment("a1", "coordinator", "t1");
        holdCycleStore.increment("a1", "coordinator", "t1");

        var advice = advisor.evaluate(descriptor, new RoleStatus.Fit("coordinator", 0.85));
        assertThat(advice).isInstanceOf(TransitionAdvice.PromotionWarranted.class);
        assertThat(holdCycleStore.get("a1", "coordinator", "t1").consecutiveCycles()).isEqualTo(0);
    }

    @Test
    void fit_returns_hold_for_advisory_mode() {
        var descriptor = descriptorWithPolicy(RoleChangePolicy.ADVISORY);
        var advice = advisor.evaluate(descriptor, new RoleStatus.Fit("coordinator", 0.85));
        assertThat(advice).isInstanceOf(TransitionAdvice.Hold.class);
    }

    @Test
    void marginal_returns_hold() {
        var policy = new RoleChangePolicy(RoleChangePolicy.Mode.AUTO_ABOVE_THRESHOLD,
                0.7, 0.4, 2);
        var descriptor = descriptorWithPolicy(policy);
        var advice = advisor.evaluate(descriptor,
                new RoleStatus.Marginal("coordinator", 0.55, 15));
        assertThat(advice).isInstanceOf(TransitionAdvice.Hold.class);
    }

    @Test
    void unfit_accumulates_hold_cycles_before_demotion() {
        var policy = new RoleChangePolicy(RoleChangePolicy.Mode.AUTO_ABOVE_THRESHOLD,
                0.7, 0.4, 3);
        var descriptor = descriptorWithPolicy(policy);
        var unfit = new RoleStatus.Unfit("coordinator", 0.25, 10, 15);

        var advice1 = advisor.evaluate(descriptor, unfit);
        assertThat(advice1).isInstanceOf(TransitionAdvice.Hold.class);

        var advice2 = advisor.evaluate(descriptor, unfit);
        assertThat(advice2).isInstanceOf(TransitionAdvice.Hold.class);

        var advice3 = advisor.evaluate(descriptor, unfit);
        assertThat(advice3).isInstanceOf(TransitionAdvice.DemotionAdvised.class);
        var demotion = (TransitionAdvice.DemotionAdvised) advice3;
        assertThat(demotion.holdCyclesElapsed()).isEqualTo(3);
        assertThat(demotion.holdCyclesRequired()).isEqualTo(3);
    }

    @Test
    void declared_fit_resets_hold_cycles() {
        var policy = new RoleChangePolicy(RoleChangePolicy.Mode.AUTO_ABOVE_THRESHOLD,
                0.7, 0.4, 2);
        var descriptor = descriptorWithPolicy(policy);
        holdCycleStore.increment("a1", "coordinator", "t1");

        advisor.evaluate(descriptor, new RoleStatus.DeclaredFit("coordinator"));
        assertThat(holdCycleStore.get("a1", "coordinator", "t1").consecutiveCycles()).isEqualTo(0);
    }

    private static AgentDescriptor descriptorWithPolicy(RoleChangePolicy policy) {
        return AgentDescriptor.builder()
                .agentId("a1").name("Agent").version("1.0")
                .provider("test").modelFamily("test").modelVersion("v1")
                .slot("worker")
                .disposition(AgentDisposition.builder()
                        .socialOrient("collaborative").ruleFollowing("principled")
                        .riskAppetite("measured").autonomy("semi-autonomous").build())
                .tenancyId("t1")
                .roleProfile(new RoleProfile(
                        List.of(new RoleDeclaration("coordinator", null)), policy))
                .build();
    }

    static class StubRoleHoldCycleStore implements RoleHoldCycleStore {
        private final java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.atomic.AtomicReference<HoldCycleState>> store =
                new java.util.concurrent.ConcurrentHashMap<>();

        @Override
        public HoldCycleState get(final String agentId, final String roleName,
                                  final String tenancyId) {
            final var ref = store.get(key(agentId, roleName, tenancyId));
            return ref == null ? HoldCycleState.EMPTY : ref.get();
        }

        @Override
        public void increment(final String agentId, final String roleName,
                              final String tenancyId) {
            store.computeIfAbsent(key(agentId, roleName, tenancyId),
                                  k -> new java.util.concurrent.atomic.AtomicReference<>(HoldCycleState.EMPTY))
                 .updateAndGet(s -> new HoldCycleState(s.consecutiveCycles() + 1, java.time.Instant.now()));
        }

        @Override
        public void reset(final String agentId, final String roleName,
                          final String tenancyId) {
            store.computeIfAbsent(key(agentId, roleName, tenancyId),
                                  k -> new java.util.concurrent.atomic.AtomicReference<>(HoldCycleState.EMPTY))
                 .set(new HoldCycleState(0, java.time.Instant.now()));
        }

        private static String key(final String agentId, final String roleName,
                                  final String tenancyId) {
            return agentId + "|" + roleName + "|" + tenancyId;
        }
    }

}
