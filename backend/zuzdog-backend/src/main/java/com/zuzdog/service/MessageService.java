package com.zuzdog.service;

import com.zuzdog.dao.MatchDao;
import com.zuzdog.dao.MessageDao;
import com.zuzdog.model.ChatMessage;
import org.springframework.stereotype.Service;

import java.util.List;

//  logic  for messaging.
// Enforces that messages can only be sent between matched users 

@Service
public class MessageService {

    private final MessageDao messageDao;
    private final MatchDao matchDao;

    public MessageService(MessageDao messageDao, MatchDao matchDao) {
        this.messageDao = messageDao;
        this.matchDao = matchDao;
    }

    // Send a message, but only if sender and receiver are matched.
    // Throws if there's no match between them.
    public long sendMessage(long senderId, long receiverId, String body) {
        if (!matchDao.existsBetween(senderId, receiverId)) {
            throw new IllegalStateException(
                    "Cannot send message: users " + senderId + " and " + receiverId + " are not matched");
        }
        return messageDao.insert(senderId, receiverId, body);
    }

    // Full chat history between two users, oldest first.
    public List<ChatMessage> getThread(long userA, long userB) {
        return messageDao.findThread(userA, userB);
    }

    // Most recent message per conversation partner, for the conversation list screen.
    public List<ChatMessage> getConversations(long userId) {
        return messageDao.findConversations(userId);
    }
}
