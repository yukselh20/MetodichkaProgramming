-- This table stores the saved progress of a game for a specific user and case.
CREATE TABLE IF NOT EXISTS game_sessions (
    -- The user ID who owns this save. It's a foreign key to the users table.
    owner_id INTEGER NOT NULL,

    case_title VARCHAR(255) NOT NULL,

    -- The entire game state is stored as a single JSONB object.
    game_state_data JSONB NOT NULL,

    -- Timestamp to show when the game was last saved.
    last_saved_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    -- Defines a composite primary key. A user can only have one save per case title.
    -- This is what allows the "ON CONFLICT" clause in DAO to work correctly.
    PRIMARY KEY (owner_id, case_title),

    CONSTRAINT fk_owner
        FOREIGN KEY(owner_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);