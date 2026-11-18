package io.casehub.eidos.runtime.health.jpa;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.Map;

public class PostgresTestResource implements QuarkusTestResourceLifecycleManager {

    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:17-alpine");

    @Override
    public Map<String, String> start() {
        PG.start();
        return Map.of(
                "quarkus.datasource.jdbc.url", PG.getJdbcUrl(),
                "quarkus.datasource.username", PG.getUsername(),
                "quarkus.datasource.password", PG.getPassword());
    }

    @Override
    public void stop() {
        PG.stop();
    }
}
