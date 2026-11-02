package io.casehub.eidos.org.memory;

import io.casehub.eidos.org.api.*;
import io.casehub.eidos.org.api.spi.GroupInstanceStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryGroupInstanceStoreTest {

    private GroupInstanceStore store;

    @BeforeEach void setUp() {
        store = new InMemoryGroupInstanceStore();
    }

    private GroupInstance newInstance(String id, String tenancyId, GroupLifecycleState state, long version) {
        return new GroupInstance(id, "spec-1", tenancyId, state, FormationOrigin.TOP_DOWN,
            null, null, null, List.of(), List.of(), List.of(), Instant.now(), Instant.now(), version);
    }

    @Test void saveAndFind() {
        var gi = newInstance("gi-1", "t1", GroupLifecycleState.ACTIVE, 0);
        store.save(gi);
        assertThat(store.findById("gi-1", "t1")).isPresent()
            .hasValueSatisfying(found -> {
                assertThat(found.instanceId()).isEqualTo("gi-1");
                assertThat(found.version()).isEqualTo(1);
            });
    }

    @Test void findByIdEnforcesTenancy() {
        store.save(newInstance("gi-1", "t1", GroupLifecycleState.ACTIVE, 0));
        assertThat(store.findById("gi-1", "t2")).isEmpty();
    }

    @Test void casRejectsStaleVersion() {
        store.save(newInstance("gi-1", "t1", GroupLifecycleState.ACTIVE, 0));
        var stale = newInstance("gi-1", "t1", GroupLifecycleState.DISSOLVING, 0);
        assertThatThrownBy(() -> store.save(stale))
            .isInstanceOf(StaleGroupInstanceException.class);
    }

    @Test void casAcceptsCurrentVersion() {
        store.save(newInstance("gi-1", "t1", GroupLifecycleState.ACTIVE, 0));
        var current = newInstance("gi-1", "t1", GroupLifecycleState.DISSOLVING, 1);
        store.save(current);
        assertThat(store.findById("gi-1", "t1")).isPresent()
            .hasValueSatisfying(found ->
                assertThat(found.state()).isEqualTo(GroupLifecycleState.DISSOLVING));
    }

    @Test void findByQuery() {
        store.save(newInstance("gi-1", "t1", GroupLifecycleState.ACTIVE, 0));
        store.save(newInstance("gi-2", "t1", GroupLifecycleState.FORMING, 0));
        store.save(newInstance("gi-3", "t2", GroupLifecycleState.ACTIVE, 0));

        var query = new GroupInstanceQuery("t1", null, GroupLifecycleState.ACTIVE, null);
        var results = store.find(query);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).instanceId()).isEqualTo("gi-1");
    }

    @Test void findBySpecId() {
        store.save(newInstance("gi-1", "t1", GroupLifecycleState.ACTIVE, 0));
        var query = new GroupInstanceQuery("t1", "spec-1", null, null);
        assertThat(store.find(query)).hasSize(1);
        var noMatch = new GroupInstanceQuery("t1", "spec-other", null, null);
        assertThat(store.find(noMatch)).isEmpty();
    }

    @Test void findByAgentId() {
        var member = new ActualMembership("agent-1", Instant.now());
        var adoption = new RoleAdoption("agent-1", "reviewer", null, Instant.now());
        var gi = new GroupInstance("gi-1", "spec-1", "t1",
            GroupLifecycleState.ACTIVE, FormationOrigin.TOP_DOWN,
            null, null, null, List.of(member), List.of(adoption), List.of(),
            Instant.now(), Instant.now(), 0);
        store.save(gi);
        store.save(newInstance("gi-2", "t1", GroupLifecycleState.ACTIVE, 0));

        var query = new GroupInstanceQuery("t1", null, null, "agent-1");
        assertThat(store.find(query)).hasSize(1);
    }

    @Test void remove() {
        store.save(newInstance("gi-1", "t1", GroupLifecycleState.ACTIVE, 0));
        store.remove("gi-1", "t1");
        assertThat(store.findById("gi-1", "t1")).isEmpty();
    }

    @Test void removeEnforcesTenancy() {
        store.save(newInstance("gi-1", "t1", GroupLifecycleState.ACTIVE, 0));
        store.remove("gi-1", "t2");
        assertThat(store.findById("gi-1", "t1")).isPresent();
    }
}
