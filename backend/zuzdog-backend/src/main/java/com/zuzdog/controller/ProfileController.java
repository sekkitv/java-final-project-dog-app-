package com.zuzdog.controller;

import com.zuzdog.dto.ProfileResponse;
import com.zuzdog.dto.UpdateProfileRequest;
import com.zuzdog.security.AuthenticationFilter;
import com.zuzdog.service.ProfileService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    // The filter already validated the Bearer token and stored the user id on the request,
    // so by the time this method runs we know exactly who is calling.
    @GetMapping("/api/profile")
    public ProfileResponse getProfile(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AuthenticationFilter.AUTHENTICATED_USER_ID_ATTR);
        return profileService.getProfile(userId);
    }

    @PutMapping("/api/profile")
    public ProfileResponse updateProfile(HttpServletRequest request, @RequestBody UpdateProfileRequest body) {
        Long userId = (Long) request.getAttribute(AuthenticationFilter.AUTHENTICATED_USER_ID_ATTR);
        return profileService.updateProfile(userId, body);
    }
}
