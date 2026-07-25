package io.casehub.eidos.runtime.registrar;

import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.DescriptorTemplate;
import io.casehub.eidos.api.TemplateRef;
import io.casehub.eidos.api.TemplateRegistry;
import io.casehub.eidos.api.spi.AgentDescriptorRegistrar;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DescriptorCollectorTest {

    private TemplateRegistry templateRegistry() {
        var reg = new SimpleTemplateRegistry();
        reg.register(new DescriptorTemplate("style", "Style", List.of(), "Follow conventions."));
        reg.register(new DescriptorTemplate("role", "Role", List.of("role_name"),
                                            "You are a ${role_name}."));
        return reg;
    }

    private AgentDescriptor descriptorWithTemplates(List<TemplateRef> refs) {
        return AgentDescriptor.builder()
                              .agentId("a").name("n").slot("s").tenancyId("t")
                              .templates(refs).build();
    }

    @Test
    void valid_refs_pass() {
        var refs = List.of(
                new TemplateRef("style", Map.of()),
                new TemplateRef("role", Map.of("role_name", "reviewer")));
        AgentDescriptorRegistrar registrar = () -> List.of(descriptorWithTemplates(refs));
        assertThatNoException().isThrownBy(() ->
                                                   DescriptorCollector.collectAndValidate(List.of(registrar), templateRegistry()));
    }

    @Test
    void unknown_template_id_throws() {
        var                      refs      = List.of(new TemplateRef("nonexistent", Map.of()));
        AgentDescriptorRegistrar registrar = () -> List.of(descriptorWithTemplates(refs));
        assertThatThrownBy(() ->
                                   DescriptorCollector.collectAndValidate(List.of(registrar), templateRegistry()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("nonexistent");
    }

    @Test
    void missing_required_arg_throws() {
        var                      refs      = List.of(new TemplateRef("role", Map.of()));
        AgentDescriptorRegistrar registrar = () -> List.of(descriptorWithTemplates(refs));
        assertThatThrownBy(() ->
                                   DescriptorCollector.collectAndValidate(List.of(registrar), templateRegistry()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("role_name");
    }

    @Test
    void extra_arg_throws() {
        var                      refs      = List.of(new TemplateRef("style", Map.of("bogus", "value")));
        AgentDescriptorRegistrar registrar = () -> List.of(descriptorWithTemplates(refs));
        assertThatThrownBy(() ->
                                   DescriptorCollector.collectAndValidate(List.of(registrar), templateRegistry()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("bogus");
    }

    @Test
    void null_templates_passes() {
        var                      d         = AgentDescriptor.builder().agentId("a").name("n").slot("s").tenancyId("t").build();
        AgentDescriptorRegistrar registrar = () -> List.of(d);
        assertThatNoException().isThrownBy(() ->
                                                   DescriptorCollector.collectAndValidate(List.of(registrar), templateRegistry()));
    }

    @Test
    void empty_templates_passes() {
        var d = AgentDescriptor.builder().agentId("a").name("n").slot("s").tenancyId("t")
                               .templates(List.of()).build();
        AgentDescriptorRegistrar registrar = () -> List.of(d);
        assertThatNoException().isThrownBy(() ->
                                                   DescriptorCollector.collectAndValidate(List.of(registrar), templateRegistry()));
    }

    static class SimpleTemplateRegistry implements TemplateRegistry {
        private final java.util.concurrent.ConcurrentHashMap<String, DescriptorTemplate> store = new java.util.concurrent.ConcurrentHashMap<>();

        @Override
        public void register(DescriptorTemplate t)                       {store.put(t.id(), t);}

        @Override
        public java.util.Optional<DescriptorTemplate> resolve(String id) {return java.util.Optional.ofNullable(store.get(id));}

        @Override
        public List<DescriptorTemplate> all()                            {return List.copyOf(store.values());}
    }
}
