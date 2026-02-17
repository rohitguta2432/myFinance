package com.myfinance.backend.exception;

import java.time.LocalDateTime;

public record ErrorResponse(
        int status,
        String message,
        String details,
        LocalDateTime timestamp) {

    public ErrorResponse(int status, String message) {
        this(status, message, null, LocalDateTime.now());
    }

    public ErrorResponse(int status, String message, String details) {
        this(status, message, details, LocalDateTime.now());
    }
}
