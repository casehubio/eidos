package io.casehub.eidos.runtime.preferences;

import io.casehub.platform.api.preferences.PreferenceKey;
import io.casehub.platform.api.preferences.SingleValuePreference;

public final class GoalPreferenceKeys {

    private GoalPreferenceKeys() {}

    public static final PreferenceKey<PromotionThreshold> PROMOTION_THRESHOLD =
            new PreferenceKey<>("casehub.eidos", "goal.promotion-threshold",
                                new PromotionThreshold(0.8),
                                s -> new PromotionThreshold(Double.parseDouble(s)));

    public static final PreferenceKey<PromotionMinCount> PROMOTION_MIN_COUNT =
            new PreferenceKey<>("casehub.eidos", "goal.promotion-min-count",
                                new PromotionMinCount(10),
                                s -> new PromotionMinCount(Integer.parseInt(s)));

    public static final PreferenceKey<DemotionThreshold> DEMOTION_THRESHOLD =
            new PreferenceKey<>("casehub.eidos", "goal.demotion-threshold",
                                new DemotionThreshold(0.7),
                                s -> new DemotionThreshold(Double.parseDouble(s)));

    public static final PreferenceKey<DemotionMinCount> DEMOTION_MIN_COUNT =
            new PreferenceKey<>("casehub.eidos", "goal.demotion-min-count",
                                new DemotionMinCount(10),
                                s -> new DemotionMinCount(Integer.parseInt(s)));

    public static final PreferenceKey<GoalDecayFactor> DECAY_FACTOR =
            new PreferenceKey<>("casehub.eidos", "goal.decay-factor",
                                new GoalDecayFactor(0.20),
                                s -> new GoalDecayFactor(Double.parseDouble(s)));

    public record PromotionThreshold(double value) implements SingleValuePreference {
        @Override public String toSerializedValue() { return String.valueOf(value); }
    }

    public record PromotionMinCount(int value) implements SingleValuePreference {
        @Override public String toSerializedValue() { return String.valueOf(value); }
    }

    public record DemotionThreshold(double value) implements SingleValuePreference {
        @Override public String toSerializedValue() { return String.valueOf(value); }
    }

    public record DemotionMinCount(int value) implements SingleValuePreference {
        @Override public String toSerializedValue() { return String.valueOf(value); }
    }

    public record GoalDecayFactor(double value) implements SingleValuePreference {
        @Override public String toSerializedValue() { return String.valueOf(value); }
    }
}
