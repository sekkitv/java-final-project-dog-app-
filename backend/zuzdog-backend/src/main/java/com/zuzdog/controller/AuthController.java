package com.zuzdog.controller;

import com.zuzdog.dto.AuthResponse;
import com.zuzdog.dto.LoginRequest;
import com.zuzdog.dto.RegisterRequest;
import com.zuzdog.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// The AuthenticationFilter skips /auth/**, so both endpoints are reachable without a token.

// This controller handles user registration and login. It delegates the actual work to the AuthService.
@RestController
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    // Explnatation about the @valid and @RequestBody annotations:
    // @RequestBody tells Spring that the json body of the request should be like RegisterRequest from the DTO package.
    // @Valid tells sprint to validate the request body againts RegisterRequest`s validation annotations such as @NotBlank etc
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/auth/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
