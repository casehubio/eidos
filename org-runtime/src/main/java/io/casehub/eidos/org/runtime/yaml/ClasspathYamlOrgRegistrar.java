package io.casehub.eidos.org.runtime.yaml;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.casehub.eidos.org.api.spi.OrgRegistrar;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Map;

@ApplicationScoped
public class ClasspathYamlOrgRegistrar implements OrgRegistrar {

    private static final String RESOURCE_PATH = "META-INF/eidos/organization.yaml";

    @Override
    public OrgDefinition organization() {
        final Enumeration<URL> urls;
        try {
            urls = Thread.currentThread().getContextClassLoader().getResources(RESOURCE_PATH);
        } catch (final IOException e) {
            throw new IllegalStateException("Failed to scan classpath for " + RESOURCE_PATH, e);
        }

        var allUnits = new ArrayList<io.casehub.eidos.org.api.OrganizationalUnit>();
        var allRelationships = new ArrayList<io.casehub.eidos.org.api.AgentRelationship>();

        while (urls.hasMoreElements()) {
            var url = urls.nextElement();
            try (var stream = url.openStream()) {
                var org = loadFrom(stream);
                allUnits.addAll(org.units());
                allRelationships.addAll(org.relationships());
            } catch (final Exception e) {
                throw new IllegalStateException(
                    "Failed to load organization from " + url + ": " + e.getMessage(), e);
            }
        }

        return new OrgDefinition(allUnits, allRelationships);
    }

    @SuppressWarnings("unchecked")
    OrgDefinition loadFrom(InputStream yaml) {
        if (yaml == null) return new OrgDefinition(null, null);
        try {
            var mapper = EidosOrgModule.createMapper();
            var rawMap = (Map<String, Object>) mapper.readValue(yaml, Map.class);
            if (rawMap == null) return new OrgDefinition(null, null);

            var orgMap = (Map<String, Object>) rawMap.get("organization");
            if (orgMap == null) return new OrgDefinition(null, null);

            var node = mapper.valueToTree(orgMap);
            return mapper.treeToValue(node, YamlOrganization.class)
                .toOrgDefinition();
        } catch (final IOException e) {
            throw new IllegalStateException("Failed to parse YAML: " + e.getMessage(), e);
        }
    }
}
