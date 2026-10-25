CREATE INDEX idx_admin_actions_created_at ON admin_actions (created_at);
CREATE INDEX idx_admin_actions_actor_id_created_at ON admin_actions (actor_id, created_at);
