package com.zuzdog.model;

// The kind of event that triggered a notification.
// stored in the DB as a VARCHAR ('MATCH' / 'HANGOUT_JOIN' / 'MESSAGE') via a CHECK constraint
// on the notifications table. Read/write is done with NotificationType.valueOf(...) and .name().
public enum NotificationType {
    MATCH,
    HANGOUT_JOIN,
    MESSAGE
}