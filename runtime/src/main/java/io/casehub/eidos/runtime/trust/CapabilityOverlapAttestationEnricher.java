package io.casehub.eidos.runtime.trust;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.casehub.eidos.api.AgentCapability;
import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentRegistry;
import io.casehub.ledger.api.model.LedgerAttestation;
import io.casehub.ledger.api.model.LedgerEntry;
import io.casehub.ledger.api.spi.LedgerAttestationEnricher;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class CapabilityOverlapAttestationEnricher implements LedgerAttestationEnricher {

    private static final Logger LOG = Logger.getLogger(CapabilityOverlapAttestationEnricher.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final AgentRegistry registry;

    @Inject
    public CapabilityOverlapAttestationEnricher(AgentRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void enrich(LedgerAttestation attestation, LedgerEntry entry) {
        var attestorOpt = registry.findById(attestation.attestorId, entry.tenancyId);
        var scoredOpt = registry.findById(entry.actorId, entry.tenancyId);
        if (attestorOpt.isEmpty() || scoredOpt.isEmpty()) return;

        Set<String> attestorCaps = capabilityNames(attestorOpt.get());
        Set<String> scoredCaps = capabilityNames(scoredOpt.get());
        if (attestorCaps.isEmpty() && scoredCaps.isEmpty()) return;

        double overlap = jaccard(attestorCaps, scoredCaps);

        try {
            Map<String, Object> snapshot = attestation.relevanceSnapshot != null
                    ? MAPPER.readValue(attestation.relevanceSnapshot, MAP_TYPE)
                    : new LinkedHashMap<>();
            snapshot.put(CapabilityOverlapRelevanceFactor.SNAPSHOT_KEY, overlap);
            attestation.relevanceSnapshot = MAPPER.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            LOG.warnf("Failed to write capability-overlap to relevance snapshot: %s", e.getMessage());
        }
    }

    @Override
    public String name() {
        return "capability-overlap";
    }

    private static Set<String> capabilityNames(AgentDescriptor descriptor) {
        return descriptor.capabilities().stream()
                .map(AgentCapability::name)
                .collect(Collectors.toSet());
    }

    static double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() && b.isEmpty()) return 0.0;
        long intersection = a.stream().filter(b::contains).count();
        long union = a.size() + b.size() - intersection;
        return union == 0 ? 0.0 : (double) intersection / union;
    }
}
