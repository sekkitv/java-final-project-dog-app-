package com.zuzdog.dto;

import com.zuzdog.model.Notification;
import java.util.List;

// return by get /notification. contains the list of notifications.
public record NotificationsResponse(
        List<Notification> notifications,
        int unreadCount) {
}