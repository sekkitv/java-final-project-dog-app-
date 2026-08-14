package com.zuzdog.controller;

import com.zuzdog.dto.SwipeRequest;
import com.zuzdog.security.AuthenticationFilter;
import com.zuzdog.service.SwipeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// REST endpoint for swipes. 
// gets request recongize user token and return answer 
@RestController
public class SwipeController {

    private final SwipeService swipeService;

    public SwipeController(SwipeService swipeService) {
        this.swipeService = swipeService;
    }

   // we check the match only later in swipe consumer 
    @PostMapping("/api/swipe")
    public Map<String, String> swipe(HttpServletRequest request, @Valid @RequestBody SwipeRequest body) {
        Long userId = (Long) request.getAttribute(AuthenticationFilter.AUTHENTICATED_USER_ID_ATTR);
        swipeService.processSwipe(userId, body.targetId(), body.action());
        return Map.of("status", "accepted");
    }
}
