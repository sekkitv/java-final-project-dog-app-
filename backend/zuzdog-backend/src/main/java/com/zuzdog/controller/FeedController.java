package com.zuzdog.controller;

import com.zuzdog.dto.FeedCandidate;
import com.zuzdog.dto.FeedResponse;
import com.zuzdog.security.AuthenticationFilter;
import com.zuzdog.service.FeedService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// front call  this function 
@RestController
public class FeedController {

    private final FeedService feedService;

    public FeedController(FeedService feedService) {
        this.feedService = feedService;
    }

    @GetMapping("/api/feed")
    public FeedResponse getFeed(HttpServletRequest request,
                                 @RequestParam(required = false) Integer limit) {
        Long userId = (Long) request.getAttribute(AuthenticationFilter.AUTHENTICATED_USER_ID_ATTR); // security check token from header and compare to session service saved token 


        List<FeedCandidate> candidates;
        //return candidate and use the limit from url param 
        if (limit != null) {
            candidates = feedService.getFeedForUser(userId, limit);
        } else {
            candidates = feedService.getFeedForUser(userId);
        }

        return new FeedResponse(candidates);
    }
}
