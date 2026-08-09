package com.zuzdog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zuzdog.controller.MessageController;
import com.zuzdog.dao.MatchDao;
import com.zuzdog.dao.MessageDao;
import com.zuzdog.dao.NotificationDao;
import com.zuzdog.dao.UserDao;
import com.zuzdog.dto.ConversationSummary;
import com.zuzdog.exception.GlobalExceptionHandler;
import com.zuzdog.model.ChatMessage;
import com.zuzdog.model.Match;
import com.zuzdog.model.Notification;
import com.zuzdog.model.NotificationType;
import com.zuzdog.model.User;
import com.zuzdog.security.AuthenticationFilter;
import com.zuzdog.security.SecurityProperties;
import com.zuzdog.security.SessionService;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Step 2.5 / Commit 5 test for the Message API.
// Same style as HangoutApiTest: no Spring context, no database. The real
// MessageController + MessageService run inside a standalone MockMvc chained
// with the real AuthenticationFilter, but the DAOs are HashMap-backed fakes
// so we never touch Postgres. Real SQL is covered by the *_DaoTest files.
class MessageApiTest {

    private SessionService sessionService;
    private FakeUserDao userDao;
    private FakeMatchDao matchDao;
    private FakeMessageDao messageDao;
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Three seeded users. Alice<->Bob are matched; Carol is not matched with anyone.
    private static final long ALICE_ID = 1L;
    private static final long BOB_ID = 2L;
    private static final long CAROL_ID = 3L;
    private static final String ALICE_NAME = "alice";
    private static final String BOB_NAME = "bob";
    private static final String CAROL_NAME = "carol";

