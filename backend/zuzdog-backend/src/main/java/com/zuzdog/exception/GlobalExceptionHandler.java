package com.zuzdog.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

// Turns exceptions thrown anywhere in the controllers into JSON error bodies.
// The frontend api.js reads data.message on errors, so every error body has a "message" field.

// This annotation makes this class a global exception handler for all controllers.
// We dont need to write try/catch in controllers, we can just throw ApiException and this class will handle it.
@RestControllerAdvice
public class GlobalExceptionHandler {

    //Handles ApiException thrown by services and returns a JSON error response with the appropriate HTTP status code.
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, String>> handleApiException(ApiException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(Map.of("message", ex.getMessage()));
    }

    // Handles validation errors thrown by Spring when request body validation fails.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + " must not be blank")
                .orElse("Invalid request");
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }
}
