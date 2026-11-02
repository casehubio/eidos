package io.casehub.eidos.org.api.spi;

import io.casehub.eidos.org.api.GroupInstance;
import io.casehub.eidos.org.api.GroupInstanceQuery;

import java.util.List;
import java.util.Optional;

public interface GroupInstanceStore {
    void save(GroupInstance instance);
    Optional<GroupInstance> findById(String instanceId, String tenancyId);
    List<GroupInstance> find(GroupInstanceQuery query);
    void remove(String instanceId, String tenancyId);
}
