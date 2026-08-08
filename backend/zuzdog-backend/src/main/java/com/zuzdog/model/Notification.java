package com.zuzdog.model;

import java.time.Instant;

// mirrors one row of the notifications table.
// referenceId is a Long (nullable) and not a foreign key on purpose  it loosely points at the
// related match/message/hangout row but is allowed to stay NULL or dangle if that row is cleaned up.
public class Notification {

    private long notificationId;
    private long userId;
    private NotificationType type;
    private Long referenceId;
    private String title;
    private String body;
    private Instant createdAt;
    private Instant readAt; // null means the user has not opened the notification yet

    public long getNotificationId() { return notificationId; }
    public void setNotificationId(long notificationId) { this.notificationId = notificationId; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }

    public Long getReferenceId() { return referenceId; }
    public void setReferenceId(Long referenceId) { this.referenceId = referenceId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getReadAt() { return readAt; }
    public void setReadAt(Instant readAt) { this.readAt = readAt; }
}