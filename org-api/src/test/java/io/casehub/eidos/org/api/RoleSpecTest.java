package io.casehub.eidos.org.api;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoleSpecTest {

    @Test void minimalRoleSpec() {
        var spec = new RoleSpec("reviewer", null, 0, -1, Set.of(), List.of());
        assertThat(spec.roleName()).isEqualTo("reviewer");
        assertThat(spec.minCardinality()).isZero();
        assertThat(spec.maxCardinality()).isEqualTo(-1);
        assertThat(spec.incompatibleWith()).isEmpty();
        assertThat(spec.policies()).isEmpty();
    }

    @Test void requiresRoleName() {
        assertThatThrownBy(() -> new RoleSpec(null, null, 0, -1, Set.of(), List.of()))
            .isInstanceOf(NullPointerException.class);
    }

    @Test void rejectsBlankRoleName() {
        assertThatThrownBy(() -> new RoleSpec("  ", null, 0, -1, Set.of(), List.of()))
            .isInstanceOf(OrgValidationException.class)
            .hasMessageContaining("blank");
    }

    @Test void rejectsNegativeMinCardinality() {
        assertThatThrownBy(() -> new RoleSpec("r", null, -1, -1, Set.of(), List.of()))
            .isInstanceOf(OrgValidationException.class)
            .hasMessageContaining("minCardinality");
    }

    @Test void rejectsMaxLessThanMin() {
        assertThatThrownBy(() -> new RoleSpec("r", null, 3, 1, Set.of(), List.of()))
            .isInstanceOf(OrgValidationException.class)
            .hasMessageContaining("maxCardinality");
    }

    @Test void unboundedMaxIsValid() {
        var spec = new RoleSpec("r", null, 3, -1, Set.of(), List.of());
        assertThat(spec.maxCardinality()).isEqualTo(-1);
    }

    @Test void withVocabulary() {
        var spec = new RoleSpec("reviewer", "urn:casehub:roles", 1, 3, Set.of(), List.of());
        assertThat(spec.roleVocabulary()).isEqualTo("urn:casehub:roles");
    }

    @Test void withIncompatibility() {
        var spec = new RoleSpec("auditor", null, 0, -1, Set.of("developer"), List.of());
        assertThat(spec.incompatibleWith()).containsExactly("developer");
    }

    @Test void withPolicy() {
        var policy = new RolePolicy("code-review", PolicyKind.ELIGIBILITY, Map.of());
        var spec = new RoleSpec("reviewer", null, 0, -1, Set.of(), List.of(policy));
        assertThat(spec.policies()).hasSize(1);
        assertThat(spec.policies().get(0).kind()).isEqualTo(PolicyKind.ELIGIBILITY);
    }

    @Test void defensiveCopy() {
        var mutableSet = new HashSet<>(Set.of("x"));
        var spec = new RoleSpec("r", null, 0, -1, mutableSet, List.of());
        mutableSet.add("y");
        assertThat(spec.incompatibleWith()).containsExactly("x");
    }
}
