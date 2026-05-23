package io.casehub.eidos.runtime.registry;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import io.casehub.eidos.api.AgentRegistry;
import io.casehub.eidos.api.ReactiveAgentRegistry;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static org.assertj.core.api.Assertions.assertThat;

class BlockingReactiveParityTest {

    private static final JavaClasses API_CLASSES = new ClassFileImporter()
        .importPackages("io.casehub.eidos.api");

    @Test
    void reactive_registry_methods_return_uni() {
        ArchRule rule = methods()
            .that().areDeclaredIn(ReactiveAgentRegistry.class)
            .should().haveRawReturnType(assignableTo(Uni.class));

        rule.check(API_CLASSES);
    }

    @Test
    void reactive_registry_has_at_least_one_method() {
        long count = API_CLASSES.get(ReactiveAgentRegistry.class)
            .getMethods().size();
        assertThat(count).isGreaterThanOrEqualTo(1);
    }

    @Test
    void reactive_registry_has_same_method_names_as_blocking() {
        JavaClasses apiClasses = new ClassFileImporter()
            .importPackages("io.casehub.eidos.api");

        var blockingMethods = apiClasses.get(AgentRegistry.class)
            .getMethods().stream()
            .map(m -> m.getName())
            .collect(Collectors.toSet());

        var reactiveMethods = apiClasses.get(ReactiveAgentRegistry.class)
            .getMethods().stream()
            .map(m -> m.getName())
            .collect(Collectors.toSet());

        assertThat(reactiveMethods)
            .as("ReactiveAgentRegistry must have the same method names as AgentRegistry")
            .containsExactlyInAnyOrderElementsOf(blockingMethods);
    }

    private static com.tngtech.archunit.base.DescribedPredicate<com.tngtech.archunit.core.domain.JavaClass> assignableTo(Class<?> type) {
        return com.tngtech.archunit.base.DescribedPredicate.describe(
            "assignable to " + type.getSimpleName(),
            c -> c.isAssignableTo(type)
        );
    }
}
