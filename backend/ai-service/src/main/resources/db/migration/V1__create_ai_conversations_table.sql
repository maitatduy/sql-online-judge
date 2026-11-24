CREATE TABLE ai_conversations (
    id BINARY(16) NOT NULL PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    problem_id BINARY(16) NULL,
    problem_slug VARCHAR(160) NULL,
    title VARCHAR(200) NOT NULL,
    last_message_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by BINARY(16) NULL,
    updated_by BINARY(16) NULL,
    INDEX idx_ai_conversations_user_id_last_message_at (user_id, last_message_at)
);
