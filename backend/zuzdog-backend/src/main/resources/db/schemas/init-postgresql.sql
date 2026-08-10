-- we will create 7 main tables for out application - 
-- Users, dogs, swipes, matches, messages, notifications, hangouts

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

-- hangouts table stores group events created by a user on the map.
-- organizer_name is denormalised here on purpose: it is a snapshot of the organizer`s username
-- at creation time, so the map list keeps showing a name even if the organizer later renames
-- their account. activity_type is stored as a VARCHAR with a CHECK constraint (same pattern
-- as swipes.action and notifications.type), not as a native PG enum.
CREATE TABLE IF NOT EXISTS hangouts (
    hangout_id          BIGSERIAL      PRIMARY KEY,
    organizer_user_id   BIGINT         NOT NULL REFERENCES users (user_id) ON DELETE CASCADE, --when deleting a user their hangouts are deleted too
    title               VARCHAR(120)   NOT NULL,
    description         TEXT           NOT NULL DEFAULT '',
    organizer_name      VARCHAR(80)   NOT NULL, -- snapshot of the organizer`s username
    latitude            DOUBLE PRECISION NOT NULL, -- event latitude for the map pin
    longitude           DOUBLE PRECISION NOT NULL, -- event longitude for the map pin
    event_time          TIMESTAMPTZ,  -- nullable: a hangout can be an always-open spot
    activity_type       VARCHAR(32)   NOT NULL DEFAULT 'MEETUP' CHECK (activity_type IN ('MEETUP', 'DOG_FRIENDLY_BUSINESS', 'WATER_SPOT', 'POOP_BAGS_SPOT')),
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- hangout_participants is the junction table for the many-to-many between hangouts and users.
-- (hangout_id, user_id) is the natural primary key — a user can only be signed up once per hangout.
CREATE TABLE IF NOT EXISTS hangout_participants (
    hangout_id  BIGINT        NOT NULL REFERENCES hangouts (hangout_id) ON DELETE CASCADE, --when the hangout is deleted the signups go too
    user_id     BIGINT        NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    signed_up_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    PRIMARY KEY (hangout_id, user_id)
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

-- "Hangouts by event time" — used by the map list to sort upcoming events
CREATE INDEX IF NOT EXISTS idx_hangouts_event_time ON hangouts (event_time);

-- "Hangouts by activity type" — used when the map filters by activity (MEETUP / DOG_FRIENDLY_BUSINESS / ...)
CREATE INDEX IF NOT EXISTS idx_hangouts_activity_type ON hangouts (activity_type);

-- "Hangouts a user signed up for" — used to list the user`s hangouts on their profile
CREATE INDEX IF NOT EXISTS idx_hangout_participants_user ON hangout_participants (user_id);
