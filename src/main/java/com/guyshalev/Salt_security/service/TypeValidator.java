package com.guyshalev.Salt_security.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/**
 * Component responsible for validating parameter values against specified types.
 * Handles validation of various data types including primitives and complex types.
 */
@Component
public class TypeValidator {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}$");
    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    private static final Pattern AUTH_TOKEN_PATTERN = Pattern.compile("^Bearer [a-zA-Z0-9]+$");
    private static final String DATE_PATTERN = "dd-MM-yyyy";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN);

    /**
     * Validates a JSON value against a specified type.
     *
     * @param value The JSON value to validate
     * @param type The expected type. Must be one of: "Auth-Token", "UUID", "Email", "Date",
     *             "Boolean", "Int", "String", or "List"
     * @return true if the value matches the type, false otherwise
     */
    public boolean isValidType(JsonNode value, String type) {
        if (value == null || type == null) return false;

        return switch (type) {
            case "Auth-Token" -> validateAuthToken(value);
            case "UUID" -> validateUUID(value);
            case "Email" -> validateEmail(value);
            case "Date" -> validateDate(value);
            case "Boolean" -> validateBoolean(value);
            case "Int" -> validateInt(value);
            case "String" -> validateString(value);
            case "List" -> validateList(value);
            default -> false;
        };
    }

    private boolean validateString(JsonNode value) {
        return value.isTextual();
    }

    private boolean validateInt(JsonNode value) {
        return value.isInt() || value.isLong() ||
                (value.isTextual() && value.asText().matches("-?\\d+"));
    }

    /**
     * Validates if a value is a valid boolean.
     * Accepts both boolean nodes and string representations ("true"/"false").
     *
     * @param value The value to validate
     * @return true if the value is a valid boolean, false otherwise
     */
    private boolean validateBoolean(JsonNode value) {
        return value.isBoolean() ||
                (value.isTextual() && (value.asText().equalsIgnoreCase("true")
                        || value.asText().equalsIgnoreCase("false")));
    }

    /**
     * Validates if a value is a valid date in dd-MM-yyyy format.
     *
     * @param value The value to validate
     * @return true if the value is a valid date string, false otherwise
     */
    private boolean validateDate(JsonNode value) {
        if (!value.isTextual()) return false;
        try {
            LocalDate.parse(value.asText(), DATE_FORMATTER);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private boolean validateEmail(JsonNode value) {
        return value.isTextual() && EMAIL_PATTERN.matcher(value.asText()).matches();
    }

    private boolean validateUUID(JsonNode value) {
        return value.isTextual() && UUID_PATTERN.matcher(value.asText()).matches();
    }

    private boolean validateAuthToken(JsonNode value) {
        return value.isTextual() && AUTH_TOKEN_PATTERN.matcher(value.asText()).matches();
    }

    private boolean validateList(JsonNode value) {
        return value.isArray();
    }
}
