CREATE TABLE IF NOT EXISTS user_social_profile (
    id UUID PRIMARY KEY,
    username VARCHAR(80) NOT NULL UNIQUE,
    email VARCHAR(320) NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    date_of_birth DATE,
    phone_number VARCHAR(40),
    country VARCHAR(100),
    city VARCHAR(100),
    bio TEXT,
    avatar_url VARCHAR(500),
    website_url VARCHAR(500),
    interests TEXT,
    occupation VARCHAR(160),
    employer VARCHAR(160),
    education VARCHAR(240),
    relationship_status VARCHAR(60),
    privacy_level VARCHAR(30) NOT NULL DEFAULT 'PUBLIC',
    preferred_language VARCHAR(20) NOT NULL DEFAULT 'en',
    timezone VARCHAR(80) NOT NULL DEFAULT 'UTC',
    updated_at TIMESTAMPTZ NOT NULL
);

GRANT USAGE ON SCHEMA public TO ryow;
GRANT SELECT, INSERT, UPDATE ON user_social_profile TO ryow;