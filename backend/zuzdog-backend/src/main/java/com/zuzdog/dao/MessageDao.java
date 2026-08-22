package com.zuzdog.dao;

import com.zuzdog.dto.ConversationSummary;
import com.zuzdog.model.ChatMessage;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class MessageDao {

    //take a sql row and make an object out of it
    private static final RowMapper<ChatMessage> MESSAGE_ROW_MAPPER = (rs, rowNum) -> {
        ChatMessage msg = new ChatMessage();
        msg.setMessageId(rs.getLong("message_id"));
        msg.setSenderId(rs.getLong("sender_id"));
        msg.setReceiverId(rs.getLong("receiver_id"));
        msg.setBody(rs.getString("body"));
        msg.setSentAt(JdbcMappingUtils.getInstant(rs, "sent_at"));
        return msg;
    };

    // Maps a ConversationSummary row (the result of findConversationSummaries) into
    // the ConversationSummary record directly via its constructor.
    private static final RowMapper<ConversationSummary> SUMMARY_ROW_MAPPER = (rs, rowNum) ->
            new ConversationSummary(
                    rs.getLong("other_user_id"),
                    rs.getString("other_username"),
                    rs.getString("last_message"),
                    JdbcMappingUtils.getInstant(rs, "last_message_time"),
                    rs.getInt("unread_count"));

    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert insertActor;

    public MessageDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.insertActor = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("messages")
                .usingColumns("sender_id", "receiver_id", "body")
                .usingGeneratedKeyColumns("message_id");
    }

// saves a new message to the database and return id for the new message 
    public long insert(long senderId, long receiverId, String body) {
        Map<String, Object> params = new HashMap<>();
        params.put("sender_id", senderId);
        params.put("receiver_id", receiverId);
        params.put("body", body);
        Number key = insertActor.executeAndReturnKey(params);
        return key.longValue();
    }

    // Full chat history between two users, oldest first
    public List<ChatMessage> findThread(long userA, long userB) {
        String sql = """
                SELECT message_id, sender_id, receiver_id, body, sent_at
                FROM messages
                WHERE (sender_id = ? AND receiver_id = ?)
                   OR (sender_id = ? AND receiver_id = ?)
                ORDER BY sent_at ASC
                """;
        return jdbcTemplate.query(sql, MESSAGE_ROW_MAPPER, userA, userB, userB, userA);
    }

    // marks what senderId sent to receiverId as read, called when the thread is opened
    public int markThreadAsRead(long receiverId, long senderId) {
        return jdbcTemplate.update(
                "UPDATE messages SET read_at = NOW() WHERE receiver_id = ? AND sender_id = ? AND read_at IS NULL",
                receiverId, senderId);
    }

    // one row per chat partner for the conversation list. one query that:
    //   - DISTINCT ON picks the newest message per partner
    //   - joins users for the partner name
    //   - LEFT JOIN counts the unread ones (read_at IS NULL)
    // newest first
    public List<ConversationSummary> findConversationSummaries(long userId) {
        String sql = """
                SELECT
                    conv.other_user_id,
                    u.username       AS other_username,
                    lm.body          AS last_message,
                    lm.sent_at       AS last_message_time,
                    COALESCE(uc.unread_count, 0) AS unread_count
                FROM (
                    SELECT DISTINCT ON (other_user_id) other_user_id, message_id, sent_at
                    FROM (
                        SELECT message_id, sent_at,
                               CASE WHEN sender_id = ? THEN receiver_id ELSE sender_id END AS other_user_id
                        FROM messages
                        WHERE sender_id = ? OR receiver_id = ?
                    ) t
                    ORDER BY other_user_id, sent_at DESC
                ) conv
                JOIN messages lm ON lm.message_id = conv.message_id
                JOIN users u    ON u.user_id     = conv.other_user_id
                LEFT JOIN (
                    SELECT sender_id AS partner_id, COUNT(*) AS unread_count
                    FROM messages
                    WHERE receiver_id = ? AND read_at IS NULL
                    GROUP BY sender_id
                ) uc ON uc.partner_id = conv.other_user_id
                ORDER BY lm.sent_at DESC
                """;
        return jdbcTemplate.query(sql, SUMMARY_ROW_MAPPER, userId, userId, userId, userId);
    }
}
