package io.casehub.eidos.runtime.registrar;

import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.spi.AgentDescriptorRegistrar;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

final class DescriptorCollector {

    private DescriptorCollector() {}

    static List<AgentDescriptor> collectAndValidate(Iterable<AgentDescriptorRegistrar> registrars) {
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
        }
        return List.copyOf(all);
    }
}
