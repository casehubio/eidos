package io.casehub.eidos.api;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

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

    @Test
    void findEffective_defaultDelegatesToFindById() {
        var store    = new ConcurrentHashMap<String, AgentDescriptor>();
        var registry = storeBackedRegistry(store);

        var descriptor = AgentDescriptor.builder()
                                        .agentId("a1").name("Agent").slot("default").tenancyId("t1")
                                        .disposition(AgentDisposition.builder()
                                                                     .dispositionProfile(DispositionValue.of("Fi"), DispositionValue.of("Ne"))
                                                                     .build())
                                        .build();
        registry.register(descriptor);

        var effective = registry.findEffective("a1", "t1");
        assertThat(effective).isPresent();
        assertThat(effective.get().disposition().dispositionProfile())
                .extracting(DispositionValue::term)
                .containsExactly("Fi", "Ne");
    }

    @Test
    void findEffective_defaultReturnsEmptyWhenNotFound() {
        var registry = storeBackedRegistry(new ConcurrentHashMap<>());
        assertThat(registry.findEffective("missing", "t1")).isEmpty();
    }

    @Test
    void findEffective_composesWithLivedDispositionSource() {
        var store = new ConcurrentHashMap<String, AgentDescriptor>();
        var authored = AgentDisposition.builder()
                                       .dispositionProfile(DispositionValue.of("Fi"), DispositionValue.of("Ne"))
                                       .build();
        var evolved = AgentDisposition.builder()
                                      .dispositionProfile(
                                              new DispositionValue("Fi", 0.6),
                                              new DispositionValue("Ne", 0.4))
                                      .build();

        LivedDispositionSource source = (agentId, tenancyId, auth) -> evolved;

        AgentRegistry registry = new AgentRegistry() {
            @Override
            public void register(AgentDescriptor d) {store.put(d.agentId(), d);}

            @Override
            public Optional<AgentDescriptor> findById(String id, String tid) {
                return Optional.ofNullable(store.get(id)).filter(d -> d.tenancyId().equals(tid));
            }

            @Override
            public List<AgentMatch> find(AgentQuery q) {return List.of();}

            @Override
            public Optional<AgentDescriptor> findEffective(String agentId, String tenancyId) {
                return findById(agentId, tenancyId).map(descriptor -> {
                    var effective = source.resolveEffective(agentId, tenancyId, descriptor.disposition());
                    if (effective == descriptor.disposition()) {return descriptor;}
                    return descriptor.toBuilder().disposition(effective).build();
                });
            }
        };

        registry.register(AgentDescriptor.builder()
                                         .agentId("a1").name("Agent").slot("default").tenancyId("t1")
                                         .disposition(authored).build());

        // findById returns authored disposition
        var byId = registry.findById("a1", "t1").orElseThrow();
        assertThat(byId.disposition().dispositionProfile())
                .extracting(DispositionValue::weight)
                .containsExactly(1.0, 1.0);

        // findEffective returns composed disposition
        var effective = registry.findEffective("a1", "t1").orElseThrow();
        assertThat(effective.disposition().dispositionProfile())
                .extracting(DispositionValue::weight)
                .containsExactly(0.6, 0.4);

        // All other descriptor fields are preserved
        assertThat(effective.agentId()).isEqualTo("a1");
        assertThat(effective.name()).isEqualTo("Agent");
        assertThat(effective.slot()).isEqualTo("default");
    }

    @Test
    void findEffective_noOpSourceReturnsAuthored() {
        var                    store = new ConcurrentHashMap<String, AgentDescriptor>();
        LivedDispositionSource noOp  = (agentId, tenancyId, authored) -> authored;

        AgentRegistry registry = new AgentRegistry() {
            @Override
            public void register(AgentDescriptor d) {store.put(d.agentId(), d);}

            @Override
            public Optional<AgentDescriptor> findById(String id, String tid) {
                return Optional.ofNullable(store.get(id)).filter(d -> d.tenancyId().equals(tid));
            }

            @Override
            public List<AgentMatch> find(AgentQuery q) {return List.of();}

            @Override
            public Optional<AgentDescriptor> findEffective(String agentId, String tenancyId) {
                return findById(agentId, tenancyId).map(descriptor -> {
                    var effective = noOp.resolveEffective(agentId, tenancyId, descriptor.disposition());
                    if (effective == descriptor.disposition()) {return descriptor;}
                    return descriptor.toBuilder().disposition(effective).build();
                });
            }
        };

        var authored = AgentDisposition.builder()
                                       .dispositionProfile(DispositionValue.of("Ti"), DispositionValue.of("Se"))
                                       .build();
        registry.register(AgentDescriptor.builder()
                                         .agentId("a1").name("Agent").slot("default").tenancyId("t1")
                                         .disposition(authored).build());

        var effective = registry.findEffective("a1", "t1").orElseThrow();
        // Same object — no rebuild needed
        assertThat(effective.disposition()).isSameAs(authored);
    }


}
