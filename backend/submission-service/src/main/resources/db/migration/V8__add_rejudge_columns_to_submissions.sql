ALTER TABLE submissions
    ADD COLUMN rejudge_batch_id BINARY(16) NULL,
    ADD COLUMN round_start_attempt INT NOT NULL DEFAULT 1;

CREATE INDEX idx_submissions_problem_id_contest_id ON submissions (problem_id, contest_id);
