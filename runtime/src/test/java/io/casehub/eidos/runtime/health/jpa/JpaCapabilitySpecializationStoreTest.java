package io.casehub.eidos.runtime.health.jpa;

import io.casehub.eidos.api.CapabilitySpecializationStore;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@TestTransaction
class JpaCapabilitySpecializationStoreTest {

    @Inject CapabilitySpecializationStore store;

    @Test
    void single_decline_counted() {
        store.recordDecline("a1", "t1", "code-review", "rust");
        assertThat(store.declineCount("a1", "t1", "code-review", "rust")).isEqualTo(1);
    }

    @Test
    void multiple_declines_accumulate() {
        store.recordDecline("a1", "t1", "code-review", "rust");
        store.recordDecline("a1", "t1", "code-review", "rust");
        store.recordDecline("a1", "t1", "code-review", "rust");
        assertThat(store.declineCount("a1", "t1", "code-review", "rust")).isEqualTo(3);
    }

    @Test
    void learned_exclusions_returns_populated_domains_with_counts() {
        store.recordDecline("a1", "t1", "code-review", "rust");
        store.recordDecline("a1", "t1", "code-review", "rust");
        store.recordDecline("a1", "t1", "code-review", "go");

        var exclusions = store.learnedExclusions("a1", "t1", "code-review");
        assertThat(exclusions).containsEntry("rust", 2).containsEntry("go", 1);
    }

    @Test
    void empty_map_when_no_declines() {
        assertThat(store.learnedExclusions("a1", "t1", "code-review")).isEmpty();
    }

    @Test
    void zero_count_for_unknown_triple() {
        assertThat(store.declineCount("unknown", "t1", "code-review", "rust")).isEqualTo(0);
    }

    @Test
    void zero_count_for_unknown_domain() {
        store.recordDecline("a1", "t1", "code-review", "rust");
        assertThat(store.declineCount("a1", "t1", "code-review", "go")).isEqualTo(0);
    }

    @Test
    void clear_declines_removes_all_domains() {
        store.recordDecline("a1", "t1", "code-review", "rust");
        store.recordDecline("a1", "t1", "code-review", "go");
        store.clearDeclines("a1", "t1", "code-review");
        assertThat(store.declineCount("a1", "t1", "code-review", "rust")).isEqualTo(0);
        assertThat(store.declineCount("a1", "t1", "code-review", "go")).isEqualTo(0);
    }

    @Test
    void clear_code_review_does_not_remove_code_review_enhanced() {
        store.recordDecline("a1", "t1", "code-review", "rust");
        store.recordDecline("a1", "t1", "code-review-enhanced", "rust");
        store.clearDeclines("a1", "t1", "code-review");
        assertThat(store.declineCount("a1", "t1", "code-review", "rust")).isEqualTo(0);
        assertThat(store.declineCount("a1", "t1", "code-review-enhanced", "rust")).isEqualTo(1);
    }

    @Test
    void isolation_by_agent_id() {
        store.recordDecline("a1", "t1", "code-review", "rust");
        assertThat(store.declineCount("a2", "t1", "code-review", "rust")).isEqualTo(0);
    }

    @Test
    void isolation_by_tenancy_id() {
        store.recordDecline("a1", "t1", "code-review", "rust");
        assertThat(store.declineCount("a1", "t2", "code-review", "rust")).isEqualTo(0);
    }
}
