package io.casehub.eidos.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatNoException;

import org.junit.jupiter.api.Test;

class RoleDeclarationTest {

  @Test
  void creates_with_name_and_vocabulary() {
    var decl = new RoleDeclaration("coordinator", "urn:casehub:vocab:roles");
    assertThat(decl.roleName()).isEqualTo("coordinator");
    assertThat(decl.roleVocabulary()).isEqualTo("urn:casehub:vocab:roles");
  }

  @Test
  void creates_with_null_vocabulary() {
    assertThatNoException().isThrownBy(() ->
        new RoleDeclaration("analyst", null));
  }

  @Test
  void null_name_rejected() {
    assertThatNullPointerException()
        .isThrownBy(() -> new RoleDeclaration(null, null))
        .withMessageContaining("roleName");
  }

  @Test
  void blank_name_rejected() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new RoleDeclaration("  ", null))
        .withMessageContaining("roleName");
  }
}
