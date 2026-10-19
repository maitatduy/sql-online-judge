ALTER TABLE admin_actions
    ADD COLUMN previous_value VARCHAR(100) NULL,
    ADD COLUMN new_value VARCHAR(100) NULL;
