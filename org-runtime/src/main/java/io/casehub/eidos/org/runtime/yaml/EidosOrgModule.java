package io.casehub.eidos.org.runtime.yaml;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.casehub.eidos.org.api.RelationshipKind;
import io.casehub.yaml.jackson.YamlMappers;

public class EidosOrgModule extends SimpleModule {

    public EidosOrgModule() {
        addDeserializer(RelationshipKind.class, new RelationshipKindDeserializer());
    }

    public static ObjectMapper createMapper() {
        return YamlMappers.create()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .registerModule(new EidosOrgModule());
    }
}
