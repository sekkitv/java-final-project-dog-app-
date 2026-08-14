package com.zuzdog.dto;

import java.time.Instant;

// One row for GET /api/matches — the OTHER user in the match (not the caller),
// enough to render a match list (avatar + name + when it happened).
public record MatchSummaryResponse(long userId, String username, String photoUrl, Instant matchedAt) {
}
