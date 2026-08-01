package com.zuzdog.exception;

import org.springframework.http.HttpStatus;

// Services throw this to signal an HTTP error without knowing anything about the web layer.
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    // All the services throw this exception when they encounter an error that should be commmunicated to the client
    // such as for example "username already taken" or "user not found".
    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
