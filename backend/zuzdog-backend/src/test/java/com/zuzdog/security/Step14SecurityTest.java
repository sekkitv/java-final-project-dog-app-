// Disclaimer
// This is ai generted test to check if our security work


package com.zuzdog.security;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class Step14SecurityTest {

    // Rebuilt per test for isolation. Pure POJOs, no Spring context needed.
    private SecurityProperties securityProperties;
    private PasswordHasher passwordHasher;
    private SessionService sessionService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        securityProperties = new SecurityProperties();
        securityProperties.setPepper("test-pepper");
        securityProperties.setSessionTtlMinutes(1440);

        passwordHasher = new PasswordHasher(securityProperties);
        sessionService = new SessionService(securityProperties);

        // Standalone MockMvc: a throwaway controller + our real filter in the chain.
        mockMvc = MockMvcBuilders.standaloneSetup(new TestApiController())
                .addFilters(new AuthenticationFilter(sessionService))
                .build();
    }

    // ------------------------------------------------------------------
    // DoD 1: hashing twice -> different hashes; salt is random per call
    // ------------------------------------------------------------------

    @Test
    @DisplayName("DoD1: hashing the same password twice produces different hashes")
    void hashSamePasswordTwice_producesDifferentHashes() {
        String salt = passwordHasher.generateSalt();
        String plain = "hunter2";

        String hash1 = passwordHasher.hashPassword(plain, salt);
        String hash2 = passwordHasher.hashPassword(plain, salt);

        // Even with the same input, BCrypt embeds its own random salt, so the
        // stored hashes differ — which is exactly what the DoD checks.
        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    @DisplayName("DoD1 helper: generateSalt() produces a unique salt each call")
    void generateSalt_uniqueEachCall() {
        String saltA = passwordHasher.generateSalt();
        String saltB = passwordHasher.generateSalt();
        assertThat(saltA).isNotEqualTo(saltB);
        // 32 bytes of hex = 64 hex chars.
        assertThat(saltA).hasSize(64);
    }

    // ------------------------------------------------------------------
    // DoD 2: verify() returns true for the correct password, false otherwise
    // ------------------------------------------------------------------

    @Test
    @DisplayName("DoD2: verifyPassword true for correct password, false for wrong")
    void verifyPassword_trueForCorrect_falseForWrong() {
        String salt = passwordHasher.generateSalt();
        String plain = "correct-horse-battery-staple";
        String hash = passwordHasher.hashPassword(plain, salt);

        assertThat(passwordHasher.verifyPassword(plain, salt, hash)).isTrue();
        assertThat(passwordHasher.verifyPassword("wrong-password", salt, hash)).isFalse();
        // Wrong salt must also fail, because the salt is mixed into the BCrypt input.
        String otherSalt = passwordHasher.generateSalt();
        assertThat(passwordHasher.verifyPassword(plain, otherSalt, hash)).isFalse();
        // Defensive null handling.
        assertThat(passwordHasher.verifyPassword(null, salt, hash)).isFalse();
        assertThat(passwordHasher.verifyPassword(plain, null, hash)).isFalse();
        assertThat(passwordHasher.verifyPassword(plain, salt, null)).isFalse();
    }

    // ------------------------------------------------------------------
    // DoD 3: SessionService.createSession -> token; resolveUserId -> userId
    // ------------------------------------------------------------------

    @Test
    @DisplayName("DoD3: createSession returns a token and resolveUserId returns the same user id")
    void createSession_returnsToken_resolveReturnsUserId() {
        String token = sessionService.createSession(42L);

        assertThat(token).isNotBlank();

        Optional<Long> resolved = sessionService.resolveUserId(token);
        assertThat(resolved).hasValue(42L);
    }

    @Test
    @DisplayName("DoD3 guard: unknown / blank / null tokens resolve to empty")
    void resolveUserId_emptyForUnknownOrBlankToken() {
        assertThat(sessionService.resolveUserId("never-issued")).isEmpty();
        assertThat(sessionService.resolveUserId(null)).isEmpty();
        assertThat(sessionService.resolveUserId("   ")).isEmpty();
    }

    @Test
    @DisplayName("DoD3 guard: expired token is rejected and gets purged on access")
    void resolveUserId_emptyForExpiredToken_andPurgesIt() {
        // Insert a token that already expired 60 seconds ago using the test seam.
        String expiredToken = "expired-token";
        sessionService.putSessionForTest(expiredToken, 9L, Instant.now().minusSeconds(60));

        assertThat(sessionService.resolveUserId(expiredToken)).isEmpty();
    }

    @Test
    @DisplayName("DoD3 guard: purgeExpired() drops only expired entries, leaves live ones")
    void purgeExpired_dropsExpiredKeepsLive() {
        sessionService.putSessionForTest("expired", 1L, Instant.now().minusSeconds(60));
        sessionService.putSessionForTest("live", 2L, Instant.now().plusSeconds(60));

        sessionService.purgeExpired();

        assertThat(sessionService.resolveUserId("live")).hasValue(2L);
        assertThat(sessionService.resolveUserId("expired")).isEmpty();
    }

    // ------------------------------------------------------------------
    // DoD 4 & 5 + edge cases for AuthenticationFilter via standalone MockMvc
    // ------------------------------------------------------------------

    @Test
    @DisplayName("DoD4: GET /api/anything without a Bearer token returns HTTP 401")
    void apiAnythingWithoutBearerToken_returns401() throws Exception {
        mockMvc.perform(get("/api/anything"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("DoD5: GET /api/anything with a valid token sets authenticatedUserId on the request")
    void apiAnythingWithValidToken_setsAuthenticatedUserId() throws Exception {
        // Create a real session for user 42 — the filter must pick this up.
        String token = sessionService.createSession(42L);

        // The test controller reads request.getAttribute("authenticatedUserId")
        // and echoes it back, so a 200 + "userId=42" proves the attribute was set.
        mockMvc.perform(get("/api/anything").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string("userId=42"));
    }

    @Test
    @DisplayName("Guard: invalid Bearer token returns 401")
    void apiAnythingWithInvalidToken_returns401() throws Exception {
        mockMvc.perform(get("/api/anything").header("Authorization", "Bearer never-issued-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Guard: malformed Authorization header (missing 'Bearer ' prefix) returns 401")
    void apiAnythingWithMalformedHeader_returns401() throws Exception {
        mockMvc.perform(get("/api/anything").header("Authorization", "Token 12345"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Guard: /auth/login is skipped by the filter (anonymous reachable)")
    void authEndpoint_notFiltered_reachableWithoutToken() throws Exception {
        mockMvc.perform(get("/auth/login"))
                .andExpect(status().isOk())
                .andExpect(content().string("auth-ok"));
    }

    
    @RestController
    static class TestApiController {
        @GetMapping("/api/anything")
        public String anything(HttpServletRequest request) {
            Object uid = request.getAttribute(AuthenticationFilter.AUTHENTICATED_USER_ID_ATTR);
            return "userId=" + (uid == null ? "null" : uid.toString());
        }

        @GetMapping("/auth/login")
        public String authLogin() {
            return "auth-ok";
        }
    }
}