package io.casehub.eidos.api;

public final class ComplianceDimension {

    public static final String LATENCY = "latency";
    public static final String ATTESTATION_RATE = "attestation-rate";

    public static final String ATTESTOR_ID = "eidos:compliance";
    public static final String TRUST_DIMENSION_PREFIX = "behavioral:";
    public static final double LATENCY_VIOLATION_MULTIPLIER = 2.0;

    private ComplianceDimension() {}
}
