ALTER TABLE posts
    ADD COLUMN hidden_reason VARCHAR(16) NULL;

ALTER TABLE comments
    ADD COLUMN hidden_reason VARCHAR(16) NULL;
