CREATE TABLE IF NOT EXISTS apps (
    id VARCHAR(32) PRIMARY KEY,
    icon TEXT,
    cover TEXT,
    title VARCHAR(255),
    description TEXT,
    repo_url TEXT,
    author VARCHAR(255),
    rate VARCHAR(16)
);