package io.casehub.eidos.api;

import java.util.List;
import java.util.Objects;

public record RoleProfile(
    List<RoleDeclaration> declaredRoles,
    RoleChangePolicy changePolicy) {

  public RoleProfile {
    Objects.requireNonNull(declaredRoles, "declaredRoles must not be null");
    Objects.requireNonNull(changePolicy, "changePolicy must not be null");
    declaredRoles = List.copyOf(declaredRoles);
    if (declaredRoles.size() > AgentDescriptorValidator.MAX_ROLE_DECLARATIONS) {
      throw new AgentValidationException("declaredRoles",
          "exceeds maximum count " + AgentDescriptorValidator.MAX_ROLE_DECLARATIONS
              + " (was " + declaredRoles.size() + ")");
    }
    if (declaredRoles.size() > 1) {
      long distinctNames = declaredRoles.stream()
          .map(RoleDeclaration::roleName).distinct().count();
      if (distinctNames < declaredRoles.size()) {
        throw new IllegalArgumentException("duplicate role name in declaredRoles");
      }
    }
  }

  public boolean hasRole(String roleName) {
    return declaredRoles.stream().anyMatch(r -> r.roleName().equals(roleName));
  }
}
