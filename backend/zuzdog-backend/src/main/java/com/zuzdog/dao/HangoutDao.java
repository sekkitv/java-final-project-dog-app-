package com.zuzdog.dao;

import com.zuzdog.model.Hangout;
import com.zuzdog.model.HangoutActivityType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Data access for the hangouts table. SimpleJdbcInsert is used for inserts so we get back
// the generated hangout_id, exactly like MessageDao does for messages.
// findAll / findById return the same row PLUS two computed fields:
// participant_count  - COUNT over hangout_participants
// is_user_signed_up - whether the requesting user is among them
// those are computed with a LEFT JOIN + an EXISTS subquery bound to a single user id.
@Repository
public class HangoutDao {

    // converts a result row into a Hangout object. the two computed columns
    // (participant_count, is_user_signed_up) are read with getInt / getBoolean.
    private static final RowMapper<Hangout> HANGOUT_ROW_MAPPER = (rs, rowNum) -> {
        Hangout h = new Hangout();
        h.setHangoutId(rs.getLong("hangout_id"));
        h.setOrganizerUserId(rs.getLong("organizer_user_id"));
        h.setTitle(rs.getString("title"));
        h.setDescription(rs.getString("description"));
        h.setOrganizerName(rs.getString("organizer_name"));
        h.setLatitude(rs.getDouble("latitude"));
        h.setLongitude(rs.getDouble("longitude"));
        h.setEventTime(JdbcMappingUtils.getInstant(rs, "event_time")); // nullable
        h.setActivityType(HangoutActivityType.valueOf(rs.getString("activity_type")));
        h.setCreatedAt(JdbcMappingUtils.getInstant(rs, "created_at"));
        h.setParticipantCount(rs.getInt("participant_count"));
        h.setUserSignedUp(rs.getBoolean("is_user_signed_up"));
        return h;
    };

    // the base SELECT used by findAll and findById. the ? bound for is_user_signed_up is the
    // requesting user id. the GROUP BY collapses the LEFT JOIN back to one row per hangout.
    private static final String SELECT_WITH_STATS = """
            SELECT h.hangout_id, h.organizer_user_id, h.title, h.description, h.organizer_name,
                   h.latitude, h.longitude, h.event_time, h.activity_type, h.created_at,
                   COUNT(p.user_id) AS participant_count,
                   EXISTS (SELECT 1 FROM hangout_participants p2
                           WHERE p2.hangout_id = h.hangout_id AND p2.user_id = ?) AS is_user_signed_up
            FROM hangouts h
            LEFT JOIN hangout_participants p ON p.hangout_id = h.hangout_id
            GROUP BY h.hangout_id
            """;

    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert insertHangout;

    public HangoutDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.insertHangout = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("hangouts")
                .usingColumns("organizer_user_id", "title", "description",
                        "organizer_name", "latitude", "longitude",
                        "event_time", "activity_type")
                .usingGeneratedKeyColumns("hangout_id");
    }

    // inserts a new hangout and returns the generated id.
    // eventTime may be null (an "always-open spot"); activityType stored as its enum name().
    public long insert(long organizerUserId, String organizerName, String title, String description,
                        double latitude, double longitude, Instant eventTime,
                        HangoutActivityType activityType) {
        Map<String, Object> params = new HashMap<>();
        params.put("organizer_user_id", organizerUserId);
        params.put("title", title);
        params.put("description", description == null ? "" : description);
        params.put("organizer_name", organizerName);
        params.put("latitude", latitude);
        params.put("longitude", longitude);
        params.put("event_time", eventTime);
        params.put("activity_type", activityType.name());
        Number key = insertHangout.executeAndReturnKey(params); //executeandreutnrkey is a simplejdbcinsert method, it generated a primary key 
        return key.longValue();
    }

    // all hangouts, newest first, each enriched with participant_count and is_user_signed_up
    // for the given requesting user.
    public List<Hangout> findAll(long userId) {
        String sql = SELECT_WITH_STATS + " ORDER BY h.created_at DESC";
        return jdbcTemplate.query(sql, HANGOUT_ROW_MAPPER, userId); //the 3rd args is for the ? paramater in SQL query
    }

    // a single hangout by id, enriched with the same two computed fields for the given user.
    public Optional<Hangout> findById(long hangoutId, long userId) {
        String sql = SELECT_WITH_STATS + " HAVING h.hangout_id = ?";
        List<Hangout> rows = jdbcTemplate.query(sql, HANGOUT_ROW_MAPPER, userId, hangoutId);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }
}