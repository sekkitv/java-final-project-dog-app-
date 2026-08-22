package com.zuzdog.service;

import com.zuzdog.dao.MatchDao;
import com.zuzdog.dao.MessageDao;
import com.zuzdog.dto.ConversationSummary;
import com.zuzdog.exception.ApiException;
import com.zuzdog.model.ChatMessage;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

//  logic  for messaging.
// Enforces that messages can only be sent between matched users.
// If sender and receiver are NOT matched we throw ApiException(FORBIDDEN) which
// GlobalExceptionHandler turns into an HTTP 403 - that is the message gate.

@Service
public class MessageService {

    private final MessageDao messageDao;
    private final MatchDao matchDao;
    private final NotificationService notificationService;

    public MessageService(MessageDao messageDao, MatchDao matchDao, NotificationService notificationService) {
        this.messageDao = messageDao;
        this.matchDao = matchDao;
        this.notificationService = notificationService;
    }

    // Send a message, but only if sender and receiver are matched.
    // No match -> ApiException(FORBIDDEN) -> HTTP 403.
    public long sendMessage(long senderId, long receiverId, String body) {
        if (!matchDao.existsBetween(senderId, receiverId)) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Messaging not allowed: you are not matched with this user");
        }
        long messageId = messageDao.insert(senderId, receiverId, body);
        // when a message is sent we need to send a notification to the receiver.
        notificationService.notifyMessage(senderId, receiverId, messageId);
        return messageId;
    }

    // Full chat history between two users, oldest first (chronological).
    // Fetching the thread also marks whatever userB sent to userA as read.
    public List<ChatMessage> getThread(long userA, long userB) {
        messageDao.markThreadAsRead(userA, userB);
        return messageDao.findThread(userA, userB);
    }

    // Conversation list screen: one summary row per conversation partner.
    // Each row has the partner's username, the last message + time, and the
    // real unread count (messages with read_at IS NULL).
    public List<ConversationSummary> getConversationSummaries(long userId) {
        return messageDao.findConversationSummaries(userId);
    }
}
