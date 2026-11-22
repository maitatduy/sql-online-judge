ALTER TABLE contests
    ADD COLUMN started_announced_at DATETIME(6) NULL,
    ADD COLUMN finished_announced_at DATETIME(6) NULL;
