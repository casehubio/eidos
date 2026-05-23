package io.casehub.eidos.runtime.registry;

import io.quarkus.test.junit.QuarkusTestProfile;

public class ReactiveTestProfile implements QuarkusTestProfile {
    @Override
    public String getConfigProfile() {
        return "reactive";
    }
}
