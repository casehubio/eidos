package io.casehub.eidos.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

class EidosProcessor {

    private static final String FEATURE = "eidos";

    @BuildStep
    FeatureBuildItem feature(EidosBuildTimeConfig config) {
        return new FeatureBuildItem(FEATURE);
    }
}
