package io.casehub.eidos.api;

import java.util.Objects;

public record RoleDeclaration(
    String roleName,
    String roleVocabulary) {

  public RoleDeclaration {
    Objects.requireNonNull(roleName, "roleName must not be null");
    if (roleName.isBlank()) {
      throw new IllegalArgumentException("roleName must not be blank");
    }
  }
}
