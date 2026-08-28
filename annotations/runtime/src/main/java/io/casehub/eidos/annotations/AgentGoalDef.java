package io.casehub.eidos.annotations;

import io.casehub.eidos.api.GoalPriority;
import io.casehub.eidos.api.Visibility;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface AgentGoalDef {
    String name();
    String description();
    GoalPriority priority() default GoalPriority.PRIMARY;
    Visibility visibility() default Visibility.PUBLIC;
    String[] capabilities() default {};
}
