package io.casehub.eidos.annotations.deployment.test;

import io.casehub.eidos.api.DescriptorTemplate;
import io.casehub.eidos.api.spi.TemplateRegistrar;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class TestTemplateRegistrar implements TemplateRegistrar {

    @Override
    public List<DescriptorTemplate> templates() {
        return List.of(
            new DescriptorTemplate("safety-primer", "Safety Primer",
                List.of("domain"), "Safety guidelines for ${domain}"),
            new DescriptorTemplate("jurisdiction-notice", "Jurisdiction Notice",
                List.of("region"), "Operating under ${region} jurisdiction")
        );
    }
}
