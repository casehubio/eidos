package io.casehub.eidos.vocab;

import io.casehub.eidos.api.DispositionValue;
import io.casehub.eidos.api.VocabularyTerm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class MbtiTypeTermTest {

    @Test void sixteen_types_defined() {
        assertThat(MbtiTypeTerm.values()).hasSize(16);
    }

    @Test void uri_is_mbti() {
        assertThat(MbtiTypeTerm.URI).isEqualTo("urn:casehub:vocab:mbti");
    }

    @Test void intp_specializes_ti_and_ne() {
        assertThat(MbtiTypeTerm.INTP.specializes())
                .containsExactly(JungianFunctionTerm.TI, JungianFunctionTerm.NE);
    }

    @Test void intj_specializes_ni_and_te() {
        assertThat(MbtiTypeTerm.INTJ.specializes())
                .containsExactly(JungianFunctionTerm.NI, JungianFunctionTerm.TE);
    }

    @Test void enfj_specializes_fe_and_ni() {
        assertThat(MbtiTypeTerm.ENFJ.specializes())
                .containsExactly(JungianFunctionTerm.FE, JungianFunctionTerm.NI);
    }

    @ParameterizedTest
    @EnumSource(MbtiTypeTerm.class)
    void specializes_returns_exactly_two_functions(MbtiTypeTerm type) {
        assertThat(type.specializes()).hasSize(2);
    }

    @ParameterizedTest
    @EnumSource(MbtiTypeTerm.class)
    void dominant_and_auxiliary_are_opposite_categories(MbtiTypeTerm type) {
        var fns = type.specializes();
        JungianFunctionTerm dominant = (JungianFunctionTerm) fns.get(0);
        JungianFunctionTerm auxiliary = (JungianFunctionTerm) fns.get(1);
        assertThat(dominant.category()).isNotEqualTo(auxiliary.category());
    }

    @ParameterizedTest
    @EnumSource(MbtiTypeTerm.class)
    void default_profile_has_eight_functions(MbtiTypeTerm type) {
        assertThat(type.defaultProfile()).hasSize(8);
    }

    @ParameterizedTest
    @EnumSource(MbtiTypeTerm.class)
    void default_profile_weights_sum_to_one(MbtiTypeTerm type) {
        double sum = type.defaultProfile().stream()
                .mapToDouble(DispositionValue::weight).sum();
        assertThat(sum).isCloseTo(1.0, within(0.001));
    }

    @ParameterizedTest
    @EnumSource(MbtiTypeTerm.class)
    void default_profile_dominant_has_highest_weight(MbtiTypeTerm type) {
        var profile = type.defaultProfile();
        JungianFunctionTerm expectedDominant = (JungianFunctionTerm) type.specializes().get(0);
        var highest = profile.stream()
                .max(Comparator.comparingDouble(DispositionValue::weight))
                .orElseThrow();
        assertThat(highest.term()).isEqualTo(expectedDominant.value());
    }

    @ParameterizedTest
    @EnumSource(MbtiTypeTerm.class)
    void default_profile_dominant_in_high_tier(MbtiTypeTerm type) {
        JungianFunctionTerm dominant = (JungianFunctionTerm) type.specializes().get(0);
        var weight = type.defaultProfile().stream()
                .filter(dv -> dv.term().equals(dominant.value()))
                .findFirst().orElseThrow().weight();
        assertThat(weight).isGreaterThanOrEqualTo(JungianFunctionTerm.DOMINANT_MIN);
    }

    @ParameterizedTest
    @EnumSource(MbtiTypeTerm.class)
    void default_profile_auxiliary_in_low_tier(MbtiTypeTerm type) {
        JungianFunctionTerm auxiliary = (JungianFunctionTerm) type.specializes().get(1);
        var weight = type.defaultProfile().stream()
                .filter(dv -> dv.term().equals(auxiliary.value()))
                .findFirst().orElseThrow().weight();
        assertThat(weight).isGreaterThanOrEqualTo(JungianFunctionTerm.AUXILIARY_MIN);
        assertThat(weight).isLessThanOrEqualTo(JungianFunctionTerm.AUXILIARY_MAX);
    }

    @ParameterizedTest
    @EnumSource(MbtiTypeTerm.class)
    void all_types_have_value_label_description(MbtiTypeTerm type) {
        assertThat(type.value()).isNotBlank();
        assertThat(type.label()).isNotBlank();
        assertThat(type.description()).isNotBlank();
    }

    @ParameterizedTest
    @EnumSource(MbtiTypeTerm.class)
    void all_values_are_lowercase(MbtiTypeTerm type) {
        assertThat(type.value()).isEqualTo(type.value().toLowerCase());
    }
}
