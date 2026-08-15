package com.zuzdog.dao;

import com.zuzdog.model.Notification;
import com.zuzdog.model.NotificationType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class NotificationDao {

    // converts a database row into a Notification object
    private static final RowMapper<Notification> NOTIFICATION_ROW_MAPPER = (rs, rowNum) -> {
        Notification n = new Notification();
        n.setNotificationId(rs.getLong("notification_id"));
        n.setUserId(rs.getLong("user_id"));
        n.setType(NotificationType.valueOf(rs.getString("type")));
        long ref = rs.getLong("reference_id");
        n.setReferenceId(rs.wasNull() ? null : ref); // reference_id is nullable
        n.setTitle(rs.getString("title"));
        n.setBody(rs.getString("body"));
        n.setCreatedAt(JdbcMappingUtils.getInstant(rs, "created_at"));
        n.setReadAt(JdbcMappingUtils.getInstant(rs, "read_at")); // null when unread
        return n;
    };

    private static final String SELECT_ALL_COLUMNS =
            "SELECT notification_id, user_id, type, reference_id, title, body, created_at, read_at FROM notifications";

    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert insertNotification;


    // constructor that initializes the JdbcTemplate and SimpleJdbcInsert for the notifications table
    // we need it to insert new notifications and get the generated notification_id back
    public NotificationDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.insertNotification = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("notifications")
                .usingColumns("user_id", "type", "reference_id", "title", "body")
                .usingGeneratedKeyColumns("notification_id");
    }

    // saves a new notification and returns the generated notification_id.
    // pass null for referenceId when there is no related row to point at.
    public long insert(long userId, NotificationType type, Long referenceId, String title, String body) {
        Map<String, Object> params = new HashMap<>();
        params.put("user_id", userId);
        params.put("type", type.name()); // store the enum as a string in the database
        params.put("reference_id", referenceId);
        params.put("title", title);
        params.put("body", body);
        Number key = insertNotification.executeAndReturnKey(params);
        return key.longValue();
    }

    // user notfications but 50 max most recent 
    public List<Notification> findForUser(long userId) {
        // we use ? placeholder for paramater to prevent SQL injection, and pass userID as a paramater to the query.
        String sql = SELECT_ALL_COLUMNS + " WHERE user_id = ? ORDER BY created_at DESC LIMIT 50";
        return jdbcTemplate.query(sql, NOTIFICATION_ROW_MAPPER, userId);
    }

    // number of unread notifications for a user, used for the bell badge count
    public int countUnread(long userId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND read_at IS NULL",
                Integer.class, userId);
        return count != null ? count : 0;
    }

    // marks every unread notification for the user as read (read_at = NOW).
    // returns the number of rows that were updated.
    public int markAllRead(long userId) {
        String sql = """
                UPDATE notifications
                SET read_at = NOW()
                WHERE user_id = ? AND read_at IS NULL
                """;
        return jdbcTemplate.update(sql, userId);
    }
}