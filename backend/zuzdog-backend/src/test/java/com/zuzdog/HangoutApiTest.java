package com.zuzdog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zuzdog.controller.HangoutController;
import com.zuzdog.dao.HangoutDao;
import com.zuzdog.dao.HangoutParticipantDao;
import com.zuzdog.dao.NotificationDao;
import com.zuzdog.dao.UserDao;
import com.zuzdog.exception.GlobalExceptionHandler;
import com.zuzdog.model.Hangout;
import com.zuzdog.model.HangoutActivityType;
import com.zuzdog.model.Notification;
import com.zuzdog.model.NotificationType;
import com.zuzdog.model.User;
import com.zuzdog.security.AuthenticationFilter;
import com.zuzdog.security.SecurityProperties;
import com.zuzdog.security.SessionService;
import com.zuzdog.service.HangoutService;
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


class HangoutApiTest {

    private SessionService sessionService;
    private FakeUserDao userDao;
    private FakeHangoutDao hangoutDao;
    private FakeHangoutParticipantDao participantDao;
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // The authenticated user. Seeded into userDao before each test so the
    // service can look up the organizerName. id=1, username="alice".
    private static final long ALICE_ID = 1L;
    private static final String ALICE_NAME = "alice";

    @BeforeEach
    void setUp() {
        SecurityProperties securityProperties = new SecurityProperties();
        securityProperties.setPepper("test-pepper");
        securityProperties.setSessionTtlMinutes(1440);

        sessionService = new SessionService(securityProperties);
        userDao = new FakeUserDao();
        participantDao = new FakeHangoutParticipantDao();
        hangoutDao = new FakeHangoutDao(participantDao);

        // seed the one user the authenticated token resolves to.
        User alice = new User();
        alice.setUserId(ALICE_ID);
        alice.setUsername(ALICE_NAME);
        userDao.byId.put(ALICE_ID, alice);
        userDao.byUsername.put(ALICE_NAME, alice);

        HangoutService hangoutService = new HangoutService(hangoutDao, participantDao, userDao,
                new NotificationService(new FakeNotificationDao()));

        mockMvc = MockMvcBuilders
                .standaloneSetup(new HangoutController(hangoutService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(new AuthenticationFilter(sessionService))
                .build();
    }


    @Test
    @DisplayName("test1: POST /api/hangouts returns 201 and the hangout appears in GET /api/hangouts")
    void createHangout_returns201_andAppearsInList() throws Exception {
        String token = tokenFor(ALICE_ID);

        MvcResult created = mockMvc.perform(post("/api/hangouts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Park Meetup","latitude":32.08,"longitude":34.78}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hangoutId").isNumber())
                .andExpect(jsonPath("$.organizerUserId").value(ALICE_ID))
                .andExpect(jsonPath("$.title").value("Park Meetup"))
                .andExpect(jsonPath("$.organizerName").value(ALICE_NAME))
                .andExpect(jsonPath("$.latitude").value(32.08))
                .andExpect(jsonPath("$.longitude").value(34.78))
                .andExpect(jsonPath("$.activityType").value("MEETUP"))
                .andExpect(jsonPath("$.participantCount").value(0))
                .andExpect(jsonPath("$.isUserSignedUp").value(false))
                .andReturn();

        long hangoutId = objectMapper.readTree(created.getResponse().getContentAsString()).get("hangoutId").asLong();

        // GET /api/hangouts lists it back with the same stats.
        mockMvc.perform(get("/api/hangouts").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].hangoutId").value(hangoutId))
                .andExpect(jsonPath("$[0].title").value("Park Meetup"))
                .andExpect(jsonPath("$[0].participantCount").value(0))
                .andExpect(jsonPath("$[0].isUserSignedUp").value(false));
    }

    @Test
    @DisplayName("test1 guard: organizerName comes from the user's row, not the request body")
    void createHangout_organizerNameFromUserRow_notFromRequest() throws Exception {
        String token = tokenFor(ALICE_ID);

        // The request body has NO organizerName field (the DTO does not declare one),
        // so the only source the service can use is UserDao.findById -> username.
        mockMvc.perform(post("/api/hangouts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Picnic","latitude":0.0,"longitude":0.0,"description":"hello"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.organizerName").value(ALICE_NAME));
    }

    @Test
    @DisplayName("test1 guard: blank title returns 400 (validation kicks in before the service runs)")
    void createHangout_blankTitle_returns400() throws Exception {
        String token = tokenFor(ALICE_ID);

        mockMvc.perform(post("/api/hangouts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"   ","latitude":1.0,"longitude":1.0}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("test1 guard: missing latitude returns 400")
    void createHangout_missingLatitude_returns400() throws Exception {
        String token = tokenFor(ALICE_ID);

        mockMvc.perform(post("/api/hangouts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"NoCoords","longitude":1.0}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("test1 guard: unknown activityType returns 400")
    void createHangout_invalidActivityType_returns400() throws Exception {
        String token = tokenFor(ALICE_ID);

        mockMvc.perform(post("/api/hangouts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"X","latitude":1.0,"longitude":1.0,"activityType":"NOT_A_REAL_TYPE"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("test1 guard: activityType defaults to MEETUP when omitted")
    void createHangout_defaultsActivityTypeToMeetup() throws Exception {
        String token = tokenFor(ALICE_ID);

        mockMvc.perform(post("/api/hangouts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"DefaultType","latitude":1.0,"longitude":1.0}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.activityType").value("MEETUP"));
    }

    @Test
    @DisplayName("test2: POST signup flips isUserSignedUp to true and bumps participantCount to 1")
    void signup_flipsSignedUp_andBumpsCount() throws Exception {
        String token = tokenFor(ALICE_ID);
        long hangoutId = createHangout(token, "H1");

        // before signup: count 0, not signed up
        mockMvc.perform(get("/api/hangouts").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$[0].participantCount").value(0))
                .andExpect(jsonPath("$[0].isUserSignedUp").value(false));

        // signup as alice
        mockMvc.perform(post("/api/hangouts/" + hangoutId + "/signup")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hangoutId").value(hangoutId))
                .andExpect(jsonPath("$.participantCount").value(1))
                .andExpect(jsonPath("$.isUserSignedUp").value(true));

        // reflected in the list too
        mockMvc.perform(get("/api/hangouts").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$[0].participantCount").value(1))
                .andExpect(jsonPath("$[0].isUserSignedUp").value(true));
    }

    @Test
    @DisplayName("test2: a second user signing up bumps participantCount to 2, alice still signed up")
    void secondUserSignup_bumpsCountToTwo() throws Exception {
        String aliceToken = tokenFor(ALICE_ID);
        long hangoutId = createHangout(aliceToken, "Group");

        // alice signs up
        mockMvc.perform(post("/api/hangouts/" + hangoutId + "/signup")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(jsonPath("$.participantCount").value(1));

        // seed a second user and sign up as them
        long bobId = 2L;
        User bob = new User();
        bob.setUserId(bobId);
        bob.setUsername("bob");
        userDao.byId.put(bobId, bob);
        userDao.byUsername.put("bob", bob);
        String bobToken = tokenFor(bobId);

        mockMvc.perform(post("/api/hangouts/" + hangoutId + "/signup")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participantCount").value(2))
                .andExpect(jsonPath("$.isUserSignedUp").value(true));

        // alice's view still shows her as signed up, but count is now 2
        mockMvc.perform(get("/api/hangouts").header("Authorization", "Bearer " + aliceToken))
                .andExpect(jsonPath("$[0].participantCount").value(2))
                .andExpect(jsonPath("$[0].isUserSignedUp").value(true));
    }

    @Test
    @DisplayName("test2 guard: signing up twice is idempotent (count stays 1, no 4xx/5xx)")
    void signup_isIdempotent() throws Exception {
        String token = tokenFor(ALICE_ID);
        long hangoutId = createHangout(token, "Idempotent");

        mockMvc.perform(post("/api/hangouts/" + hangoutId + "/signup")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.participantCount").value(1));

        // second signup -> still 1, no error
        mockMvc.perform(post("/api/hangouts/" + hangoutId + "/signup")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participantCount").value(1))
                .andExpect(jsonPath("$.isUserSignedUp").value(true));

        // only one participant row was actually stored
        assertThat(participantDao.signupsFor(hangoutId)).hasSize(1);
    }

    @Test
    @DisplayName("test2 guard: signup on a non-existent hangout returns 404")
    void signup_unknownHangout_returns404() throws Exception {
        String token = tokenFor(ALICE_ID);

        mockMvc.perform(post("/api/hangouts/999999/signup")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------
    // Auth guards: every /api/hangouts endpoint requires a Bearer token
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Guard: GET /api/hangouts without a token returns 401")
    void getHangouts_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/hangouts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Guard: POST /api/hangouts without a token returns 401")
    void createHangout_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/hangouts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"X","latitude":1.0,"longitude":1.0}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Guard: POST signup without a token returns 401")
    void signup_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/hangouts/1/signup"))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------
    // helpers + fakes
    // ------------------------------------------------------------------

    // issue a real session token for the given user — the AuthenticationFilter
    // resolves it and sets authenticatedUserId on the request, exactly as in prod.
    private String tokenFor(long userId) {
        return sessionService.createSession(userId);
    }

    // create a hangout via the API and return its generated id. Asserts 201 so a
    // setup failure fails the test loudly instead of silently cascading.
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

    // HashMap-backed UserDao. super(new JdbcTemplate()) just satisfies the parent
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

    // HashMap-backed HangoutDao. Stores rows in insertion order so findAll returns
    // newest-first (we reverse on read, matching the DAO's ORDER BY created_at DESC).
    // findAll / findById fill the same two computed fields (participantCount,
    // isUserSignedUp) that the real SQL aggregate produces.
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
            // newest first, matching the real DAO's ORDER BY created_at DESC
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

        // return a copy with the two computed fields filled in from the
        // participant fake, mirroring what the SQL aggregate does.
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
            copy.setParticipantCount(participantRowCount(source.getHangoutId()));
            copy.setUserSignedUp(participantIsSignedUp(source.getHangoutId(), userId));
            return copy;
        }

        private int participantRowCount(long hangoutId) {
            return participants.countByHangout(hangoutId);
        }

        private boolean participantIsSignedUp(long hangoutId, long userId) {
            return participants.isParticipant(hangoutId, userId);
        }
    }

    // HashSet-backed HangoutParticipantDao. add() honors the idempotent ON CONFLICT
    // DO NOTHING semantics — a duplicate (hangoutId, userId) is a no-op.
    static class FakeHangoutParticipantDao extends HangoutParticipantDao {
        private static record Pair(long hangoutId, long userId) {}
        private final Set<Pair> rows = new HashSet<>();

        FakeHangoutParticipantDao() {
            super(new JdbcTemplate());
        }

        @Override
        public int add(long hangoutId, long userId) {
            Pair p = new Pair(hangoutId, userId);
            return rows.add(p) ? 1 : 0; // matches ON CONFLICT DO NOTHING
        }

        @Override
        public int countByHangout(long hangoutId) {
            return (int) rows.stream().filter(p -> p.hangoutId == hangoutId).count();
        }

        @Override
        public boolean isParticipant(long hangoutId, long userId) {
            return rows.contains(new Pair(hangoutId, userId));
        }

        // the new signup flow calls findParticipantUserIds to fan out HANGOUT_JOIN
        // notifications. Without this override the call falls through to the real
        // HangoutParticipantDao, which runs SQL against the empty JdbcTemplate
        // passed to super() and throws. We return the userIds of every stored pair
        // for this hangout, in insertion order (LinkedHashSet preserves it in the
        // records set; we sort by stream order here).
        @Override
        public List<Long> findParticipantUserIds(long hangoutId) {
            return rows.stream()
                    .filter(p -> p.hangoutId == hangoutId)
                    .map(p -> p.userId)
                    .toList();
        }

        // test-only view: every (hangoutId, userId) pair for a given hangout.
        List<Pair> signupsFor(long hangoutId) {
            return rows.stream().filter(p -> p.hangoutId == hangoutId).toList();
        }
    }

    // No-op NotificationDao. HangoutService.signup now fires HANGOUT_JOIN
    // notifications on every first RSVP, so the service-under-test needs a
    // NotificationService wired in. These hangout-flow tests do not assert on
    // the created notifications (those live in a future notifications test), so
    // every method is a harmless stub: insert returns a fake incrementing id,
    // the reads return empty, markAllRead does nothing. super(new JdbcTemplate())
    // satisfies the parent constructor without ever touching a real DB, matching
    // the pattern used by every other fake in this file.
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