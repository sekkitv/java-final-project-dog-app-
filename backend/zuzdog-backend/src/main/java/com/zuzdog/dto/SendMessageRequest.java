package com.zuzdog.dto;

import jakarta.validation.constraints.NotBlank;

// Body of POST /api/messages/with/{otherUserId}.
// body must be present and non-blank (@NotBlank); a blank body is a 400
// before the service even runs — handled by GlobalExceptionHandler.
public record SendMessageRequest(@NotBlank String body) {
}