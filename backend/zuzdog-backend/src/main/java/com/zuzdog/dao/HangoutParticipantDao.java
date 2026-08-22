package com.zuzdog.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// data access for the hangout_participants table.
// (hangout_id, user_id) is the primary key, so add() uses ON CONFLICT DO
// NOTHING and signing up twice does nothing instead of failing.
@Repository
public class HangoutParticipantDao {

    private final JdbcTemplate jdbcTemplate;

    public HangoutParticipantDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // sign a user up. returns 1 the first time and 0 if they were already in,
    // thanks to ON CONFLICT DO NOTHING
    public int add(long hangoutId, long userId) {
        String sql = """
                INSERT INTO hangout_participants (hangout_id, user_id)
                VALUES (?, ?)
                ON CONFLICT (hangout_id, user_id) DO NOTHING
                """;
        return jdbcTemplate.update(sql, hangoutId, userId);
    }

    // remove a user`s signup. returns affected rows (1 if they were signed up, 0 if not).
    public int remove(long hangoutId, long userId) {
        String sql = "DELETE FROM hangout_participants WHERE hangout_id = ? AND user_id = ?";
        return jdbcTemplate.update(sql, hangoutId, userId);
    }

    // how many users are signed up for a hangout
    public int countByHangout(long hangoutId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM hangout_participants WHERE hangout_id = ?",
                Integer.class, hangoutId);
        return count != null ? count : 0;
    }

    // is this user signed up for this hangout. HangoutDao already gets this in
    // its big query, this is for when we need it on its own
    public boolean isParticipant(long hangoutId, long userId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM hangout_participants WHERE hangout_id = ? AND user_id = ?",
                Integer.class, hangoutId, userId);
        return count != null && count > 0;
    }

    // who created the hangout, so we can notify them when someone signs up
    public Optional<Long> findOrganizerUserId(long hangoutId) {
        List<Long> rows = jdbcTemplate.queryForList(
                "SELECT organizer_user_id FROM hangouts WHERE hangout_id = ?",
                Long.class, hangoutId);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    // all the user ids signed up for a hangout, in signup order.
    // NotificationService uses it to send everyone a HANGOUT_JOIN notification
    public List<Long> findParticipantUserIds(long hangoutId) {
        return jdbcTemplate.queryForList(
                "SELECT user_id FROM hangout_participants WHERE hangout_id = ? ORDER BY signed_up_at",
                Long.class, hangoutId);
    }
}