package io.casehub.eidos.api;

import java.util.List;

public interface RuntimeCollaborationQuery {
    List<Collaborator> collaborators(String agentId, String tenancyId);
}
