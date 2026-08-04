package io.casehub.eidos.eval;

import io.casehub.eidos.api.AgentDisposition;
import io.casehub.eidos.api.DispositionValue;
import org.junit.jupiter.api.Test;

import dev.langchain4j.model.chat.ChatModel;

import static org.assertj.core.api.Assertions.assertThat;

class BriefingCoherenceJudgeTest {

    @Test
    void returns_sentinel_for_non_jungian_vocabulary() {
        var judge = new BriefingCoherenceJudge((ChatModel) null, null);
        var disposition = AgentDisposition.builder()
                .socialOrient("collaborative")
                .build();

        var result = judge.evaluate("Some briefing", disposition, null, "test-agent");

        assertThat(result.overallCoherence()).isEqualTo(-1.0);
        assertThat(result.functions()).isEmpty();
        assertThat(result.tensions()).isEmpty();
    }

    @Test
    void returns_sentinel_for_null_disposition() {
        var judge = new BriefingCoherenceJudge((ChatModel) null, null);

        var result = judge.evaluate("Some briefing", null, "urn:casehub:vocab:jungian", "test-agent");

        assertThat(result.overallCoherence()).isEqualTo(-1.0);
    }

    @Test
    void returns_sentinel_for_empty_profile() {
        var judge = new BriefingCoherenceJudge((ChatModel) null, null);
        var disposition = AgentDisposition.builder().build();

        var result = judge.evaluate("Some briefing", disposition, "urn:casehub:vocab:jungian", "test-agent");

        assertThat(result.overallCoherence()).isEqualTo(-1.0);
    }

    @Test
    void returns_sentinel_for_baseline_vocabulary() {
        var judge = new BriefingCoherenceJudge((ChatModel) null, null);
        var disposition = AgentDisposition.builder()
                .dispositionProfile(new DispositionValue("te", 0.40))
                .build();

        var result = judge.evaluate("Some briefing", disposition, "urn:casehub:vocab:baseline", "test-agent");

        assertThat(result.overallCoherence()).isEqualTo(-1.0);
    }
}
