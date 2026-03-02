package com.meridian.inventoryservice.presentation;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.access.AccessDeniedException;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Centralised exception interceptor mapping granular Java exceptions into
 * standard, readable HTTP responses across the inventory domain.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Intercepts Java Bean Validation failures (e.g., negative stock allocations).
     *
     * @param ex The native framework validation exception.
     * @return HTTP 400 Bad Request detailing all specific field violations.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    /**
     * Catch-all for domain logic guard clause trips (e.g., duplicate product SKUs).
     *
     * @param ex The thrown IllegalArgumentException.
     * @return HTTP 400 Bad Request with the provided message.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }

    /**
     * Captures fundamental state-based capability limits (e.g., insufficient stock
     * for an order).
     *
     * @param ex The thrown IllegalStateException.
     * @return HTTP 409 Conflict.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalStateException(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }

    /**
     * Captures lookups for products that strictly do not exist.
     *
     * @param ex The native NoSuchElementException.
     * @return HTTP 404 Not Found.
     */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNoSuchElementException(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    /**
     * Captures unauthorized invocation attempts on restricted {@code @PreAuthorize}
     * routes.
     *
     * @param ex The Spring Security access denied exception.
     * @return HTTP 403 Forbidden.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDeniedException(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied: " + ex.getMessage()));
    }

    /**
     * Ultimate safety net to catch entirely unexpected faults without bleeding raw
     * stack traces to REST clients.
     *
     * @param ex Any untracked Exception.
     * @return HTTP 500 Internal Server Error.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "An unexpected error occurred: " + ex.getMessage()));
    }
}
