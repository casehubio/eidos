package io.casehub.eidos.org.runtime.yaml;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.casehub.eidos.org.api.RelationshipKind;

import java.io.IOException;

public class RelationshipKindDeserializer extends StdDeserializer<RelationshipKind> {

    public RelationshipKindDeserializer() {
        super(RelationshipKind.class);
    }

    @Override
    public RelationshipKind deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getText().trim().toUpperCase();
        return RelationshipKind.valueOf(value);
    }
}
