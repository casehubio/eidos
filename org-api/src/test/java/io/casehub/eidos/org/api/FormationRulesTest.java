package io.casehub.eidos.org.api;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FormationRulesTest {

    @Test void defaultValues() {
        var rules = new FormationRules(null, 2, -1, true, 1.0);
        assertThat(rules.allowedPaths()).containsExactlyInAnyOrder(FormationPath.values());
        assertThat(rules.minGroupSize()).isEqualTo(2);
        assertThat(rules.proposalQuorum()).isEqualTo(1.0);
    }

    @Test void rejectsZeroMinSize() {
        assertThatThrownBy(() -> new FormationRules(Set.of(), 0, -1, true, 1.0))
            .isInstanceOf(OrgValidationException.class)
            .hasMessageContaining("minGroupSize");
    }

    @Test void rejectsMaxLessThanMin() {
        assertThatThrownBy(() -> new FormationRules(Set.of(), 3, 1, true, 1.0))
            .isInstanceOf(OrgValidationException.class)
            .hasMessageContaining("maxGroupSize");
    }

    @Test void rejectsInvalidQuorum() {
        assertThatThrownBy(() -> new FormationRules(Set.of(), 2, -1, true, 1.5))
            .isInstanceOf(OrgValidationException.class)
            .hasMessageContaining("proposalQuorum");
    }

    @Test void rejectsNegativeQuorum() {
        assertThatThrownBy(() -> new FormationRules(Set.of(), 2, -1, true, -0.1))
            .isInstanceOf(OrgValidationException.class)
            .hasMessageContaining("proposalQuorum");
    }

    @Test void restrictedPaths() {
        var rules = new FormationRules(Set.of(FormationPath.TOP_DOWN), 2, -1, false, 1.0);
        assertThat(rules.allowedPaths()).containsExactly(FormationPath.TOP_DOWN);
        assertThat(rules.allowSubGroups()).isFalse();
    }

    @Test void unboundedMaxIsValid() {
        var rules = new FormationRules(Set.of(), 1, -1, true, 0.5);
        assertThat(rules.maxGroupSize()).isEqualTo(-1);
        assertThat(rules.proposalQuorum()).isEqualTo(0.5);
    }
}
