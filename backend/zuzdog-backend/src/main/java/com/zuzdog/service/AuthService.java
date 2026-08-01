package com.zuzdog.service;

import com.zuzdog.dao.DogDao;
import com.zuzdog.dao.UserDao;
import com.zuzdog.dto.AuthResponse;
import com.zuzdog.dto.LoginRequest;
import com.zuzdog.dto.RegisterRequest;
import com.zuzdog.exception.ApiException;
import com.zuzdog.model.Dog;
import com.zuzdog.model.User;
import com.zuzdog.security.PasswordHasher;
import com.zuzdog.security.SessionService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


// Service Annotation is using use to mark the class as service provider, so that Sprint can automatically detect it and register it as a "bean" in the app context.
@Service
public class AuthService {

    private static final double DEFAULT_MAX_DISTANCE = 25.0;
    private static final String DEFAULT_DOG_NAME = "My Dog";
    
    // We use default auto error to prevent leaking information about whether the username exists or the passwrod is wrong.
    private static final String DEFAULT_AUTH_ERROR = "Invalid Credentials";

    private final UserDao userDao;
    private final DogDao dogDao;
    private final PasswordHasher passwordHasher;
    private final SessionService sessionService;

    public AuthService(UserDao userDao, DogDao dogDao,
                       PasswordHasher passwordHasher, SessionService sessionService) {
        this.userDao = userDao;
        this.dogDao = dogDao;
        this.passwordHasher = passwordHasher;
        this.sessionService = sessionService;
    }

    // @Transactional makes the user insert and the dog insert atomic:
    // either both rows exist or neither does.
    // regiser new user and dog and return the auth response with token, userId and username
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userDao.findByUsername(request.username()).isPresent()) {
            // Conflict is transltaed to HTTP 409, which is the standard code for "resource already exists style"
            throw new ApiException(HttpStatus.CONFLICT, "Username already taken");
        }

        String salt = passwordHasher.generateSalt();
        String hash = passwordHasher.hashPassword(request.password(), salt);

        User user = new User();
        user.setUsername(request.username());
        user.setPasswordHash(hash);
        user.setSalt(salt);
        // email is NOT NULL UNIQUE in the schema and the frontend does not collect it yet
        user.setEmail(request.username() + "@zuzdog.com");
        // primitive double defaults to 0.0 which violates CHECK (max_distance > 0)
        user.setMaxDistance(DEFAULT_MAX_DISTANCE);
        user.setLat(request.lat());
        user.setLng(request.lng());

        long userId;
        try {
            userId = userDao.insert(user);
        } catch (DuplicateKeyException ex) {
            // another request grabbed the same username between our check and the insert
            throw new ApiException(HttpStatus.CONFLICT, "Username already taken");
        }

        // when user is registered, we also create a default dog for the user
        // Later the user can edit his dog profile and add more dogs.
        Dog dog = new Dog();
        dog.setUserId(userId);
        dog.setDogName(DEFAULT_DOG_NAME);
        dogDao.insert(dog);

        String token = sessionService.createSession(userId);
        return new AuthResponse(token, userId, user.getUsername());
    }

    // Login and return auth response
    public AuthResponse login(LoginRequest request) {
        // same message for unknown user and wrong password, so we do not reveal which one failed
        // the .orElseThrow() is a java Optional library feature that allows us to throw an exception if the user is not found
        // because findByUsername return as an Optional<User> if no user is found we can throw an exception
        User user = userDao.findByUsername(request.username())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, DEFAULT_AUTH_ERROR));

        if (!passwordHasher.verifyPassword(request.password(), user.getSalt(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, DEFAULT_AUTH_ERROR);
        }
        
        // update user location if lat and lng are provided so the user experience is more accurate
        if (request.lat() != null && request.lng() != null) {
            userDao.updateLocation(user.getUserId(), request.lat(), request.lng());
        }

        //return new session token
        String token = sessionService.createSession(user.getUserId());
        return new AuthResponse(token, user.getUserId(), user.getUsername());
    }
}
