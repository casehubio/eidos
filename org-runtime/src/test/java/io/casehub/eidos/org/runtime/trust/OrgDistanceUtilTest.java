package io.casehub.eidos.org.runtime.trust;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class OrgDistanceUtilTest {

    @Test
    void null_snapshot_returns_negative_one() {
        assertThat(OrgDistanceUtil.extractDistance(null)).isEqualTo(-1);
    }

    @Test
    void missing_key_returns_negative_one() {
        assertThat(OrgDistanceUtil.extractDistance(Map.of("other", 5))).isEqualTo(-1);
    }

    @Test
    void non_number_value_returns_negative_one() {
        assertThat(OrgDistanceUtil.extractDistance(Map.of("org-distance", "two"))).isEqualTo(-1);
    }

    @Test
    void integer_distance_returned() {
        assertThat(OrgDistanceUtil.extractDistance(Map.of("org-distance", 3))).isEqualTo(3);
    }

    @Test
    void double_distance_truncated_to_int() {
        assertThat(OrgDistanceUtil.extractDistance(Map.of("org-distance", 2.7))).isEqualTo(2);
    }

    @Test
    void zero_distance_returned() {
        assertThat(OrgDistanceUtil.extractDistance(Map.of("org-distance", 0))).isEqualTo(0);
    }

    @Test
    void snapshot_key_matches_existing_enricher() {
        assertThat(OrgDistanceUtil.SNAPSHOT_KEY).isEqualTo("org-distance");
    }
}
