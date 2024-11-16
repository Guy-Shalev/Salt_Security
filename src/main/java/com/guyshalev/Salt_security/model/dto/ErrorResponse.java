package com.guyshalev.Salt_security.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standard error response format for the API.
 * Used to provide consistent error information across all endpoints.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponse {
    private String message;
    private Map<String, String> details;
    private LocalDateTime timestamp;
}