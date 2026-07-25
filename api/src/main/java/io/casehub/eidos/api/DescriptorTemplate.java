package io.casehub.eidos.api;

import java.util.List;

public record DescriptorTemplate(
        String id,
        String name,
        List<String> parameters,
        String content
) {
    public DescriptorTemplate {
        AgentDescriptorValidator.validateRequired("template.id", id, AgentDescriptorValidator.MAX_TEMPLATE_ID);
        AgentDescriptorValidator.validateRequired("template.name", name, AgentDescriptorValidator.MAX_TEMPLATE_NAME);
        AgentDescriptorValidator.validateRequired("template.content", content,
                AgentDescriptorValidator.MAX_TEMPLATE_CONTENT, 0x000A);
        parameters = parameters != null ? List.copyOf(parameters) : List.of();
        AgentDescriptorValidator.validateItems("template.parameters", parameters,
                AgentDescriptorValidator.MAX_PARAMETER_NAME);
    }
}
