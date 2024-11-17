package com.guyshalev.Salt_security.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guyshalev.Salt_security.dal.ModelRepository;
import com.guyshalev.Salt_security.mapper.ModelMapper;
import com.guyshalev.Salt_security.model.dto.ModelDTO;
import com.guyshalev.Salt_security.model.dto.ValidationResultDTO;
import com.guyshalev.Salt_security.model.entity.Model;
import com.guyshalev.Salt_security.validator.RequestValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service responsible for validating API requests against predefined models.
 * Provides functionality to store models and validate incoming requests against these models.
 */
@Service
@Slf4j
public class ValidationService {

    private final ModelRepository modelRepository;
    private final ObjectMapper objectMapper;
    private final RequestValidator requestValidator;
    private final TypeValidator typeValidator;
    private final ModelMapper modelMapper;

    public ValidationService(ModelRepository modelRepository,
                             ObjectMapper objectMapper,
                             RequestValidator requestValidator,
                             TypeValidator typeValidator,
                             ModelMapper modelMapper) {
        this.modelRepository = modelRepository;
        this.objectMapper = objectMapper;
        this.requestValidator = requestValidator;
        this.typeValidator = typeValidator;
        this.modelMapper = modelMapper;
    }


    /**
     * Saves API models to the database. Replaces all existing models with the new ones.
     * Validates each model's structure before saving.
     *
     * @param jsonModels JSON string containing an array of API models
     * @throws IllegalArgumentException if the input is not a valid JSON array or contains invalid models
     */
    @Transactional
    public void saveModels(String jsonModels) {
        try {
            JsonNode modelsNode = objectMapper.readTree(jsonModels);

            if (!modelsNode.isArray()) {
                throw new IllegalArgumentException("Input must be an array of models");
            }

            modelRepository.deleteAll();

            for (JsonNode modelNode : modelsNode) {
                // Validate model structure
                Map<String, String> validationErrors = requestValidator.validateModel(modelNode);
                if (!validationErrors.isEmpty()) {
                    throw new IllegalArgumentException("Invalid model structure: " + validationErrors);
                }

                String path = modelNode.get("path").asText();
                String method = modelNode.get("method").asText();
                modelRepository.save(new Model(path, method, modelNode.toString()));
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to process models: " + e.getMessage());
        }
    }

    /**
     * Validates an API request against stored models.
     * Performs both structural validation and type checking against the matching model.
     *
     * @param jsonRequest JSON string containing the request to validate
     * @return ValidationResultDTO containing validation result and any anomalies found
     */
    @Transactional(readOnly = true)
    public ValidationResultDTO validateRequest(String jsonRequest) {
        try {
            JsonNode request = objectMapper.readTree(jsonRequest);

            // Validate request structure
            Map<String, String> structureErrors = requestValidator.validateRequest(request);
            if (!structureErrors.isEmpty()) {
                return new ValidationResultDTO(false, structureErrors);
            }

            String path = request.get("path").asText();
            String method = request.get("method").asText();

            // Find matching model
            Optional<Model> modelOpt = modelRepository.findByPathAndMethod(path, method);
            if (modelOpt.isEmpty()) {
                return new ValidationResultDTO(false,
                        Map.of("error", "No model found for path '" + path + "' and method '" + method + "'"));
            }

            JsonNode model = objectMapper.readTree(modelOpt.get().getJsonContent());
            Map<String, String> anomalies = validateAgainstModel(request, model);

            return new ValidationResultDTO(anomalies.isEmpty(), anomalies);
        } catch (Exception e) {
            return new ValidationResultDTO(false,
                    Map.of("error", "Failed to validate request: " + e.getMessage()));
        }
    }

    /**
     * Validates a request against a specific model.
     * Checks all parameters sections (query_params, headers, body).
     *
     * @param request The request to validate
     * @param model The model to validate against
     * @return Map of validation anomalies found, empty if valid
     */
    private Map<String, String> validateAgainstModel(JsonNode request, JsonNode model) {
        Map<String, String> anomalies = new HashMap<>();
        validateParameterSection(request, model, "query_params", anomalies);
        validateParameterSection(request, model, "headers", anomalies);
        validateParameterSection(request, model, "body", anomalies);
        return anomalies;
    }

    /**
     * Validates a specific section of parameters in the request against the model.
     * Checks for required parameters, unexpected parameters, and type validation.
     *
     * @param request The request containing parameters
     * @param model The model containing parameter definitions
     * @param section The section to validate (query_params, headers, or body)
     * @param anomalies Map to store any validation anomalies found
     */
    private void validateParameterSection(JsonNode request, JsonNode model, String section, Map<String, String> anomalies) {
        JsonNode requestParams = request.get(section);
        JsonNode modelParams = model.get(section);

        if (modelParams == null || !modelParams.isArray()) {
            return;
        }

        // Create map of model parameters
        Map<String, JsonNode> modelParamsMap = new HashMap<>();
        for (JsonNode param : modelParams) {
            modelParamsMap.put(param.get("name").asText(), param);
        }

        // Check request parameters
        if (requestParams != null && requestParams.isArray()) {
            for (JsonNode requestParam : requestParams) {
                String paramName = requestParam.get("name").asText();
                JsonNode modelParam = modelParamsMap.get(paramName);

                if (modelParam == null) {
                    anomalies.put(section + "." + paramName, "Unexpected parameter");
                    continue;
                }

                validateParameterValue(requestParam, modelParam, section + "." + paramName, anomalies);
            }
        }

        // Check for missing required parameters
        for (Map.Entry<String, JsonNode> entry : modelParamsMap.entrySet()) {
            if (entry.getValue().get("required").asBoolean() &&
                    (requestParams == null || !hasParameter(requestParams, entry.getKey()))) {
                anomalies.put(section + "." + entry.getKey(), "Required parameter is missing");
            }
        }
    }

    /**
     * Validates a single parameter value against its model definition.
     *
     * @param requestParam The parameter from the request
     * @param modelParam The parameter definition from the model
     * @param path The path of the parameter for error reporting
     * @param anomalies Map to store any validation anomalies found
     */
    private void validateParameterValue(JsonNode requestParam, JsonNode modelParam, String path, Map<String, String> anomalies) {
        JsonNode value = requestParam.get("value");
        if (value == null) {
            anomalies.put(path, "Value is missing");
            return;
        }

        boolean isValid = false;
        for (JsonNode type : modelParam.get("types")) {
            if (typeValidator.isValidType(value, type.asText())) {
                isValid = true;
                break;
            }
        }

        if (!isValid) {
            anomalies.put(path, String.format(
                    "Value '%s' does not match any of the allowed types: %s",
                    value.toString(),
                    modelParam.get("types").toString()
            ));
        }
    }

    /**
     * Checks if a parameter with the given name exists in the parameters array.
     *
     * @param params The parameters array to search
     * @param paramName The name of the parameter to find
     * @return true if the parameter exists, false otherwise
     */
    private boolean hasParameter(JsonNode params, String paramName) {
        for (JsonNode param : params) {
            if (param.get("name").asText().equals(paramName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Retrieves all stored API models.
     *
     * @return List of ModelDTO objects representing all stored models
     */
    @Transactional(readOnly = true)
    public List<ModelDTO> getAllModels() {
        return modelMapper.toDTOList(modelRepository.findAll());
    }
}
