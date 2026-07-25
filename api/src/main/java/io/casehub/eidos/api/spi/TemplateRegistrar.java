package io.casehub.eidos.api.spi;

import io.casehub.eidos.api.DescriptorTemplate;
import java.util.List;

@FunctionalInterface
public interface TemplateRegistrar {
    List<DescriptorTemplate> templates();
}
