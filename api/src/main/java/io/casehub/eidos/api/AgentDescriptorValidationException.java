package io.casehub.eidos.api;

public final class AgentDescriptorValidationException extends IllegalArgumentException {

    private final String fieldName;

    public AgentDescriptorValidationException(final String fieldName, final String message) {
        super("AgentDescriptor field '" + fieldName + "': " + message);
        this.fieldName = fieldName;
    }

    public String fieldName() {
        return fieldName;
    }
}
