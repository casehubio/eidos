package io.casehub.eidos.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourcePatternsBuildItem;

class EidosProcessor {

    private static final String FEATURE = "eidos";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    NativeImageResourcePatternsBuildItem nativeFlywayResources() {
        return NativeImageResourcePatternsBuildItem.builder()
                .includeGlob("db/eidos/migration/*.sql")
                .build();
    }
}
