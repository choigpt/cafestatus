CREATE TABLE cafes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    address VARCHAR(255),
    owner_token VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_cafes_lat_lng ON cafes (latitude, longitude);
CREATE UNIQUE INDEX uq_cafes_name_lat_lng ON cafes (name, latitude, longitude);

CREATE TABLE cafe_live_status (
    cafe_id BIGINT NOT NULL PRIMARY KEY,
    crowd_level VARCHAR(20) NOT NULL,
    party2 VARCHAR(10) NOT NULL,
    party3 VARCHAR(10) NOT NULL,
    party4 VARCHAR(10) NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_status_cafe FOREIGN KEY (cafe_id) REFERENCES cafes(id) ON DELETE CASCADE
);
