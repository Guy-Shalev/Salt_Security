package com.guyshalev.Salt_security.validator;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class RequestValidator {

    private static final List<String> VALID_TYPES = List.of(
            "Int", "String", "Boolean", "List", "Date", "Email", "UUID", "Auth-Token"
    );

    public Map<String, String> validateRequest(JsonNode request) {
        Map<String, String> errors = new HashMap<>();

        if (!request.isObject()) {
            errors.put("root", "Must be a JSON object");
            return errors;
        }

        // Validate required fields
        if (!request.has("path") || !request.get("path").isTextual()) {
            errors.put("path", "Required and must be a string");
        }
        if (!request.has("method") || !request.get("method").isTextual()) {
            errors.put("method", "Required and must be a string");
        }

        // Validate parameter sections
        validateParameterSection(request, "query_params", errors);
        validateParameterSection(request, "headers", errors);
        validateParameterSection(request, "body", errors);

        return errors;
    }

    private void validateParameterSection(JsonNode root, String section, Map<String, String> errors) {
        JsonNode sectionNode = root.get(section);
        if (sectionNode == null) return;

        if (!sectionNode.isArray()) {
            errors.put(section, "Must be an array");
            return;
        }

        for (int i = 0; i < sectionNode.size(); i++) {
            JsonNode param = sectionNode.get(i);
            String path = section + "[" + i + "]";

            if (!param.isObject()) {
                errors.put(path, "Must be an object");
                continue;
            }

            if (!param.has("name") || !param.get("name").isTextual()) {
                errors.put(path + ".name", "Required and must be a string");
            }

            if (!param.has("value")) {
                errors.put(path + ".value", "Required");
            }
        }
    }

    public Map<String, String> validateModel(JsonNode model) {
        Map<String, String> errors = new HashMap<>();

        if (!model.isObject()) {
            errors.put("root", "Must be a JSON object");
            return errors;
        }

        // Validate required fields
        if (!model.has("path") || !model.get("path").isTextual()) {
            errors.put("path", "Required and must be a string");
        }
        if (!model.has("method") || !model.get("method").isTextual()) {
            errors.put("method", "Required and must be a string");
        }

        // Validate parameter sections
        validateModelParameterSection(model, "query_params", errors);
        validateModelParameterSection(model, "headers", errors);
        validateModelParameterSection(model, "body", errors);

        return errors;
    }

    private void validateModelParameterSection(JsonNode root, String section, Map<String, String> errors) {
        JsonNode sectionNode = root.get(section);
        if (sectionNode == null) return;

        if (!sectionNode.isArray()) {
            errors.put(section, "Must be an array");
            return;
        }

        for (int i = 0; i < sectionNode.size(); i++) {
            JsonNode param = sectionNode.get(i);
            String path = section + "[" + i + "]";

            if (!param.isObject()) {
                errors.put(path, "Must be an object");
                continue;
            }

            if (!param.has("name") || !param.get("name").isTextual()) {
                errors.put(path + ".name", "Required and must be a string");
            }

            validateTypes(param.get("types"), path, errors);

            if (!param.has("required") || !param.get("required").isBoolean()) {
                errors.put(path + ".required", "Required and must be a boolean");
            }
        }
    }

    private void validateTypes(JsonNode types, String path, Map<String, String> errors) {
        if (types == null || !types.isArray()) {
            errors.put(path + ".types", "Required and must be an array");
            return;
        }

        if (types.isEmpty()) {
            errors.put(path + ".types", "Must contain at least one type");
            return;
        }

        for (int i = 0; i < types.size(); i++) {
            JsonNode type = types.get(i);
            if (!type.isTextual() || !VALID_TYPES.contains(type.asText())) {
                errors.put(path + ".types[" + i + "]",
                        "Must be one of: " + String.join(", ", VALID_TYPES));
            }
        }
    }
}
