package com.gakkomobile.notifications.settings;

import com.gakkomobile.notifications.NotificationMode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_notification_settings")
@Getter
@Setter
@NoArgsConstructor
class UserNotificationSettingsEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "academic_grades", nullable = false, columnDefinition = "notification_mode")
    private NotificationMode academicGrades = NotificationMode.PUSH_AND_INAPP;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "academic_schedule_changes", nullable = false, columnDefinition = "notification_mode")
    private NotificationMode academicScheduleChanges = NotificationMode.PUSH_AND_INAPP;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "academic_cancelled_classes", nullable = false, columnDefinition = "notification_mode")
    private NotificationMode academicCancelledClasses = NotificationMode.PUSH_AND_INAPP;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "financial_new_charges", nullable = false, columnDefinition = "notification_mode")
    private NotificationMode financialNewCharges = NotificationMode.PUSH_AND_INAPP;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "events_student", nullable = false, columnDefinition = "notification_mode")
    private NotificationMode eventsStudent = NotificationMode.INAPP_ONLY;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {

        this.updatedAt = Instant.now();
    }
}