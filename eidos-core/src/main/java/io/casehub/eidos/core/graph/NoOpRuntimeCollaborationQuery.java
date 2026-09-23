package io.casehub.eidos.core.graph;

import io.casehub.eidos.api.Collaborator;
import io.casehub.eidos.api.RuntimeCollaborationQuery;

import java.util.List;

public class NoOpRuntimeCollaborationQuery implements RuntimeCollaborationQuery {
    @Override
    public List<Collaborator> collaborators(final String agentId, final String tenancyId) {
        return List.of();
    }
}
