package io.casehub.eidos.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ExtensionDataCopier {

    private ExtensionDataCopier() {}

    public static Map<String, Object> deepCopy(Map<String, Object> source) {
        var result = new LinkedHashMap<String, Object>(source.size());
        for (var entry : source.entrySet()) {
            result.put(entry.getKey(), copyValue(entry.getValue()));
        }
        return Collections.unmodifiableMap(result);
    }

    public static long estimateSize(Map<String, Object> source) {
        return estimateMapSize(source);
    }

    @SuppressWarnings("unchecked")
    private static Object copyValue(Object value) {
        if (value == null) return null;
        if (value instanceof String || value instanceof Number || value instanceof Boolean) {
            return value;
        }
        if (value instanceof Map<?, ?> map) {
            var result = new LinkedHashMap<String, Object>(map.size());
            for (var entry : map.entrySet()) {
                result.put((String) entry.getKey(), copyValue(entry.getValue()));
            }
            return Collections.unmodifiableMap(result);
        }
        if (value instanceof List<?> list) {
            var result = new ArrayList<>(list.size());
            for (var item : list) {
                result.add(copyValue(item));
            }
            return Collections.unmodifiableList(result);
        }
        throw new IllegalArgumentException(
            "extensionData values must be Map, List, String, Number, Boolean, or null; got: "
            + value.getClass().getName());
    }

    private static long estimateMapSize(Map<?, ?> map) {
        long size = 2;
        for (var entry : map.entrySet()) {
            size += ((String) entry.getKey()).length() + 6;
            size += estimateValueSize(entry.getValue());
        }
        return size;
    }

    private static long estimateListSize(List<?> list) {
        long size = 2;
        for (var item : list) {
            size += estimateValueSize(item) + 2;
        }
        return size;
    }

    private static long estimateValueSize(Object value) {
        if (value == null) return 4;
        if (value instanceof String s) return s.length();
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value).length();
        }
        if (value instanceof Map<?, ?> map) return estimateMapSize(map);
        if (value instanceof List<?> list) return estimateListSize(list);
        return 0;
    }
}
