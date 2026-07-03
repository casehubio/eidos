package io.casehub.eidos.api;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class MatchDegreeTest {

    @Test
    void exact_beats_everything() {
        assertThat((MatchDegree) new MatchDegree.Exact()).isLessThan(new MatchDegree.Plugin(1));
        assertThat((MatchDegree) new MatchDegree.Exact()).isLessThan(new MatchDegree.Specialization(1));
        assertThat((MatchDegree) new MatchDegree.Exact()).isLessThan(new MatchDegree.None());
    }

    @Test
    void plugin_beats_specialization_at_any_depth() {
        assertThat((MatchDegree) new MatchDegree.Plugin(5)).isLessThan(new MatchDegree.Specialization(1));
    }

    @Test
    void lower_depth_plugin_beats_higher() {
        assertThat((MatchDegree) new MatchDegree.Plugin(1)).isLessThan(new MatchDegree.Plugin(3));
    }

    @Test
    void lower_depth_specialization_beats_higher() {
        assertThat((MatchDegree) new MatchDegree.Specialization(1)).isLessThan(new MatchDegree.Specialization(3));
    }

    @Test
    void none_loses_to_everything() {
        assertThat((MatchDegree) new MatchDegree.None()).isGreaterThan(new MatchDegree.Exact());
        assertThat((MatchDegree) new MatchDegree.None()).isGreaterThan(new MatchDegree.Plugin(100));
        assertThat((MatchDegree) new MatchDegree.None()).isGreaterThan(new MatchDegree.Specialization(100));
    }

    @Test
    void same_type_same_depth_are_equal() {
        assertThat(new MatchDegree.Exact().compareTo(new MatchDegree.Exact())).isZero();
        assertThat(new MatchDegree.Plugin(2).compareTo(new MatchDegree.Plugin(2))).isZero();
        assertThat(new MatchDegree.None().compareTo(new MatchDegree.None())).isZero();
    }

    @Test
    void sorting_produces_owlsmx_order() {
        var degrees = new ArrayList<>(List.of(
            new MatchDegree.None(),
            new MatchDegree.Specialization(2),
            new MatchDegree.Plugin(1),
            new MatchDegree.Exact(),
            new MatchDegree.Plugin(3),
            new MatchDegree.Specialization(1)
        ));
        Collections.sort(degrees);
        assertThat(degrees).containsExactly(
            new MatchDegree.Exact(),
            new MatchDegree.Plugin(1),
            new MatchDegree.Plugin(3),
            new MatchDegree.Specialization(1),
            new MatchDegree.Specialization(2),
            new MatchDegree.None()
        );
    }
}
