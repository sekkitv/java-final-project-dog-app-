package com.zuzdog.controller;

import com.zuzdog.dto.CreateHangoutRequest;
import com.zuzdog.dto.HangoutResponse;
import com.zuzdog.security.AuthenticationFilter;
import com.zuzdog.service.HangoutService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// REST endpoints for hangouts. All three are under /api/** so the AuthenticationFilter
// runs and sets the authenticated user id on the request; we read it via the public
// constant on AuthenticationFilter (same pattern as ProfileController/FeedController).
@RestController
public class HangoutController {

    private final HangoutService hangoutService;

    public HangoutController(HangoutService hangoutService) {
        this.hangoutService = hangoutService;
    }

    // List all hangouts with per-user signup state.
    @GetMapping("/api/hangouts")
    public List<HangoutResponse> getHangouts(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AuthenticationFilter.AUTHENTICATED_USER_ID_ATTR);
        return hangoutService.getAllHangouts(userId);
    }

    // Create a new hangout. organizerName is NOT taken from the request the service
    // fills it from the authenticated user`s row so it cannot be spoofed.
    @PostMapping("/api/hangouts")
    @ResponseStatus(HttpStatus.CREATED)
    public HangoutResponse createHangout(HttpServletRequest request,
                                          @Valid @RequestBody CreateHangoutRequest body) {
        Long userId = (Long) request.getAttribute(AuthenticationFilter.AUTHENTICATED_USER_ID_ATTR);
        return hangoutService.createHangout(userId, body);
    }

    // Sign the requesting user up for a hangout. Idempotent: signing up twice is a no-op
    // and still returns the hangout with the current participantCount.
    @PostMapping("/api/hangouts/{id}/signup")
    public HangoutResponse signup(HttpServletRequest request, @PathVariable long id) {
        Long userId = (Long) request.getAttribute(AuthenticationFilter.AUTHENTICATED_USER_ID_ATTR);
        return hangoutService.signup(id, userId);
    }
}