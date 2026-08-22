package com.zuzdog.dto;

import java.time.Instant;

// one row for GET /api/matches - the other user in the match, with what the
// match list needs: photo, name and when it happened
public record MatchSummaryResponse(long userId, String username, String photoUrl, Instant matchedAt) {
}
