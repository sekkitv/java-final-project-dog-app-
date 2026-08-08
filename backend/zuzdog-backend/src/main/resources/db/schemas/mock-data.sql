
-- Notes:
--   - password_hash is a placeholder
--   - All INSERTs use ON CONFLICT DO NOTHING so the script is safe to re-run.
--   - User IDs are assigned in the order listed below (1..10). The swipes/matches , so it is absoulutly safe
--     below hard-code those IDs intentionally for clarity.

-- disclaimer -- Mock data has been generated with ai.

-- Users

INSERT INTO users (username, password_hash, email, user_age, description, max_distance, lat, lng)
VALUES
    ('maya_tlv',     'PLACEHOLDER', 'maya@example.com',     28, 'Coffee & corgis. Love beach weekends.',   30, 32.0809, 34.7806),
    ('yoni_dad',     'PLACEHOLDER', 'yoni@example.com',     32, 'Engineer, dog dad, fetch enthusiast.',    25, 32.0871, 34.7742),
    ('noa_paws',     'PLACEHOLDER', 'noa@example.com',      26, 'Rescue advocate. Slow walks, lots of treats.', 20, 32.0923, 34.7688),
    ('amir_gold',    'PLACEHOLDER', 'amir@example.com',     35, 'Golden retriever dad. Fluent in ball-throwing.', 35, 32.0755, 34.7851),
    ('lily_shi',     'PLACEHOLDER', 'lily@example.com',     29, 'Shiba mom. Independent dog, social human.', 15, 32.1012, 34.7910),
    ('omer_husk',    'PLACEHOLDER', 'omer@example.com',     31, 'Husky owner. We run at sunrise.',          40, 32.0688, 34.7720),
    ('dana_jer',     'PLACEHOLDER', 'dana@example.com',     27, 'Jerusalem dog walker, loves the old city.', 25, 31.7857, 35.2151),
    ('ariel_hfa',    'PLACEHOLDER', 'ariel@example.com',    33, 'Haifa hill hiker, big-dog household.',     30, 32.7940, 34.9896),
    ('ron_bsv',      'PLACEHOLDER', 'ron@example.com',      30, 'Beer-Sheva newbie, mixed-breed fan.',      30, 31.2520, 34.7915),
    ('sara_eilat',   'PLACEHOLDER', 'sara@example.com',     34, 'Desert dog mom, weekend hikes in the south.', 50, 29.5577, 34.9519)
ON CONFLICT (username) DO NOTHING;

-- Dogs (one per user, matched by username)

INSERT INTO dogs (user_id, dog_name, breed, dog_age, traits, description)
SELECT user_id, 'Coco',  'Corgi',              2, 'playful,curious',     'Loves belly rubs and short hikes.'
FROM users WHERE username = 'maya_tlv'
ON CONFLICT (user_id, dog_name) DO NOTHING;

INSERT INTO dogs (user_id, dog_name, breed, dog_age, traits, description)
SELECT user_id, 'Bolt',  'Border Collie',      3, 'energetic,smart',     'Needs a job — fetch counts.'
FROM users WHERE username = 'yoni_dad'
ON CONFLICT (user_id, dog_name) DO NOTHING;

INSERT INTO dogs (user_id, dog_name, breed, dog_age, traits, description)
SELECT user_id, 'Mocha', 'Mixed',              4, 'gentle,shy',          'Rescue; warming up to new friends.'
FROM users WHERE username = 'noa_paws'
ON CONFLICT (user_id, dog_name) DO NOTHING;

INSERT INTO dogs (user_id, dog_name, breed, dog_age, traits, description)
SELECT user_id, 'Sunny', 'Golden Retriever',   5, 'friendly,loyal',      'Will sit for literally any snack.'
FROM users WHERE username = 'amir_gold'
ON CONFLICT (user_id, dog_name) DO NOTHING;

INSERT INTO dogs (user_id, dog_name, breed, dog_age, traits, description)
SELECT user_id, 'Mochi', 'Shiba Inu',          2, 'independent,alert',   'Judgey stare, sweet heart.'
FROM users WHERE username = 'lily_shi'
ON CONFLICT (user_id, dog_name) DO NOTHING;

INSERT INTO dogs (user_id, dog_name, breed, dog_age, traits, description)
SELECT user_id, 'Storm', 'Siberian Husky',     3, 'vocal,active',        'Howls at sirens, loves snow videos.'
FROM users WHERE username = 'omer_husk'
ON CONFLICT (user_id, dog_name) DO NOTHING;

INSERT INTO dogs (user_id, dog_name, breed, dog_age, traits, description)
SELECT user_id, 'Rocky', 'Labrador',           4, 'goofy,fast',          'Lab energy, neighborhood favorite.'
FROM users WHERE username = 'dana_jer'
ON CONFLICT (user_id, dog_name) DO NOTHING;

INSERT INTO dogs (user_id, dog_name, breed, dog_age, traits, description)
SELECT user_id, 'Bear',  'German Shepherd',    6, 'calm,protective',     'Gentle giant, great with kids.'
FROM users WHERE username = 'ariel_hfa'
ON CONFLICT (user_id, dog_name) DO NOTHING;

INSERT INTO dogs (user_id, dog_name, breed, dog_age, traits, description)
SELECT user_id, 'Mango', 'Mixed',              2, 'goofy,fast',          'Puppy energy x100.'
FROM users WHERE username = 'ron_bsv'
ON CONFLICT (user_id, dog_name) DO NOTHING;

