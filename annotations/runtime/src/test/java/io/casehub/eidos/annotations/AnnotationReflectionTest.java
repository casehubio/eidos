package io.casehub.eidos.annotations;

import io.casehub.eidos.api.GoalPriority;
import io.casehub.eidos.api.ConstraintSeverity;
import io.casehub.eidos.api.Visibility;
import org.junit.jupiter.api.Test;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;
import static org.assertj.core.api.Assertions.*;

class AnnotationReflectionTest {

    @Test
    void identityRetentionAndTarget() {
        assertThat(Identity.class.getAnnotation(java.lang.annotation.Retention.class).value())
            .isEqualTo(RetentionPolicy.RUNTIME);
        assertThat(Identity.class.getAnnotation(java.lang.annotation.Target.class).value())
            .containsExactly(ElementType.TYPE);
    }

    @Test
    void identitySlotIsRequired() throws NoSuchMethodException {
        assertThat(Identity.class.getDeclaredMethod("slot").getDefaultValue()).isNull();
    }

    @Test
    void identityIdDefaultsToEmpty() throws NoSuchMethodException {
        assertThat(Identity.class.getDeclaredMethod("id").getDefaultValue()).isEqualTo("");
    }

    @Test
    void dispositionRetentionAndTarget() {
        assertThat(Disposition.class.getAnnotation(java.lang.annotation.Retention.class).value())
            .isEqualTo(RetentionPolicy.RUNTIME);
        assertThat(Disposition.class.getAnnotation(java.lang.annotation.Target.class).value())
            .containsExactly(ElementType.TYPE);
    }

    @Test
    void agentGoalDefDescriptionIsRequired() throws NoSuchMethodException {
        assertThat(AgentGoalDef.class.getDeclaredMethod("description").getDefaultValue()).isNull();
    }

    @Test
    void agentGoalDefPriorityDefaultIsPrimary() throws NoSuchMethodException {
        assertThat(AgentGoalDef.class.getDeclaredMethod("priority").getDefaultValue())
            .isEqualTo(GoalPriority.PRIMARY);
    }

    @Test
    void agentConstraintDefDescriptionIsRequired() throws NoSuchMethodException {
        assertThat(AgentConstraintDef.class.getDeclaredMethod("description").getDefaultValue()).isNull();
    }

    @Test
    void agentConstraintDefSeverityDefaultIsHard() throws NoSuchMethodException {
        assertThat(AgentConstraintDef.class.getDeclaredMethod("severity").getDefaultValue())
            .isEqualTo(ConstraintSeverity.HARD);
    }

    @Test
    void agentConstraintDefVisibilityDefaultIsPublic() throws NoSuchMethodException {
        assertThat(AgentConstraintDef.class.getDeclaredMethod("visibility").getDefaultValue())
            .isEqualTo(Visibility.PUBLIC);
    }
}
