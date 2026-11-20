package io.casehub.eidos.org.runtime.trust;

import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentMatch;
import io.casehub.eidos.api.AgentQuery;
import io.casehub.eidos.api.AgentRegistry;
import io.casehub.eidos.api.CapabilityHealth;
import io.casehub.eidos.api.OrgAwareDispatch;
import io.casehub.ledger.api.spi.AttestationRelevanceFactor;
import io.casehub.ledger.api.spi.LedgerAttestationEnricher;
import io.quarkus.test.junit.QuarkusTest;
import io.casehub.ledger.api.spi.AttestationRelevanceContext;
import io.casehub.platform.api.identity.ActorType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class TrustBeansCdiTest {

    @Inject OrgProximityRelevanceFactor relevanceFactor;
    @Inject OrgDistanceAttestationEnricher attestationEnricher;
    @Inject OrgAwareDispatch orgAwareDispatch;

    @Test
    void relevance_factor_is_wired_as_attestation_relevance_factor() {
        assertThat(relevanceFactor)
                .isNotNull()
                .isInstanceOf(AttestationRelevanceFactor.class);
        assertThat(relevanceFactor.name()).isEqualTo("org-proximity");
    }

    @Test
    void relevance_factor_receives_default_config_weights() {
        var ctx = new AttestationRelevanceContext(
                UUID.randomUUID(), "att", ActorType.AGENT,
                "role", "scored", "tenant", "cap", null,
                Instant.now(), Map.of("org-distance", 1));
        var result = relevanceFactor.assess(ctx);
        assertThat(result.weight()).isEqualTo(1.0);
    }

    @Test
    void attestation_enricher_is_wired_as_ledger_attestation_enricher() {
        assertThat(attestationEnricher)
                .isNotNull()
                .isInstanceOf(LedgerAttestationEnricher.class);
        assertThat(attestationEnricher.name()).isEqualTo("org-distance");
    }

    @Test
    void org_aware_dispatch_resolves_to_default_implementation() {
        assertThat(orgAwareDispatch).isNotNull();
        assertThat(orgAwareDispatch.getClass().getSimpleName())
                .contains("DefaultOrgAwareDispatch");
    }

    @ApplicationScoped
    static class StubAgentRegistry implements AgentRegistry {
        @Override public void register(AgentDescriptor descriptor) {}
        @Override public Optional<AgentDescriptor> findById(String agentId, String tenancyId) {
            return Optional.empty();
        }
        @Override public List<AgentMatch> find(AgentQuery query) { return List.of(); }
    }

    @ApplicationScoped
    static class StubCapabilityHealth implements CapabilityHealth {
        @Override public CapabilityStatus probe(AgentDescriptor descriptor, String capabilityTag,
                                                ProbeContext context) {
            return new CapabilityStatus.Ready();
        }
    }
}
