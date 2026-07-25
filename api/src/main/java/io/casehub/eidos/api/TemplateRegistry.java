package io.casehub.eidos.api;

import java.util.List;
import java.util.Optional;

public interface TemplateRegistry {
    void register(DescriptorTemplate template);
    Optional<DescriptorTemplate> resolve(String id);
    List<DescriptorTemplate> all();
}
