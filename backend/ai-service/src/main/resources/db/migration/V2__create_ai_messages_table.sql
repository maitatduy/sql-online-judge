CREATE TABLE ai_messages (
    id BINARY(16) NOT NULL PRIMARY KEY,
    conversation_id BINARY(16) NOT NULL,
    role VARCHAR(16) NOT NULL,
    content MEDIUMTEXT NOT NULL,
    is_blocked BOOLEAN NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by BINARY(16) NULL,
    updated_by BINARY(16) NULL,
    INDEX idx_ai_messages_conversation_id_created_at (conversation_id, created_at),
    CONSTRAINT fk_ai_messages_ai_conversations FOREIGN KEY (conversation_id)
        REFERENCES ai_conversations (id) ON DELETE CASCADE
);
