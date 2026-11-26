package io.casehub.eidos.api;

import java.time.Instant;
import java.util.List;

public interface DetectedRoleSignalStore {

    void record(DetectedRoleSignal signal);

    List<DetectedRoleSignal> signals(String agentId, String roleName,
                                      String tenancyId, Instant since);

    List<DetectedRoleSignal> signalsByAgent(String agentId,
                                             String tenancyId, Instant since);

    int purge(String agentId, String tenancyId, Instant olderThan);

    void clear(String agentId, String tenancyId);
}
