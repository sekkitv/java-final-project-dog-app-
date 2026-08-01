package com.zuzdog.service;

import com.zuzdog.dao.DogDao;
import com.zuzdog.dao.UserDao;
import com.zuzdog.dto.ProfileResponse;
import com.zuzdog.exception.ApiException;
import com.zuzdog.model.Dog;
import com.zuzdog.model.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ProfileService {

    private final UserDao userDao;
    private final DogDao dogDao;

    public ProfileService(UserDao userDao, DogDao dogDao) {
        this.userDao = userDao;
        this.dogDao = dogDao;
    }

    // serach for the user profile by userId , if not found throw 404 error
    public ProfileResponse getProfile(long userId) {
        User user = userDao.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        Dog dog = dogDao.findPrimaryByUserId(userId).orElse(null);

        // ProfileResponse has no password or salt fields, so they cannot leak here
        // ProfileResponse class is a record class that is configured in DTO package
        return new ProfileResponse(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getUserAge(),
                user.getDescription(),
                user.getPhotoUrl(),
                user.getMaxDistance(),
                user.getLat(),
                user.getLng(),
                dog == null ? null : dog.getDogId(),
                dog == null ? null : dog.getDogName(),
                dog == null ? null : dog.getBreed(),
                dog == null ? null : dog.getDogAge(),
                dog == null ? null : dog.getTraits(),
                dog == null ? null : dog.getDescription(),
                dog == null ? null : dog.getPhotoUrl());
    }
}
