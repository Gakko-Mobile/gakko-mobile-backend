package com.gakkomobile.surveys.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "survey_reminder_dispatches")
@Getter
@Setter
@NoArgsConstructor
public class SurveyReminderDispatchEntity {

    @EmbeddedId
    private SurveyReminderDispatchKey id;

    @CreationTimestamp
    @Column(name = "dispatched_at", nullable = false, updatable = false)
    private OffsetDateTime dispatchedAt;
}
