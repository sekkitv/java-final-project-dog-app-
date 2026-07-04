-- we will create 5 main tables for out application - 
-- Users, dogs, swipes, matches, messages

CREATE TABLE IF NOT EXISTS users (
    user_id            BIGSERIAL     PRIMARY KEY,   -- bigserial is used automatically to give a unique id for each user, also gives us a bigger range of values
    username      VARCHAR(64)   NOT NULL UNIQUE,
    password_hash VARCHAR(255)  NOT NULL,
    email         VARCHAR(255)  NOT NULL UNIQUE,
    user_age           INTEGER      CHECK (user_age IS NULL OR (user_age > 0 AND user_age <= 120)), -- age cannot be less than 0 or more than 120
    description   TEXT, -- TEXT is used for long text fields.
    photo_url     VARCHAR(512),
    max_distance  DOUBLE PRECISION NOT NULL DEFAULT 25.0 CHECK (max_distance > 0), -- max distance cannot be less then 0, default val will be set to 25
    lat           DOUBLE PRECISION, -- double precision it uses 8 bytes to store the value and that is how we can store a bigger value for lat since it is long
    lng           DOUBLE PRECISION,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(), -- timestamptz is used to convert into UTC time zone , then we can use it to translate to israel time zone
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


CREATE TABLE IF NOT EXISTS dogs (
    dog_id      BIGSERIAL       PRIMARY KEY, 
    user_id     BIGINT          NOT NULL REFERENCES users (user_id) ON DELETE CASCADE, --when deleting a user the dog needs to be deleted too
    dog_name    VARCHAR(100)    NOT NULL, --must have dog name
    breed       VARCHAR(100),
    dog_age     INTEGER         CHECK (dog_age IS NULL OR (dog_age > 0 AND dog_age <= 25)), -- i dont know a dog that is older than 25 years old.
    traits      TEXT,
    description TEXT,
    photo_url   VARCHAR(512),
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, dog_name)
);

CREATE TABLE IF NOT EXISTS swipes (
    sender_id   BIGINT          NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    target_id   BIGINT          NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    action      VARCHAR(10)     NOT NULL CHECK (action IN ('UP', 'DOWN')), --up for like, down for dislike
    swiped_at   TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    PRIMARY KEY (sender_id, target_id), --each user has only 1 row on other users
    CHECK (sender_id <> target_id) --check that sender and target are not the same
);

CREATE TABLE IF NOT EXISTS matches (
    user1_id    BIGINT          NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    user2_id    BIGINT          NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    match_date  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user1_id, user2_id),
    CHECK (user1_id < user2_id) --we do this so we won`t have duplicated , if we have 3 and 5 , we will always get (3,5) and that is how we avoid duplicated
);

CREATE TABLE IF NOT EXISTS messages (
    message_id  BIGSERIAL       PRIMARY KEY,
    sender_id   BIGINT          NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    receiver_id BIGINT          NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    body        TEXT            NOT NULL,
    sent_at     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CHECK (sender_id <> receiver_id),
    CHECK (char_length(trim(body)) > 0) -- we do it to reject empty messages and pure whitespaces.
);

--INDEXES--
-- we create indexes to improve the performance of our queries, especially for swipes and matches since they will be queried a lot.
-- the indexes help us find the rows we need much faster instead of scanning the entire table.

-- Discovery feed: filter users by location
CREATE INDEX IF NOT EXISTS idx_users_lat_lng ON users (lat, lng)
    WHERE lat IS NOT NULL AND lng IS NOT NULL;

-- Lookups by dog owner
CREATE INDEX IF NOT EXISTS idx_dogs_user_id ON dogs (user_id);

-- "Who did I swipe?" (sender side) and "who swiped on me?" (target side, used in match detection)
CREATE INDEX IF NOT EXISTS idx_swipes_sender ON swipes (sender_id);
CREATE INDEX IF NOT EXISTS idx_swipes_target_action ON swipes (target_id, action);

-- "Who am I matched with?" — look up by either side
CREATE INDEX IF NOT EXISTS idx_matches_user1 ON matches (user1_id);
CREATE INDEX IF NOT EXISTS idx_matches_user2 ON matches (user2_id);

-- "Messages I sent" and "messages I received" in chronological order so we can display them in the chat interface
CREATE INDEX IF NOT EXISTS idx_messages_pair ON messages (sender_id, receiver_id, sent_at);
CREATE INDEX IF NOT EXISTS idx_messages_receiver ON messages (receiver_id, sent_at);