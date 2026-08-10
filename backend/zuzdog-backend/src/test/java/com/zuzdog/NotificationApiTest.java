package com.zuzdog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zuzdog.controller.HangoutController;
import com.zuzdog.controller.MessageController;
import com.zuzdog.controller.NotificationController;
import com.zuzdog.dao.HangoutDao;
import com.zuzdog.dao.HangoutParticipantDao;
import com.zuzdog.dao.MatchDao;
import com.zuzdog.dao.MessageDao;
import com.zuzdog.dao.NotificationDao;
import com.zuzdog.dao.SwipeDao;
import com.zuzdog.dao.UserDao;
import com.zuzdog.exception.GlobalExceptionHandler;
import com.zuzdog.messaging.SwipeConsumer;
import com.zuzdog.messaging.SwipeMessage;
import com.zuzdog.model.ChatMessage;
import com.zuzdog.model.Hangout;
import com.zuzdog.model.HangoutActivityType;
import com.zuzdog.model.Match;
import com.zuzdog.model.Notification;
import com.zuzdog.model.NotificationType;
import com.zuzdog.model.SwipeAction;
import com.zuzdog.model.User;
import com.zuzdog.security.AuthenticationFilter;
import com.zuzdog.security.SecurityProperties;
import com.zuzdog.security.SessionService;
import com.zuzdog.service.HangoutService;
import com.zuzdog.service.MessageService;
import com.zuzdog.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Spring context, no Postgres. The three real controllers (Notification / Message / Hangout)
// run inside a single standalone MockMvc chained with the real AuthenticationFilter, but every
// DAO is a HashMap/HashSet-backed fake so no SQL ever runs.
//
//   (1) GET  /api/notifications        returns {notifications, unreadCount}        -> group A
//   (2) POST /api/notifications/read   marks all read, returns updated list         -> group B
//   (3) match         -> both users get a MATCH notification                        -> group C
//   (4) message sent  -> receiver gets a MESSAGE notification                     -> group D
//   (5) hangout RSVP  -> organizer + other participants get HANGOUT_JOIN          -> group E
// Plus auth guards and idempotency re-checks that the guards actually hold.
class NotificationApiTest {

    private SessionService sessionService;
    private FakeUserDao userDao;
    private FakeSwipeDao swipeDao;
    private FakeMatchDao matchDao;
    private FakeMessageDao messageDao;
    private FakeHangoutDao hangoutDao;
    private FakeHangoutParticipantDao participantDao;
    private FakeNotificationDao notificationDao;
    private NotificationService notificationService;
    private SwipeConsumer swipeConsumer;
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // seeded users. alice <-> bob are used for matches/messages; carol is the third
    // participant in the hangout fan-out test.
    private static final long ALICE_ID = 1L;
    private static final long BOB_ID = 2L;
    private static final long CAROL_ID = 3L;

