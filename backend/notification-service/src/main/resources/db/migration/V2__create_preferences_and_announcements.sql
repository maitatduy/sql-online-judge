CREATE TABLE notification_preferences (
    id BINARY(16) NOT NULL PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    is_email_enabled BOOLEAN NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by BINARY(16) NULL,
    updated_by BINARY(16) NULL,
    UNIQUE KEY uk_notification_preferences_user_id (user_id)
);

CREATE TABLE announcements (
    id BINARY(16) NOT NULL PRIMARY KEY,
    title VARCHAR(300) NOT NULL,
    link VARCHAR(300) NULL,
    recipient_count INT NOT NULL,
    is_delivered BOOLEAN NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by BINARY(16) NULL,
    updated_by BINARY(16) NULL,
    INDEX idx_announcements_created_at (created_at)
);
