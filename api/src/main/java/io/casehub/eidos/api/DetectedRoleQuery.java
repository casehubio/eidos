package io.casehub.eidos.api;

import java.util.List;

public interface DetectedRoleQuery {

    List<DetectedRole> detectedRoles(String agentId, String tenancyId);
}