    @BeforeEach
    void setUp() {
        SecurityProperties securityProperties = new SecurityProperties();
        securityProperties.setPepper("test-pepper");
        securityProperties.setSessionTtlMinutes(1440);

        sessionService = new SessionService(securityProperties);
        userDao = new FakeUserDao();
        swipeDao = new FakeSwipeDao();
        matchDao = new FakeMatchDao();
        messageDao = new FakeMessageDao(userDao);
        participantDao = new FakeHangoutParticipantDao();
        hangoutDao = new FakeHangoutDao(participantDao);
        notificationDao = new FakeNotificationDao();

        notificationService = new NotificationService(notificationDao);
        swipeConsumer = new SwipeConsumer(swipeDao, matchDao, notificationService);

        seedUser(ALICE_ID, "alice");
        seedUser(BOB_ID, "bob");
        seedUser(CAROL_ID, "carol");

        // a single mockMvc that mounts all three controllers + the auth filter, so we can
        // drive the message-trigger and hangout-trigger end-to-end via HTTP, plus the
        // notification endpoints themselves.
        MessageService messageService = new MessageService(messageDao, matchDao, notificationService);
        HangoutService hangoutService = new HangoutService(hangoutDao, participantDao, userDao, notificationService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(
                        new NotificationController(notificationService),
                        new MessageController(messageService),
                        new HangoutController(hangoutService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(new AuthenticationFilter(sessionService))
                .build();
    }

    // ------------------------------------------------------------------
    // group A: GET /api/notifications returns {notifications, unreadCount}
    // ------------------------------------------------------------------

    @Test
    @DisplayName("A1: GET /api/notifications returns the exact {notifications, unreadCount} shape")
    void getNotifications_returnsShape() throws Exception {
        // seed two notifications directly through the service so the fake has real rows.
        notificationService.notifyMessage(BOB_ID, ALICE_ID, 100L);   // alice gets 1 MESSAGE
        notificationService.notifyHangoutJoin(7L, BOB_ID, ALICE_ID, List.of()); // alice gets 1 HANGOUT_JOIN

        String token = tokenFor(ALICE_ID);

        MvcResult result = mockMvc.perform(get("/api/notifications").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                // top-level keys are exactly notifications + unreadCount
                .andExpect(jsonPath("$.notifications").isArray())
                .andExpect(jsonPath("$.unreadCount").value(2))
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode notifs = json.get("notifications");
        assertThat(notifs.size()).isEqualTo(2);
        // each notification exposes its full set of fields 
        for (JsonNode n : notifs) {
            assertThat(n.has("notificationId")).isTrue();
            assertThat(n.has("userId")).isTrue();
            assertThat(n.has("type")).isTrue();
            assertThat(n.has("referenceId")).isTrue();
            assertThat(n.has("title")).isTrue();
            assertThat(n.has("body")).isTrue();
            assertThat(n.has("createdAt")).isTrue();
            assertThat(n.has("readAt")).isTrue();
        }
    }

    @Test
    @DisplayName("A2: GET returns only the caller's notifications, not other users'")
    void getNotifications_onlyOwnRows() throws Exception {
        notificationService.notifyMessage(BOB_ID, ALICE_ID, 1L); // alice gets one
        notificationService.notifyMessage(ALICE_ID, BOB_ID, 2L); // bob gets one

        // alice's view
        mockMvc.perform(get("/api/notifications").header("Authorization", "Bearer " + tokenFor(ALICE_ID)))
                .andExpect(jsonPath("$.notifications.length()").value(1))
                .andExpect(jsonPath("$.notifications[0].userId").value(ALICE_ID))
                .andExpect(jsonPath("$.unreadCount").value(1));

        // bob's view
        mockMvc.perform(get("/api/notifications").header("Authorization", "Bearer " + tokenFor(BOB_ID)))
                .andExpect(jsonPath("$.notifications.length()").value(1))
                .andExpect(jsonPath("$.notifications[0].userId").value(BOB_ID))
                .andExpect(jsonPath("$.unreadCount").value(1));
    }

    // ------------------------------------------------------------------
    // group B: POST /api/notifications/read marks all as read and returns updated list
    // ------------------------------------------------------------------

    @Test
    @DisplayName("B1: POST /read marks ALL the caller's notifications read, unreadCount becomes 0")
    void markRead_flipsAllUnread() throws Exception {
        notificationService.notifyMessage(BOB_ID, ALICE_ID, 1L);
        notificationService.notifyMessage(CAROL_ID, ALICE_ID, 2L);
        notificationService.notifyMatch(ALICE_ID, BOB_ID); // alice gets one too

        String token = tokenFor(ALICE_ID);

        // sanity: alice starts with 3 unread
        mockMvc.perform(get("/api/notifications").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.unreadCount").value(3));

        // mark all read -> response itself shows the updated list with unreadCount 0
        MvcResult markResult = mockMvc.perform(post("/api/notifications/read").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(0))
                .andExpect(jsonPath("$.notifications.length()").value(3))
                .andReturn();

        // every returned notification now has readAt set
        JsonNode notifs = objectMapper.readTree(markResult.getResponse().getContentAsString()).get("notifications");
        for (JsonNode n : notifs) {
            assertThat(n.get("readAt").isNull()).isFalse();
        }

        // a follow-up GET confirms the read state persisted (not just in the POST response)
        mockMvc.perform(get("/api/notifications").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.unreadCount").value(0))
                .andExpect(jsonPath("$.notifications.length()").value(3));

        // bob's notifications were untouched by alice's mark-read
        assertThat(notificationDao.unreadForUser(BOB_ID)).isEqualTo(1);
    }

    @Test
    @DisplayName("B2: POST /read is idempotent — calling it again is still 200 and stays at 0")
    void markRead_isIdempotent() throws Exception {
        notificationService.notifyMessage(BOB_ID, ALICE_ID, 1L);
        String token = tokenFor(ALICE_ID);

        mockMvc.perform(post("/api/notifications/read").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(0));

        // second call must not error and must not change the count
        mockMvc.perform(post("/api/notifications/read").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(0))
                .andExpect(jsonPath("$.notifications.length()").value(1));
    }

    @Test
    @DisplayName("B3: POST /read with no notifications at all returns 200 with an empty list")
    void markRead_whenEmpty_returnsEmptyList() throws Exception {
        String token = tokenFor(ALICE_ID);

        mockMvc.perform(post("/api/notifications/read").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifications.length()").value(0))
                .andExpect(jsonPath("$.unreadCount").value(0));
    }

    // ------------------------------------------------------------------
    // group C: when two users match, BOTH receive a MATCH notification
    // ------------------------------------------------------------------

    @Test
    @DisplayName("C1: two users match -> both receive a MATCH notification, referenceId is null")
    void match_triggersNotifications_bothSides() throws Exception {
        // at this point alice and bob have not swiped. alice swipes up bob first: no match yet,
        // the listener just records the swipe.
        swipeConsumer.onSwipeMessage(new SwipeMessage(ALICE_ID, BOB_ID, SwipeAction.UP, Instant.now()));

        // both still have zero notifications (no match was detected on alice's swipe)
        assertThat(notificationDao.rows).isEmpty();

        // now bob swipes up alice -> a match is detected -> notifyMatch fires for both
        swipeConsumer.onSwipeMessage(new SwipeMessage(BOB_ID, ALICE_ID, SwipeAction.UP, Instant.now()));

        List<Notification> alice = notificationDao.forUser(ALICE_ID);
        List<Notification> bob = notificationDao.forUser(BOB_ID);

        assertThat(alice).hasSize(1);
        assertThat(bob).hasSize(1);
        assertThat(alice.get(0).getType()).isEqualTo(NotificationType.MATCH);
        assertThat(bob.get(0).getType()).isEqualTo(NotificationType.MATCH);
        // matches table has no integer PK, so referenceId stays null by design
        assertThat(alice.get(0).getReferenceId()).isNull();
        assertThat(bob.get(0).getReferenceId()).isNull();

        // alice and bob see it via the API too
        mockMvc.perform(get("/api/notifications").header("Authorization", "Bearer " + tokenFor(ALICE_ID)))
                .andExpect(jsonPath("$.notifications[0].type").value("MATCH"))
                .andExpect(jsonPath("$.unreadCount").value(1));
        mockMvc.perform(get("/api/notifications").header("Authorization", "Bearer " + tokenFor(BOB_ID)))
                .andExpect(jsonPath("$.notifications[0].type").value("MATCH"))
                .andExpect(jsonPath("$.unreadCount").value(1));
    }

    @Test
    @DisplayName("C2: a non-UP swipe creates no match and no notifications")
    void match_onlyFiresOnUpSwipes() {
        // a DOWN swipe goes through the listener and is ignored before any DAO call
        swipeConsumer.onSwipeMessage(new SwipeMessage(ALICE_ID, BOB_ID, SwipeAction.DOWN, Instant.now()));
        assertThat(matchDao.matches).isEmpty();
        assertThat(notificationDao.rows).isEmpty();
    }

    @Test
    @DisplayName("C3: replaying the same swipe message (JMS redelivery) does not duplicate MATCH notifications")
    void match_isIdempotentUnderReplay() {
        // first round: alice swipes, bob swipes -> 1 match -> 2 notifications
        swipeConsumer.onSwipeMessage(new SwipeMessage(ALICE_ID, BOB_ID, SwipeAction.UP, Instant.now()));
        swipeConsumer.onSwipeMessage(new SwipeMessage(BOB_ID, ALICE_ID, SwipeAction.UP, Instant.now()));
        assertThat(notificationDao.rows).hasSize(2);

        // bob's swipe message is redelivered (e.g. consumer restarted). insertMatch hits the
        // ON CONFLICT guard and returns 0, so no second MATCH is fired.
        swipeConsumer.onSwipeMessage(new SwipeMessage(BOB_ID, ALICE_ID, SwipeAction.UP, Instant.now()));
        assertThat(notificationDao.rows).hasSize(2);

        // alice and bob each still have exactly one MATCH
        assertThat(notificationDao.forUser(ALICE_ID)).hasSize(1);
        assertThat(notificationDao.forUser(BOB_ID)).hasSize(1);
    }

    // ------------------------------------------------------------------
    // group D: when a message is sent, the receiver gets a MESSAGE notification
    // ------------------------------------------------------------------

    @Test
    @DisplayName("D1: POST /api/messages/with/{id} creates a MESSAGE notification for the receiver only")
    void message_triggersReceiverNotification() throws Exception {
        // alice and bob must be matched for the message gate to pass
        matchDao.addMatch(ALICE_ID, BOB_ID);

        String token = tokenFor(ALICE_ID);
        mockMvc.perform(post("/api/messages/with/" + BOB_ID)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body":"hi bob"}
                                """))
                .andExpect(status().isCreated());

        // receiver gets exactly one MESSAGE notification whose referenceId is the message_id
        List<Notification> bob = notificationDao.forUser(BOB_ID);
        assertThat(bob).hasSize(1);
        assertThat(bob.get(0).getType()).isEqualTo(NotificationType.MESSAGE);
        assertThat(bob.get(0).getReferenceId()).isNotNull();

        // sender alice gets nothing
        assertThat(notificationDao.forUser(ALICE_ID)).isEmpty();
    }

    @Test
    @DisplayName("D2: a message between unmatched users is rejected with 403 and creates no notification")
    void message_unmatched_createsNoNotification() throws Exception {
        // deliberately do NOT seed a match here
        String token = tokenFor(ALICE_ID);

        mockMvc.perform(post("/api/messages/with/" + BOB_ID)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body":"should fail"}
                                """))
                .andExpect(status().isForbidden());

        // no message inserted, no notification fired
        assertThat(messageDao.messages).isEmpty();
        assertThat(notificationDao.rows).isEmpty();
    }

    // ------------------------------------------------------------------
    // group E: when someone joins a hangout, organizer + other participants
    //         get a HANGOUT_JOIN notification (the joining user does NOT)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("E1: third user RSVP -> organizer AND existing participants notified, joiner is NOT")
    void hangoutJoin_fansOutToOrganizerAndParticipants() throws Exception {
        // alice organizes a hangout (createHangout seeds her as organizerUserId)
        String aliceToken = tokenFor(ALICE_ID);
        long hangoutId = createHangout(aliceToken, "Park");

        // alice signs up first. She is the organizer AND the joiner, so notifyHangoutJoin's
        // recipient set = {organizer=alice} minus {joining=alice} = empty -> no notification.
        mockMvc.perform(post("/api/hangouts/" + hangoutId + "/signup").header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk());
        assertThat(notificationDao.rows).isEmpty();

        // bob signs up second. Existing participants = [alice]. Organizer = alice. Joining = bob.
        // recipients = {alice (organizer, != joiner)} ∪ {alice (participant, != joiner)} = {alice}.
        // -> alice gets a HANGOUT_JOIN, bob gets nothing.
        String bobToken = tokenFor(BOB_ID);
        mockMvc.perform(post("/api/hangouts/" + hangoutId + "/signup").header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk());

        List<Notification> aliceNotifs = notificationDao.forUser(ALICE_ID);
        assertThat(aliceNotifs).hasSize(1);
        assertThat(aliceNotifs.get(0).getType()).isEqualTo(NotificationType.HANGOUT_JOIN);
        assertThat(aliceNotifs.get(0).getReferenceId()).isEqualTo(hangoutId);
        assertThat(notificationDao.forUser(BOB_ID)).isEmpty();

        // carol signs up third. Existing participants = [alice, bob]. Organizer = alice. Joining = carol.
        // recipients = {alice, bob}. -> alice gets a SECOND HANGOUT_JOIN, bob gets his FIRST,
        // carol gets nothing.
        String carolToken = tokenFor(CAROL_ID);
        mockMvc.perform(post("/api/hangouts/" + hangoutId + "/signup").header("Authorization", "Bearer " + carolToken))
                .andExpect(status().isOk());

        assertThat(notificationDao.forUser(ALICE_ID)).hasSize(2);
        assertThat(notificationDao.forUser(BOB_ID)).hasSize(1);
        assertThat(notificationDao.forUser(CAROL_ID)).isEmpty();
    }

    @Test
    @DisplayName("E2: re-POSTing signup is idempotent — no duplicate HANGOUT_JOIN batch")
    void hangoutJoin_idempotentOnReSignup() throws Exception {
        String aliceToken = tokenFor(ALICE_ID);
        long hangoutId = createHangout(aliceToken, "Group");
        mockMvc.perform(post("/api/hangouts/" + hangoutId + "/signup").header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk());

        String bobToken = tokenFor(BOB_ID);
        mockMvc.perform(post("/api/hangouts/" + hangoutId + "/signup").header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk());
        // alice now has 1 HANGOUT_JOIN from bob's signup
        assertThat(notificationDao.forUser(ALICE_ID)).hasSize(1);

        // bob re-posts signup (button double-click, retry). participantDao.add returns 0, so
        // HangoutService skips the notifyHangoutJoin call entirely.
        mockMvc.perform(post("/api/hangouts/" + hangoutId + "/signup").header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk());

        // alice still has exactly 1 HANGOUT_JOIN, not 2.
        assertThat(notificationDao.forUser(ALICE_ID)).hasSize(1);
    }

    @Test
    @DisplayName("E3: RSVP on a non-existent hangout returns 404 and creates no notification")
    void hangoutJoin_unknownHangout_returns404() throws Exception {
        String token = tokenFor(ALICE_ID);

        mockMvc.perform(post("/api/hangouts/999999/signup").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());

        assertThat(notificationDao.rows).isEmpty();
    }

    // ------------------------------------------------------------------
    // auth guards
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Guard: GET /api/notifications without a token returns 401")
    void getNotifications_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Guard: POST /api/notifications/read without a token returns 401")
    void markRead_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/notifications/read"))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------
    // helpers + fakes
    // ------------------------------------------------------------------

    private void seedUser(long id, String username) {
        User u = new User();
        u.setUserId(id);
        u.setUsername(username);
        userDao.byId.put(id, u);
        userDao.byUsername.put(username, u);
    }

    private String tokenFor(long userId) {
        return sessionService.createSession(userId);
    }

    // create a hangout via the API and return its generated id. Asserts 201 so a setup
    // failure fails the test loudly.
    private long createHangout(String token, String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/hangouts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\",\"latitude\":1.0,\"longitude\":1.0}"))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("hangoutId").asLong();
    }

    // HashMap-backed UserDao. super(new JdbcTemplate()) only satisfies the parent constructor
    // (which builds a SimpleJdbcInsert we never call); every method the service touches is
    // overridden to use the in-memory maps.
    static class FakeUserDao extends UserDao {
        final Map<Long, User> byId = new HashMap<>();
        final Map<String, User> byUsername = new HashMap<>();

        FakeUserDao() {
            super(new JdbcTemplate());
        }

        @Override
        public Optional<User> findById(long userId) {
            return Optional.ofNullable(byId.get(userId));
        }

        @Override
        public Optional<User> findByUsername(String username) {
            return Optional.ofNullable(byUsername.get(username));
        }
    }

    // HashSet-backed SwipeDao. SwipeConsumer only calls insert + existsUpSwipe; we record every
    // swipe in a set and answer the lookup from it. We treat insert as an upsert, matching the
    // real SQL (ON CONFLICT ... DO UPDATE).
    static class FakeSwipeDao extends SwipeDao {
        private static record Row(long senderId, long targetId, SwipeAction action) {}
        private final Set<Row> rows = new HashSet<>();

        FakeSwipeDao() {
            super(new JdbcTemplate());
        }

        @Override
        public int insert(long senderId, long targetId, SwipeAction action) {
            rows.removeIf(r -> r.senderId == senderId && r.targetId == targetId);
            rows.add(new Row(senderId, targetId, action));
            return 1;
        }

        @Override
        public boolean existsUpSwipe(long fromUserId, long toUserId) {
            return rows.stream().anyMatch(r ->
                    r.senderId == fromUserId && r.targetId == toUserId && r.action == SwipeAction.UP);
        }
    }

    // HashSet-backed MatchDao. insertMatch returns 1 the first time a pair is added and 0 on
    // any replay, mirroring the real INSERT...ON CONFLICT DO NOTHING's row count — that flagged
    // return is what SwipeConsumer uses to decide whether to fire MATCH notifications.
    static class FakeMatchDao extends MatchDao {
        private static record Pair(long a, long b) {}
        final Set<Pair> matches = new HashSet<>();

        FakeMatchDao() {
            super(new JdbcTemplate());
        }

        @Override
        public int insertMatch(long userA, long userB) {
            long[] pair = Match.pairInOrder(userA, userB);
            return matches.add(new Pair(pair[0], pair[1])) ? 1 : 0;
        }

        // test seam: record a match without going through the consumer (e.g. for the message gate)
        void addMatch(long userA, long userB) {
            long[] pair = Match.pairInOrder(userA, userB);
            matches.add(new Pair(pair[0], pair[1]));
        }

        @Override
        public boolean existsBetween(long userA, long userB) {
            long[] pair = Match.pairInOrder(userA, userB);
            return matches.contains(new Pair(pair[0], pair[1]));
        }
    }

    // ArrayList-backed MessageDao. Only the methods the message-trigger path touches are
    // overridden; the rest are never called by NotificationApiTest, so they keep the real
    // implementation (which would throw against the empty JdbcTemplate if reached — that's
    // fine, it would surface a coding mistake as a test failure).
    static class FakeMessageDao extends MessageDao {
        final List<ChatMessage> messages = new ArrayList<>();
        private long nextId = 1;

        FakeMessageDao(FakeUserDao userDao) {
            super(new JdbcTemplate());
        }

        @Override
        public long insert(long senderId, long receiverId, String body) {
            ChatMessage m = new ChatMessage();
            m.setMessageId(nextId++);
            m.setSenderId(senderId);
            m.setReceiverId(receiverId);
            m.setBody(body);
            m.setSentAt(Instant.now());
            messages.add(m);
            return m.getMessageId();
        }
    }

    // in-memory HangoutDao. insert mints an id; findById returns a copy of the stored row with
    // the two computed fields (participantCount, isUserSignedUp) filled in from the participant
    // fake — same shape the real SQL aggregate produces, including organizerUserId which
    // HangoutService.signup reads to source the organizer for fan-out.
    static class FakeHangoutDao extends HangoutDao {
        final Map<Long, Hangout> byId = new LinkedHashMap<>();
        private final FakeHangoutParticipantDao participants;
        private long nextId = 1;

        FakeHangoutDao(FakeHangoutParticipantDao participants) {
            super(new JdbcTemplate());
            this.participants = participants;
        }

        @Override
        public long insert(long organizerUserId, String organizerName, String title, String description,
                            double latitude, double longitude, Instant eventTime,
                            HangoutActivityType activityType) {
            long id = nextId++;
            Hangout h = new Hangout();
            h.setHangoutId(id);
            h.setOrganizerUserId(organizerUserId);
            h.setTitle(title);
            h.setDescription(description == null ? "" : description);
            h.setOrganizerName(organizerName);
            h.setLatitude(latitude);
            h.setLongitude(longitude);
            h.setEventTime(eventTime);
            h.setActivityType(activityType);
            h.setCreatedAt(Instant.now());
            h.setParticipantCount(0);
            h.setUserSignedUp(false);
            byId.put(id, h);
            return id;
        }

        @Override
        public List<Hangout> findAll(long userId) {
            return byId.values().stream()
                    .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                    .map(h -> withStats(h, userId))
                    .toList();
        }

        @Override
        public Optional<Hangout> findById(long hangoutId, long userId) {
            Hangout h = byId.get(hangoutId);
            return h == null ? Optional.empty() : Optional.of(withStats(h, userId));
        }

        // return a copy with the two computed fields filled in from the participant fake,
        // mirroring what the SQL aggregate does.
        private Hangout withStats(Hangout source, long userId) {
            Hangout copy = new Hangout();
            copy.setHangoutId(source.getHangoutId());
            copy.setOrganizerUserId(source.getOrganizerUserId());
            copy.setTitle(source.getTitle());
            copy.setDescription(source.getDescription());
            copy.setOrganizerName(source.getOrganizerName());
            copy.setLatitude(source.getLatitude());
            copy.setLongitude(source.getLongitude());
            copy.setEventTime(source.getEventTime());
            copy.setActivityType(source.getActivityType());
            copy.setCreatedAt(source.getCreatedAt());
            copy.setParticipantCount(participants.countByHangout(source.getHangoutId()));
            copy.setUserSignedUp(participants.isParticipant(source.getHangoutId(), userId));
            return copy;
        }
    }

    // HashSet-backed HangoutParticipantDao. add() honors the idempotent ON CONFLICT DO NOTHING
    // semantics so the signup flow can distinguish first signup (returns 1) from a replay (0).
    static class FakeHangoutParticipantDao extends HangoutParticipantDao {
        private static record Pair(long hangoutId, long userId) {}
        private final Set<Pair> rows = new HashSet<>();

        FakeHangoutParticipantDao() {
            super(new JdbcTemplate());
        }

        @Override
        public int add(long hangoutId, long userId) {
            return rows.add(new Pair(hangoutId, userId)) ? 1 : 0;
        }

        @Override
        public int countByHangout(long hangoutId) {
            return (int) rows.stream().filter(p -> p.hangoutId == hangoutId).count();
        }

        @Override
        public boolean isParticipant(long hangoutId, long userId) {
            return rows.contains(new Pair(hangoutId, userId));
        }

        @Override
        public List<Long> findParticipantUserIds(long hangoutId) {
            return rows.stream()
                    .filter(p -> p.hangoutId == hangoutId)
                    .map(p -> p.userId)
                    .toList();
        }
    }

    // Tracking NotificationDao. Unlike the no-op fakes in MessageApiTest / HangoutApiTest, this
    // one actually stores inserted notifications so the tests can assert on counts, types and
    // referenceIds. readAt starts null and gets stamped by markAllRead. findForUser returns
    // newest first (reverse insertion order), matching the real ORDER BY created_at DESC.
    static class FakeNotificationDao extends NotificationDao {
        final List<Notification> rows = new ArrayList<>();
        private long nextId = 1;

        FakeNotificationDao() {
            super(new JdbcTemplate());
        }

        @Override
        public long insert(long userId, NotificationType type, Long referenceId, String title, String body) {
            Notification n = new Notification();
            n.setNotificationId(nextId++);
            n.setUserId(userId);
            n.setType(type);
            n.setReferenceId(referenceId);
            n.setTitle(title);
            n.setBody(body);
            n.setCreatedAt(Instant.now());
            n.setReadAt(null);
            rows.add(n);
            return n.getNotificationId();
        }

        @Override
        public List<Notification> findForUser(long userId) {
            List<Notification> out = new ArrayList<>();
            for (int i = rows.size() - 1; i >= 0; i--) {
                if (rows.get(i).getUserId() == userId) {
                    out.add(rows.get(i));
                }
            }
            return out;
        }

        @Override
        public int countUnread(long userId) {
            return unreadForUser(userId);
        }

        @Override
        public int markAllRead(long userId) {
            int flipped = 0;
            for (Notification n : rows) {
                if (n.getUserId() == userId && n.getReadAt() == null) {
                    n.setReadAt(Instant.now());
                    flipped++;
                }
            }
            return flipped;
        }

        // test-only views
        int unreadForUser(long userId) {
            return (int) rows.stream()
                    .filter(n -> n.getUserId() == userId && n.getReadAt() == null)
                    .count();
        }

        List<Notification> forUser(long userId) {
            return rows.stream().filter(n -> n.getUserId() == userId).toList();
        }
    }
}