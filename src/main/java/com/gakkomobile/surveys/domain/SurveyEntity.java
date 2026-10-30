package com.gakkomobile.surveys.domain;

import com.gakkomobile.user.UserEntity;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "surveys")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
public class SurveyEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id")
    private UserEntity creator;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "questions", nullable = false, columnDefinition = "jsonb")
    private List<SurveyQuestion> questions = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private SurveyStatus status = SurveyStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 50)
    private AudienceTargetType targetType = AudienceTargetType.ALL;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "target_ids", columnDefinition = "jsonb")
    private List<String> targetIds = new ArrayList<>();

    @Column(name = "is_anonymous", nullable = false)
    private boolean anonymous = true;

    @Column(name = "allow_resubmission", nullable = false)
    private boolean allowResubmission = false;

    @Column(name = "require_login", nullable = false)
    private boolean requireLogin = true;

    @Column(name = "show_results_to_students", nullable = false)
    private boolean showResultsToStudents = false;

    @Column(name = "randomize_question_order", nullable = false)
    private boolean randomizeQuestionOrder = false;

    @Column(name = "response_limit")
    private Integer responseLimit;

    @Column(name = "confirmation_message", columnDefinition = "TEXT")
    private String confirmationMessage;

    @Column(name = "opens_at")
    private OffsetDateTime opensAt;

    @Column(name = "closes_at")
    private OffsetDateTime closesAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "reminder_intervals", columnDefinition = "jsonb")
    private List<Double> reminderIntervals = new ArrayList<>();

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {

        if (Objects.isNull(this.id)) {
            this.id = UuidCreator.getTimeOrderedEpoch();
        }
    }
}
