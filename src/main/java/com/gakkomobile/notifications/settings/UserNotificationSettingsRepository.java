package com.gakkomobile.notifications.settings;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
interface UserNotificationSettingsRepository extends JpaRepository<UserNotificationSettingsEntity, UUID> {

    @Modifying
    @Query(value = """
        INSERT INTO user_notification_settings (
            user_id,
            academic_grades,
            academic_schedule_changes,
            academic_cancelled_classes,
            financial_new_charges,
            events_student
        ) VALUES (
            :userId,
            'PUSH_AND_INAPP',
            'PUSH_AND_INAPP',
            'PUSH_AND_INAPP',
            'PUSH_AND_INAPP',
            'INAPP_ONLY'
        )
        ON CONFLICT (user_id) DO NOTHING
        """, nativeQuery = true)
    void insertDefaultSettingsNative(@Param("userId") UUID userId);
}
