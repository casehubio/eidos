package io.casehub.eidos.runtime.registrar;

import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.TemplateRegistry;
import io.casehub.eidos.api.spi.AgentDescriptorRegistrar;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

final class DescriptorCollector {

    private DescriptorCollector() {}

    static List<AgentDescriptor> collectAndValidate(Iterable<AgentDescriptorRegistrar> registrars,
                                                    TemplateRegistry templateRegistry) {
        var all = new ArrayList<AgentDescriptor>();
        registrars.forEach(r -> all.addAll(r.descriptors()));

        var seen = new HashSet<String>();
        for (var d : all) {
            var key = d.agentId() + "\0" + d.tenancyId();
            if (!seen.add(key)) {
                throw new IllegalStateException(
                        "Duplicate descriptor: agentId=" + d.agentId()
                        + ", tenancyId=" + d.tenancyId());
            }

            if (d.templates() != null) {
                for (var ref : d.templates()) {
                    var template = templateRegistry.resolve(ref.templateId())
                                                   .orElseThrow(() -> new IllegalStateException(
                                                           "Descriptor '" + d.agentId() + "' references unknown template: " + ref.templateId()));
                    var declared = Set.copyOf(template.parameters());
                    var provided = ref.args().keySet();
                    var missing  = new TreeSet<>(declared);
                    missing.removeAll(provided);
                    if (!missing.isEmpty()) {
                        throw new IllegalStateException("Descriptor '" + d.agentId()
                                                        + "', template '" + ref.templateId() + "': missing args " + missing);
                    }
                    var extra = new TreeSet<>(provided);
                    extra.removeAll(declared);
                    if (!extra.isEmpty()) {
                        throw new IllegalStateException("Descriptor '" + d.agentId()
                                                        + "', template '" + ref.templateId() + "': unexpected args " + extra);
                    }
                }
            }
        }
        return List.copyOf(all);
    }
}
