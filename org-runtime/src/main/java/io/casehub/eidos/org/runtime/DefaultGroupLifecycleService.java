package io.casehub.eidos.org.runtime;

import io.casehub.eidos.org.api.*;
import io.casehub.eidos.org.api.spi.GroupInstanceStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class DefaultGroupLifecycleService implements GroupLifecycleService {

    private static final int MAX_CAS_RETRIES = 3;

    private final GroupInstanceStore store;
    private final OrgRegistry orgRegistry;
    private final List<GroupLifecycleListener> listeners;

    public DefaultGroupLifecycleService(GroupInstanceStore store,
                                        OrgRegistry orgRegistry,
                                        Instance<GroupLifecycleListener> listenerInstances) {
        this.store = store;
        this.orgRegistry = orgRegistry;
        this.listeners = listenerInstances.stream().toList();
    }

    public DefaultGroupLifecycleService(GroupInstanceStore store,
                                        OrgRegistry orgRegistry,
                                        List<GroupLifecycleListener> listeners) {
        this.store = store;
        this.orgRegistry = orgRegistry;
        this.listeners = listeners;
    }

    @Override
    public GroupInstance createGroup(String specId, String tenancyId, FormationOrigin origin) {
        var spec = resolveSpec(specId, tenancyId);
        validateFormationPath(spec, origin);

        var now = Instant.now();
        var gi = new GroupInstance(
            UUID.randomUUID().toString(), specId, tenancyId,
            GroupLifecycleState.FORMING, origin,
            spec.topology(), spec.topologyVocabulary(), spec.authorityModel(),
            List.of(), List.of(), List.of(),
            now, now, 0
        );
        store.save(gi);
        var saved = store.findById(gi.instanceId(), tenancyId).orElseThrow();
        fireStateTransition(saved.instanceId(), null, GroupLifecycleState.FORMING, tenancyId);
        return saved;
    }

    @Override
    public GroupInstance proposeGroup(String specId, String tenancyId, String proposerAgentId) {
        var spec = resolveSpec(specId, tenancyId);
        validateFormationPath(spec, FormationOrigin.PROPOSAL);

        var now = Instant.now();
        var gi = new GroupInstance(
            UUID.randomUUID().toString(), specId, tenancyId,
            GroupLifecycleState.PROPOSED, FormationOrigin.PROPOSAL,
            spec.topology(), spec.topologyVocabulary(), spec.authorityModel(),
            List.of(), List.of(), List.of(),
            now, now, 0
        );
        store.save(gi);
        var saved = store.findById(gi.instanceId(), tenancyId).orElseThrow();
        var proposal = new FormationProposal(specId, tenancyId, FormationOrigin.PROPOSAL,
            List.of(proposerAgentId), java.util.Map.of());
        for (var l : listeners) l.onFormationProposal(proposal);
        return saved;
    }

    @Override
    public void approveProposal(String instanceId, String tenancyId) {
        mutate(instanceId, tenancyId, gi -> {
            requireState(gi, GroupLifecycleState.PROPOSED);
            return withState(gi, GroupLifecycleState.FORMING);
        });
        fireStateTransition(instanceId, GroupLifecycleState.PROPOSED, GroupLifecycleState.FORMING, tenancyId);
    }

    @Override
    public void rejectProposal(String instanceId, String tenancyId, String reason) {
        mutate(instanceId, tenancyId, gi -> {
            requireState(gi, GroupLifecycleState.PROPOSED);
            return withState(gi, GroupLifecycleState.REJECTED);
        });
        fireStateTransition(instanceId, GroupLifecycleState.PROPOSED, GroupLifecycleState.REJECTED, tenancyId);
    }

    @Override
    public void dissolveGroup(String instanceId, String tenancyId, String reason) {
        var gi = mutate(instanceId, tenancyId, current -> {
            requireNonTerminal(current);
            if (current.state() == GroupLifecycleState.DISSOLVING || current.state() == GroupLifecycleState.PROPOSED)
                throw new OrgValidationException("state", "cannot dissolve from " + current.state());
            return withState(current, GroupLifecycleState.DISSOLVING);
        });
        for (var l : listeners) l.onDissolution(instanceId, tenancyId, reason);
        fireStateTransition(instanceId, gi.state(), GroupLifecycleState.DISSOLVING, tenancyId);

        var updated = store.findById(instanceId, tenancyId).orElseThrow();
        if (updated.roleAdoptions().isEmpty() && updated.members().isEmpty()) {
            mutate(instanceId, tenancyId, current -> {
                if (current.state() != GroupLifecycleState.DISSOLVING) return current;
                return withState(current, GroupLifecycleState.DISSOLVED);
            });
            fireStateTransition(instanceId, GroupLifecycleState.DISSOLVING, GroupLifecycleState.DISSOLVED, tenancyId);
        }
    }

    @Override
    public void changeTopology(String instanceId, String newTopology, String newTopologyVocabulary, String tenancyId) {
        var before = store.findById(instanceId, tenancyId)
            .orElseThrow(() -> new OrgValidationException("instanceId", "not found: " + instanceId));
        requireStateOneOf(before, GroupLifecycleState.ACTIVE, GroupLifecycleState.FORMING);
        String oldTopology = before.topology();

        mutate(instanceId, tenancyId, gi -> {
            requireStateOneOf(gi, GroupLifecycleState.ACTIVE, GroupLifecycleState.FORMING);
            return new GroupInstance(
                gi.instanceId(), gi.specId(), gi.tenancyId(),
                gi.state(), gi.formationOrigin(),
                newTopology, newTopologyVocabulary, gi.effectiveAuthority(),
                gi.members(), gi.roleAdoptions(), gi.missionCommitments(),
                gi.createdAt(), Instant.now(), gi.version()
            );
        });
        for (var l : listeners) l.onTopologyChange(instanceId, oldTopology, newTopology, tenancyId);
    }

    @Override
    public void joinGroup(String instanceId, String agentId, String tenancyId) {
        mutate(instanceId, tenancyId, gi -> {
            requireNonTerminal(gi);
            if (gi.state() == GroupLifecycleState.DISSOLVING)
                throw new OrgValidationException("state", "cannot join a dissolving group");
            if (gi.members().stream().anyMatch(m -> m.agentId().equals(agentId)))
                throw new OrgValidationException("agentId", "agent " + agentId + " is already a member");
            var newMembers = new ArrayList<>(gi.members());
            newMembers.add(new ActualMembership(agentId, Instant.now()));
            return new GroupInstance(
                gi.instanceId(), gi.specId(), gi.tenancyId(),
                gi.state(), gi.formationOrigin(),
                gi.topology(), gi.topologyVocabulary(), gi.effectiveAuthority(),
                newMembers, gi.roleAdoptions(), gi.missionCommitments(),
                gi.createdAt(), Instant.now(), gi.version()
            );
        });
        for (var l : listeners) l.onMemberJoined(instanceId, agentId, tenancyId);
    }

    @Override
    public void leaveGroup(String instanceId, String agentId, String tenancyId) {
        mutate(instanceId, tenancyId, gi -> {
            requireNonTerminal(gi);
            var newAdoptions = gi.roleAdoptions().stream()
                .filter(ra -> !ra.agentId().equals(agentId))
                .toList();
            var newMembers = gi.members().stream()
                .filter(m -> !m.agentId().equals(agentId))
                .toList();
            return new GroupInstance(
                gi.instanceId(), gi.specId(), gi.tenancyId(),
                gi.state(), gi.formationOrigin(),
                gi.topology(), gi.topologyVocabulary(), gi.effectiveAuthority(),
                newMembers, newAdoptions, gi.missionCommitments(),
                gi.createdAt(), Instant.now(), gi.version()
            );
        });
        for (var l : listeners) l.onMemberLeft(instanceId, agentId, tenancyId);

        if (shouldAutoComplete(instanceId, tenancyId)) {
            mutate(instanceId, tenancyId, current -> withState(current, GroupLifecycleState.DISSOLVED));
            fireStateTransition(instanceId, GroupLifecycleState.DISSOLVING, GroupLifecycleState.DISSOLVED, tenancyId);
        }
    }

    @Override
    public void adoptRole(String instanceId, String agentId, String roleName, String tenancyId) {
        mutate(instanceId, tenancyId, gi -> {
            requireNonTerminal(gi);
            if (gi.state() == GroupLifecycleState.REORGANISING)
                throw new OrgValidationException("state", "membership frozen during REORGANISING");
            if (gi.state() == GroupLifecycleState.DISSOLVING)
                throw new OrgValidationException("state", "cannot adopt role on dissolving group");

            if (gi.members().stream().noneMatch(m -> m.agentId().equals(agentId)))
                throw new OrgValidationException("agentId", "agent " + agentId + " is not a member (must join first)");

            var spec = resolveSpec(gi.specId(), tenancyId);
            var roleSpec = spec.roleSpecs().stream()
                .filter(rs -> rs.roleName().equals(roleName))
                .findFirst()
                .orElseThrow(() -> new OrgValidationException("roleName", "role '" + roleName + "' not defined in spec"));

            long currentCount = gi.roleAdoptions().stream()
                .filter(ra -> ra.roleName().equals(roleName))
                .count();
            if (roleSpec.maxCardinality() != -1 && currentCount >= roleSpec.maxCardinality())
                throw new OrgValidationException("cardinality", "role '" + roleName + "' at max cardinality (" + roleSpec.maxCardinality() + ")");

            var heldRoles = gi.roleAdoptions().stream()
                .filter(ra -> ra.agentId().equals(agentId))
                .map(RoleAdoption::roleName)
                .toList();
            for (var held : heldRoles) {
                if (roleSpec.incompatibleWith().contains(held))
                    throw new OrgValidationException("incompatible", "role '" + roleName + "' is incompatible with held role '" + held + "'");
                spec.roleSpecs().stream()
                    .filter(rs -> rs.roleName().equals(held))
                    .findFirst()
                    .ifPresent(heldSpec -> {
                        if (heldSpec.incompatibleWith().contains(roleName))
                            throw new OrgValidationException("incompatible", "held role '" + held + "' is incompatible with '" + roleName + "'");
                    });
            }

            var newAdoptions = new ArrayList<>(gi.roleAdoptions());
            newAdoptions.add(new RoleAdoption(agentId, roleName, roleSpec.roleVocabulary(), Instant.now()));
            return new GroupInstance(
                gi.instanceId(), gi.specId(), gi.tenancyId(),
                gi.state(), gi.formationOrigin(),
                gi.topology(), gi.topologyVocabulary(), gi.effectiveAuthority(),
                gi.members(), newAdoptions, gi.missionCommitments(),
                gi.createdAt(), Instant.now(), gi.version()
            );
        });
        for (var l : listeners) l.onRoleAdoption(instanceId, agentId, roleName, tenancyId);
    }

    @Override
    public void releaseRole(String instanceId, String agentId, String roleName, String tenancyId) {
        mutate(instanceId, tenancyId, gi -> {
            requireNonTerminal(gi);
            var newAdoptions = gi.roleAdoptions().stream()
                .filter(ra -> !(ra.agentId().equals(agentId) && ra.roleName().equals(roleName)))
                .toList();
            if (newAdoptions.size() == gi.roleAdoptions().size())
                throw new OrgValidationException("roleName", "agent " + agentId + " does not hold role '" + roleName + "'");
            return new GroupInstance(
                gi.instanceId(), gi.specId(), gi.tenancyId(),
                gi.state(), gi.formationOrigin(),
                gi.topology(), gi.topologyVocabulary(), gi.effectiveAuthority(),
                gi.members(), newAdoptions, gi.missionCommitments(),
                gi.createdAt(), Instant.now(), gi.version()
            );
        });
        for (var l : listeners) l.onRoleRelease(instanceId, agentId, roleName, tenancyId);

        if (shouldAutoComplete(instanceId, tenancyId)) {
            mutate(instanceId, tenancyId, current -> withState(current, GroupLifecycleState.DISSOLVED));
            fireStateTransition(instanceId, GroupLifecycleState.DISSOLVING, GroupLifecycleState.DISSOLVED, tenancyId);
        }
    }

    @Override
    public void commitToMission(String instanceId, String agentId, String missionName, String tenancyId) {
        mutate(instanceId, tenancyId, gi -> {
            requireNonTerminal(gi);
            var newCommitments = new ArrayList<>(gi.missionCommitments());
            newCommitments.add(new MissionCommitment(agentId, missionName, Instant.now()));
            return new GroupInstance(
                gi.instanceId(), gi.specId(), gi.tenancyId(),
                gi.state(), gi.formationOrigin(),
                gi.topology(), gi.topologyVocabulary(), gi.effectiveAuthority(),
                gi.members(), gi.roleAdoptions(), newCommitments,
                gi.createdAt(), Instant.now(), gi.version()
            );
        });
    }

    @Override
    public void withdrawFromMission(String instanceId, String agentId, String missionName, String tenancyId) {
        mutate(instanceId, tenancyId, gi -> {
            var newCommitments = gi.missionCommitments().stream()
                .filter(mc -> !(mc.agentId().equals(agentId) && mc.missionName().equals(missionName)))
                .toList();
            return new GroupInstance(
                gi.instanceId(), gi.specId(), gi.tenancyId(),
                gi.state(), gi.formationOrigin(),
                gi.topology(), gi.topologyVocabulary(), gi.effectiveAuthority(),
                gi.members(), gi.roleAdoptions(), newCommitments,
                gi.createdAt(), Instant.now(), gi.version()
            );
        });
    }

    @Override
    public void fulfillMission(String instanceId, String missionName, String tenancyId) {
        mutate(instanceId, tenancyId, gi -> {
            var newCommitments = gi.missionCommitments().stream()
                .filter(mc -> !mc.missionName().equals(missionName))
                .toList();
            return new GroupInstance(
                gi.instanceId(), gi.specId(), gi.tenancyId(),
                gi.state(), gi.formationOrigin(),
                gi.topology(), gi.topologyVocabulary(), gi.effectiveAuthority(),
                gi.members(), gi.roleAdoptions(), newCommitments,
                gi.createdAt(), Instant.now(), gi.version()
            );
        });
    }

    @Override
    public Optional<GroupInstance> findInstance(String instanceId, String tenancyId) {
        return store.findById(instanceId, tenancyId);
    }

    @Override
    public List<GroupInstance> findInstances(GroupInstanceQuery query) {
        return store.find(query);
    }

    @Override
    public List<GroupInstance> instancesForAgent(String agentId, String tenancyId) {
        return store.find(new GroupInstanceQuery(tenancyId, null, null, agentId));
    }

    private OrganizationalUnit resolveSpec(String specId, String tenancyId) {
        return orgRegistry.findUnit(specId, tenancyId)
            .orElseThrow(() -> new OrgValidationException("specId", "spec not found: " + specId));
    }

    private void validateFormationPath(OrganizationalUnit spec, FormationOrigin origin) {
        if (spec.policy() != null && spec.policy().formationRules() != null) {
            var allowed = spec.policy().formationRules().allowedPaths();
            var path = switch (origin) {
                case TOP_DOWN -> FormationPath.TOP_DOWN;
                case PROPOSAL, EMERGENCE -> origin == FormationOrigin.PROPOSAL
                    ? FormationPath.BOTTOM_UP : FormationPath.EMERGENCE;
            };
            if (!allowed.contains(path))
                throw new OrgValidationException("formation", "formation path " + path + " not allowed for spec " + spec.unitId());
        }
    }

    private GroupInstance mutate(String instanceId, String tenancyId,
                                java.util.function.UnaryOperator<GroupInstance> mutation) {
        for (int attempt = 0; attempt < MAX_CAS_RETRIES; attempt++) {
            var gi = store.findById(instanceId, tenancyId)
                .orElseThrow(() -> new OrgValidationException("instanceId", "not found: " + instanceId));
            var updated = mutation.apply(gi);
            if (updated == gi) return gi;
            try {
                store.save(updated);
                return store.findById(instanceId, tenancyId).orElseThrow();
            } catch (StaleGroupInstanceException e) {
                if (attempt == MAX_CAS_RETRIES - 1) throw e;
            }
        }
        throw new StaleGroupInstanceException(instanceId, -1);
    }

    private GroupInstance withState(GroupInstance gi, GroupLifecycleState newState) {
        return new GroupInstance(
            gi.instanceId(), gi.specId(), gi.tenancyId(),
            newState, gi.formationOrigin(),
            gi.topology(), gi.topologyVocabulary(), gi.effectiveAuthority(),
            gi.members(), gi.roleAdoptions(), gi.missionCommitments(),
            gi.createdAt(), Instant.now(), gi.version()
        );
    }

    private void requireState(GroupInstance gi, GroupLifecycleState expected) {
        if (gi.state() != expected)
            throw new OrgValidationException("state", "expected " + expected + " but was " + gi.state());
    }

    private void requireStateOneOf(GroupInstance gi, GroupLifecycleState... allowed) {
        for (var s : allowed) if (gi.state() == s) return;
        throw new OrgValidationException("state", "invalid state: " + gi.state());
    }

    private void requireNonTerminal(GroupInstance gi) {
        if (gi.state().isTerminal())
            throw new OrgValidationException("state", "group is in terminal state: " + gi.state());
    }

    private boolean shouldAutoComplete(String instanceId, String tenancyId) {
        var current = store.findById(instanceId, tenancyId).orElse(null);
        return current != null
            && current.state() == GroupLifecycleState.DISSOLVING
            && current.members().isEmpty();
    }

    private void fireStateTransition(String instanceId, GroupLifecycleState from,
                                     GroupLifecycleState to, String tenancyId) {
        for (var l : listeners) l.onStateTransition(instanceId, from, to, tenancyId);
    }
}
