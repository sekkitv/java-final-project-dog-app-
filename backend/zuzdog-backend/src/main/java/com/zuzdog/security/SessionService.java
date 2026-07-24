package com.zuzdog.security;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SessionService is our custom, in-memory session store.
 *
 * Why we have this at all:
 *   we authenticate with our own Bearer-token system: a logged-in user gets a UUID token, and every
 *   protected request must carry it in the "Authorization: Bearer <token>" header.
 *
 *   Where does the token live? We can't put it in the database without a
 *   sessions table (the schema has none), so we keep sessions in RAM inside a
 *   ConcurrentHashMap. That makes login/logout fast and stateless-ish, but it
 *   means tokens are lost on restart
 * 
 *   @Scheduled sweeper thread calls purgeExpired(). ConcurrentHashMap handles
 *   that safely without us needing synchronized blocks.
 *
 * Expiry:
 *   Each session lives for sessionTtlMinutes (config in application.properties,
 *   pulled from SecurityProperties, default 1440 = 24h). Two layers of cleanup:
 *     1. Lazy: resolveUserId() rejects and removes any token that is past its
 *        expiry the moment someone tries to use it.
 *     2. Active: @Scheduled purgeExpired() sweeps the map once a minute and
 *        drops expired entries so memory doesn't grow forever.
 *   The @Scheduled method only runs if Spring scheduling is enabled, which is
 *   why we added @EnableScheduling on ZuzdogApplication.
 */
@Service
public class SessionService {

    private final ConcurrentHashMap<String, SessionEntry> sessions = new ConcurrentHashMap<>();
    private final SecurityProperties securityProperties;

    public SessionService(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    /**
     * Create a brand-new session for a user and return the opaque token the
     * We use UUID.randomUUID() because:
     *   - it's cryptographically random enough for session tokens (122 bits of
     *     entropy) and collision-free in practice,
     *   - it needs no DB write,
     *   - its String form is URL/header-safe.
     */
    public String createSession(long userId) {
        String token = UUID.randomUUID().toString();
        // we take current time and add from securityProperties which is configured in
        // application.properties and add it and thats how we get the expiry time.
        Instant expiresAt = Instant.now().plusSeconds(60L * securityProperties.getSessionTtlMinutes());
        // add to concurrent hashmap
        sessions.put(token, new SessionEntry(userId, expiresAt));
        return token;
    }

    /**
     * we want to check if the token exists and is valid
     * if it expires we use lazy cleanup and remove it from the map
     * it is called lazy because the token is really there and we would not remove it 
     * untill someone tries to use it , and if it expired we remove it
     * 
     */
    public Optional<Long> resolveUserId(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        SessionEntry entry = sessions.get(token);
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.expiresAt().isBefore(Instant.now())) {
            sessions.remove(token);
            return Optional.empty();
        }
        return Optional.of(entry.userId());
    }

    /**
     * Background sweep that drops every session whose expiry has passed.
     *
     * fixedDelay = 60_000 means "wait 60s after the previous run ends before
     * this is the "smart" cleanup that keeps the map from growing forever.
     */ 
    @Scheduled(fixedDelay = 60_000L)
    public void purgeExpired() {
        Instant now = Instant.now();
        sessions.entrySet().removeIf(e -> e.getValue().expiresAt().isBefore(now));
    }

    /**
        * I made this for testing purposes, so that we can put a session in the map
        * with a specific token and expiry time to check that all work
     */
    void putSessionForTest(String token, long userId, Instant expiresAt) {
        sessions.put(token, new SessionEntry(userId, expiresAt));
    }

    // Tiny immutable value class. Using a record makes it obviously immutable,
    // which is what we want for safe ConcurrentHashMap storage.
    private record SessionEntry(long userId, Instant expiresAt) {}
}