package io.casehub.eidos.api;

import org.junit.jupiter.api.Test;

import static io.casehub.eidos.api.DispositionAxis.*;
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
}
