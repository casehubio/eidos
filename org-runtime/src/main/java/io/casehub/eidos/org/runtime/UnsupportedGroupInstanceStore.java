package io.casehub.eidos.org.runtime;

import io.casehub.eidos.org.api.GroupInstance;
import io.casehub.eidos.org.api.GroupInstanceQuery;
import io.casehub.eidos.org.api.spi.GroupInstanceStore;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@DefaultBean
@ApplicationScoped
public class UnsupportedGroupInstanceStore implements GroupInstanceStore {

    @Override
    public void save(GroupInstance instance) {
        throw new UnsupportedOperationException("No GroupInstanceStore configured");
    }

    @Override
    public Optional<GroupInstance> findById(String instanceId, String tenancyId) {
        throw new UnsupportedOperationException("No GroupInstanceStore configured");
    }

    @Override
    public List<GroupInstance> find(GroupInstanceQuery query) {
        throw new UnsupportedOperationException("No GroupInstanceStore configured");
    }

    @Override
    public void remove(String instanceId, String tenancyId) {
        throw new UnsupportedOperationException("No GroupInstanceStore configured");
    }
}
