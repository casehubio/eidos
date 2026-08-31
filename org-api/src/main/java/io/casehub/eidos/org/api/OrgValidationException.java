package io.casehub.eidos.org.api;

public class OrgValidationException extends IllegalArgumentException {
    private final String field;

    public OrgValidationException(String field, String message) {
        super(field + ": " + message);
        this.field = field;
    }

    public String field() { return field; }
}
