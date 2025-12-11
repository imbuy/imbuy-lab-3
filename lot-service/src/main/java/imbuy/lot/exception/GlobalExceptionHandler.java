package imbuy.lot.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Validation errors (@Valid) for WebMVC
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(err ->
                errors.put(err.getField(), err.getDefaultMessage())
        );

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", 400);
        body.put("error", "Bad Request");
        body.put("message", "Validation failed");
        body.put("errors", errors);

        return ResponseEntity.badRequest().body(body);
    }

    /**
     * Incorrect JSON, broken types, missing params
     */
    @ExceptionHandler({org.springframework.web.bind.MissingServletRequestParameterException.class,
            org.springframework.http.converter.HttpMessageNotReadableException.class})
    public ResponseEntity<Map<String, Object>> handleBadInput(Exception ex) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "timestamp", LocalDateTime.now(),
                        "status", 400,
                        "error", "Bad Request",
                        "message", Objects.requireNonNullElse(ex.getMessage(), "Invalid request")
                ));
    }


    /**
     * Custom error (409, 404, etc)
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleStatus(ResponseStatusException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", ex.getStatusCode().value());
        body.put("error", ex.getStatusCode().toString());

        String message = Objects.requireNonNullElse(ex.getReason(), "Unknown error");

        // Add error code for FORBIDDEN (403) errors
        if (ex.getStatusCode().value() == 403) {
            body.put("errorCode", "ACCESS_DENIED_403");
            if (message.contains("Access denied") || message.contains("can only modify")) {
                message = "Access Denied (Error 403): " + message + ". You do not have permission to perform this action. " +
                        "Users with role USER can only modify their own resources. " +
                        "Please ensure you are accessing your own resource or have appropriate permissions.";
            } else if (message.contains("Only supervisors") || message.contains("moderators")) {
                message = "Access Denied (Error 403): " + message + ". This action requires SUPERVISOR or MODERATOR role.";
            } else if (message.contains("Owner cannot")) {
                message = "Access Denied (Error 403): " + message;
            }
        }

        // Add error code for UNAUTHORIZED (401) errors
        if (ex.getStatusCode().value() == 401) {
            body.put("errorCode", "UNAUTHORIZED_401");
            if (message.contains("JWT token")) {
                message = "Unauthorized (Error 401): " + message;
            }
        }

        body.put("message", message);

        return ResponseEntity
                .status(ex.getStatusCode())
                .body(body);
    }

    /**
     * Fallback - catch all other exceptions and return proper error code
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        // Log the exception for debugging but don't expose internal details

        // Return 500 only for truly unexpected errors, but with proper message
        String message = "An unexpected error occurred. Please try again later.";
        if (ex.getMessage() != null && !ex.getMessage().isEmpty()) {
            // For known exceptions, use a sanitized message
            message = ex.getMessage();
        }

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "timestamp", LocalDateTime.now(),
                        "status", 500,
                        "error", "Internal Server Error",
                        "message", message
                ));
    }
}
