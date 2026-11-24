CREATE TABLE notifications (
    id BINARY(16) NOT NULL PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    event_id BINARY(16) NOT NULL,
    type VARCHAR(30) NOT NULL,
    title VARCHAR(300) NOT NULL,
    link VARCHAR(300) NULL,
    read_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by BINARY(16) NULL,
    updated_by BINARY(16) NULL,
    UNIQUE KEY uk_notifications_event_id_user_id (event_id, user_id),
    INDEX idx_notifications_user_id_id (user_id, id),
    INDEX idx_notifications_user_id_read_at (user_id, read_at)
);
