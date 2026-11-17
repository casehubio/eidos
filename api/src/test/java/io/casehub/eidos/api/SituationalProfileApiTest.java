package io.casehub.eidos.api;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SituationalProfileApiTest {

    @Test
    void ampdFacetShiftValidFacet() {
        var shift = new AmpdFacetShift("Anxiousness", 15);
        assertThat(shift.facet()).isEqualTo("Anxiousness");
        assertThat(shift.delta()).isEqualTo(15);
    }

    @Test
    void ampdFacetShiftRejectsUnknownFacet() {
        assertThatThrownBy(() -> new AmpdFacetShift("NotAFacet", 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown AMPD facet");
    }

    @Test
    void ampdFacetShiftRejectsDeltaOutOfRange() {
        assertThatThrownBy(() -> new AmpdFacetShift("Anxiousness", 51))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("[-50, 50]");
        assertThatThrownBy(() -> new AmpdFacetShift("Anxiousness", -51))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ampdFacetShiftAcceptsBoundaryValues() {
        assertThat(new AmpdFacetShift("Hostility", 50).delta()).isEqualTo(50);
        assertThat(new AmpdFacetShift("Hostility", -50).delta()).isEqualTo(-50);
        assertThat(new AmpdFacetShift("Hostility", 0).delta()).isZero();
    }

    @Test
    void situationalProfileValidMode() {
        var profile = new SituationalProfile("stress",
                List.of(new AmpdFacetShift("Anxiousness", 15)), "estimated");
        assertThat(profile.mode()).isEqualTo("stress");
        assertThat(profile.shifts()).hasSize(1);
    }

    @Test
    void situationalProfileRejectsUnknownMode() {
        assertThatThrownBy(() -> new SituationalProfile("panic",
                List.of(), "estimated"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown situational mode");
    }

    @Test
    void situationalProfileAllFiveModes() {
        for (String mode : SituationalProfile.VALID_MODES) {
            var profile = new SituationalProfile(mode, List.of(), "test");
            assertThat(profile.mode()).isEqualTo(mode);
        }
    }

    @Test
    void dispositionRejectsDuplicateModes() {
        var profiles = List.of(
                new SituationalProfile("stress", List.of(), "a"),
                new SituationalProfile("stress", List.of(), "b"));
        assertThatThrownBy(() -> AgentDisposition.builder()
                .situationalProfiles(profiles).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate situational mode");
    }

    @Test
    void dispositionAcceptsDistinctModes() {
        var profiles = List.of(
                new SituationalProfile("stress", List.of(new AmpdFacetShift("Anxiousness", 15)), "estimated"),
                new SituationalProfile("flow", List.of(new AmpdFacetShift("Anxiousness", -10)), "estimated"));
        var disp = AgentDisposition.builder().situationalProfiles(profiles).build();
        assertThat(disp.situationalProfiles()).hasSize(2);
    }

    @Test
    void dispositionDefaultsToEmptyProfiles() {
        var disp = AgentDisposition.builder().build();
        assertThat(disp.situationalProfiles()).isEmpty();
    }

    @Test
    void all13FacetsAreValid() {
        assertThat(AmpdFacetShift.VALID_FACETS).hasSize(13);
        for (String facet : List.of("Anxiousness", "Callousness", "Deceitfulness",
                "Emotional Lability", "Grandiosity", "Hostility", "Impulsivity",
                "Intimacy Avoidance", "Manipulativeness", "Risk Taking",
                "Suspiciousness", "Withdrawal", "Attention Seeking")) {
            assertThat(AmpdFacetShift.VALID_FACETS).contains(facet);
        }
    }
}
