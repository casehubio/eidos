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
                                                   DescriptorCollector.collectAndValidate(List.of(registrar), templateRegistry(), NoOpVocabRegistry.INSTANCE, null));
    }

    @Test
    void unknown_template_id_throws() {
        var                      refs      = List.of(new TemplateRef("nonexistent", Map.of()));
        AgentDescriptorRegistrar registrar = () -> List.of(descriptorWithTemplates(refs));
        assertThatThrownBy(() ->
                                   DescriptorCollector.collectAndValidate(List.of(registrar), templateRegistry(), NoOpVocabRegistry.INSTANCE, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("nonexistent");
    }

    @Test
    void missing_required_arg_throws() {
        var                      refs      = List.of(new TemplateRef("role", Map.of()));
        AgentDescriptorRegistrar registrar = () -> List.of(descriptorWithTemplates(refs));
        assertThatThrownBy(() ->
                                   DescriptorCollector.collectAndValidate(List.of(registrar), templateRegistry(), NoOpVocabRegistry.INSTANCE, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("role_name");
    }

    @Test
    void extra_arg_throws() {
        var                      refs      = List.of(new TemplateRef("style", Map.of("bogus", "value")));
        AgentDescriptorRegistrar registrar = () -> List.of(descriptorWithTemplates(refs));
        assertThatThrownBy(() ->
                                   DescriptorCollector.collectAndValidate(List.of(registrar), templateRegistry(), NoOpVocabRegistry.INSTANCE, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("bogus");
    }

    @Test
    void null_templates_passes() {
        var                      d         = AgentDescriptor.builder().agentId("a").name("n").slot("s").tenancyId("t").build();
        AgentDescriptorRegistrar registrar = () -> List.of(d);
        assertThatNoException().isThrownBy(() ->
                                                   DescriptorCollector.collectAndValidate(List.of(registrar), templateRegistry(), NoOpVocabRegistry.INSTANCE, null));
    }

    @Test
    void empty_templates_passes() {
        var d = AgentDescriptor.builder().agentId("a").name("n").slot("s").tenancyId("t")
                               .templates(List.of()).build();
        AgentDescriptorRegistrar registrar = () -> List.of(d);
        assertThatNoException().isThrownBy(() ->
                                                   DescriptorCollector.collectAndValidate(List.of(registrar), templateRegistry(), NoOpVocabRegistry.INSTANCE, null));
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

    enum NoOpVocabRegistry implements io.casehub.eidos.api.VocabularyRegistry {
        INSTANCE;

        @Override
        public <T extends Enum<T> & io.casehub.eidos.api.VocabularyTerm> void register(Class<T> vocab)                                                                                                                                            {}

        @Override
        public boolean isRegistered(String vocabUri)                                                                                                                                                                                              {return false;}

        @Override
        public java.util.Set<String> registeredUris()                                                                                                                                                                                             {return java.util.Set.of();}

        @Override
        public java.util.Optional<? extends io.casehub.eidos.api.VocabularyTerm> resolve(String vocabUri, String value)                                                                                                                           {return java.util.Optional.empty();}

        @Override
        public List<? extends io.casehub.eidos.api.VocabularyTerm> allTerms(String vocabUri)                                                                                                                                                      {return List.of();}

        @Override
        public java.util.Optional<String> equivalentValues(String f, String v, String t)                                                                                                                                                          {return java.util.Optional.empty();}

        @Override
        public java.util.Optional<String> equivalentValues(String f, String v, String t, io.casehub.eidos.api.DispositionAxis a)                                                                                                                  {return java.util.Optional.empty();}

        @Override
        public <T extends Enum<T> & io.casehub.eidos.api.VocabularyTerm> java.util.Optional<T> resolve(Class<T> vocab, String value)                                                                                                              {return java.util.Optional.empty();}

        @Override
        public <S extends Enum<S> & io.casehub.eidos.api.VocabularyTerm, T extends Enum<T> & io.casehub.eidos.api.VocabularyTerm> java.util.Optional<T> equivalentValues(S from, Class<T> targetVocab)                                            {return java.util.Optional.empty();}

        @Override
        public <S extends Enum<S> & io.casehub.eidos.api.VocabularyTerm, T extends Enum<T> & io.casehub.eidos.api.VocabularyTerm> java.util.Optional<T> equivalentValues(S from, Class<T> targetVocab, io.casehub.eidos.api.DispositionAxis axis) {return java.util.Optional.empty();}

        @Override
        public java.util.Optional<io.casehub.eidos.api.VocabularyMetadata> vocabularyMetadata(String uri)                                                                                                                                         {return java.util.Optional.empty();}

        @Override
        public boolean subsumes(String vocabUri, String generalValue, String specificValue)                                                                                                                                                       {return false;}

        @Override
        public io.casehub.eidos.api.MatchDegree match(String vocabUri, String declaredValue, String requestedValue)                                                                                                                               {return new io.casehub.eidos.api.MatchDegree.None();}

        @Override
        public List<? extends io.casehub.eidos.api.VocabularyTerm> ancestors(String vocabUri, String value)                                                                                                                                       {return List.of();}

        @Override
        public List<? extends io.casehub.eidos.api.VocabularyTerm> descendants(String vocabUri, String value)                                                                                                                                     {return List.of();}

        @Override
        public java.util.Map<String, java.util.Set<String>> expandForMatchingByVocabulary(String value)                                                                                                                                           {return java.util.Map.of();}
    }

}
