package io.casehub.eidos.annotations.deployment;

import io.quarkus.test.QuarkusUnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class BuildTimeValidationTest {

    @RegisterExtension
    static final QuarkusUnitTest duplicateIdTest = new QuarkusUnitTest()
                                                           .withApplicationRoot(root -> root
                                                                                                .addClass(io.casehub.eidos.annotations.deployment.test.DuplicateIdAgentA.class)
                                                                                                .addClass(io.casehub.eidos.annotations.deployment.test.DuplicateIdAgentB.class))
                                                           .overrideConfigKey("casehub.eidos.reactive.enabled", "false")
                                                           .overrideConfigKey("quarkus.datasource.db-kind", "h2")
                                                           .overrideConfigKey("quarkus.datasource.jdbc.url", "jdbc:h2:mem:duptest;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
                                                           .overrideConfigKey("quarkus.datasource.devservices.enabled", "false")
                                                           .overrideConfigKey("quarkus.flyway.migrate-at-start", "false")
                                                           .overrideConfigKey("quarkus.hibernate-orm.database.generation", "none")
                                                           .assertException(t -> {
                                                               var msg = rootMessage(t);
                                                               if (!msg.contains("Duplicate agentId 'duplicate-agent'")) {
                                                                   throw new AssertionError("Expected 'Duplicate agentId' but got: " + msg, t);
                                                               }
                                                           });

    @Test
    void duplicateAgentIdDetectedAtBuildTime() {
    }

    private static String rootMessage(Throwable t) {
        var current = t;
        while (current.getCause() != null) {current = current.getCause();}
        return current.getMessage() != null ? current.getMessage() : current.toString();
    }
}
