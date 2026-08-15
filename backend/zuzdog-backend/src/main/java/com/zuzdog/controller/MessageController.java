package com.zuzdog.controller;

import com.zuzdog.dto.ConversationSummary;
import com.zuzdog.dto.SendMessageRequest;
import com.zuzdog.model.ChatMessage;
import com.zuzdog.security.AuthenticationFilter;
import com.zuzdog.service.MessageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// REST endpoints for messaging. All three live under /api/** so the
// AuthenticationFilter runs and sets authenticatedUserId on the request;
// we read it via the public constant on AuthenticationFilter (same pattern as
// ProfileController / FeedController / HangoutController).
@RestController
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    // GET /api/messages/conversations — one summary row per conversation partner
    // (partner id, username, last message, last time, unread proxy count).
    @GetMapping("/api/messages/conversations")
    public List<ConversationSummary> getConversations(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AuthenticationFilter.AUTHENTICATED_USER_ID_ATTR);
        return messageService.getConversationSummaries(userId);
    }

    // GET /api/messages/with/{otherUserId} — the full thread with one partner,
    // returned in chronological order (oldest first).
    @GetMapping("/api/messages/with/{otherUserId}")
    public List<ChatMessage> getThread(HttpServletRequest request, @PathVariable long otherUserId) {
        Long userId = (Long) request.getAttribute(AuthenticationFilter.AUTHENTICATED_USER_ID_ATTR);
        return messageService.getThread(userId, otherUserId);
    }

    // POST /api/messages/with/{otherUserId} — send a message to one partner.
    // Returns 201 on success. If the two users are not matched, the service
    // throws ApiException(FORBIDDEN) which GlobalExceptionHandler turns into 403.
    @PostMapping("/api/messages/with/{otherUserId}")
    @ResponseStatus(HttpStatus.CREATED)
    public void sendMessage(HttpServletRequest request,
                            @PathVariable long otherUserId,
                            @Valid @RequestBody SendMessageRequest body) {
        Long userId = (Long) request.getAttribute(AuthenticationFilter.AUTHENTICATED_USER_ID_ATTR);
        messageService.sendMessage(userId, otherUserId, body.body());
    }
}