CREATE TABLE IF NOT EXISTS tags (
    id SERIAL NOT NULL,
    name VARCHAR(32),
    category VARCHAR(32),
    PRIMARY KEY(id)
);
