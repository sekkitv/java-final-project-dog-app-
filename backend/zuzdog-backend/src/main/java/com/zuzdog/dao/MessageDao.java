package com.zuzdog.dao;

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

    // Most recent message from each conversation so we could present in before we enter the conv at front . 
    public List<ChatMessage> findConversations(long userId) {
        String sql = """
                SELECT DISTINCT ON (other_user_id) message_id, sender_id, receiver_id, body, sent_at
                FROM (
                    SELECT message_id, sender_id, receiver_id, body, sent_at,
                           CASE WHEN sender_id = ? THEN receiver_id ELSE sender_id END AS other_user_id
                    FROM messages
                    WHERE sender_id = ? OR receiver_id = ?
                ) t
                ORDER BY other_user_id, sent_at DESC
                """;
        return jdbcTemplate.query(sql, MESSAGE_ROW_MAPPER, userId, userId, userId);
    }
}
