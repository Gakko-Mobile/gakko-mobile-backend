package com.gakkomobile.surveys.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SurveyReminderDispatchKey implements Serializable {

    @Column(name = "survey_id", nullable = false)
    private UUID surveyId;

    @Column(name = "interval_days", nullable = false)
    private BigDecimal intervalDays;

    @Override
    public boolean equals(Object other) {

        if (this == other) {
            return true;
        }
        if (!(other instanceof SurveyReminderDispatchKey otherKey)) {
            return false;
        }
        return Objects.equals(surveyId, otherKey.surveyId)
                && Objects.equals(intervalDays, otherKey.intervalDays);
    }

    @Override
    public int hashCode() {

        return Objects.hash(surveyId, intervalDays);
    }
}
