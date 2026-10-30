package io.casehub.eidos.api;

public final class ComplianceDimension {

    public static final String LATENCY = "latency";
    public static final String ATTESTATION_RATE = "attestation-rate";
    public static final String DELEGATION = "delegation";
    public static final String ESCALATION = "escalation";

    public static final String ATTESTOR_ID = "eidos:compliance";
    public static final String TRUST_DIMENSION_PREFIX = "behavioral:";
    // Latency violation at 2× p50 — standard SLA practice for tail latency detection
    public static final double LATENCY_VIOLATION_MULTIPLIER = 2.0;

    private ComplianceDimension() {}
}
