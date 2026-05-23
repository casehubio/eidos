package io.casehub.eidos.runtime.registry;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import io.casehub.eidos.api.ReactiveAgentRegistry;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

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

    private static com.tngtech.archunit.base.DescribedPredicate<com.tngtech.archunit.core.domain.JavaClass> assignableTo(Class<?> type) {
        return com.tngtech.archunit.base.DescribedPredicate.describe(
            "assignable to " + type.getSimpleName(),
            c -> c.isAssignableTo(type)
        );
    }
}
