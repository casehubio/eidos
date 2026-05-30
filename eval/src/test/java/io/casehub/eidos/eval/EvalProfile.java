package io.casehub.eidos.eval;

import io.quarkus.test.junit.QuarkusTestProfile;

public class EvalProfile implements QuarkusTestProfile {
    @Override
    public String getConfigProfile() {
        return "eval";
    }
}
