package io.casehub.eidos.annotations.deployment;

import io.casehub.eidos.annotations.AgentConstraintDef;
import io.casehub.eidos.annotations.AgentGoalDef;
import io.casehub.eidos.annotations.Disposition;
import io.casehub.eidos.annotations.Identity;
import io.casehub.eidos.api.AgentConstraint;
import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentDisposition;
import io.casehub.eidos.api.AgentGoal;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class AnnotationParityTest {

    private static final Map<String, String> IDENTITY_RENAMES = Map.of(
            "id", "agentId",
            "vocabulary", "domainVocabulary"
    );

    @Test
    void everyIdentityFieldHasBuilderSetter() {
        var annotationFields = Arrays.stream(Identity.class.getDeclaredMethods())
                .map(m -> m.getName()).collect(Collectors.toSet());
        var builderSetters = Arrays.stream(AgentDescriptor.Builder.class.getDeclaredMethods())
                .map(m -> m.getName()).collect(Collectors.toSet());
        for (var field : annotationFields) {
            var builderName = IDENTITY_RENAMES.getOrDefault(field, field);
            assertThat(builderSetters).as("Builder setter for @Identity.%s() (→ %s)", field, builderName)
                    .contains(builderName);
        }
    }

    @Test
    void everyDispositionFieldHasBuilderSetter() {
        var nonBuilderFields = java.util.Set.of("mbtiType", "enneagramType", "axisVocabularies");
        var annotationFields = Arrays.stream(Disposition.class.getDeclaredMethods())
                                     .map(m -> m.getName())
                                     .filter(f -> !nonBuilderFields.contains(f))
                                     .collect(Collectors.toSet());
        var builderSetters = Arrays.stream(AgentDisposition.Builder.class.getDeclaredMethods())
                                   .map(m -> m.getName()).collect(Collectors.toSet());
        for (var field : annotationFields) {
            assertThat(builderSetters).as("Builder setter for @Disposition.%s()", field)
                                      .contains(field);
        }
    }

    @Test
    void everyGoalDefFieldHasRecordComponent() {
        var annotationFields = Arrays.stream(AgentGoalDef.class.getDeclaredMethods())
                .map(m -> m.getName()).collect(Collectors.toSet());
        var recordComponents = Arrays.stream(AgentGoal.class.getRecordComponents())
                .map(c -> c.getName()).collect(Collectors.toSet());
        for (var field : annotationFields) {
            assertThat(recordComponents).as("AgentGoal component for @AgentGoalDef.%s()", field)
                    .contains(field);
        }
    }

    @Test
    void everyConstraintDefFieldHasRecordComponent() {
        var annotationFields = Arrays.stream(AgentConstraintDef.class.getDeclaredMethods())
                .map(m -> m.getName()).collect(Collectors.toSet());
        var recordComponents = Arrays.stream(AgentConstraint.class.getRecordComponents())
                .map(c -> c.getName()).collect(Collectors.toSet());
        for (var field : annotationFields) {
            assertThat(recordComponents).as("AgentConstraint component for @AgentConstraintDef.%s()", field)
                    .contains(field);
        }
    }
}
