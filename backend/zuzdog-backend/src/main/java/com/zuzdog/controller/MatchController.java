package com.zuzdog.controller;

import com.zuzdog.dto.MatchSummaryResponse;
import com.zuzdog.security.AuthenticationFilter;
import com.zuzdog.service.MatchService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// REST endpoint for matches. Lives under /api/** so the AuthenticationFilter runs
// and sets the authenticated user id on the request - same pattern as every
// other controller in this app.
@RestController
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    // GET /api/matches — every match for the caller, newest first, flattened to
    // "the other person" (userId/username/photoUrl/matchedAt).
    @GetMapping("/api/matches")
    public List<MatchSummaryResponse> getMatches(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AuthenticationFilter.AUTHENTICATED_USER_ID_ATTR);
        return matchService.getMatchSummariesForUser(userId);
    }
}
