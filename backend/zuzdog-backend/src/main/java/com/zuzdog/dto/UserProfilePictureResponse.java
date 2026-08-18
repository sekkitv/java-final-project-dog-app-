package com.zuzdog.dto;

// nothing sensitive here, for security reasons.
// we only return userId + photoUrl, so the frontend can render the avatar of the user in a conversation with
public record UserProfilePictureResponse(long userId, String photoUrl) {
}
