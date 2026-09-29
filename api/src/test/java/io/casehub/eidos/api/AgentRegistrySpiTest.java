package io.casehub.eidos.api;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import static org.assertj.core.api.Assertions.*;

class AgentRegistrySpiTest {

    @Test
    void anonymous_implementation_satisfies_contract() {
        AgentRegistry registry = new AgentRegistry() {
            @Override public void register(AgentDescriptor d) {}
            @Override public Optional<AgentDescriptor> findById(String id, String tenancyId) { return Optional.empty(); }
            @Override public List<AgentMatch> find(AgentQuery q) { return List.of(); }
        };
        assertThat(registry.findById("x", "default")).isEmpty();
        assertThat(registry.find(AgentQuery.all("default"))).isEmpty();
    }

    private AgentRegistry storeBackedRegistry(ConcurrentHashMap<String, AgentDescriptor> store) {
        return new AgentRegistry() {
            @Override public void register(AgentDescriptor d) { store.put(d.agentId(), d); }
            @Override public Optional<AgentDescriptor> findById(String id, String tid) {
                return Optional.ofNullable(store.get(id)).filter(d -> d.tenancyId().equals(tid));
            }
            @Override public List<AgentMatch> find(AgentQuery q) { return List.of(); }
        };
    }

    @Test
    void updateGoalLifecycleState_transitionsMatchingGoal() {
        var store = new ConcurrentHashMap<String, AgentDescriptor>();
        var registry = storeBackedRegistry(store);

        var goal = new AgentGoal("research", "Do research", GoalPriority.PRIMARY,
                Visibility.PUBLIC, List.of(), null, GoalLifecycleState.ACTIVE, null);
        registry.register(AgentDescriptor.builder()
                .agentId("a1").name("Agent").slot("default").tenancyId("t1")
                .goals(List.of(goal)).build());

        registry.updateGoalLifecycleState("a1", "t1", "research", GoalLifecycleState.DORMANT);

        var updated = registry.findById("a1", "t1").orElseThrow();
        assertThat(updated.goals()).hasSize(1);
        assertThat(updated.goals().get(0).lifecycleState()).isEqualTo(GoalLifecycleState.DORMANT);
    }

    @Test
    void updateGoalLifecycleState_leavesOtherGoalsUnchanged() {
        var store = new ConcurrentHashMap<String, AgentDescriptor>();
        var registry = storeBackedRegistry(store);

        registry.register(AgentDescriptor.builder()
                .agentId("a1").name("Agent").slot("default").tenancyId("t1")
                .goals(List.of(
                        new AgentGoal("research", "Research", GoalPriority.PRIMARY,
                                Visibility.PUBLIC, List.of(), null, GoalLifecycleState.ACTIVE, null),
                        new AgentGoal("learn", "Learn", GoalPriority.SECONDARY,
                                Visibility.PUBLIC, List.of(), null, GoalLifecycleState.ACTIVE, null)))
                .build());

        registry.updateGoalLifecycleState("a1", "t1", "research", GoalLifecycleState.ABANDONED);

        var updated = registry.findById("a1", "t1").orElseThrow();
        assertThat(updated.goals()).hasSize(2);
        assertThat(updated.goals().stream().filter(g -> g.name().equals("research"))
                .findFirst().orElseThrow().lifecycleState()).isEqualTo(GoalLifecycleState.ABANDONED);
        assertThat(updated.goals().stream().filter(g -> g.name().equals("learn"))
                .findFirst().orElseThrow().lifecycleState()).isEqualTo(GoalLifecycleState.ACTIVE);
    }

    @Test
    void updateGoalLifecycleState_noOpWhenAgentNotFound() {
        AgentRegistry registry = new AgentRegistry() {
            @Override public void register(AgentDescriptor d) {
                throw new AssertionError("register should not be called");
            }
            @Override public Optional<AgentDescriptor> findById(String id, String tid) {
                return Optional.empty();
            }
            @Override public List<AgentMatch> find(AgentQuery q) { return List.of(); }
        };

        registry.updateGoalLifecycleState("missing", "t1", "goal", GoalLifecycleState.DORMANT);
    }
}
