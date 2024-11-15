package com.guyshalev.Salt_security.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

@Component
public class TypeValidator {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    private static final Pattern AUTH_TOKEN_PATTERN = Pattern.compile("^Bearer [a-zA-Z0-9]+$");
    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{2}-\\d{2}-\\d{4}$");

    public boolean isValidType(Object value, String type) {
        if (value == null) return false;

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

    private boolean validateDate(Object value) {
        return value instanceof String && DATE_PATTERN.matcher((String) value).matches();
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