INSERT INTO dogs (user_id, dog_name, breed, dog_age, traits, description)
SELECT user_id, 'Sandy', 'Saluki',             5, 'calm,independent',    'Desert heritage, short-burst runner.'
FROM users WHERE username = 'sara_eilat'
ON CONFLICT (user_id, dog_name) DO NOTHING;

-- -----------------------------------------------------------------------------
-- Swipes
--   maya (1) <-> noa (3)    mutual UP  -> match
--   amir (4) <-> lily (5)   mutual UP  -> match
--   maya (1) DOWN yoni (2)
--   omer (6) DOWN ariel (8)
--   dana (7) DOWN sara (10)   (cross-country, too far)
--   ron  (9) DOWN ariel (8)
-- -----------------------------------------------------------------------------
INSERT INTO swipes (sender_id, target_id, action) VALUES
    (1, 3, 'UP'),
    (3, 1, 'UP'),
    (4, 5, 'UP'),
    (5, 4, 'UP'),
    (1, 2, 'DOWN'),
    (6, 8, 'DOWN'),
    (7, 10, 'DOWN'),
    (9, 8, 'DOWN')
ON CONFLICT (sender_id, target_id) DO NOTHING;

-- -----------------------------------------------------------------------------
-- Matches (canonical ordering: smaller id first)
--   (1,3)  maya  <-> noa
--   (4,5)  amir  <-> lily
-- -----------------------------------------------------------------------------
INSERT INTO matches (user1_id, user2_id) VALUES
    (1, 3),
    (4, 5)
ON CONFLICT (user1_id, user2_id) DO NOTHING;

-- -----------------------------------------------------------------------------
-- Messages (chronological)
--   Thread maya (1) <-> noa (3)
--   Thread amir (4) <-> lily (5)
-- -----------------------------------------------------------------------------
INSERT INTO messages (sender_id, receiver_id, body, sent_at) VALUES
    (1, 3, 'Hey! Saw Mocha in your photos — Mocha is such a cute name. Want to set up a playdate?', NOW() - INTERVAL '2 days'),
    (3, 1, 'Yes! Coco and Mocha would get along. Yarkon Park on Sunday?',                   NOW() - INTERVAL '2 days' + INTERVAL '15 minutes'),
    (1, 3, 'Perfect. 10am by the dog beach?',                                                NOW() - INTERVAL '2 days' + INTERVAL '30 minutes'),
    (3, 1, 'See you there!',                                                                  NOW() - INTERVAL '2 days' + INTERVAL '45 minutes'),

    (4, 5, 'Mochi looks like trouble in the best way.',                                      NOW() - INTERVAL '1 day'),
    (5, 4, 'He really is. Sunny looks like the gentlest golden ever.',                       NOW() - INTERVAL '1 day' + INTERVAL '20 minutes'),
    (4, 5, 'She is. Want to meet up at the park this weekend?',                              NOW() - INTERVAL '23 hours');

-- -----------------------------------------------------------------------------
-- Notifications
--   user_id 1 (maya) gets a MATCH notification for match (1,3),
--   a MESSAGE notification for the latest message from noa (3),
--   and a HANGOUT_JOIN notification (reference_id left NULL for the demo).
--   user_id 3 (noa) gets the matching MATCH notification and a MESSAGE one.
--   Some are pre-read (read_at set) and some unread (read_at NULL) so the
--   countUnread / markAllRead behavior is visible after running init-db.ps1.
--   reference_id is kept loose (plain BIGINT) — it points at the related row
--   but is NOT enforced as a foreign key.
-- -----------------------------------------------------------------------------
INSERT INTO notifications (user_id, type, reference_id, title, body, created_at, read_at) VALUES
    -- maya (1): match with noa (3). The matches table has no integer key, so reference_id is NULL for MATCH.
    (1, 'MATCH',  NULL,
        'New match!',
        'You matched with noa_paws. Say hi!',
        NOW() - INTERVAL '2 days',
        NULL), -- unread
    -- maya (1): last message from noa (3) arrived 2 days ago.
    (1, 'MESSAGE', (SELECT message_id FROM messages WHERE sender_id = 3 AND receiver_id = 1 ORDER BY sent_at DESC LIMIT 1),
        'New message from noa_paws',
        'See you there!',
        NOW() - INTERVAL '2 days' + INTERVAL '45 minutes',
        NULL), -- unread
    -- maya (1): someone joined a hangout she organized (demo, reference_id NULL).
    (1, 'HANGOUT_JOIN', NULL,
        'New RSVP',
        'Someone joined your hangout.',
        NOW() - INTERVAL '12 hours',
        NOW()), -- already read
    -- noa (3): mirror match notification for maya (1).
    (3, 'MATCH', NULL,
        'New match!',
        'You matched with maya_tlv. Say hi!',
        NOW() - INTERVAL '2 days',
        NULL), -- unread
    -- noa (3): a message from maya arrived earlier — mark it read so counts differ.
    (3, 'MESSAGE', (SELECT message_id FROM messages WHERE sender_id = 1 AND receiver_id = 3 ORDER BY sent_at DESC LIMIT 1),
        'New message from maya_tlv',
        'Perfect. 10am by the dog beach?',
        NOW() - INTERVAL '2 days' + INTERVAL '30 minutes',
        NOW()); -- already read
