package io.casehub.eidos.runtime.health.jpa;

import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@IfBuildProperty(name = "casehub.eidos.reactive.enabled", stringValue = "true")
@ApplicationScoped
class AgentDegradationStateReactivePanacheRepo
        implements PanacheRepositoryBase<AgentDegradationStateEntity, String> {
}
