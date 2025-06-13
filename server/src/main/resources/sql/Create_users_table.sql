-- This table stores user account information.
-- The 'id' is the primary key and will be used to link to game saves.
CREATE TABLE IF NOT EXISTS users (
    -- SERIAL is a PostgreSQL type that auto-increments, perfect for primary keys.
    id SERIAL PRIMARY KEY,

    -- Username must be unique to prevent duplicate accounts.
    username VARCHAR(255) NOT NULL UNIQUE,

    -- The password hash is stored, never the plain-text password.
    -- SHA-512 produces a 128-character hex string.
    password_hash VARCHAR(128) NOT NULL,

    -- Timestamps to track when the user was created.
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Adding an index on the username column can speed up login lookups.
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);