package io.casehub.eidos.vocab;

import io.casehub.eidos.api.AmpdFacetShift;
import io.casehub.eidos.api.SituationalProfile;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SituationalProfileDefaultsTest {

    @Test
    void all12FamiliesHaveProfiles() {
        var all = SituationalProfileDefaults.all();
        assertThat(all).hasSize(12);
        for (ArchetypeFamily family : ArchetypeFamily.values()) {
            assertThat(all).containsKey(family);
        }
    }

    @Test
    void eachFamilyHas5Modes() {
        for (ArchetypeFamily family : ArchetypeFamily.values()) {
            var profiles = SituationalProfileDefaults.forFamily(family);
            assertThat(profiles).hasSize(5)
                    .as("Family %s should have 5 modes", family);
            var modes = profiles.stream().map(SituationalProfile::mode).toList();
            assertThat(modes).containsExactlyInAnyOrder(
                    "stress", "flow", "social_collaborative",
                    "social_competitive", "social_hierarchical");
        }
    }

    @Test
    void allShiftsHaveValidFacetsAndBounds() {
        for (var entry : SituationalProfileDefaults.all().entrySet()) {
            for (var profile : entry.getValue()) {
                for (var shift : profile.shifts()) {
                    assertThat(AmpdFacetShift.VALID_FACETS)
                            .as("%s/%s facet '%s'", entry.getKey(), profile.mode(), shift.facet())
                            .contains(shift.facet());
                    assertThat(shift.delta())
                            .as("%s/%s delta for '%s'", entry.getKey(), profile.mode(), shift.facet())
                            .isBetween(-50, 50);
                }
            }
        }
    }

    @Test
    void total60Profiles() {
        int total = SituationalProfileDefaults.all().values().stream()
                .mapToInt(java.util.List::size).sum();
        assertThat(total).isEqualTo(60);
    }

    @Test
    void allProfilesHaveProvenance() {
        for (var profiles : SituationalProfileDefaults.all().values()) {
            for (var profile : profiles) {
                assertThat(profile.provenance()).isNotNull().isNotBlank();
            }
        }
    }

    @Test
    void caregiverStressProfileMatchesExpectations() {
        var profiles = SituationalProfileDefaults.forFamily(ArchetypeFamily.CAREGIVER);
        var stress = profiles.stream().filter(p -> "stress".equals(p.mode())).findFirst().orElseThrow();
        assertThat(stress.shifts()).isNotEmpty();
        var anxiousness = stress.shifts().stream()
                .filter(s -> "Anxiousness".equals(s.facet())).findFirst();
        assertThat(anxiousness).isPresent();
        assertThat(anxiousness.get().delta()).isPositive();
    }
}
