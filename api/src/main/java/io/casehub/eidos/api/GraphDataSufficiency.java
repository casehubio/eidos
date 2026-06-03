package io.casehub.eidos.api;

import java.time.Instant;
import java.util.List;

public record GraphDataSufficiency(
    int sampleCount,
    SufficiencyLevel level,
    Instant dataFrom,    // null if no data
    Instant dataThrough, // null if no data
    List<String> warnings
) {
    public static GraphDataSufficiency forCount(int count, Instant from,
                                                Instant through, List<String> warnings) {
        SufficiencyLevel level = count >= 10 ? SufficiencyLevel.SUFFICIENT
                               : count >= 5  ? SufficiencyLevel.INDICATIVE
                               :               SufficiencyLevel.INSUFFICIENT;
        return new GraphDataSufficiency(count, level, from, through, warnings);
    }

    public static GraphDataSufficiency empty(List<String> warnings) {
        return new GraphDataSufficiency(0, SufficiencyLevel.INSUFFICIENT, null, null, warnings);
    }
}
