package io.casehub.eidos.api;

import java.util.Map;

public record TemplateRef(
        String templateId,
        Map<String, String> args
) {
    static final int MAX_TEMPLATE_ARG_VALUE = 1000;

    public TemplateRef {
        AgentDescriptorValidator.validateRequired("templateRef.templateId", templateId,
                AgentDescriptorValidator.MAX_TEMPLATE_ID);
        args = args != null ? Map.copyOf(args) : Map.of();
        AgentDescriptorValidator.validateMapKeys("templateRef.args", args.keySet(),
                AgentDescriptorValidator.MAX_PARAMETER_NAME);
        AgentDescriptorValidator.validateItems("templateRef.args.values", args.values(),
                MAX_TEMPLATE_ARG_VALUE);
    }
}
