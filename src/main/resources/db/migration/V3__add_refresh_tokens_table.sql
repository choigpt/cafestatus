CREATE TABLE refresh_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    token VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_refresh_tokens_token UNIQUE (token),
    CONSTRAINT fk_refresh_tokens_owner FOREIGN KEY (owner_id) REFERENCES owners(id) ON DELETE CASCADE
);

CREATE INDEX idx_refresh_tokens_owner_id ON refresh_tokens (owner_id);
