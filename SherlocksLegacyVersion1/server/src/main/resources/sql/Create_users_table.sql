-- This table stores user account information.
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    -- The password hash is stored, never the plain-text password.
    -- SHA-512 produces a 128-character hex string.
    password_hash VARCHAR(128) NOT NULL,

    -- Timestamps to track when the user was created.
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);