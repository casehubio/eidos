package io.casehub.eidos.org.api;

public class StaleGroupInstanceException extends RuntimeException {
    private final String instanceId;
    private final long expectedVersion;

    public StaleGroupInstanceException(String instanceId, long expectedVersion) {
        super("GroupInstance " + instanceId + " is stale (expected version " + expectedVersion + ")");
        this.instanceId = instanceId;
        this.expectedVersion = expectedVersion;
    }

    public String instanceId() { return instanceId; }
    public long expectedVersion() { return expectedVersion; }
}