    @BeforeEach
    void setUp() {
        SecurityProperties securityProperties = new SecurityProperties();
        securityProperties.setPepper("test-pepper");
        securityProperties.setSessionTtlMinutes(1440);

        sessionService = new SessionService(securityProperties);
        userDao = new FakeUserDao();
        matchDao = new FakeMatchDao();
        messageDao = new FakeMessageDao(userDao);

        seedUser(ALICE_ID, ALICE_NAME);
        seedUser(BOB_ID, BOB_NAME);
        seedUser(CAROL_ID, CAROL_NAME);

        // the only match in the system: alice <-> bob
        matchDao.addMatch(ALICE_ID, BOB_ID);

        MessageService messageService = new MessageService(messageDao, matchDao,
                new NotificationService(new FakeNotificationDao()));

        mockMvc = MockMvcBuilders
                .standaloneSetup(new MessageController(messageService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(new AuthenticationFilter(sessionService))
                .build();
    }

    // ------------------------------------------------------------------
    // test 1: send a message to a matched user returns 201
    // ------------------------------------------------------------------

    @Test
    @DisplayName("test1: POST /api/messages/with/{id} to a matched user returns 201")
    void sendMessageToMatchedUser_returns201() throws Exception {
        String token = tokenFor(ALICE_ID);

        mockMvc.perform(post("/api/messages/with/" + BOB_ID)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body":"hello bob"}
                                """))
                .andExpect(status().isCreated());

        // the message really was stored
        assertThat(messageDao.messages).hasSize(1);
        ChatMessage stored = messageDao.messages.get(0);
        assertThat(stored.getSenderId()).isEqualTo(ALICE_ID);
        assertThat(stored.getReceiverId()).isEqualTo(BOB_ID);
        assertThat(stored.getBody()).isEqualTo("hello bob");
    }

    @Test
    @DisplayName("test1 guard: blank body returns 400 (validation kicks in before the service runs)")
    void sendMessage_blankBody_returns400() throws Exception {
        String token = tokenFor(ALICE_ID);

        mockMvc.perform(post("/api/messages/with/" + BOB_ID)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body":"   "}
                                """))
                .andExpect(status().isBadRequest());

        // nothing was stored
        assertThat(messageDao.messages).isEmpty();
    }

    @Test
    @DisplayName("test1 guard: missing body field returns 400")
    void sendMessage_missingBody_returns400() throws Exception {
        String token = tokenFor(ALICE_ID);

        mockMvc.perform(post("/api/messages/with/" + BOB_ID)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {}
                                """))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------
    // test 2: the thread returns messages in chronological order
    // ------------------------------------------------------------------

    @Test
    @DisplayName("test2: GET /api/messages/with/{id} returns the thread in chronological order")
    void getThread_returnsMessagesInChronologicalOrder() throws Exception {
        String token = tokenFor(ALICE_ID);

        // alice sends first, bob replies, alice sends again. We insert them in a
        // deliberately non-chronological order to prove the thread sorts by sent_at.
        messageDao.insertManual(ALICE_ID, BOB_ID, "first",  Instant.parse("2026-01-01T10:00:00Z"));
        messageDao.insertManual(BOB_ID, ALICE_ID, "second", Instant.parse("2026-01-01T11:00:00Z"));
        messageDao.insertManual(ALICE_ID, BOB_ID, "third",  Instant.parse("2026-01-01T12:00:00Z"));

        MvcResult result = mockMvc.perform(get("/api/messages/with/" + BOB_ID)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode arr = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(arr.isArray()).isTrue();
        assertThat(arr).hasSize(3);
        // chronological: first, second, third is ascending if input was; verify stored order.
        // We inserted first(10:00) second(11:00) third(12:00) — if input is inserted out of insertion order we still expect sorted.
        assertThat(arr.get(0).get("body").asText()).isEqualTo("first");
        assertThat(arr.get(1).get("body").asText()).isEqualTo("second");
        assertThat(arr.get(2).get("body").asText()).isEqualTo("third");
        assertThat(arr.get(0).get("senderId").asLong()).isEqualTo(ALICE_ID);
        assertThat(arr.get(1).get("senderId").asLong()).isEqualTo(BOB_ID);
    }

    @Test
    @DisplayName("test2 guard: thread is sorted by sent_at ASC even when inserted out of order")
    void getThread_sortedBySentAtAsc() throws Exception {
        String token = tokenFor(ALICE_ID);

        // deliberately insert the latest one first
        messageDao.insertManual(ALICE_ID, BOB_ID, "late",  Instant.parse("2026-01-01T15:00:00Z"));
        messageDao.insertManual(ALICE_ID, BOB_ID, "early", Instant.parse("2026-01-01T08:00:00Z"));
        messageDao.insertManual(ALICE_ID, BOB_ID, "mid",   Instant.parse("2026-01-01T11:00:00Z"));

        MvcResult result = mockMvc.perform(get("/api/messages/with/" + BOB_ID)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode arr = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(arr.get(0).get("body").asText()).isEqualTo("early");
        assertThat(arr.get(1).get("body").asText()).isEqualTo("mid");
        assertThat(arr.get(2).get("body").asText()).isEqualTo("late");
    }

    @Test
    @DisplayName("test2 guard: an empty thread returns 200 with an empty array")
    void getThread_empty_returnsEmptyArray() throws Exception {
        String token = tokenFor(ALICE_ID);

        mockMvc.perform(get("/api/messages/with/" + BOB_ID)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ------------------------------------------------------------------
    // test 3: conversations list returns one summary per partner
    // ------------------------------------------------------------------

    @Test
    @DisplayName("test3: GET /api/messages/conversations returns a summary per partner with last message + unread count")
    void getConversations_returnsSummaryPerPartner() throws Exception {
        String token = tokenFor(ALICE_ID);

        // alice <-> bob exchange; alice also receives one from a direction
        messageDao.insertManual(ALICE_ID, BOB_ID, "a1", Instant.parse("2026-01-01T10:00:00Z"));
        messageDao.insertManual(BOB_ID, ALICE_ID, "b1", Instant.parse("2026-01-01T11:00:00Z"));
        messageDao.insertManual(ALICE_ID, BOB_ID, "a2", Instant.parse("2026-01-01T12:00:00Z"));

        MvcResult result = mockMvc.perform(get("/api/messages/conversations")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode arr = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(arr).hasSize(1);
        JsonNode summary = arr.get(0);
        assertThat(summary.get("otherUserId").asLong()).isEqualTo(BOB_ID);
        assertThat(summary.get("otherUsername").asText()).isEqualTo(BOB_NAME);
        // last message is the newest by sent_at -> "a2"
        assertThat(summary.get("lastMessage").asText()).isEqualTo("a2");
        // unread proxy: count of messages alice received from bob = 1 ("b1")
        assertThat(summary.get("unreadCount").asInt()).isEqualTo(1);
    }

    @Test
    @DisplayName("test3 guard: conversations ordered by most recent first")
    void getConversations_orderedByMostRecentFirst() throws Exception {
        String token = tokenFor(ALICE_ID);

        // two partners: bob (older) and carol -> but carol is not matched, so
        // we still allow the dao to store messages; the summaries endpoint is
        // just a read over the messages table, the match gate only applies to SEND.
        messageDao.insertManual(ALICE_ID, BOB_ID, "to-bob-old", Instant.parse("2026-01-01T10:00:00Z"));
        messageDao.insertManual(ALICE_ID, CAROL_ID, "to-carol-new", Instant.parse("2026-01-02T10:00:00Z"));

        MvcResult result = mockMvc.perform(get("/api/messages/conversations")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode arr = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(arr).hasSize(2);
        // carol's thread is newer -> comes first
        assertThat(arr.get(0).get("otherUserId").asLong()).isEqualTo(CAROL_ID);
        assertThat(arr.get(1).get("otherUserId").asLong()).isEqualTo(BOB_ID);
    }

    @Test
    @DisplayName("test3 guard: no messages -> empty conversations list")
    void getConversations_noMessages_returnsEmpty() throws Exception {
        String token = tokenFor(ALICE_ID);

        mockMvc.perform(get("/api/messages/conversations")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ------------------------------------------------------------------
    // test 4: send a message to a non-matched user returns 403
    // ------------------------------------------------------------------

    @Test
    @DisplayName("test4: POST to a non-matched user returns 403")
    void sendMessageToNonMatchedUser_returns403() throws Exception {
        String token = tokenFor(ALICE_ID);

        // alice is matched with bob, NOT with carol (id 3)
        mockMvc.perform(post("/api/messages/with/" + CAROL_ID)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body":"hi carol"}
                                """))
                .andExpect(status().isForbidden());

        // nothing was stored — the gate rejected before insert
        assertThat(messageDao.messages).isEmpty();
    }

