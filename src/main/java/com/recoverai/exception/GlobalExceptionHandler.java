package com.recoverai.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AiServiceException.class)
    public ResponseEntity<Map<String, String>> handleAiServiceException(
            AiServiceException exception) {

        Map<String, String> response = Map.of(
                "error", "AI service unavailable",
                "message", exception.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(response);
    }
}
