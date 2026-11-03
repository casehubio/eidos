package io.casehub.eidos.org.runtime.yaml;

import io.casehub.eidos.org.api.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SocietyYamlDeserializerTest {

    private final ClasspathYamlOrgRegistrar registrar = new ClasspathYamlOrgRegistrar();
    private io.casehub.eidos.org.api.spi.OrgRegistrar.OrgDefinition org;

    @BeforeEach void loadSocietyYaml() {
        var yaml = getClass().getResourceAsStream("/society-org.yaml");
        org = registrar.loadFrom(yaml);
    }

    @Test void loadsTwoUnitsAndOneRelationship() {
        assertThat(org.units()).hasSize(2);
        assertThat(org.relationships()).hasSize(1);
    }

    @Test void topologyFieldsDeserialized() {
        var guild = unitById("dev-guild");
        assertThat(guild.topology()).isEqualTo("mesh");
        assertThat(guild.topologyVocabulary()).isEqualTo("urn:casehub:vocab:topology");
    }

    @Test void authorityModelDeserialized() {
        var guild = unitById("dev-guild");
        assertThat(guild.authorityModel()).isEqualTo(AuthorityModel.DISTRIBUTED);

        var squad = unitById("ops-squad");
        assertThat(squad.authorityModel()).isEqualTo(AuthorityModel.CENTRALISED);
    }

    @Test void selfDeterminationDeserialized() {
        var guild = unitById("dev-guild");
        assertThat(guild.selfDetermination()).isEqualTo(0.8);

        var squad = unitById("ops-squad");
        assertThat(squad.selfDetermination()).isEqualTo(0.3);
    }

    @Test void groupPolicyDeserialized() {
        var guild = unitById("dev-guild");
        assertThat(guild.policy()).isNotNull();
        assertThat(guild.policy().normativePriority()).isEqualTo(2);
        assertThat(guild.policy().normSpecificity()).isEqualTo(NormSpecificity.DOMAIN);
    }

    @Test void formationRulesDeserialized() {
        var rules = unitById("dev-guild").policy().formationRules();
        assertThat(rules).isNotNull();
        assertThat(rules.allowedPaths()).containsExactlyInAnyOrder(FormationPath.BOTTOM_UP, FormationPath.EMERGENCE);
        assertThat(rules.minGroupSize()).isEqualTo(3);
        assertThat(rules.maxGroupSize()).isEqualTo(10);
        assertThat(rules.allowSubGroups()).isTrue();
        assertThat(rules.proposalQuorum()).isEqualTo(0.6);
    }

    @Test void roleSpecsDeserialized() {
        var guild = unitById("dev-guild");
        assertThat(guild.roleSpecs()).hasSize(2);

        var lead = guild.roleSpecs().stream()
            .filter(r -> r.roleName().equals("lead")).findFirst().orElseThrow();
        assertThat(lead.roleVocabulary()).isEqualTo("urn:casehub:vocab:roles");
        assertThat(lead.minCardinality()).isEqualTo(1);
        assertThat(lead.maxCardinality()).isEqualTo(1);
        assertThat(lead.incompatibleWith()).isEmpty();
        assertThat(lead.policies()).hasSize(2);

        var contributor = guild.roleSpecs().stream()
            .filter(r -> r.roleName().equals("contributor")).findFirst().orElseThrow();
        assertThat(contributor.minCardinality()).isEqualTo(2);
        assertThat(contributor.maxCardinality()).isEqualTo(-1);
    }

    @Test void rolePoliciesDeserialized() {
        var lead = unitById("dev-guild").roleSpecs().stream()
            .filter(r -> r.roleName().equals("lead")).findFirst().orElseThrow();

        var obligation = lead.policies().stream()
            .filter(p -> p.kind() == PolicyKind.OBLIGATION).findFirst().orElseThrow();
        assertThat(obligation.capabilityName()).isEqualTo("code-review");
        assertThat(obligation.expectations()).containsEntry("turnaround", "24h");

        var permission = lead.policies().stream()
            .filter(p -> p.kind() == PolicyKind.PERMISSION).findFirst().orElseThrow();
        assertThat(permission.capabilityName()).isEqualTo("merge-authority");
    }

    @Test void missionsDeserialized() {
        var guild = unitById("dev-guild");
        assertThat(guild.missions()).hasSize(2);

        var apiMission = guild.missions().stream()
            .filter(m -> m.missionName().equals("platform-api-v2")).findFirst().orElseThrow();
        assertThat(apiMission.description()).isEqualTo("Deliver v2 of the platform API");

        var debtMission = guild.missions().stream()
            .filter(m -> m.missionName().equals("tech-debt-reduction")).findFirst().orElseThrow();
        assertThat(debtMission.description()).isEqualTo("Reduce critical tech debt by 30%");
    }

    @Test void topDownFormationRulesDeserialized() {
        var rules = unitById("ops-squad").policy().formationRules();
        assertThat(rules.allowedPaths()).containsExactly(FormationPath.TOP_DOWN);
        assertThat(rules.proposalQuorum()).isEqualTo(1.0);
        assertThat(rules.allowSubGroups()).isFalse();
    }

    @Test void lowercaseEnumsDeserialized() {
        var yaml = getClass().getResourceAsStream("/society-lowercase-enums.yaml");
        var lcOrg = registrar.loadFrom(yaml);
        var unit = lcOrg.units().getFirst();

        assertThat(unit.authorityModel()).isEqualTo(AuthorityModel.DISTRIBUTED);
        assertThat(unit.policy().normSpecificity()).isEqualTo(NormSpecificity.DOMAIN);
        assertThat(unit.policy().formationRules().allowedPaths()).containsExactly(FormationPath.BOTTOM_UP);

        var policy = unit.roleSpecs().getFirst().policies().getFirst();
        assertThat(policy.kind()).isEqualTo(PolicyKind.ELIGIBILITY);
    }

    @Test void unitWithoutNewFieldsStillWorks() {
        var yaml = getClass().getResourceAsStream("/gastown-org.yaml");
        var gastownOrg = registrar.loadFrom(yaml);

        var oversight = gastownOrg.units().stream()
            .filter(u -> u.unitId().equals("oversight")).findFirst().orElseThrow();
        assertThat(oversight.topology()).isNull();
        assertThat(oversight.authorityModel()).isNull();
        assertThat(oversight.selfDetermination()).isEqualTo(0.0);
        assertThat(oversight.policy()).isNull();
        assertThat(oversight.roleSpecs()).isEmpty();
        assertThat(oversight.missions()).isEmpty();
    }

    private OrganizationalUnit unitById(String id) {
        return org.units().stream()
            .filter(u -> u.unitId().equals(id)).findFirst().orElseThrow();
    }
}
