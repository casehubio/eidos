package io.casehub.eidos.annotations.deployment;

import io.quarkus.builder.item.SimpleBuildItem;
import java.util.Set;

public final class EidosAnnotationProcessedBuildItem extends SimpleBuildItem {

    private final Set<String> processedClassNames;

    public EidosAnnotationProcessedBuildItem(Set<String> processedClassNames) {
        this.processedClassNames = Set.copyOf(processedClassNames);
    }

    public Set<String> processedClassNames() {
        return processedClassNames;
    }
}
