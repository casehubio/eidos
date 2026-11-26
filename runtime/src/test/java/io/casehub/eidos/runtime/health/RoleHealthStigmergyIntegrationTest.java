package io.casehub.eidos.runtime.health;

import io.casehub.eidos.api.*;
import io.casehub.eidos.api.CapabilityHealth.ProbeContext;
import io.casehub.eidos.api.RoleHealth.RoleStatus;
import jakarta.enterprise.inject.Instance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RoleHealthStigmergyIntegrationTest {

    private StubGraphQuery graphQuery;
    private StubSignalStore signalStore;
    private DefaultRoleHealth roleHealth;

    private static final Instant NOW = Instant.now();

    @BeforeEach
    void setUp() {
        graphQuery = new StubGraphQuery();
        signalStore = new StubSignalStore();

        var taskSource = new TaskOutcomeEvidenceSource(graphQuery, 90, 90);
        var stigmergySource = new StigmergyEvidenceSource(signalStore, 90, 90.0, 0.5);
        roleHealth = new DefaultRoleHealth(stubInstance(taskSource, stigmergySource), 90);
    }

    @Test
    void both_sources_contribute_evidence() {
        var descriptor = descriptorWithRoles("coordinator",
            new RoleChangePolicy(RoleChangePolicy.Mode.AUTO_ABOVE_THRESHOLD, 0.5, 0.3, 2));

        graphQuery.setHistory("coordinator", historyWithOutcomes(10, TaskResult.SUCCEEDED, 0.9));

        for (int i = 0; i < 8; i++) {
            signalStore.record(new DetectedRoleSignal("a1", "t1", "coordinator",
                "delegation", null, 0.85, NOW.minus(i, ChronoUnit.DAYS)));
        }

        var status = roleHealth.probe(descriptor, "coordinator", ProbeContext.of(null));
        assertThat(status).isInstanceOf(RoleStatus.Fit.class);

        var evidence = roleHealth.lastProbeEvidence();
        assertThat(evidence).hasSize(2);
        assertThat(evidence.stream().map(RoleEvidence::sourceId).toList())
            .containsExactly("task-outcome", "stigmergy");
        assertThat(evidence.stream().filter(e -> e.sourceId().equals("stigmergy"))
            .findFirst().orElseThrow().weight()).isEqualTo(0.5);
    }

    @Test
    void stigmergy_supplements_marginal_task_outcomes() {
        var descriptor = descriptorWithRoles("coordinator",
            new RoleChangePolicy(RoleChangePolicy.Mode.AUTO_ABOVE_THRESHOLD, 0.7, 0.3, 2));

        graphQuery.setHistory("coordinator", historyWithOutcomes(10, TaskResult.PARTIALLY, 0.5));

        for (int i = 0; i < 10; i++) {
            signalStore.record(new DetectedRoleSignal("a1", "t1", "coordinator",
                "delegation", null, 0.95, NOW.minus(i, ChronoUnit.DAYS)));
        }

        var withStigmergy = roleHealth.probe(descriptor, "coordinator", ProbeContext.of(null));

        var taskOnlyHealth = new DefaultRoleHealth(
            stubInstance(new TaskOutcomeEvidenceSource(graphQuery, 90, 90)), 90);
        var withoutStigmergy = taskOnlyHealth.probe(descriptor, "coordinator", ProbeContext.of(null));

        assertThat(confidence(withStigmergy)).isGreaterThan(confidence(withoutStigmergy));
    }

    @Test
    void empty_stigmergy_store_does_not_affect_probe() {
        var descriptor = descriptorWithRoles("coordinator",
            new RoleChangePolicy(RoleChangePolicy.Mode.AUTO_ABOVE_THRESHOLD, 0.5, 0.3, 2));

        graphQuery.setHistory("coordinator", historyWithOutcomes(15, TaskResult.SUCCEEDED, 0.9));

        var status = roleHealth.probe(descriptor, "coordinator", ProbeContext.of(null));
        assertThat(status).isInstanceOf(RoleStatus.Fit.class);

        var stigmergyEvidence = roleHealth.lastProbeEvidence().stream()
            .filter(e -> e.sourceId().equals("stigmergy"))
            .findFirst().orElseThrow();
        assertThat(stigmergyEvidence.effectiveCount()).isZero();
    }

    private static double confidence(RoleStatus status) {
        return switch (status) {
            case RoleStatus.Fit f -> f.confidence();
            case RoleStatus.Marginal m -> m.confidence();
            case RoleStatus.Unfit u -> u.confidence();
            default -> -1.0;
        };
    }

    private static AgentDescriptor descriptorWithRoles(String roleName, RoleChangePolicy policy) {
        return AgentDescriptor.builder()
            .agentId("a1").name("Agent").version("1.0")
            .provider("test").modelFamily("test").modelVersion("v1")
            .slot("worker")
            .disposition(AgentDisposition.builder()
                .socialOrient("collaborative").ruleFollowing("principled")
                .riskAppetite("measured").autonomy("semi-autonomous").build())
            .tenancyId("t1")
            .roleProfile(new RoleProfile(
                List.of(new RoleDeclaration(roleName, null)), policy))
            .build();
    }

    private static AgentTaskHistory historyWithOutcomes(int count, TaskResult result,
                                                        double confidence) {
        var outcomes = new ArrayList<AgentOutcome>();
        for (int i = 0; i < count; i++) {
            outcomes.add(new AgentOutcome("t" + i, result, confidence, Instant.now(), null));
        }
        return new AgentTaskHistory("a1", "t1", List.of(), outcomes, List.of(), List.of(),
            GraphDataSufficiency.forCount(count, Instant.now(), Instant.now(), List.of()));
    }

    @SuppressWarnings("unchecked")
    private static Instance<RoleEvidenceSource> stubInstance(RoleEvidenceSource... sources) {
        var list = List.of(sources);
        return new Instance<>() {
            @Override public Instance<RoleEvidenceSource> select(Annotation... qualifiers) { return this; }
            @Override public <U extends RoleEvidenceSource> Instance<U> select(Class<U> subtype, Annotation... qualifiers) { return (Instance<U>) this; }
            @Override public <U extends RoleEvidenceSource> Instance<U> select(jakarta.enterprise.util.TypeLiteral<U> subtype, Annotation... qualifiers) { return (Instance<U>) this; }
            @Override public boolean isUnsatisfied() { return list.isEmpty(); }
            @Override public boolean isAmbiguous() { return list.size() > 1; }
            @Override public boolean isResolvable() { return list.size() == 1; }
            @Override public RoleEvidenceSource get() { return list.get(0); }
            @Override public Iterator<RoleEvidenceSource> iterator() { return list.iterator(); }
            @Override public void destroy(RoleEvidenceSource instance) {}
            @Override public Handle<RoleEvidenceSource> getHandle() { return null; }
            @Override public Iterable<? extends Handle<RoleEvidenceSource>> handles() { return List.of(); }
            @Override public void forEach(java.util.function.Consumer<? super RoleEvidenceSource> action) { list.forEach(action); }
        };
    }

    static class StubSignalStore implements DetectedRoleSignalStore {
        private final List<DetectedRoleSignal> signals = new ArrayList<>();

        @Override public void record(DetectedRoleSignal signal) { signals.add(signal); }

        @Override
        public List<DetectedRoleSignal> signals(String agentId, String roleName,
                                                 String tenancyId, Instant since) {
            return signals.stream()
                .filter(s -> s.agentId().equals(agentId))
                .filter(s -> s.roleName().equals(roleName))
                .filter(s -> s.tenancyId().equals(tenancyId))
                .filter(s -> !s.observedAt().isBefore(since))
                .toList();
        }

        @Override
        public List<DetectedRoleSignal> signalsByAgent(String agentId,
                                                        String tenancyId, Instant since) {
            return signals.stream()
                .filter(s -> s.agentId().equals(agentId))
                .filter(s -> s.tenancyId().equals(tenancyId))
                .filter(s -> !s.observedAt().isBefore(since))
                .toList();
        }

        @Override public int purge(String agentId, String tenancyId, Instant olderThan) { return 0; }
        @Override public void clear(String agentId, String tenancyId) { signals.clear(); }
    }

    static class StubGraphQuery implements AgentGraphQuery {
        private final Map<String, AgentTaskHistory> historyByRole = new HashMap<>();

        void setHistory(String role, AgentTaskHistory history) {
            historyByRole.put(role, history);
        }

        @Override public AgentTaskHistory agentHistory(String agentId, String tenancyId) {
            return emptyHistory();
        }
        @Override public AgentTaskHistory historyByCapability(String agentId, String capabilityTag, String tenancyId) {
            return emptyHistory();
        }
        @Override public AgentTaskHistory historyByRole(String agentId, String roleName, String tenancyId, Instant since) {
            return historyByRole.getOrDefault(roleName, emptyHistory());
        }
        @Override public AgentTaskHistory historyByRole(String agentId, String roleName, String tenancyId, Instant since, String taskDomain) {
            return historyByRole(agentId, roleName, tenancyId, since);
        }
        @Override public List<String> topAgentsByOutcome(String capabilityTag, String taskDomain, String tenancyId, int limit) {
            return List.of();
        }
        @Override public List<AttestationRef> attestationsFor(String agentId, String tenancyId) {
            return List.of();
        }
        @Override public List<String> coActiveAgents(String externalRef, String tenancyId) {
            return List.of();
        }

        private static AgentTaskHistory emptyHistory() {
            return new AgentTaskHistory("a1", "t1", List.of(), List.of(), List.of(), List.of(),
                GraphDataSufficiency.empty(List.of()));
        }
    }
}
