package io.casehub.eidos.api;

public final class AgentValidationException extends IllegalArgumentException {

    private final String fieldName;

    public AgentValidationException(final String fieldName, final String message) {
        super("field '" + fieldName + "': " + message);
        this.fieldName = fieldName;
    }

    public String fieldName() {
        return fieldName;
    }
}
