CREATE TABLE IF NOT EXISTS schemas (
    id SERIAL NOT NULL,
    name VARCHAR(32),
    schema TEXT,
    PRIMARY KEY(id)
);
