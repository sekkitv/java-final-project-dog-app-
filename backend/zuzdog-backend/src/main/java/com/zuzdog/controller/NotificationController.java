package com.zuzdog.controller;

import com.zuzdog.dto.NotificationsResponse;
import com.zuzdog.model.Notification;
import com.zuzdog.security.AuthenticationFilter;
import com.zuzdog.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// REST endpoints for notifications. Both routes sit under /api/** so the
// AuthenticationFilter runs and sets the authenticated user id on the request
// we read it via the public constant on AuthenticationFilter, the same pattern used by
// HangoutController / ProfileController / FeedController.
//
// The controller never touches NotificationDao directly — it goes through
// NotificationService, which keeps the service-as-controller-boundary convention used
// across the rest of the app.
@RestController
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }


    @GetMapping("/api/notifications")
    public NotificationsResponse list(HttpServletRequest request) {
        long userId = (long) request.getAttribute(AuthenticationFilter.AUTHENTICATED_USER_ID_ATTR);
        List<Notification> notifications = notificationService.getForUser(userId);
        int unread = notificationService.unreadCount(userId);
        return new NotificationsResponse(notifications, unread);
    }

    // mark all notification for the user as read.
    @PostMapping("/api/notifications/read")
    public NotificationsResponse markRead(HttpServletRequest request) {
        long userId = (long) request.getAttribute(AuthenticationFilter.AUTHENTICATED_USER_ID_ATTR);
        notificationService.markAllRead(userId);
        List<Notification> notifications = notificationService.getForUser(userId);
        int unread = notificationService.unreadCount(userId);
        return new NotificationsResponse(notifications, unread);
    }
}