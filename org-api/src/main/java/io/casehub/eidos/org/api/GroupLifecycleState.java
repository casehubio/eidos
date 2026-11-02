package io.casehub.eidos.org.api;

public enum GroupLifecycleState {
    PROPOSED, FORMING, ACTIVE, REORGANISING, DISSOLVING,
    REJECTED, FAILED, DISSOLVED;

    public boolean isTerminal() {
        return this == REJECTED || this == FAILED || this == DISSOLVED;
    }

    public boolean isActive() {
        return this == ACTIVE;
    }
}
