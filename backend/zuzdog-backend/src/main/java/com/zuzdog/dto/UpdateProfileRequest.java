package com.zuzdog.dto;

// PUT /api/profile body, null field means "leave "
public record UpdateProfileRequest(
        Integer userAge,
        String description,
        Double maxDistance,
        String dogName,
        String breed,
        Integer dogAge,
        String traits,
        String dogDescription) {
}
