package io.casehub.eidos.org.api;

public record RelationshipScope(
    String capabilityName,
    String domain,
    String custom
) {
    public boolean isEmpty() {
        return capabilityName == null && domain == null && custom == null;
    }
}
