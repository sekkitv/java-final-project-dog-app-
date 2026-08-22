package com.zuzdog.dto;

import java.time.Instant;

// one row of the conversation list (GET /api/messages/conversations).
// MessageDao builds it in one query that joins messages with users:
//   otherUserId     - the other person in the chat
//   otherUsername   - their username
//   lastMessage     - the newest message in the thread
//   lastMessageTime - when it was sent
//   unreadCount     - how many of their messages are still unread
public record ConversationSummary(
        long otherUserId,
        String otherUsername,
        String lastMessage,
        Instant lastMessageTime,
        int unreadCount) {
}