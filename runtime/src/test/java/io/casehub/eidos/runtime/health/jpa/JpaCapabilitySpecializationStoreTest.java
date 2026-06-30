package io.casehub.eidos.runtime.health.jpa;

import io.casehub.eidos.api.CapabilitySpecializationStore;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static io.casehub.eidos.api.SpecializationSignal.DECLINE;
import static io.casehub.eidos.api.SpecializationSignal.SUCCESS;
import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@TestTransaction
class JpaCapabilitySpecializationStoreTest {

    @Inject CapabilitySpecializationStore store;

    @Test
    void single_decline_counted() {
        store.record("a1", "t1", "code-review", "rust", DECLINE);
        assertThat(store.count("a1", "t1", "code-review", "rust", DECLINE)).isEqualTo(1);
    }

    @Test
    void multiple_declines_accumulate() {
        store.record("a1", "t1", "code-review", "rust", DECLINE);
        store.record("a1", "t1", "code-review", "rust", DECLINE);
        store.record("a1", "t1", "code-review", "rust", DECLINE);
        assertThat(store.count("a1", "t1", "code-review", "rust", DECLINE)).isEqualTo(3);
    }

    @Test
    void learned_exclusions_returns_populated_domains_with_counts() {
        store.record("a1", "t1", "code-review", "rust", DECLINE);
        store.record("a1", "t1", "code-review", "rust", DECLINE);
        store.record("a1", "t1", "code-review", "go", DECLINE);

        final var exclusions = store.learned("a1", "t1", "code-review", DECLINE);
        assertThat(exclusions).containsEntry("rust", 2).containsEntry("go", 1);
    }

    @Test
    void empty_map_when_no_declines() {
        assertThat(store.learned("a1", "t1", "code-review", DECLINE)).isEmpty();
    }

    @Test
    void zero_count_for_unknown_triple() {
        assertThat(store.count("unknown", "t1", "code-review", "rust", DECLINE)).isEqualTo(0);
    }

    @Test
    void zero_count_for_unknown_domain() {
        store.record("a1", "t1", "code-review", "rust", DECLINE);
        assertThat(store.count("a1", "t1", "code-review", "go", DECLINE)).isEqualTo(0);
    }

    @Test
    void clear_declines_removes_all_domains() {
        store.record("a1", "t1", "code-review", "rust", DECLINE);
        store.record("a1", "t1", "code-review", "go", DECLINE);
        store.clear("a1", "t1", "code-review", DECLINE);
        assertThat(store.count("a1", "t1", "code-review", "rust", DECLINE)).isEqualTo(0);
        assertThat(store.count("a1", "t1", "code-review", "go", DECLINE)).isEqualTo(0);
    }

    @Test
    void clear_code_review_does_not_remove_code_review_enhanced() {
        store.record("a1", "t1", "code-review", "rust", DECLINE);
        store.record("a1", "t1", "code-review-enhanced", "rust", DECLINE);
        store.clear("a1", "t1", "code-review", DECLINE);
        assertThat(store.count("a1", "t1", "code-review", "rust", DECLINE)).isEqualTo(0);
        assertThat(store.count("a1", "t1", "code-review-enhanced", "rust", DECLINE)).isEqualTo(1);
    }

    @Test
    void isolation_by_agent_id() {
        store.record("a1", "t1", "code-review", "rust", DECLINE);
        assertThat(store.count("a2", "t1", "code-review", "rust", DECLINE)).isEqualTo(0);
    }

    @Test
    void isolation_by_tenancy_id() {
        store.record("a1", "t1", "code-review", "rust", DECLINE);
        assertThat(store.count("a1", "t2", "code-review", "rust", DECLINE)).isEqualTo(0);
    }

    // --- SUCCESS signal tests ---

    @Test
    void record_success_counted() {
        store.record("a1", "t1", "code-review", "rust", SUCCESS);
        assertThat(store.count("a1", "t1", "code-review", "rust", SUCCESS)).isEqualTo(1);
    }

    @Test
    void multiple_successes_accumulate() {
        store.record("a1", "t1", "code-review", "rust", SUCCESS);
        store.record("a1", "t1", "code-review", "rust", SUCCESS);
        store.record("a1", "t1", "code-review", "rust", SUCCESS);
        assertThat(store.count("a1", "t1", "code-review", "rust", SUCCESS)).isEqualTo(3);
    }

    @Test
    void learned_returns_success_domains() {
        store.record("a1", "t1", "code-review", "java", SUCCESS);
        store.record("a1", "t1", "code-review", "java", SUCCESS);
        store.record("a1", "t1", "code-review", "python", SUCCESS);

        final var proficiencies = store.learned("a1", "t1", "code-review", SUCCESS);
        assertThat(proficiencies).containsEntry("java", 2).containsEntry("python", 1);
    }

    @Test
    void clear_success_does_not_affect_declines() {
        store.record("a1", "t1", "code-review", "rust", DECLINE);
        store.record("a1", "t1", "code-review", "rust", SUCCESS);
        store.record("a1", "t1", "code-review", "rust", SUCCESS);

        store.clear("a1", "t1", "code-review", SUCCESS);

        assertThat(store.count("a1", "t1", "code-review", "rust", SUCCESS)).isEqualTo(0);
        assertThat(store.count("a1", "t1", "code-review", "rust", DECLINE)).isEqualTo(1);
    }

    @Test
    void decline_and_success_coexist_independently() {
        store.record("a1", "t1", "code-review", "rust", DECLINE);
        store.record("a1", "t1", "code-review", "rust", DECLINE);
        store.record("a1", "t1", "code-review", "rust", SUCCESS);
        store.record("a1", "t1", "code-review", "rust", SUCCESS);
        store.record("a1", "t1", "code-review", "rust", SUCCESS);

        assertThat(store.count("a1", "t1", "code-review", "rust", DECLINE)).isEqualTo(2);
        assertThat(store.count("a1", "t1", "code-review", "rust", SUCCESS)).isEqualTo(3);
    }
}
