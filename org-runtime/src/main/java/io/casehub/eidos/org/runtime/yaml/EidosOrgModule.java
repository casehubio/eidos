package io.casehub.eidos.org.runtime.yaml;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.casehub.eidos.org.api.RelationshipKind;

public class EidosOrgModule extends SimpleModule {

    public EidosOrgModule() {
        addDeserializer(RelationshipKind.class, new RelationshipKindDeserializer());
    }

    public static ObjectMapper createMapper() {
        return new ObjectMapper(new YAMLFactory())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .registerModule(new EidosOrgModule());
    }
}
