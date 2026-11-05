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
        var spec = new RoleSpec("reviewer", null, 0, -1, Set.of(), List.of(), null, null);
        assertThat(spec.roleName()).isEqualTo("reviewer");
        assertThat(spec.minCardinality()).isZero();
        assertThat(spec.maxCardinality()).isEqualTo(-1);
        assertThat(spec.incompatibleWith()).isEmpty();
        assertThat(spec.policies()).isEmpty();
    }

    @Test void requiresRoleName() {
        assertThatThrownBy(() -> new RoleSpec(null, null, 0, -1, Set.of(), List.of(), null, null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test void rejectsBlankRoleName() {
        assertThatThrownBy(() -> new RoleSpec("  ", null, 0, -1, Set.of(), List.of(), null, null))
            .isInstanceOf(OrgValidationException.class)
            .hasMessageContaining("blank");
    }

    @Test void rejectsNegativeMinCardinality() {
        assertThatThrownBy(() -> new RoleSpec("r", null, -1, -1, Set.of(), List.of(), null, null))
            .isInstanceOf(OrgValidationException.class)
            .hasMessageContaining("minCardinality");
    }

    @Test void rejectsMaxLessThanMin() {
        assertThatThrownBy(() -> new RoleSpec("r", null, 3, 1, Set.of(), List.of(), null, null))
            .isInstanceOf(OrgValidationException.class)
            .hasMessageContaining("maxCardinality");
    }

    @Test void unboundedMaxIsValid() {
        var spec = new RoleSpec("r", null, 3, -1, Set.of(), List.of(), null, null);
        assertThat(spec.maxCardinality()).isEqualTo(-1);
    }

    @Test void withVocabulary() {
        var spec = new RoleSpec("reviewer", "urn:casehub:roles", 1, 3, Set.of(), List.of(), null, null);
        assertThat(spec.roleVocabulary()).isEqualTo("urn:casehub:roles");
    }

    @Test void withIncompatibility() {
        var spec = new RoleSpec("auditor", null, 0, -1, Set.of("developer"), List.of(), null, null);
        assertThat(spec.incompatibleWith()).containsExactly("developer");
    }

    @Test void withPolicy() {
        var policy = new RolePolicy("code-review", PolicyKind.ELIGIBILITY, Map.of());
        var spec = new RoleSpec("reviewer", null, 0, -1, Set.of(), List.of(policy), null, null);
        assertThat(spec.policies()).hasSize(1);
        assertThat(spec.policies().get(0).kind()).isEqualTo(PolicyKind.ELIGIBILITY);
    }

    @Test void defensiveCopy() {
        var mutableSet = new HashSet<>(Set.of("x"));
        var spec = new RoleSpec("r", null, 0, -1, mutableSet, List.of(), null, null);
        mutableSet.add("y");
        assertThat(spec.incompatibleWith()).containsExactly("x");
    }

    @Test
    void withObligations() {
        var spec = new RoleSpec("coordinator", null, 0, -1, Set.of(), List.of(),
                                List.of("review-output", "approve-plan"), List.of());
        assertThat(spec.obligations()).containsExactly("review-output", "approve-plan");
        assertThat(spec.permissions()).isEmpty();
    }

    @Test
    void withPermissions() {
        var spec = new RoleSpec("coordinator", null, 0, -1, Set.of(), List.of(),
                                List.of(), List.of("provision-agent", "change-topology"));
        assertThat(spec.permissions()).containsExactly("provision-agent", "change-topology");
    }

    @Test
    void obligationsDefaultToEmpty() {
        var spec = new RoleSpec("r", null, 0, -1, null, null, null, null);
        assertThat(spec.obligations()).isEmpty();
        assertThat(spec.permissions()).isEmpty();
    }

    @Test
    void obligationsDefensivelyCopied() {
        var mutable = new java.util.ArrayList<>(List.of("x"));
        var spec    = new RoleSpec("r", null, 0, -1, null, null, mutable, null);
        mutable.add("y");
        assertThat(spec.obligations()).containsExactly("x");
    }

}
