package io.casehub.eidos.api;

public record RoleChangePolicy(
    Mode mode,
    double minConfidence,
    double demotionConfidence,
    int minHoldCycles) {

  public static final RoleChangePolicy ADVISORY =
      new RoleChangePolicy(Mode.ADVISORY, 0.0, 0.0, 0);

  public static final RoleChangePolicy DISABLED =
      new RoleChangePolicy(Mode.DISABLED, 0.0, 0.0, 0);

  public RoleChangePolicy {
    if (mode != Mode.ADVISORY && mode != Mode.DISABLED && demotionConfidence >= minConfidence) {
      throw new IllegalArgumentException(
          "demotionConfidence (" + demotionConfidence
              + ") must be lower than minConfidence (" + minConfidence
              + ") for hysteresis");
    }
    if (minHoldCycles < 0) {
      throw new IllegalArgumentException("minHoldCycles must be >= 0");
    }
  }

  public enum Mode {
    ADVISORY,
    AUTO_ABOVE_THRESHOLD,
    AUTOMATIC,
    DISABLED
  }
}
