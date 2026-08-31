package io.casehub.eidos.runtime.yaml;

import io.casehub.yaml.core.foreach.ForEachAdapter;
import io.casehub.yaml.core.resolver.VariableResolver;

import java.util.LinkedHashMap;
import java.util.Map;

public final class DescriptorForEachAdapter implements ForEachAdapter<Map<String, Object>> {

    @Override
    public Map<String, Object> stamp(Map<String, Object> template, String stampedId,
                                      VariableResolver scopedResolver) {
        var stripped = new LinkedHashMap<>(template);
        stripped.remove("forEach");
        stripped.remove("when");
        return new LinkedHashMap<>(scopedResolver.resolveMap(stripped, stampedId));
    }

    @Override
    public Object getForEach(Map<String, Object> element) {
        return element.get("forEach");
    }

    @Override
    public String getId(Map<String, Object> element) {
        Object id = element.get("agentId");
        return id != null ? id.toString() : null;
    }

    @Override
    public String getWhen(Map<String, Object> element) {
        Object when = element.get("when");
        return when != null ? when.toString() : null;
    }
}
