package com.zuzdog.service;

import com.zuzdog.dao.NotificationDao;
import com.zuzdog.model.Notification;
import com.zuzdog.model.NotificationType;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

//NotificationService, responsible fore all notifcation logic

@Service
public class NotificationService {

    private final NotificationDao notificationDao;

    public NotificationService(NotificationDao notificationDao) {
        this.notificationDao = notificationDao;
    }

    // when two users match, both get a MATCH notification.
    public void notifyMatch(long userA, long userB) {
        notificationDao.insert(userA, NotificationType.MATCH, null, "New match!", "");
        notificationDao.insert(userB, NotificationType.MATCH, null, "New match!", "");
    }

    // when a user sends a message, the recipient gets a MESSAGE notifacation.
    public void notifyMessage(long senderId, long receiverId, long messageId) {
        notificationDao.insert(receiverId, NotificationType.MESSAGE, messageId, "New message", "");
    }

    // when a user joins a hangout, the organizer and all existing participants gets a  notifacation.
    public void notifyHangoutJoin(long hangoutId, long joiningUserId, long organizerUserId,
                                   Collection<Long> participantUserIds) {
        // fan-out set = organizer + every existing participant except the joining user.
        // dedupe in case the organizer is also a participant.
        Set<Long> recipients = new HashSet<>();
        if (organizerUserId != joiningUserId) {
            recipients.add(organizerUserId);
        }
        if (participantUserIds != null) {
            for (long pid : participantUserIds) {
                if (pid != joiningUserId) {
                    recipients.add(pid);
                }
            }
        }
        for (long recipientId : recipients) {
            notificationDao.insert(recipientId, NotificationType.HANGOUT_JOIN, hangoutId,
                    "Someone joined your hangout", "");
        }
    }

    // all notifications for a user, newest first. thin wrapper for the controller.
    public List<Notification> getForUser(long userId) {
        return notificationDao.findForUser(userId);
    }

    // marks every unread notification for the user as read. thin wrapper for the controller.
    public void markAllRead(long userId) {
        notificationDao.markAllRead(userId);
    }

    // unread count for the badge. thin wrapper for the controller.
    public int unreadCount(long userId) {
        return notificationDao.countUnread(userId);
    }
}