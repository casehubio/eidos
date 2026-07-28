package io.casehub.eidos.eval;

import io.casehub.eidos.eval.MbtiAlignmentJudge.MbtiQuestion;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MbtiAlignmentJudgeTest {

    @Test
    void questionnaire_has_twelve_items() {
        assertThat(MbtiAlignmentJudge.QUESTIONNAIRE).hasSize(12);
    }

    @Test
    void all_four_dimensions_covered() {
        var dimensions = MbtiAlignmentJudge.QUESTIONNAIRE.stream()
                .map(MbtiQuestion::dimension)
                .distinct()
                .toList();
        assertThat(dimensions).containsExactlyInAnyOrder("EI", "SN", "TF", "JP");
    }

    @Test
    void each_dimension_has_three_questions() {
        for (String dim : java.util.List.of("EI", "SN", "TF", "JP")) {
            long count = MbtiAlignmentJudge.QUESTIONNAIRE.stream()
                    .filter(q -> q.dimension().equals(dim))
                    .count();
            assertThat(count).as("dimension " + dim).isEqualTo(3);
        }
    }

    @Test
    void question_numbers_are_sequential() {
        for (int i = 0; i < MbtiAlignmentJudge.QUESTIONNAIRE.size(); i++) {
            assertThat(MbtiAlignmentJudge.QUESTIONNAIRE.get(i).number()).isEqualTo(i + 1);
        }
    }

    @Test
    void a_is_pole_values_are_valid_mbti_letters() {
        for (var q : MbtiAlignmentJudge.QUESTIONNAIRE) {
            assertThat(q.aIsPole()).matches("[EISNTFJP]");
        }
    }
}
