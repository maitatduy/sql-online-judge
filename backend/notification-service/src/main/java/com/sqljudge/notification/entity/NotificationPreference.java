package com.sqljudge.notification.entity;

import com.sqljudge.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "notification_preferences",
        indexes =
                @Index(
                        name = "uk_notification_preferences_user_id",
                        columnList = "user_id",
                        unique = true))
@Getter
@NoArgsConstructor
public class NotificationPreference extends BaseEntity {

    @Column(name = "user_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID userId;

    @Column(name = "is_email_enabled", nullable = false)
    private boolean emailEnabled;

    public static NotificationPreference of(UUID userId, boolean emailEnabled) {
        NotificationPreference preference = new NotificationPreference();
        preference.userId = userId;
        preference.emailEnabled = emailEnabled;
        return preference;
    }

    public void changeEmail(boolean enabled) {
        emailEnabled = enabled;
    }
}
