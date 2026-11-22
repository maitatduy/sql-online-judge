ALTER TABLE submissions
    ADD COLUMN contest_id BINARY(16) NULL;

CREATE INDEX idx_submissions_contest_id_created_at ON submissions (contest_id, created_at);
