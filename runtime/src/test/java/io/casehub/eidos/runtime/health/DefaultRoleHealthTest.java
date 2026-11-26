package io.casehub.eidos.runtime.health;

import io.casehub.eidos.api.*;
import io.casehub.eidos.api.CapabilityHealth.ProbeContext;
import io.casehub.eidos.api.RoleHealth.RoleStatus;
import jakarta.enterprise.inject.Instance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultRoleHealthTest {

    private StubGraphQuery graphQuery;
    private DefaultRoleHealth roleHealth;

    @BeforeEach
    void setUp() {
        graphQuery = new StubGraphQuery();
        var source = new TaskOutcomeEvidenceSource(graphQuery, 90, 90);
        roleHealth = new DefaultRoleHealth(stubInstance(source), 90);
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

    @Test
    void undeclared_when_no_role_profile() {
        var descriptor = descriptorWithoutRoleProfile();
        var status = roleHealth.probe(descriptor, "coordinator", ProbeContext.of(null));
        assertThat(status).isInstanceOf(RoleStatus.Undeclared.class);
        assertThat(status.roleName()).isEqualTo("coordinator");
    }

    @Test
    void undeclared_when_role_not_in_profile() {
        var descriptor = descriptorWithRoles("analyst");
        var status = roleHealth.probe(descriptor, "coordinator", ProbeContext.of(null));
        assertThat(status).isInstanceOf(RoleStatus.Undeclared.class);
    }

    @Test
    void declared_fit_when_zero_outcomes() {
        var descriptor = descriptorWithRoles("coordinator");
        graphQuery.setHistory("coordinator", emptyHistory());
        var status = roleHealth.probe(descriptor, "coordinator", ProbeContext.of(null));
        assertThat(status).isInstanceOf(RoleStatus.DeclaredFit.class);
    }

    @Test
    void declared_fit_when_insufficient_outcomes() {
        var descriptor = descriptorWithRoles("coordinator");
        graphQuery.setHistory("coordinator", historyWithOutcomes(3, TaskResult.SUCCEEDED, 0.9));
        var status = roleHealth.probe(descriptor, "coordinator", ProbeContext.of(null));
        assertThat(status).isInstanceOf(RoleStatus.DeclaredFit.class);
    }

    @Test
    void fit_when_above_min_confidence() {
        var descriptor = descriptorWithRoles("coordinator",
                new RoleChangePolicy(RoleChangePolicy.Mode.AUTO_ABOVE_THRESHOLD,
                        0.5, 0.3, 2));
        graphQuery.setHistory("coordinator", historyWithOutcomes(20, TaskResult.SUCCEEDED, 0.9));
        var status = roleHealth.probe(descriptor, "coordinator", ProbeContext.of(null));
        assertThat(status).isInstanceOf(RoleStatus.Fit.class);
        assertThat(((RoleStatus.Fit) status).confidence()).isGreaterThan(0.5);
    }

    @Test
    void marginal_when_between_demotion_and_min_confidence() {
        var descriptor = descriptorWithRoles("coordinator",
                new RoleChangePolicy(RoleChangePolicy.Mode.AUTO_ABOVE_THRESHOLD,
                        0.8, 0.3, 2));
        graphQuery.setHistory("coordinator", historyWithOutcomes(20, TaskResult.SUCCEEDED, 0.7));
        var status = roleHealth.probe(descriptor, "coordinator", ProbeContext.of(null));
        assertThat(status).isInstanceOf(RoleStatus.Marginal.class);
    }

    @Test
    void unfit_when_below_demotion_confidence() {
        var descriptor = descriptorWithRoles("coordinator",
                new RoleChangePolicy(RoleChangePolicy.Mode.AUTO_ABOVE_THRESHOLD,
                        0.7, 0.4, 2));
        graphQuery.setHistory("coordinator", historyWithOutcomes(15, TaskResult.FAILED, 0.9));
        var status = roleHealth.probe(descriptor, "coordinator", ProbeContext.of(null));
        assertThat(status).isInstanceOf(RoleStatus.Unfit.class);
        var unfit = (RoleStatus.Unfit) status;
        assertThat(unfit.failureCount()).isEqualTo(15);
        assertThat(unfit.totalOutcomes()).isEqualTo(15);
    }

    @Test
    void decay_reduces_confidence_for_old_outcomes() {
        var descriptor = descriptorWithRoles("coordinator",
                                             new RoleChangePolicy(RoleChangePolicy.Mode.AUTO_ABOVE_THRESHOLD,
                                                                  0.5, 0.3, 2));
        var recentHistory = historyWithOutcomes(20, TaskResult.SUCCEEDED, 0.9);
        graphQuery.setHistory("coordinator", recentHistory);
        var recentStatus = roleHealth.probe(descriptor, "coordinator", ProbeContext.of(null));
        assertThat(recentStatus).isInstanceOf(RoleStatus.Fit.class);
        double recentConfidence = ((RoleStatus.Fit) recentStatus).confidence();

        var oldHistory = historyWithAgedOutcomes(20, TaskResult.SUCCEEDED, 0.9,
                                                 Instant.now().minus(80, java.time.temporal.ChronoUnit.DAYS));
        graphQuery.setHistory("coordinator", oldHistory);
        var oldStatus = roleHealth.probe(descriptor, "coordinator", ProbeContext.of(null));
        double oldConfidence = switch (oldStatus) {
            case RoleStatus.Fit f -> f.confidence();
            case RoleStatus.Marginal m -> m.confidence();
            case RoleStatus.Unfit u -> u.confidence();
            default -> -1;
        };
        assertThat(oldConfidence).isLessThan(recentConfidence);
    }

    @Test
    void zero_half_life_disables_decay() {
        var noDecaySource = new TaskOutcomeEvidenceSource(graphQuery, 90, 0);
        var noDecayHealth = new DefaultRoleHealth(stubInstance(noDecaySource), 90);
        var descriptor = descriptorWithRoles("coordinator",
                                             new RoleChangePolicy(RoleChangePolicy.Mode.AUTO_ABOVE_THRESHOLD,
                                                                  0.5, 0.3, 2));
        var oldHistory = historyWithAgedOutcomes(20, TaskResult.SUCCEEDED, 0.9,
                                                 Instant.now().minus(80, java.time.temporal.ChronoUnit.DAYS));
        graphQuery.setHistory("coordinator", oldHistory);

        var status = noDecayHealth.probe(descriptor, "coordinator", ProbeContext.of(null));
        assertThat(status).isInstanceOf(RoleStatus.Fit.class);
    }

    @Test
    void domain_scoped_probe_uses_domain_specific_history() {
        var descriptor = descriptorWithRoles("coordinator",
                                             new RoleChangePolicy(RoleChangePolicy.Mode.AUTO_ABOVE_THRESHOLD,
                                                                  0.5, 0.3, 2));
        graphQuery.setHistory("coordinator", "java", historyWithOutcomes(20, TaskResult.SUCCEEDED, 0.9));
        graphQuery.setHistory("coordinator", "python", historyWithOutcomes(15, TaskResult.FAILED, 0.9));

        var javaStatus = roleHealth.probe(descriptor, "coordinator", ProbeContext.of("java"));
        assertThat(javaStatus).isInstanceOf(RoleStatus.Fit.class);

        var pythonStatus = roleHealth.probe(descriptor, "coordinator", ProbeContext.of("python"));
        assertThat(pythonStatus).isInstanceOf(RoleStatus.Unfit.class);
    }

    @Test
    void null_domain_falls_back_to_aggregate_history() {
        var descriptor = descriptorWithRoles("coordinator",
                                             new RoleChangePolicy(RoleChangePolicy.Mode.AUTO_ABOVE_THRESHOLD,
                                                                  0.5, 0.3, 2));
        graphQuery.setHistory("coordinator", historyWithOutcomes(20, TaskResult.SUCCEEDED, 0.9));

        var status = roleHealth.probe(descriptor, "coordinator", ProbeContext.of(null));
        assertThat(status).isInstanceOf(RoleStatus.Fit.class);
    }

    @Test
    void domain_scoped_declared_fit_when_no_domain_history() {
        var descriptor = descriptorWithRoles("coordinator");
        graphQuery.setHistory("coordinator", "rust", emptyHistory());

        var status = roleHealth.probe(descriptor, "coordinator", ProbeContext.of("rust"));
        assertThat(status).isInstanceOf(RoleStatus.DeclaredFit.class);
    }

    @Test
    void zero_sources_returns_declared_fit() {
        var emptyHealth = new DefaultRoleHealth(stubInstance(), 90);
        var descriptor = descriptorWithRoles("coordinator",
                                             new RoleChangePolicy(RoleChangePolicy.Mode.AUTO_ABOVE_THRESHOLD,
                                                                  0.5, 0.3, 2));
        var status = emptyHealth.probe(descriptor, "coordinator", ProbeContext.of(null));
        assertThat(status).isInstanceOf(RoleStatus.DeclaredFit.class);
    }

    @Test
    void multi_source_aggregation_combines_weighted_evidence() {
        var source1     = new FixedEvidenceSource("s1", 9.0, 10.0, 1.0);
        var source2     = new FixedEvidenceSource("s2", 4.0, 10.0, 1.0);
        var multiHealth = new DefaultRoleHealth(stubInstance(source1, source2), 90);

        var descriptor = descriptorWithRoles("coordinator",
                                             new RoleChangePolicy(RoleChangePolicy.Mode.AUTO_ABOVE_THRESHOLD,
                                                                  0.5, 0.3, 2));
        var status = multiHealth.probe(descriptor, "coordinator", ProbeContext.of(null));
        assertThat(status).isNotInstanceOf(RoleStatus.DeclaredFit.class);
        assertThat(status).isNotInstanceOf(RoleStatus.Undeclared.class);

        var evidence = multiHealth.lastProbeEvidence();
        assertThat(evidence).hasSize(2);
        assertThat(evidence).extracting(RoleEvidence::sourceId).containsExactly("s1", "s2");
    }


    // ── Helpers ──

    private static AgentDescriptor descriptorWithoutRoleProfile() {
        return AgentDescriptor.builder()
                .agentId("a1").name("Agent").version("1.0")
                .provider("test").modelFamily("test").modelVersion("v1")
                .slot("worker")
                .disposition(AgentDisposition.builder()
                        .socialOrient("collaborative").ruleFollowing("principled")
                        .riskAppetite("measured").autonomy("semi-autonomous").build())
                .tenancyId("t1")
                .build();
    }

    private static AgentDescriptor descriptorWithRoles(String roleName) {
        return descriptorWithRoles(roleName, RoleChangePolicy.ADVISORY);
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

    private static AgentTaskHistory emptyHistory() {
        return new AgentTaskHistory("a1", "t1", List.of(), List.of(), List.of(), List.of(),
                GraphDataSufficiency.empty(List.of()));
    }

    private static AgentTaskHistory historyWithOutcomes(int count, TaskResult result,
                                                        double confidence) {
        var outcomes = new java.util.ArrayList<AgentOutcome>();
        for (int i = 0; i < count; i++) {
            outcomes.add(new AgentOutcome("t" + i, result, confidence, Instant.now(), null));
        }
        return new AgentTaskHistory("a1", "t1", List.of(), outcomes, List.of(), List.of(),
                GraphDataSufficiency.forCount(count, Instant.now(), Instant.now(), List.of()));
    }

    private static AgentTaskHistory historyWithAgedOutcomes(int count, TaskResult result,
                                                            double confidence, Instant observedAt) {
        var outcomes = new java.util.ArrayList<AgentOutcome>();
        for (int i = 0; i < count; i++) {
            outcomes.add(new AgentOutcome("t" + i, result, confidence, observedAt, null));
        }
        return new AgentTaskHistory("a1", "t1", List.of(), outcomes, List.of(), List.of(),
                                    GraphDataSufficiency.forCount(count, observedAt, observedAt, List.of()));
    }


    static class StubGraphQuery implements AgentGraphQuery {
        private final Map<String, AgentTaskHistory> historyByRole = new HashMap<>();

        void setHistory(String role, AgentTaskHistory history) {
            historyByRole.put(role, history);
        }

        void setHistory(String role, String taskDomain, AgentTaskHistory history) {
            historyByRole.put(role + "|" + taskDomain, history);
        }

        @Override
        public AgentTaskHistory agentHistory(String agentId, String tenancyId) {
            return emptyHistory();
        }

        @Override
        public AgentTaskHistory historyByCapability(String agentId, String capabilityTag,
                                                    String tenancyId) {
            return emptyHistory();
        }

        @Override
        public AgentTaskHistory historyByRole(String agentId, String roleName,
                                              String tenancyId, Instant since) {
            return historyByRole.getOrDefault(roleName, emptyHistory());
        }

        @Override
        public AgentTaskHistory historyByRole(String agentId, String roleName,
                                              String tenancyId, Instant since,
                                              String taskDomain) {
            if (taskDomain == null) {
                return historyByRole(agentId, roleName, tenancyId, since);
            }
            return historyByRole.getOrDefault(roleName + "|" + taskDomain, emptyHistory());
        }

        @Override
        public List<String> topAgentsByOutcome(String capabilityTag, String taskDomain,
                                               String tenancyId, int limit) {
            return List.of();
        }

        @Override
        public List<AttestationRef> attestationsFor(String agentId, String tenancyId) {
            return List.of();
        }

        @Override
        public List<String> coActiveAgents(String externalRef, String tenancyId) {
            return List.of();
        }
    }

    record FixedEvidenceSource(String id, double qualitySum, double effectiveCount,
                               double weight) implements

    RoleEvidenceSource {
        @Override
        public RoleEvidence evaluate (String agentId, String roleName,
                                                                     String tenancyId, ProbeContext context){
            return new RoleEvidence(id, qualitySum, effectiveCount, weight, Instant.now());
        }

        @Override
        public String sourceId () {return id;}
    }
}
