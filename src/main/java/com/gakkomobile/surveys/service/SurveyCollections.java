package com.gakkomobile.surveys.service;

import com.gakkomobile.surveys.domain.SurveyQuestion;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class SurveyCollections {

    private SurveyCollections() {
    }

    static List<String> copyStrings(List<String> values) {

        if (Objects.isNull(values)) {
            return new ArrayList<>();
        }
        return new ArrayList<>(values);
    }

    static List<Double> copyDoubles(List<Double> values) {

        if (Objects.isNull(values)) {
            return new ArrayList<>();
        }
        return new ArrayList<>(values);
    }

    static List<SurveyQuestion> copyQuestions(List<SurveyQuestion> questions) {

        if (Objects.isNull(questions)) {
            return new ArrayList<>();
        }
        return new ArrayList<>(questions);
    }

    static List<String> immutableStrings(List<String> values) {

        if (Objects.isNull(values)) {
            return List.of();
        }
        return List.copyOf(values);
    }

    static List<Double> immutableDoubles(List<Double> values) {

        if (Objects.isNull(values)) {
            return List.of();
        }
        return List.copyOf(values);
    }
}
