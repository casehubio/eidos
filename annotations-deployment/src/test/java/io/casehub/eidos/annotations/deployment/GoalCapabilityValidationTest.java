package io.casehub.eidos.annotations.deployment;

import io.quarkus.test.QuarkusUnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class GoalCapabilityValidationTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addClass(io.casehub.eidos.annotations.deployment.test.InvalidGoalCapRefAgent.class))
            .overrideConfigKey("casehub.eidos.reactive.enabled", "false")
            .overrideConfigKey("quarkus.datasource.db-kind", "h2")
            .overrideConfigKey("quarkus.datasource.jdbc.url", "jdbc:h2:mem:capreftest;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
            .overrideConfigKey("quarkus.datasource.devservices.enabled", "false")
            .overrideConfigKey("quarkus.flyway.migrate-at-start", "false")
            .overrideConfigKey("quarkus.hibernate-orm.database.generation", "none")
            .assertException(t -> {
                var current = t;
                while (current.getCause() != null) current = current.getCause();
                var msg = current.getMessage() != null ? current.getMessage() : current.toString();
                if (!msg.contains("references capability 'nonexistent-capability' not declared in @Discoverable")) {
                    throw new AssertionError("Expected goal-capability validation error but got: " + msg, t);
                }
            });

    @Test
    void goalCapabilityReferenceValidatedAtBuildTime() {
    }
}
