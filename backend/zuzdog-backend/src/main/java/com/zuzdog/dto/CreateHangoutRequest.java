package com.zuzdog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

// body of POST /api/hangouts.
// title is required (@NotBlank).
// latitude/longitude are required
// description can be null, the DAO stores "" then.
// eventTime can be null, a place that is always open has no time.
// activityType can be null or blank, HangoutService puts MEETUP.
// organizerName is not in the request, the service takes it from the logged in
// user so nobody can send someone else's name.
public record CreateHangoutRequest(
        @NotBlank String title,
        @NotNull Double latitude,
        @NotNull Double longitude,
        String description,
        Instant eventTime,
        String activityType) {
}