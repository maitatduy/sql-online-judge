CREATE TABLE role_invitations (
    id BINARY(16) NOT NULL PRIMARY KEY,
    email VARCHAR(254) NOT NULL,
    role VARCHAR(20) NOT NULL,
    message VARCHAR(500) NULL,
    token_hash CHAR(64) NOT NULL,
    invited_by BINARY(16) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    accepted_at DATETIME(6) NULL,
    accepted_by BINARY(16) NULL,
    revoked_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by BINARY(16) NULL,
    updated_by BINARY(16) NULL,
    CONSTRAINT fk_role_invitations_users_inviter FOREIGN KEY (invited_by) REFERENCES users (id),
    CONSTRAINT fk_role_invitations_users_accepter FOREIGN KEY (accepted_by) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE UNIQUE INDEX uk_role_invitations_token_hash ON role_invitations (token_hash);
CREATE INDEX idx_role_invitations_email_created_at ON role_invitations (email, created_at);
CREATE INDEX idx_role_invitations_created_at ON role_invitations (created_at);
