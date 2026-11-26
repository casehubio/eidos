package io.casehub.eidos.api;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.assertj.core.api.Assertions.*;

class RoleOutcomeTest {

    @Test
    void valid_role_outcome() {
        var ro = new RoleOutcome("t1", 0.8, "coordination", Instant.now());
        assertThat(ro.roleQuality()).isEqualTo(0.8);
        assertThat(ro.dimension()).isEqualTo("coordination");
    }

    @Test
    void null_dimension_is_valid() {
        var ro = new RoleOutcome("t1", 0.5, null, Instant.now());
        assertThat(ro.dimension()).isNull();
    }

    @Test
    void rejects_quality_below_zero() {
        assertThatThrownBy(() -> new RoleOutcome("t1", -0.1, null, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_quality_above_one() {
        assertThatThrownBy(() -> new RoleOutcome("t1", 1.1, null, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void boundary_values_valid() {
        assertThatCode(() -> new RoleOutcome("t1", 0.0, null, Instant.now())).doesNotThrowAnyException();
        assertThatCode(() -> new RoleOutcome("t1", 1.0, null, Instant.now())).doesNotThrowAnyException();
    }

    @Test
    void rejects_null_task_id() {
        assertThatThrownBy(() -> new RoleOutcome(null, 0.5, null, Instant.now()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejects_null_observed_at() {
        assertThatThrownBy(() -> new RoleOutcome("t1", 0.5, null, null))
                .isInstanceOf(NullPointerException.class);
    }
}
