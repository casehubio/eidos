package io.casehub.eidos.examples.live;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

public class LiveDemoProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
            "quarkus.arc.exclude-types", "io.casehub.ledger.memory.NoOpCurrentPrincipal"
        );
    }
}
