package io.casehub.eidos.api;

import java.time.Duration;
import java.time.Instant;

public final class TemporalDecay {

    private TemporalDecay() {}

    public static double exponential(final Instant observedAt, final Instant now,
                                     final double halfLifeDays) {
        if (halfLifeDays <= 0) return 1.0;
        final double ageDays = Duration.between(observedAt, now).toSeconds() / 86400.0;
        if (ageDays <= 0) return 1.0;
        final double lambda = Math.log(2) / halfLifeDays;
        return Math.exp(-lambda * ageDays);
    }
}
