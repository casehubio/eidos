package io.casehub.eidos.api;

import java.time.Instant;

public record BackfillResult(int imported, int skipped,
                              Instant rangeFrom, Instant rangeThrough) {}
