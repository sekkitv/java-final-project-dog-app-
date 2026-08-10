package com.zuzdog.dto;

import java.time.Instant;


public record HangoutResponse(
        long hangoutId,
        long organizerUserId,
        String title,
        String description,
        String organizerName,
        double latitude,
        double longitude,
        Instant eventTime,
        String activityType,
        Instant createdAt,
        int participantCount,
        boolean isUserSignedUp) {
}