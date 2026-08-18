package com.zuzdog.controller;

import com.zuzdog.dto.ProfileResponse;
import com.zuzdog.dto.UpdateProfileRequest;
import com.zuzdog.dto.UserProfilePictureResponse;
import com.zuzdog.security.AuthenticationFilter;
import com.zuzdog.service.ProfileService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

    //returns only the OTHER user's id + photo url 
    @GetMapping("/api/profile/{userId}/photo")
    public UserProfilePictureResponse getUserProfilePicture(@PathVariable long userId) {
        return profileService.getUserProfilePicture(userId);
    }

    @PutMapping("/api/profile")
    public ProfileResponse updateProfile(HttpServletRequest request, @RequestBody UpdateProfileRequest body) {
        Long userId = (Long) request.getAttribute(AuthenticationFilter.AUTHENTICATED_USER_ID_ATTR);
        return profileService.updateProfile(userId, body);
    }

    @PostMapping("/api/profile/photos/owner")
    public ProfileResponse uploadOwnerPhoto(HttpServletRequest request, @RequestParam("file") MultipartFile file) {
        Long userId = (Long) request.getAttribute(AuthenticationFilter.AUTHENTICATED_USER_ID_ATTR);
        return profileService.uploadOwnerPhoto(userId, file);
    }

    @PostMapping("/api/profile/photos/dog")
    public ProfileResponse uploadDogPhoto(HttpServletRequest request, @RequestParam("file") MultipartFile file) {
        Long userId = (Long) request.getAttribute(AuthenticationFilter.AUTHENTICATED_USER_ID_ATTR);
        return profileService.uploadDogPhoto(userId, file);
    }
}
