package io.casehub.eidos.org.runtime.trust;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.casehub.eidos.api.OrgAwareDispatch;
import io.casehub.ledger.api.model.LedgerAttestation;
import io.casehub.ledger.api.model.LedgerEntry;
import io.casehub.ledger.api.spi.LedgerAttestationEnricher;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.LinkedHashMap;
import java.util.Map;

@ApplicationScoped
public class OrgDistanceAttestationEnricher implements LedgerAttestationEnricher {

    private static final Logger LOG = Logger.getLogger(OrgDistanceAttestationEnricher.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final OrgAwareDispatch orgAwareDispatch;

    @Inject
    public OrgDistanceAttestationEnricher(OrgAwareDispatch orgAwareDispatch) {
        this.orgAwareDispatch = orgAwareDispatch;
    }

    @Override
    public void enrich(LedgerAttestation attestation, LedgerEntry entry) {
        int distance = orgAwareDispatch.orgDistance(
                attestation.attestorId, entry.actorId, entry.tenancyId);

        if (distance < 0) {
            return;
        }

        try {
            Map<String, Object> snapshot = attestation.relevanceSnapshot != null
                    ? MAPPER.readValue(attestation.relevanceSnapshot, MAP_TYPE)
                    : new LinkedHashMap<>();
            snapshot.put(OrgDistanceUtil.SNAPSHOT_KEY, distance);
            attestation.relevanceSnapshot = MAPPER.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            LOG.warnf("Failed to write org-distance to relevance snapshot: %s", e.getMessage());
        }
    }

    @Override
    public String name() {
        return "org-distance";
    }
}
