package io.casehub.eidos.runtime.yaml;

import io.casehub.yaml.core.foreach.ForEachAdapter;
import io.casehub.yaml.core.foreach.ForEachDirective;
import io.casehub.yaml.core.resolver.VariableResolver;

import java.util.LinkedHashMap;
import java.util.List;
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
    public ForEachDirective getForEach(Map<String, Object> element) {
        Object raw = element.get("forEach");
        if (raw == null) {return null;}
        if (raw instanceof ForEachDirective d) {return d;}
        if (raw instanceof String s) {return new ForEachDirective.GroupRef(s);}
        if (raw instanceof Map<?, ?> map) {
            String as    = map.get("as") != null ? map.get("as").toString() : null;
            Object inVal = map.get("in");
            if (as != null && inVal instanceof List<?> list) {
                return new ForEachDirective.InlineIteration(as, list);
            }
            if (as != null) {
                return new ForEachDirective.InlineIteration(as, List.of());
            }
        }
        return new ForEachDirective.GroupRef(raw.toString());
    }

    @Override
    public String getWhen(Map<String, Object> element) {
        Object when = element.get("when");
        return when != null ? when.toString() : null;
    }
}
