package com.zuzdog.dto;

// Returned by register and login. Only these three fields, so password data can never leak.
// if we would respone with user object that password and salt would be included which is a huge security risk.
public record AuthResponse(
    String token,
    long userId,
    String username) {
}
