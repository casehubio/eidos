package io.casehub.eidos.vocab;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class ConscientiousnessVocabularyTest {

    final ConscientiousnessVocabularyProducer producer = new ConscientiousnessVocabularyProducer();

    @Test
    void uri_is_correct() {
        assertThat(producer.conscientiousnessVocabulary().uri())
            .isEqualTo("urn:casehub:vocab:conscientiousness");
    }

    @Test
    void covers_all_four_disposition_axes() {
        var terms = producer.conscientiousnessVocabulary().terms();
        assertThat(terms).containsKeys("strict", "principled", "flexible");
        assertThat(terms).containsKeys("conservative", "measured", "bold");
        assertThat(terms).containsKeys("collaborative", "independent", "facilitative");
        assertThat(terms).containsKeys("directed", "semi-autonomous", "autonomous");
    }

    @Test
    void strict_has_rule_bound_alias() {
        assertThat(producer.conscientiousnessVocabulary().terms().get("strict").aliases())
            .contains("rule-bound");
    }
}
