package com.teamactiontracker.backend.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private ResponseEntity<Map<String, String>> buildResponse(
            HttpStatusCode status,
            String message) {

        return ResponseEntity
                .status(status)
                .body(Map.of(
                        "status", "ERROR",
                        "message", message
                ));
    }

    // Authentication failures: HTTP 401.
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, String>> handleAuthentication(
            AuthenticationException exception) {

        logger.warn(
                "Authentication failed: {}",
                exception.getClass().getSimpleName()
        );

        String message = exception instanceof BadCredentialsException
                ? "Invalid email or password"
                : "Authentication is required";

        return buildResponse(HttpStatus.UNAUTHORIZED, message);
    }

    // Authenticated user without sufficient permissions: HTTP 403.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(
            AccessDeniedException exception) {

        logger.warn("Access denied for authenticated user");

        return buildResponse(
                HttpStatus.FORBIDDEN,
                "You do not have permission to perform this operation"
        );
    }

    // Preserve explicitly configured service response statuses.
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatus(
            ResponseStatusException exception) {

        HttpStatusCode status = exception.getStatusCode();

        String message;

        if (status.is5xxServerError()) {
            logger.error("Internal service error", exception);
            message = "An unexpected server error occurred";
        } else {
            message = exception.getReason() != null
                    ? exception.getReason()
                    : "Request could not be processed";
            logger.warn(
                    "Request rejected: {} - {}",
                    status.value(),
                    message
            );
        }

        return buildResponse(status, message);
    }

    // Bean validation failures: HTTP 400.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(
            MethodArgumentNotValidException exception) {

        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": "
                        + error.getDefaultMessage())
                .distinct()
                .collect(java.util.stream.Collectors.joining("; "));

        if (message.isBlank()) {
            message = "Request validation failed";
        }

        logger.warn("Request validation failed");

        return buildResponse(HttpStatus.BAD_REQUEST, message);
    }

    // Malformed JSON or unreadable request body: HTTP 400.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleMalformedJson(
            HttpMessageNotReadableException exception) {

        logger.warn("Malformed JSON request");

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid or malformed request body"
        );
    }

    // Invalid path or query parameter types: HTTP 400.
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception) {

        logger.warn(
                "Invalid parameter: {}",
                exception.getName()
        );

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid value for parameter: " + exception.getName()
        );
    }

    // Unsupported HTTP methods: HTTP 405.
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, String>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException exception) {

        logger.warn(
                "Unsupported HTTP method: {}",
                exception.getMethod()
        );

        return buildResponse(
                HttpStatus.METHOD_NOT_ALLOWED,
                "HTTP method not supported for this endpoint"
        );
    }

    // Invalid business input: HTTP 400.
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(
            IllegalArgumentException exception) {

        String message = exception.getMessage();

        logger.warn("Invalid business request: {}", message);

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                message != null ? message : "Invalid request"
        );
    }

    // Unexpected application errors: HTTP 500.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnexpectedException(
            Exception exception) {

        logger.error(
                "Unexpected application error occurred",
                exception
        );

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected server error occurred"
        );
    }
}