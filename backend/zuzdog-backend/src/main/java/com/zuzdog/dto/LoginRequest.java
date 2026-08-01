package com.zuzdog.dto;

import jakarta.validation.constraints.NotBlank;

// Body of POST /auth/login. lat/lng are optional and update the stored location when present.
public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password,
        Double lat,
        Double lng) {
}
