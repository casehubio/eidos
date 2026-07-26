package io.casehub.eidos.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentConstraintTest {

    @Test
    void valid_constraint_constructs_successfully() {
        var c = new AgentConstraint("never-break-cover",
                                    "Never reveal your true identity", Visibility.PRIVATE, ConstraintSeverity.HARD);
        assertThat(c.name()).isEqualTo("never-break-cover");
        assertThat(c.description()).isEqualTo("Never reveal your true identity");
        assertThat(c.visibility()).isEqualTo(Visibility.PRIVATE);
        assertThat(c.severity()).isEqualTo(ConstraintSeverity.HARD);
    }

    @Test
    void null_name_throws() {
        assertThatThrownBy(() -> new AgentConstraint(null, "desc", Visibility.PUBLIC, ConstraintSeverity.HARD))
                .isInstanceOf(AgentValidationException.class)
                .satisfies(ex -> assertThat(((AgentValidationException) ex).fieldName()).isEqualTo("constraint.name"));
    }

    @Test
    void blank_name_throws() {
        assertThatThrownBy(() -> new AgentConstraint("", "desc", Visibility.PUBLIC, ConstraintSeverity.HARD))
                .isInstanceOf(AgentValidationException.class);
    }

    @Test
    void name_exceeds_100_throws() {
        assertThatThrownBy(() -> new AgentConstraint("c".repeat(101), "desc", Visibility.PUBLIC, ConstraintSeverity.HARD))
                .isInstanceOf(AgentValidationException.class);
    }

    @Test
    void null_description_throws() {
        assertThatThrownBy(() -> new AgentConstraint("c", null, Visibility.PUBLIC, ConstraintSeverity.HARD))
                .isInstanceOf(AgentValidationException.class)
                .satisfies(ex -> assertThat(((AgentValidationException) ex).fieldName()).isEqualTo("constraint.description"));
    }

    @Test
    void description_exceeds_500_throws() {
        assertThatThrownBy(() -> new AgentConstraint("c", "d".repeat(501), Visibility.PUBLIC, ConstraintSeverity.HARD))
                .isInstanceOf(AgentValidationException.class);
    }

    @Test
    void null_visibility_throws() {
        assertThatThrownBy(() -> new AgentConstraint("c", "d", null, ConstraintSeverity.HARD))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void null_severity_throws() {
        assertThatThrownBy(() -> new AgentConstraint("c", "d", Visibility.PUBLIC, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("constraint.severity");
    }

    @Test
    void name_with_control_char_throws() {
        assertThatThrownBy(() -> new AgentConstraint("con\nstraint", "desc", Visibility.PUBLIC, ConstraintSeverity.HARD))
                .isInstanceOf(AgentValidationException.class);
    }
}
