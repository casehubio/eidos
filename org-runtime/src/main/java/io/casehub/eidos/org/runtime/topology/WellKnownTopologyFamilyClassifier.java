package io.casehub.eidos.org.runtime.topology;

import io.casehub.eidos.org.api.TopologyFamily;
import io.casehub.eidos.org.api.TopologyFamilyClassifier;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;
import java.util.Optional;

@ApplicationScoped
@DefaultBean
public class WellKnownTopologyFamilyClassifier implements TopologyFamilyClassifier {

    private static final Map<String, TopologyFamily> FAMILIES = Map.of(
        "swarm", TopologyFamily.DISTRIBUTED,
        "mesh", TopologyFamily.DISTRIBUTED,
        "hive", TopologyFamily.CENTRALISED,
        "many", TopologyFamily.INDEPENDENT
    );

    @Override
    public Optional<TopologyFamily> classify(String topology) {
        if (topology == null) return Optional.empty();
        return Optional.ofNullable(FAMILIES.get(topology));
    }
}
