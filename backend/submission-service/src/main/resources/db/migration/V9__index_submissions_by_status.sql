CREATE INDEX idx_submissions_status_updated_at ON submissions (status, updated_at);

CREATE INDEX idx_submissions_verdict_judged_at ON submissions (verdict, judged_at);
