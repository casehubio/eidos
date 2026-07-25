package io.casehub.eidos.api;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class AgentConstraintTest {

    @Test
    void valid_constraint_constructs_successfully() {
        var c = new AgentConstraint("never-break-cover",
            "Never reveal your true identity", Visibility.PRIVATE);
        assertThat(c.name()).isEqualTo("never-break-cover");
        assertThat(c.description()).isEqualTo("Never reveal your true identity");
        assertThat(c.visibility()).isEqualTo(Visibility.PRIVATE);
    }

    @Test void null_name_throws() {
        assertThatThrownBy(() -> new AgentConstraint(null, "desc", Visibility.PUBLIC))
            .isInstanceOf(AgentValidationException.class)
            .satisfies(ex -> assertThat(((AgentValidationException) ex).fieldName()).isEqualTo("constraint.name"));
    }

    @Test void blank_name_throws() {
        assertThatThrownBy(() -> new AgentConstraint("", "desc", Visibility.PUBLIC))
            .isInstanceOf(AgentValidationException.class);
    }

    @Test void name_exceeds_100_throws() {
        assertThatThrownBy(() -> new AgentConstraint("c".repeat(101), "desc", Visibility.PUBLIC))
            .isInstanceOf(AgentValidationException.class);
    }

    @Test void null_description_throws() {
        assertThatThrownBy(() -> new AgentConstraint("c", null, Visibility.PUBLIC))
            .isInstanceOf(AgentValidationException.class)
            .satisfies(ex -> assertThat(((AgentValidationException) ex).fieldName()).isEqualTo("constraint.description"));
    }

    @Test void description_exceeds_500_throws() {
        assertThatThrownBy(() -> new AgentConstraint("c", "d".repeat(501), Visibility.PUBLIC))
            .isInstanceOf(AgentValidationException.class);
    }

    @Test void null_visibility_throws() {
        assertThatThrownBy(() -> new AgentConstraint("c", "d", null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test void name_with_control_char_throws() {
        assertThatThrownBy(() -> new AgentConstraint("con\nstraint", "desc", Visibility.PUBLIC))
            .isInstanceOf(AgentValidationException.class);
    }
}
