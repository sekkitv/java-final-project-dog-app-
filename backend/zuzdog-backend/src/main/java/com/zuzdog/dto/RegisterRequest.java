package com.zuzdog.dto;

import jakarta.validation.constraints.NotBlank;

// Body of POST /auth/register. lat/lng are optional, the browser sends them when geolocation is allowed.
public record RegisterRequest(
        @NotBlank String username,
        @NotBlank String password,
        Double lat,
        Double lng) {
}
