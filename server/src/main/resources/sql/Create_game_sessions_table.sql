-- This table stores the saved progress of a game for a specific user and case.
CREATE TABLE IF NOT EXISTS game_sessions (
    -- The user ID who owns this save. It's a foreign key to the users table.
    owner_id INTEGER NOT NULL,

    -- The title of the case being saved.
    case_title VARCHAR(255) NOT NULL,

    -- The entire game state is stored as a single JSONB object.
    -- JSONB is the preferred type in PostgreSQL for storing JSON as it's binary and indexable.
    game_state_data JSONB NOT NULL,

    -- Timestamp to show when the game was last saved.
    last_saved_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    -- Defines a composite primary key. A user can only have one save per case title.
    -- This is what allows the "ON CONFLICT" clause in your DAO to work correctly.
    PRIMARY KEY (owner_id, case_title),

    -- Creates the foreign key relationship.
    -- ON DELETE CASCADE means if a user is deleted from the 'users' table,
    -- all of their associated game saves will be automatically deleted as well.
    CONSTRAINT fk_owner
        FOREIGN KEY(owner_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);