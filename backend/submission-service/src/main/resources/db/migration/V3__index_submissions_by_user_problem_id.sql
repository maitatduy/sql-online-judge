CREATE INDEX idx_submissions_user_id_problem_id_id ON submissions (user_id, problem_id, id);

DROP INDEX idx_submissions_user_id_problem_id_created_at ON submissions;
