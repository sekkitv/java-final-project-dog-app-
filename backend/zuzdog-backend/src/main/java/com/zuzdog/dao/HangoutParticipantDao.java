package com.zuzdog.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// Data access for the hangout_participants junction table.
// (hangout_id, user_id) is the natural PK, so add() uses ON CONFLICT DO NOTHING to stay
// idempotent  signing up twice is a no-op rather than an error.
@Repository
public class HangoutParticipantDao {

    private final JdbcTemplate jdbcTemplate;

    public HangoutParticipantDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // sign a user up for a hangout. returns the number of affected rows (1 on first signup,
    // 0 if they were already signed up). idempotent via ON CONFLICT DO NOTHING.
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

    // how many users are signed up for a hangout — used to check the DoD "count increases".
    public int countByHangout(long hangoutId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM hangout_participants WHERE hangout_id = ?",
                Integer.class, hangoutId);
        return count != null ? count : 0;
    }

    // is this user among the participants of this hangout — used to fill isUserSignedUp
    // if we ever need it outside the aggregate query in HangoutDao.
    public boolean isParticipant(long hangoutId, long userId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM hangout_participants WHERE hangout_id = ? AND user_id = ?",
                Integer.class, hangoutId, userId);
        return count != null && count > 0;
    }

    // look up who organized a hangout — used later (Step 2.6) to notify the organizer on RSVP.
    public Optional<Long> findOrganizerUserId(long hangoutId) {
        List<Long> rows = jdbcTemplate.queryForList(
                "SELECT organizer_user_id FROM hangouts WHERE hangout_id = ?",
                Long.class, hangoutId);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    // all participant user-ids for a hangout, in insert order (which is signup order).
    // used by NotificationService.notifyHangoutJoin to fan out a HANGOUT_JOIN notification to
    public List<Long> findParticipantUserIds(long hangoutId) {
        return jdbcTemplate.queryForList(
                "SELECT user_id FROM hangout_participants WHERE hangout_id = ? ORDER BY signed_up_at",
                Long.class, hangoutId);
    }
}