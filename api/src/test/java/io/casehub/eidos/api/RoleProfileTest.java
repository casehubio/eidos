package io.casehub.eidos.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.List;

import org.junit.jupiter.api.Test;

class RoleProfileTest {

  static final RoleChangePolicy POLICY =
      new RoleChangePolicy(RoleChangePolicy.Mode.AUTO_ABOVE_THRESHOLD, 0.8, 0.5, 3);

  @Test
  void creates_with_declared_roles_and_policy() {
    var profile = new RoleProfile(
        List.of(new RoleDeclaration("coordinator", null),
                new RoleDeclaration("analyst", null)),
        POLICY);
    assertThat(profile.declaredRoles()).hasSize(2);
    assertThat(profile.changePolicy()).isEqualTo(POLICY);
  }

  @Test
  void declared_roles_are_immutable() {
    var roles = new java.util.ArrayList<>(List.of(new RoleDeclaration("coordinator", null)));
    var profile = new RoleProfile(roles, POLICY);
    roles.add(new RoleDeclaration("intruder", null));
    assertThat(profile.declaredRoles()).hasSize(1);
  }

  @Test
  void duplicate_role_names_rejected() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new RoleProfile(
            List.of(new RoleDeclaration("coordinator", null),
                    new RoleDeclaration("coordinator", "urn:other")),
            POLICY))
        .withMessageContaining("duplicate");
  }

  @Test
  void null_roles_rejected() {
    assertThatNullPointerException()
        .isThrownBy(() -> new RoleProfile(null, POLICY));
  }

  @Test
  void null_policy_rejected() {
    assertThatNullPointerException()
        .isThrownBy(() -> new RoleProfile(List.of(), null));
  }

  @Test
  void hasRole_returns_true_for_declared_role() {
    var profile = new RoleProfile(
        List.of(new RoleDeclaration("coordinator", null)), POLICY);
    assertThat(profile.hasRole("coordinator")).isTrue();
  }

  @Test
  void hasRole_returns_false_for_undeclared_role() {
    var profile = new RoleProfile(
        List.of(new RoleDeclaration("coordinator", null)), POLICY);
    assertThat(profile.hasRole("supervisor")).isFalse();
  }

  @Test
  void empty_roles_accepted() {
    var profile = new RoleProfile(List.of(), RoleChangePolicy.ADVISORY);
    assertThat(profile.declaredRoles()).isEmpty();
  }
}
