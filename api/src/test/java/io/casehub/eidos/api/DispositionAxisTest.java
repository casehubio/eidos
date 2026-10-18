package io.casehub.eidos.api;

import org.junit.jupiter.api.Test;

import static io.casehub.eidos.api.DispositionAxis.AUTONOMY;
import static io.casehub.eidos.api.DispositionAxis.CONFLICT_MODE;
import static io.casehub.eidos.api.DispositionAxis.RISK_APPETITE;
import static io.casehub.eidos.api.DispositionAxis.RULE_FOLLOWING;
import static io.casehub.eidos.api.DispositionAxis.SOCIAL_ORIENTATION;
import static io.casehub.eidos.api.DispositionAxis.values;
import static org.assertj.core.api.Assertions.assertThat;

class DispositionAxisTest {

    @Test
    void jsonKey_returns_camelCase_for_each_axis() {
        assertThat(SOCIAL_ORIENTATION.jsonKey()).isEqualTo("socialOrient");
        assertThat(RULE_FOLLOWING.jsonKey()).isEqualTo("ruleFollowing");
        assertThat(RISK_APPETITE.jsonKey()).isEqualTo("riskAppetite");
        assertThat(AUTONOMY.jsonKey()).isEqualTo("autonomy");
        assertThat(CONFLICT_MODE.jsonKey()).isEqualTo("conflictMode");
    }

    @Test
    void jsonKey_covers_all_constants() {
        for (final DispositionAxis axis : values()) {
            assertThat(axis.jsonKey())
                .as("jsonKey() must be non-blank for %s", axis)
                .isNotBlank();
        }
    }

    @Test
    void description_is_non_blank_for_all_axes_including_conflict_mode() {
        for (final DispositionAxis axis : values()) {
            assertThat(axis.description())
                .as("description() must be non-blank for %s", axis)
                .isNotBlank();
        }
    }

    @Test
    void description_mentions_the_concept_for_key_axes() {
        assertThat(RISK_APPETITE.description()).containsIgnoringCase("risk");
        assertThat(RULE_FOLLOWING.description()).containsIgnoringCase("rule");
        assertThat(SOCIAL_ORIENTATION.description()).containsIgnoringCase("collaborat");
        assertThat(AUTONOMY.description()).containsIgnoringCase("initiative");
        assertThat(CONFLICT_MODE.description()).containsIgnoringCase("conflict");
    }

    @Test
    void displayName_returns_humanReadable_for_each_axis() {
        assertThat(SOCIAL_ORIENTATION.displayName()).isEqualTo("Social orientation");
        assertThat(RULE_FOLLOWING.displayName()).isEqualTo("Rule following");
        assertThat(RISK_APPETITE.displayName()).isEqualTo("Risk appetite");
        assertThat(AUTONOMY.displayName()).isEqualTo("Autonomy");
        assertThat(CONFLICT_MODE.displayName()).isEqualTo("Conflict mode");
    }

    @Test
    void lowPoleLabel_returns_lowEnd_term_for_each_axis() {
        assertThat(SOCIAL_ORIENTATION.lowPoleLabel()).isEqualTo("independent");
        assertThat(RULE_FOLLOWING.lowPoleLabel()).isEqualTo("flexible");
        assertThat(RISK_APPETITE.lowPoleLabel()).isEqualTo("cautious");
        assertThat(AUTONOMY.lowPoleLabel()).isEqualTo("guided");
        assertThat(CONFLICT_MODE.lowPoleLabel()).isEqualTo("avoiding");
    }

    @Test
    void highPoleLabel_returns_highEnd_term_for_each_axis() {
        assertThat(SOCIAL_ORIENTATION.highPoleLabel()).isEqualTo("collaborative");
        assertThat(RULE_FOLLOWING.highPoleLabel()).isEqualTo("strict");
        assertThat(RISK_APPETITE.highPoleLabel()).isEqualTo("bold");
        assertThat(AUTONOMY.highPoleLabel()).isEqualTo("autonomous");
        assertThat(CONFLICT_MODE.highPoleLabel()).isEqualTo("competing");
    }

    @Test
    void displayName_covers_all_constants() {
        for (final DispositionAxis axis : values()) {
            assertThat(axis.displayName())
                    .as("displayName() must be non-blank for %s", axis)
                    .isNotBlank();
        }
    }

    @Test
    void lowPoleLabel_covers_all_constants() {
        for (final DispositionAxis axis : values()) {
            assertThat(axis.lowPoleLabel())
                    .as("lowPoleLabel() must be non-blank for %s", axis)
                    .isNotBlank();
        }
    }

    @Test
    void highPoleLabel_covers_all_constants() {
        for (final DispositionAxis axis : values()) {
            assertThat(axis.highPoleLabel())
                    .as("highPoleLabel() must be non-blank for %s", axis)
                    .isNotBlank();
        }
    }


}
