package com.guyshalev.Salt_security.service;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Component responsible for validating parameter values against specified types.
 * Supports various types including Int, String, Boolean, List, Date, Email, UUID, and Auth-Token.
 * Uses regex patterns and type checking for validation.
 */
@Component
public class TypeValidator {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}$");
    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    private static final Pattern AUTH_TOKEN_PATTERN = Pattern.compile("^Bearer [a-zA-Z0-9]+$");
    private static final String DATE_PATTERN = "dd-MM-yyyy";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN);

    /**
     * Validates a value against a specified type.
     * Supports multiple type validations including basic types and complex patterns.
     *
     * @param value the value to validate
     * @param type  the expected type (Int, String, Boolean, List, Date, Email, UUID, or Auth-Token)
     * @return true if the value matches the type, false otherwise
     */
    public boolean isValidType(Object value, String type) {
        if (value == null || type == null) return false;

        return switch (type) {
            case "Auth-Token" -> validateAuthToken(value);
            case "UUID" -> validateUUID(value);
            case "Email" -> validateEmail(value);
            case "Date" -> validateDate(value);
            case "Boolean" -> validateBoolean(value);
            case "Int" -> validateInt(value);
            case "String" -> value instanceof String;
            case "List" -> value instanceof List;
            default -> false;
        };
    }

    /**
     * Validates if a value represents a valid integer.
     * Accepts Integer, Long, or String representation of numbers.
     *
     * @param value the value to validate
     * @return true if the value is a valid integer, false otherwise
     */
    private boolean validateInt(Object value) {
        if (value instanceof Integer || value instanceof Long) {
            return true;
        }
        if (value instanceof String) {
            try {
                Long.parseLong((String) value);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }

    /**
     * Validates if a value represents a valid boolean.
     * Accepts Boolean objects or Strings "true"/"false" (case-insensitive).
     *
     * @param value the value to validate
     * @return true if the value is a valid boolean, false otherwise
     */
    private boolean validateBoolean(Object value) {
        if (value instanceof Boolean) {
            return true;
        }
        if (value instanceof String) {
            String strValue = ((String) value).toLowerCase().trim();
            return strValue.equals("true") || strValue.equals("false");
        }
        return false;
    }

    /**
     * Validates if a value represents a valid date in dd-MM-yyyy format.
     * Uses DateTimeFormatter for parsing and validation.
     *
     * @param value the value to validate
     * @return true if the value is a valid date, false otherwise
     */
    private boolean validateDate(Object value) {
        if (!(value instanceof String dateStr)) {
            return false;
        }

        try {
            LocalDate.parse(dateStr, DATE_FORMATTER);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private boolean validateEmail(Object value) {
        return value instanceof String && EMAIL_PATTERN.matcher((String) value).matches();
    }

    private boolean validateUUID(Object value) {
        return value instanceof String && UUID_PATTERN.matcher((String) value).matches();
    }

    private boolean validateAuthToken(Object value) {
        if (!(value instanceof String token)) {
            return false;
        }
        return AUTH_TOKEN_PATTERN.matcher(token).matches();
    }
}
