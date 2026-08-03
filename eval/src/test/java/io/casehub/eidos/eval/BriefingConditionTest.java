package io.casehub.eidos.eval;

import io.casehub.eidos.api.AgentDescriptor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BriefingConditionTest {

    static JungianProfile intp;

    @BeforeAll
    static void loadProfile() {
        intp = new JungianProfileLoader().load().stream()
            .filter(p -> "INTP".equals(p.mbtiType()))
            .findFirst().orElseThrow();
    }

    @Test
    void jungianRich_returnsOriginalDescriptor() {
        AgentDescriptor d = BriefingCondition.JUNGIAN_RICH.apply(intp);
        assertThat(d.briefing()).isEqualTo(intp.descriptor().briefing());
        assertThat(d.disposition().dispositionProfile()).isNotEmpty();
        assertThat(d.dispositionVocabulary()).isEqualTo("urn:casehub:vocab:jungian");
    }

    @Test
    void jungianMinimal_keepFramework_stripBriefing() {
        AgentDescriptor d = BriefingCondition.JUNGIAN_MINIMAL.apply(intp);
        assertThat(d.briefing()).startsWith("You are an agent named ");
        assertThat(d.briefing()).doesNotContain("INTP");
        assertThat(d.disposition().dispositionProfile()).isNotEmpty();
        assertThat(d.dispositionVocabulary()).isEqualTo("urn:casehub:vocab:jungian");
    }

    @Test
    void baselineRich_stripFramework_keepBriefing() {
        AgentDescriptor d = BriefingCondition.BASELINE_RICH.apply(intp);
        assertThat(d.briefing()).isEqualTo(intp.descriptor().briefing());
        assertThat(d.disposition().dispositionProfile()).isEmpty();
        assertThat(d.dispositionVocabulary()).isNull();
    }

    @Test
    void baselineMinimal_stripBoth() {
        AgentDescriptor d = BriefingCondition.BASELINE_MINIMAL.apply(intp);
        assertThat(d.briefing()).startsWith("You are an agent named ");
        assertThat(d.briefing()).doesNotContain("INTP");
        assertThat(d.disposition().dispositionProfile()).isEmpty();
        assertThat(d.dispositionVocabulary()).isNull();
    }

    @Test
    void allConditions_preserveNonVariedFields() {
        for (BriefingCondition condition : BriefingCondition.values()) {
            AgentDescriptor d = condition.apply(intp);
            AgentDescriptor orig = intp.descriptor();
            assertThat(d.agentId()).as(condition + " agentId").isEqualTo(orig.agentId());
            assertThat(d.name()).as(condition + " name").isEqualTo(orig.name());
            assertThat(d.slot()).as(condition + " slot").isEqualTo(orig.slot());
            assertThat(d.tenancyId()).as(condition + " tenancyId").isEqualTo(orig.tenancyId());
            assertThat(d.capabilities()).as(condition + " capabilities").isEqualTo(orig.capabilities());
        }
    }

    @Test
    void minimalBriefing_usesRoleNotName() {
        AgentDescriptor d = BriefingCondition.BASELINE_MINIMAL.apply(intp);
        assertThat(d.briefing()).contains("Systems Analyst");
        assertThat(d.briefing()).doesNotContain("(INTP)");
    }
}
