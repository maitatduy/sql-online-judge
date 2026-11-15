ALTER TABLE submissions
    ADD COLUMN problem_number INT NULL,
    ADD COLUMN problem_slug VARCHAR(160) NULL,
    ADD COLUMN problem_title VARCHAR(200) NULL;

CREATE INDEX idx_submissions_user_id_created_at ON submissions (user_id, created_at);
