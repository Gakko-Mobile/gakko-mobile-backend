package com.gakkomobile.surveys.domain;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "survey_completions")
@Getter
@Setter
@NoArgsConstructor
public class SurveyCompletionEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "survey_id", nullable = false)
    private UUID surveyId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @CreationTimestamp
    @Column(name = "completed_at", nullable = false)
    private OffsetDateTime completedAt;

    @PrePersist
    protected void onCreate() {

        if (Objects.isNull(this.id)) {
            this.id = UuidCreator.getTimeOrderedEpoch();
        }
    }
}
