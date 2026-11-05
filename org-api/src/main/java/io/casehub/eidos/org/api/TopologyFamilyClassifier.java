package io.casehub.eidos.org.api;

import java.util.Optional;

@FunctionalInterface
public interface TopologyFamilyClassifier {
    Optional<TopologyFamily> classify(String topology);
}
