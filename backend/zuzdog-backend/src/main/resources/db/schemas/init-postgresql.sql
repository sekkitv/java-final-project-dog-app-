-- we will create 6 main tables for out application - 
-- Users, dogs, swipes, matches, messages, notifications

CREATE TABLE IF NOT EXISTS users (
    user_id       BIGSERIAL     PRIMARY KEY,   -- bigserial is used automatically to give a unique id for each user, also gives us a bigger range of values
    username      VARCHAR(64)   NOT NULL UNIQUE,
    password_hash VARCHAR(255)  NOT NULL,
    salt          VARCHAR(64)   NOT NULL,
    email         VARCHAR(255)  NOT NULL UNIQUE,
    user_age      INTEGER      CHECK (user_age IS NULL OR (user_age > 0 AND user_age <= 120)), -- age cannot be less than 0 or more than 120
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
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
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

-- notifications table stores in-app notifications for each user.
-- each notification belongs to a user (user_id), has a type (MATCH, HANGOUT_JOIN, MESSAGE),
-- an optional reference_id (points to the related match/message/hangout row, kept as a plain
-- BIGINT and NOT a foreign key so we don`t break if the referenced row is cleaned up later),
-- a title, a body, and timestamps. read_at stays NULL until the user opens the notification.
CREATE TABLE IF NOT EXISTS notifications (
    notification_id BIGSERIAL     PRIMARY KEY,
    user_id         BIGINT        NOT NULL REFERENCES users (user_id) ON DELETE CASCADE, --when deleting a user the notification is deleted too
    type            VARCHAR(20)   NOT NULL CHECK (type IN ('MATCH', 'HANGOUT_JOIN', 'MESSAGE')), --the kind of event that triggered the notification
    reference_id    BIGINT, -- nullable: points to the related match/message/hangout row (kept loose on purpose)
    title           VARCHAR(120)  NOT NULL,
    body            TEXT          NOT NULL DEFAULT '',
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    read_at         TIMESTAMPTZ   -- NULL means unread, a timestamp means the user has seen it
);

--INDEXES--
-- we create indexes to improve the performance of our queries, especially for swipes and matches since they will be queried a lot.
-- the indexes help us find the rows we need much faster instead of scanning the entire table.

-- Discovery feed: filter users by location
CREATE INDEX IF NOT EXISTS idx_users_lat_lng ON users (lat, lng)
    WHERE lat IS NOT NULL AND lng IS NOT NULL;


-- "Who did I swipe?" (sender side) and "who swiped on me?" (target side, used in match detection)
CREATE INDEX IF NOT EXISTS idx_swipes_sender ON swipes (sender_id);
CREATE INDEX IF NOT EXISTS idx_swipes_target_action ON swipes (target_id, action);

-- "Who am I matched with?" — look up by either side
CREATE INDEX IF NOT EXISTS idx_matches_user1 ON matches (user1_id);
CREATE INDEX IF NOT EXISTS idx_matches_user2 ON matches (user2_id);

-- "Messages I sent" and "messages I received" in chronological order so we can display them in the chat interface
CREATE INDEX IF NOT EXISTS idx_messages_pair ON messages (sender_id, receiver_id, sent_at);
CREATE INDEX IF NOT EXISTS idx_messages_receiver ON messages (receiver_id, sent_at);

-- "Notifications for a user" ordered newest first — used by findForUser(userId)
CREATE INDEX IF NOT EXISTS idx_notifications_user_created ON notifications (user_id, created_at DESC);

-- "Unread notifications for a user" — used by countUnread(userId) and the WHERE clause of markAllRead(userId).
-- partial index (only rows where read_at IS NULL) so the unread count query stays fast even when most rows are read.
CREATE INDEX IF NOT EXISTS idx_notifications_user_unread ON notifications (user_id) WHERE read_at IS NULL;
