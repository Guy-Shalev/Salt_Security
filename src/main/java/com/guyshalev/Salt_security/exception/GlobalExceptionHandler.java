package com.guyshalev.Salt_security.exception;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.guyshalev.Salt_security.model.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for the application.
 * Handles various types of exceptions and returns appropriate error responses.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.error("Failed to read HTTP message", ex);

        Map<String, String> details = new HashMap<>();
        String message;

        Throwable cause = ex.getRootCause();

        if (cause instanceof JsonParseException) {
            message = "Malformed JSON";
            String errorMessage = extractJsonErrorMessage(cause.getMessage());
            details.put("error", "Invalid JSON syntax: " + errorMessage);
        } else if (cause instanceof JsonMappingException jsonMapping) {
            message = "Invalid request structure";
            String field = jsonMapping.getPath().isEmpty() ?
                    "request body" : jsonMapping.getPath().get(0).getFieldName();
            String errorMessage = extractJsonErrorMessage(jsonMapping.getMessage());
            details.put("field", field);
            details.put("error", errorMessage);
        } else {
            message = "Invalid request format";
            details.put("error", "Unable to parse request body");
        }

        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse(message, details, LocalDateTime.now()));
    }

    /**
     * Extracts a clean error message from Jackson's detailed error message
     */
    private String extractJsonErrorMessage(String fullMessage) {
        // Remove location information and code details
        String message = fullMessage.replaceAll("\\(code \\d+\\)", "")  // remove code references
                .replaceAll(" at \\[Source:.*\\]", "") // remove source location
                .replaceAll("\\s+", " ")               // normalize whitespace
                .trim();

        // If the message starts with "Unexpected character", make it more user-friendly
        if (message.contains("Unexpected character")) {
            return "Missing comma or incorrect JSON syntax";
        }

        return message;
    }

    /**
     * Handles all uncaught exceptions
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        log.error("Unexpected error occurred", ex);

        String errorMessage = ex.getMessage();
        // Handle null message case
        if (errorMessage == null) {
            errorMessage = "No additional error details available";
        }

        return ResponseEntity
                .internalServerError()
                .body(new ErrorResponse(
                        "An unexpected error occurred",
                        Map.of("error", errorMessage),
                        LocalDateTime.now()));
    }

}
