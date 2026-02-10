CREATE TABLE owners (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_owners_email UNIQUE (email)
);

ALTER TABLE cafes ADD COLUMN owner_id BIGINT NULL;
ALTER TABLE cafes ADD CONSTRAINT fk_cafes_owner FOREIGN KEY (owner_id) REFERENCES owners(id) ON DELETE SET NULL;
CREATE INDEX idx_cafes_owner_id ON cafes (owner_id);
ALTER TABLE cafes DROP COLUMN owner_token;
