package com.zuzdog.dto;

// Flat owner + dog view for GET /api/profile. Dog fields stay null when the user has no dog row.
public record ProfileResponse(
        long userId,
        String username,
        String email,
        Integer userAge,
        String description,
        String photoUrl,
        double maxDistance,
        Double lat,
        Double lng,
        Long dogId,
        String dogName,
        String breed,
        Integer dogAge,
        String traits,
        String dogDescription,
        String dogPhotoUrl) {
}
