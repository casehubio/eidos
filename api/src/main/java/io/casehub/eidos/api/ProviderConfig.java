package io.casehub.eidos.api;

import java.util.Map;

public record ProviderConfig(
        String providerName,
        Map<String, String> config
) {
    public ProviderConfig {
        AgentDescriptorValidator.validateRequired("providerConfig.providerName", providerName,
                AgentDescriptorValidator.MAX_PROVIDER);
        config = config != null ? Map.copyOf(config) : Map.of();
        AgentDescriptorValidator.validateMapKeys("providerConfig.config", config.keySet(),
                AgentDescriptorValidator.MAX_PROVIDER_CONFIG_KEY);
        for (var entry : config.entrySet()) {
            AgentDescriptorValidator.validateOptional(
                    "providerConfig.config[" + entry.getKey() + "]",
                    entry.getValue(), AgentDescriptorValidator.MAX_PROVIDER_CONFIG_VALUE);
        }
    }
}
