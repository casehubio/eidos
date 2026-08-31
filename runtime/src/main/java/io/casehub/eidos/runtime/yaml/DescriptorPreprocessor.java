package io.casehub.eidos.runtime.yaml;

import io.casehub.yaml.core.condition.Truthiness;
import io.casehub.yaml.core.data.CsvDataSource;
import io.casehub.yaml.core.data.CsvParser;
import io.casehub.yaml.core.foreach.ForEachExpander;
import io.casehub.yaml.core.foreach.IterationGroup;
import io.casehub.yaml.core.resolver.VariableResolver;
import io.casehub.yaml.core.resolver.VariableSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DescriptorPreprocessor {

    static final int MAX_EXPANSION = 100;

    private DescriptorPreprocessor() {}

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> preprocess(
            Map<String, Object> rawYaml,
            Map<String, VariableSource> externalSources,
            ClassLoader classLoader) {

        var descriptorsList = (List<Map<String, Object>>) rawYaml.get("descriptors");
        if (descriptorsList == null || descriptorsList.isEmpty()) {
            return List.of();
        }

        var prefixSources = new LinkedHashMap<String, VariableSource>();

        var variables = (Map<String, Object>) rawYaml.get("variables");
        if (variables != null && !variables.isEmpty()) {
            Map<String, String> vars = new LinkedHashMap<>();
            variables.forEach((k, v) -> vars.put(k, v.toString()));
            prefixSources.put("var", vars::get);
        }

        prefixSources.putAll(externalSources);

        var resolver = new VariableResolver(prefixSources, Set.of());

        var iterationGroups = new LinkedHashMap<String, IterationGroup>();
        var iterations = (Map<String, Object>) rawYaml.get("iterations");
        if (iterations != null) {
            for (var entry : iterations.entrySet()) {
                var groupMap = (Map<String, Object>) entry.getValue();
                String as = (String) groupMap.get("as");
                Object in = groupMap.get("in");
                iterationGroups.put(entry.getKey(), new IterationGroup(as, in));
            }
        }

        var csvDataSources = new LinkedHashMap<String, CsvDataSource>();
        var dataSources = (Map<String, Object>) rawYaml.get("dataSources");
        if (dataSources != null) {
            for (var entry : dataSources.entrySet()) {
                String dsName = entry.getKey();
                if (iterationGroups.containsKey(dsName)) {
                    throw new IllegalArgumentException(
                            "Data source '" + dsName
                            + "' collides with iteration group of the same name.");
                }
                var dsMap = (Map<String, Object>) entry.getValue();
                String csvContent = loadCsvContent(dsMap, dsName, classLoader);
                csvDataSources.put(dsName, CsvParser.parse(dsName, csvContent));
            }
        }

        var adapter = new DescriptorForEachAdapter();
        var result = new ArrayList<Map<String, Object>>();
        int syntheticIndex = 0;

        for (var desc : descriptorsList) {
            String dsRef = csvDataSourceRef(desc, csvDataSources);
            if (dsRef != null) {
                result.addAll(expandCsvDescriptor(desc, csvDataSources.get(dsRef), resolver));
            } else {
                Object agentIdObj = desc.get("agentId");
                String key = agentIdObj != null ? agentIdObj.toString()
                        : "__unnamed__" + syntheticIndex++;
                var singleMap = new LinkedHashMap<String, Map<String, Object>>();
                singleMap.put(key, desc);
                var expanded = ForEachExpander.expand(
                        singleMap, iterationGroups, resolver, adapter, MAX_EXPANSION);
                result.addAll(expanded.elements());
            }
        }

        return List.copyOf(result);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> expandCsvDescriptor(
            Map<String, Object> descriptor,
            CsvDataSource dataSource,
            VariableResolver resolver) {

        var forEach = (Map<String, Object>) descriptor.get("forEach");
        String as = (String) forEach.get("as");
        var rows = dataSource.rows();

        if (rows.size() > MAX_EXPANSION) {
            throw new IllegalStateException(
                    "forEach CSV template '" + descriptor.get("agentId")
                    + "' would expand to " + rows.size()
                    + " elements (limit: " + MAX_EXPANSION + ").");
        }

        var results = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < rows.size(); i++) {
            var row = rows.get(i);
            String rowKey = String.valueOf(i);
            var rowResolver = resolver
                    .withEachContext(Map.of(as, rowKey))
                    .withEachRowContext(Map.of(as, row));

            String when = descriptor.get("when") != null
                    ? descriptor.get("when").toString() : null;
            if (when != null) {
                String resolved = rowResolver.resolveString(when,
                        descriptor.get("agentId") + "." + rowKey);
                if (!Truthiness.isTruthy(resolved)) continue;
            }

            var stripped = new LinkedHashMap<>(descriptor);
            stripped.remove("forEach");
            stripped.remove("when");
            var resolved = new LinkedHashMap<>(
                    rowResolver.resolveMap(stripped, descriptor.get("agentId") + "." + rowKey));
            results.add(resolved);
        }
        return results;
    }

    @SuppressWarnings("unchecked")
    private static String csvDataSourceRef(Map<String, Object> descriptor,
                                            Map<String, CsvDataSource> csvDataSources) {
        Object forEach = descriptor.get("forEach");
        if (forEach instanceof Map<?, ?> m) {
            Object in = m.get("in");
            if (in instanceof String ref && csvDataSources.containsKey(ref)) {
                return ref;
            }
        }
        return null;
    }

    private static String loadCsvContent(Map<String, Object> dsMap, String dsName,
                                          ClassLoader classLoader) {
        if (dsMap.containsKey("csv")) {
            return dsMap.get("csv").toString();
        }
        if (dsMap.containsKey("file")) {
            String path = dsMap.get("file").toString();
            ClassLoader cl = classLoader != null ? classLoader
                    : Thread.currentThread().getContextClassLoader();
            try (InputStream is = cl.getResourceAsStream(path)) {
                if (is == null) {
                    throw new IllegalArgumentException(
                            "CSV classpath resource not found for data source '"
                            + dsName + "': " + path);
                }
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new IllegalStateException(
                        "Failed to read CSV for data source '" + dsName + "': " + e.getMessage(), e);
            }
        }
        throw new IllegalArgumentException(
                "Data source '" + dsName + "' must declare either 'csv' (inline) or 'file' (classpath).");
    }

}
