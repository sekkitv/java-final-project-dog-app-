package com.zuzdog.dto;

import java.time.Instant;

// One row of the conversation list screen (GET /api/messages/conversations).
// Built by MessageDao in a single query joining messages <-> users:
//   otherUserId     — the id of the partner we exchanged messages with
//   otherUsername   — that partner's username
//   lastMessage     — the body of the most recent message in the thread
//   lastMessageTime — when that most recent message was sent
//   unreadCount     — proxy: how many messages this user received from the partner
//                     (no per-message read flag exists in the schema yet).
public record ConversationSummary(
        long otherUserId,
        String otherUsername,
        String lastMessage,
        Instant lastMessageTime,
        int unreadCount) {
}