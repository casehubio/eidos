package io.casehub.eidos.runtime.yaml;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentDisposition;
import io.casehub.eidos.api.VocabularyRegistry;

public class EidosDescriptorModule extends SimpleModule {

    public EidosDescriptorModule(VocabularyRegistry vocabRegistry) {
        addDeserializer(AgentDescriptor.class, new AgentDescriptorDeserializer());
        addDeserializer(AgentDisposition.class, new DispositionDeserializer(vocabRegistry));
    }

    public static ObjectMapper createMapper(VocabularyRegistry vocabRegistry) {
        return new ObjectMapper(new YAMLFactory())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
            .registerModule(new EidosDescriptorModule(vocabRegistry));
    }
}
