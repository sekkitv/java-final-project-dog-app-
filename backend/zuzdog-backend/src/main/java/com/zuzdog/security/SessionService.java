package com.zuzdog.security;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * our own session store, kept in memory.
 *
 * a user who logs in gets a UUID token and sends it back on every protected
 * request as "Authorization: Bearer <token>".
 *
 * there is no sessions table in the schema, so we keep them in a
 * ConcurrentHashMap instead. that is fast and needs no synchronized blocks,
 * but the tokens are gone after a restart.
 *
 * a session lives for sessionTtlMinutes (see application.properties, default
 * is 1440 = 24h). we clean up in two ways:
 *   1. resolveUserId() drops a token the moment someone uses it after it expired
 *   2. purgeExpired() runs every minute so the map does not keep growing
 * the second one needs @EnableScheduling, which is on ZuzdogApplication.
 */
@Service
public class SessionService {

    private final ConcurrentHashMap<String, SessionEntry> sessions = new ConcurrentHashMap<>();
    private final SecurityProperties securityProperties;

    public SessionService(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    /**
     * makes a new session for a user and returns the token.
     * we use UUID.randomUUID() because it is random enough for a token, it
     * needs no DB write, and it is safe to put in a header.
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
     * removes every session that already expired.
     * fixedDelay = 60_000 means it waits 60s after the last run before going
     * again, so the map does not keep growing.
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