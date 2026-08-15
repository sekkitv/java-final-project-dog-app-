package com.zuzdog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

// Body of POST /api/hangouts.
// title is required (@NotBlank).
// latitude/longitude are required 
// description may be null — the DAO stores "" in that case.
// eventTime may be null — an "always-open spot" has no scheduled time.
// activityType may be null/blank — HangoutService defaults it to MEETUP.
// organizerName is NOT part of the request: the service fills it from the authenticated
// user`s row so the client cannot spoof another user`s name.
public record CreateHangoutRequest(
        @NotBlank String title,
        @NotNull Double latitude,
        @NotNull Double longitude,
        String description,
        Instant eventTime,
        String activityType) {
}