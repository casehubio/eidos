package io.casehub.eidos.api;

public record RoleDeclaration(
    String roleName,
    String roleVocabulary) {

  public RoleDeclaration {
    AgentDescriptorValidator.validateRequired("roleName", roleName,
        AgentDescriptorValidator.MAX_ROLE_NAME);
    AgentDescriptorValidator.validateOptional("roleVocabulary", roleVocabulary,
        AgentDescriptorValidator.MAX_ROLE_VOCABULARY);
  }
}
