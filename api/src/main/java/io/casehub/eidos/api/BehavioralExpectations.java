package io.casehub.eidos.api;

import java.util.OptionalLong;

public final class BehavioralExpectations {

    private BehavioralExpectations() {}

    public static OptionalLong latencyBound(final AgentCapability capability) {
        return capability.latencyHintP50Ms() != null
                ? OptionalLong.of(capability.latencyHintP50Ms())
                : OptionalLong.empty();
    }

    public static boolean delegationExpected(final AgentDisposition disposition) {
        return disposition != null && disposition.delegation();
    }
}
