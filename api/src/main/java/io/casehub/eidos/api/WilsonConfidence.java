package io.casehub.eidos.api;

public final class WilsonConfidence {

    private static final double Z = 1.645;

    private WilsonConfidence() {}

    public static double score(final double sumQuality, final int n) {
        if (n == 0) return 0.0;
        final double p = sumQuality / n;
        final double z2 = Z * Z;
        final double denominator = 1.0 + z2 / n;
        final double centre = p + z2 / (2.0 * n);
        final double spread = Z * Math.sqrt((p * (1.0 - p) + z2 / (4.0 * n)) / n);
        return Math.max(0.0, (centre - spread) / denominator);
    }
}
