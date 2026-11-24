package io.casehub.eidos.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    assertThatThrownBy(() -> new RoleDeclaration(null, null))
        .isInstanceOf(AgentValidationException.class)
        .hasMessageContaining("roleName");
  }

  @Test
  void blank_name_rejected() {
    assertThatThrownBy(() -> new RoleDeclaration("  ", null))
        .isInstanceOf(AgentValidationException.class)
        .hasMessageContaining("roleName");
  }

  @Test
  void name_at_max_length_accepted() {
    var name = "a".repeat(100);
    assertThatNoException().isThrownBy(() -> new RoleDeclaration(name, null));
  }

  @Test
  void name_exceeding_max_length_rejected() {
    var name = "a".repeat(101);
    assertThatThrownBy(() -> new RoleDeclaration(name, null))
        .isInstanceOf(AgentValidationException.class)
        .hasMessageContaining("roleName");
  }

  @Test
  void vocabulary_at_max_length_accepted() {
    var vocab = "a".repeat(500);
    assertThatNoException().isThrownBy(() -> new RoleDeclaration("valid", vocab));
  }

  @Test
  void vocabulary_exceeding_max_length_rejected() {
    var vocab = "a".repeat(501);
    assertThatThrownBy(() -> new RoleDeclaration("valid", vocab))
        .isInstanceOf(AgentValidationException.class)
        .hasMessageContaining("roleVocabulary");
  }
}
