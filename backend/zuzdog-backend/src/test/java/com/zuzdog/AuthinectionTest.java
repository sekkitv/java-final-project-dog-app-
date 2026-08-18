package com.zuzdog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zuzdog.controller.AuthController;
import com.zuzdog.controller.ProfileController;
import com.zuzdog.dao.DogDao;
import com.zuzdog.dao.UserDao;
import com.zuzdog.exception.GlobalExceptionHandler;
import com.zuzdog.model.Dog;
import com.zuzdog.model.User;
import com.zuzdog.security.AuthenticationFilter;
import com.zuzdog.security.PasswordHasher;
import com.zuzdog.security.SecurityProperties;
import com.zuzdog.security.SessionService;
import com.zuzdog.service.AuthService;
import com.zuzdog.service.ProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Step 1.5 test. Same style as Step14SecurityTest: no Spring context, no database.
 * The real services are wired to fake DAOs that store rows in HashMaps, and the
 * controllers run inside a standalone MockMvc with the real AuthenticationFilter.
 * Real SQL is already covered by UserDaoTest and DogDaoTest.
 */
class AuthinectionTest {

    private SessionService sessionService;
    private FakeUserDao userDao;
    private FakeDogDao dogDao;
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        SecurityProperties securityProperties = new SecurityProperties();
        securityProperties.setPepper("test-pepper");
        securityProperties.setSessionTtlMinutes(1440);

        PasswordHasher passwordHasher = new PasswordHasher(securityProperties);
        sessionService = new SessionService(securityProperties);
        userDao = new FakeUserDao();
        dogDao = new FakeDogDao();

        AuthService authService = new AuthService(userDao, dogDao, passwordHasher, sessionService);
        ProfileService profileService = new ProfileService(userDao, dogDao, null);

        mockMvc = MockMvcBuilders
                .standaloneSetup(new AuthController(authService), new ProfileController(profileService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(new AuthenticationFilter(sessionService))
                .build();
    }

    // ------------------------------------------------------------------
    // DoD 1: register creates a user and returns {token, userId, username}
    // ------------------------------------------------------------------

    @Test
    @DisplayName("DoD1: register returns 201 with token, userId, username and stores a default dog")
    void register_returnsTokenUserIdUsername() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"secret123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.username").value("alice"));

        User stored = userDao.findByUsername("alice").orElseThrow();
        assertThat(stored.getPasswordHash()).isNotBlank();
        assertThat(dogDao.findPrimaryByUserId(stored.getUserId())).isPresent();
    }

    @Test
    @DisplayName("DoD1 guard: register response never contains password data")
    void register_responseHasNoPasswordFields() throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"bob\",\"password\":\"secret123\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain("secret123", "password", "passwordHash", "salt", "hash");
    }

    // ------------------------------------------------------------------
    // DoD 2: re-registering the same username returns 409
    // ------------------------------------------------------------------

    @Test
    @DisplayName("DoD2: registering the same username twice returns 409")
    void register_duplicateUsername_returns409() throws Exception {
        register("carol", "secret123");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"carol\",\"password\":\"otherPass1\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Username already taken"));
    }

    // ------------------------------------------------------------------
    // DoD 3: login with correct credentials returns a token
    // ------------------------------------------------------------------

    @Test
    @DisplayName("DoD3: login with correct credentials returns 200 with a token")
    void login_correctCredentials_returnsToken() throws Exception {
        register("dave", "secret123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"dave\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value("dave"));
    }

    @Test
    @DisplayName("Guard: wrong password and unknown user both return 401 with the same message")
    void login_badCredentials_sameGeneric401() throws Exception {
        register("erin", "secret123");

        MvcResult wrongPassword = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"erin\",\"password\":\"wrongPass1\"}"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        MvcResult unknownUser = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ghost\",\"password\":\"secret123\"}"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        // identical bodies, so an attacker cannot tell which part failed
        assertThat(wrongPassword.getResponse().getContentAsString())
                .isEqualTo(unknownUser.getResponse().getContentAsString());
    }

    @Test
    @DisplayName("Guard: login with lat/lng updates the stored location")
    void login_withLocation_updatesLocation() throws Exception {
        register("frank", "secret123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"frank\",\"password\":\"secret123\",\"lat\":32.08,\"lng\":34.78}"))
                .andExpect(status().isOk());

        User stored = userDao.findByUsername("frank").orElseThrow();
        assertThat(stored.getLat()).isEqualTo(32.08);
        assertThat(stored.getLng()).isEqualTo(34.78);
    }

    // ------------------------------------------------------------------
    // DoD 4 + 5: GET /api/profile with a Bearer token returns the profile
    // ------------------------------------------------------------------

    @Test
    @DisplayName("DoD4+5: profile with Bearer token returns owner and dog data without password fields")
    void profile_withToken_returnsProfileWithoutPassword() throws Exception {
        String token = register("gina", "secret123");

        MvcResult result = mockMvc.perform(get("/api/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("gina"))
                .andExpect(jsonPath("$.dogName").value("My Dog"))
                .andExpect(jsonPath("$.maxDistance").value(25.0))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain("secret123", "password", "passwordHash", "salt", "hash");
    }

    @Test
    @DisplayName("Guard: profile without a token returns 401")
    void profile_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Guard: profile with a garbage token returns 401")
    void profile_withInvalidToken_returns401() throws Exception {
        mockMvc.perform(get("/api/profile")
                        .header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Guard: blank username is rejected with 400 before the service runs")
    void register_blankUsername_returns400() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"secret123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("username must not be blank"));
    }

    // ------------------------------------------------------------------
    // helpers + fakes
    // ------------------------------------------------------------------

    private String register(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("token").asText();
    }

    // Fake DAOs extend the real ones and override every method the services call.
    // super(new JdbcTemplate()) is never used, it only satisfies the parent constructor.
    static class FakeUserDao extends UserDao {
        private final Map<Long, User> byId = new HashMap<>();
        private final Map<String, User> byUsername = new HashMap<>();
        private long nextId = 1;

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

        @Override
        public long insert(User user) {
            long id = nextId++;
            user.setUserId(id);
            byId.put(id, user);
            byUsername.put(user.getUsername(), user);
            return id;
        }

        @Override
        public int updateLocation(long userId, Double lat, Double lng) {
            User user = byId.get(userId);
            if (user == null) {
                return 0;
            }
            user.setLat(lat);
            user.setLng(lng);
            return 1;
        }
    }

    static class FakeDogDao extends DogDao {
        private final Map<Long, List<Dog>> byUserId = new HashMap<>();
        private long nextId = 1;

        FakeDogDao() {
            super(new JdbcTemplate());
        }

        @Override
        public Optional<Dog> findPrimaryByUserId(long userId) {
            List<Dog> dogs = byUserId.get(userId);
            return dogs == null || dogs.isEmpty() ? Optional.empty() : Optional.of(dogs.get(0));
        }

        @Override
        public long insert(Dog dog) {
            long id = nextId++;
            dog.setDogId(id);
            byUserId.computeIfAbsent(dog.getUserId(), k -> new ArrayList<>()).add(dog);
            return id;
        }
    }
}
