package io.casehub.eidos.eval;

import java.time.Instant;
import java.util.List;

public record BehavioralReport(
    Instant timestamp,
    String modelLabel,
    List<BehavioralPairResult> results,
    double accuracy
) {}
