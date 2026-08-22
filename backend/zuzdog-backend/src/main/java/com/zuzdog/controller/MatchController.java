package com.zuzdog.controller;

import com.zuzdog.dto.MatchSummaryResponse;
import com.zuzdog.security.AuthenticationFilter;
import com.zuzdog.service.MatchService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// REST endpoint for matches. under /api/** so the filter runs and puts the
// user id on the request, same as the other controllers
@RestController
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    // GET /api/matches - all matches for the user, newest first, with the
    // other person's id, name and photo
    @GetMapping("/api/matches")
    public List<MatchSummaryResponse> getMatches(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AuthenticationFilter.AUTHENTICATED_USER_ID_ATTR);
        return matchService.getMatchSummariesForUser(userId);
    }
}
