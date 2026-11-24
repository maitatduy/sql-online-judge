ALTER TABLE ai_messages
    ADD COLUMN block_reason VARCHAR(20) NULL,
    ADD COLUMN reply_to_id BINARY(16) NULL,
    ADD INDEX idx_ai_messages_is_blocked_created_at (is_blocked, created_at),
    ADD INDEX idx_ai_messages_role_created_at (role, created_at);