    @Test
    @DisplayName("test4 guard: the 403 carries the messaging-not-allowed message")
    void sendMessageToNonMatchedUser_carriesForbiddenMessage() throws Exception {
        String token = tokenFor(ALICE_ID);

        MvcResult result = mockMvc.perform(post("/api/messages/with/" + CAROL_ID)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body":"hi"}
                                """))
                .andExpect(status().isForbidden())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("Messaging not allowed");
    }

    @Test
    @DisplayName("test4 guard: a matched user can still send after the first 403 to someone else")
    void sendMessage_matchedStillWorksAfterAFourOFour() throws Exception {
        String token = tokenFor(ALICE_ID);

        // first attempt to carol -> 403
        mockMvc.perform(post("/api/messages/with/" + CAROL_ID)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body":"nope"}
                                """))
                .andExpect(status().isForbidden());

        // then to bob -> 201, the gate still works for the matched pair
        mockMvc.perform(post("/api/messages/with/" + BOB_ID)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body":"yes"}
                                """))
                .andExpect(status().isCreated());

        assertThat(messageDao.messages).hasSize(1);
        assertThat(messageDao.messages.get(0).getBody()).isEqualTo("yes");
    }

    // ------------------------------------------------------------------
    // auth guards: every /api/messages endpoint requires a Bearer token
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Guard: GET /api/messages/conversations without a token returns 401")
    void getConversations_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/messages/conversations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Guard: GET /api/messages/with/{id} without a token returns 401")
    void getThread_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/messages/with/" + BOB_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Guard: POST /api/messages/with/{id} without a token returns 401")
    void sendMessage_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/messages/with/" + BOB_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body":"x"}
                                """))
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

    // issue a real session token for the given user — the AuthenticationFilter
    // resolves it and sets authenticatedUserId on the request, exactly as in prod.
    private String tokenFor(long userId) {
        return sessionService.createSession(userId);
    }

    // HashMap-backed UserDao. super(new JdbcTemplate()) only satisfies the parent
    // constructor (which builds a SimpleJdbcInsert we never call); every method the
    // service actually touches is overridden to use the in-memory maps.
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

    // HashSet-backed MatchDao. existsBetween is the only method the message gate
    // uses; addMatch is a test-only seam. Pairs are stored in canonical order via
    // Match.pairInOrder so the lookup is symmetric (alice<->bob == bob<->alice).
    static class FakeMatchDao extends MatchDao {
        private static record Pair(long a, long b) {}
        private final Set<Pair> matches = new HashSet<>();

        FakeMatchDao() {
            super(new JdbcTemplate());
        }

        // test-only seam: record a match between two users (order-independent).
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

    // ArrayList-backed MessageDao. insert() assigns a generated id and a real
    // Instant.now(); insertManual() is a test seam that lets a test pin the
    // sent_at so we can assert chronological ordering deterministically.
    // findThread / findConversationSummaries mirror what the real SQL does:
    // thread filters by the two users and sorts ASC by sent_at; summaries group
    // by partner, pick the latest message, and count received messages as the
    // unread proxy.
    static class FakeMessageDao extends MessageDao {
        final List<ChatMessage> messages = new ArrayList<>();
        private final FakeUserDao userDao;
        private long nextId = 1;

        FakeMessageDao(FakeUserDao userDao) {
            super(new JdbcTemplate());
            this.userDao = userDao;
        }

        @Override
        public long insert(long senderId, long receiverId, String body) {
            return insertManual(senderId, receiverId, body, Instant.now());
        }

        // test seam: insert with a pinned sent_at. Returns the generated message id.
        long insertManual(long senderId, long receiverId, String body, Instant sentAt) {
            ChatMessage m = new ChatMessage();
            m.setMessageId(nextId++);
            m.setSenderId(senderId);
            m.setReceiverId(receiverId);
            m.setBody(body);
            m.setSentAt(sentAt);
            messages.add(m);
            return m.getMessageId();
        }

        @Override
        public List<ChatMessage> findThread(long userA, long userB) {
            return messages.stream()
                    .filter(m -> (m.getSenderId() == userA && m.getReceiverId() == userB)
                            || (m.getSenderId() == userB && m.getReceiverId() == userA))
                    .sorted(Comparator.comparing(ChatMessage::getSentAt))
                    .toList();
        }

        @Override
        public List<ConversationSummary> findConversationSummaries(long userId) {
            // group this user's messages by the partner id
            Map<Long, List<ChatMessage>> byPartner = new HashMap<>();
            for (ChatMessage m : messages) {
                if (m.getSenderId() != userId && m.getReceiverId() != userId) {
                    continue;
                }
                long partner = m.getSenderId() == userId ? m.getReceiverId() : m.getSenderId();
                byPartner.computeIfAbsent(partner, k -> new ArrayList<>()).add(m);
            }

            // build one summary per partner
            List<ConversationSummary> summaries = new ArrayList<>();
            for (Map.Entry<Long, List<ChatMessage>> e : byPartner.entrySet()) {
                long partner = e.getKey();
                List<ChatMessage> thread = e.getValue();
                // latest message by sent_at
                ChatMessage latest = thread.stream()
                        .max(Comparator.comparing(ChatMessage::getSentAt))
                        .orElseThrow();
                String username = userDao.findById(partner)
                        .map(User::getUsername)
                        .orElse("");
                // unread proxy: messages received from this partner
                int unread = (int) thread.stream()
                        .filter(m -> m.getReceiverId() == userId)
                        .count();
                summaries.add(new ConversationSummary(
                        partner, username, latest.getBody(), latest.getSentAt(), unread));
            }
            // most recent first
            summaries.sort(Comparator.comparing(ConversationSummary::lastMessageTime).reversed());
            return summaries;
        }

        @Override
        public List<ChatMessage> findConversations(long userId) {
            // kept for completeness; the controller no longer calls this path,
            // but the service still exposes getConversations -> findConversations.
            return messages.stream()
                    .filter(m -> m.getSenderId() == userId || m.getReceiverId() == userId)
                    .toList();
        }
    }

    // No-op NotificationDao. MessageService now fires a MESSAGE notification on
    // every send (DoD step 2.6), so the service-under-test needs a NotificationService
    // wired in. We never assert on the inserted notifications in these message-flow
    // tests (those assertions belong to a dedicated notifications test), so every
    // method is a harmless stub: insert returns a fake incrementing id, the reads
    // return empty, markAllRead does nothing. Building it with super(new JdbcTemplate())
    // satisfies the parent constructor without ever touching a real DB, exactly like
    // the other fakes in this file.
    static class FakeNotificationDao extends NotificationDao {
        private long nextId = 1;

        FakeNotificationDao() {
            super(new JdbcTemplate());
        }

        @Override
        public long insert(long userId, NotificationType type, Long referenceId, String title, String body) {
            return nextId++;
        }

        @Override
        public List<Notification> findForUser(long userId) {
            return List.of();
        }

        @Override
        public int countUnread(long userId) {
            return 0;
        }

        @Override
        public int markAllRead(long userId) {
            return 0;
        }
    }
}