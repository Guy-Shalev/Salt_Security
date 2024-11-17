package com.guyshalev.Salt_security.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guyshalev.Salt_security.dal.ModelRepository;
import com.guyshalev.Salt_security.mapper.ModelMapper;
import com.guyshalev.Salt_security.model.dto.ModelDTO;
import com.guyshalev.Salt_security.model.dto.ValidationResultDTO;
import com.guyshalev.Salt_security.model.entity.Model;
import com.guyshalev.Salt_security.validator.RequestValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ValidationService.
 * Tests the service layer in isolation using mocks for dependencies.
 */
@ExtendWith(MockitoExtension.class)
class ValidationServiceTest {

    @Mock
    private ModelRepository modelRepository;
    @Mock
    private RequestValidator requestValidator;
    @Mock
    private TypeValidator typeValidator;
    @Mock
    private ModelMapper modelMapper;

    private ValidationService validationService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();
        validationService = new ValidationService(
                modelRepository,
                objectMapper,
                requestValidator,
                typeValidator,
                modelMapper
        );
    }

    /**
     * SaveModels Tests
     */
    @Test
    void whenSavingValidModels_thenSuccess() throws Exception {
        // Prepare test data
        String validModels = """
                [{
                    "path": "/test",
                    "method": "GET",
                    "query_params": [],
                    "headers": [],
                    "body": []
                }]""";

        // Configure mocks
        when(requestValidator.validateModel(any(JsonNode.class))).thenReturn(new HashMap<>());

        // Execute
        assertDoesNotThrow(() -> validationService.saveModels(validModels));

        // Verify
        verify(modelRepository).deleteAll();
        verify(modelRepository).save(any(Model.class));
    }

    @Test
    void whenSavingInvalidJson_thenThrowsException() {
        String invalidJson = "invalid json";

        assertThrows(IllegalArgumentException.class,
                () -> validationService.saveModels(invalidJson));

        verify(modelRepository, never()).save(any());
    }

    @Test
    void whenSavingNonArrayJson_thenThrowsException() throws Exception {
        String nonArrayJson = """
                {
                    "path": "/test",
                    "method": "GET"
                }""";

        assertThrows(IllegalArgumentException.class,
                () -> validationService.saveModels(nonArrayJson));

        verify(modelRepository, never()).save(any());
    }

    @Test
    void whenModelValidationFails_thenThrowsException() throws Exception {
        // Prepare test data
        String models = """
                [{
                    "path": "/test",
                    "method": "GET"
                }]""";

        // Configure mock to return validation errors
        when(requestValidator.validateModel(any(JsonNode.class)))
                .thenReturn(Collections.singletonMap("error", "Invalid model"));

        // Execute and verify
        assertThrows(IllegalArgumentException.class,
                () -> validationService.saveModels(models));

        verify(modelRepository, never()).save(any());
    }

    /**
     * GetAllModels Tests
     */
    @Test
    void whenGettingAllModels_thenSuccess() {
        // Prepare test data
        List<Model> models = Collections.singletonList(new Model("/test", "GET", "{}"));
        List<ModelDTO> expectedDtos = Collections.singletonList(new ModelDTO());

        // Configure mocks
        when(modelRepository.findAll()).thenReturn(models);
        when(modelMapper.toDTOList(models)).thenReturn(expectedDtos);

        // Execute
        List<ModelDTO> result = validationService.getAllModels();

        // Verify
        assertEquals(expectedDtos, result);
        verify(modelRepository).findAll();
        verify(modelMapper).toDTOList(models);
    }

    /**
     * ValidateRequest Tests
     */
    @Test
    void whenValidatingValidRequest_thenSuccess() throws Exception {
        // Prepare test data
        String validRequest = """
                {
                    "path": "/test",
                    "method": "GET",
                    "query_params": [],
                    "headers": [],
                    "body": []
                }""";

        Model model = new Model("/test", "GET", validRequest);

        // Configure mocks
        when(requestValidator.validateRequest(any(JsonNode.class))).thenReturn(new HashMap<>());
        when(modelRepository.findByPathAndMethod(anyString(), anyString()))
                .thenReturn(Optional.of(model));

        // Execute
        ValidationResultDTO result = validationService.validateRequest(validRequest);

        // Verify
        assertTrue(result.isValid());
        assertTrue(result.getAnomalies().isEmpty());
    }

    @Test
    void whenValidatingInvalidJson_thenReturnsError() {
        String invalidJson = "invalid json";

        ValidationResultDTO result = validationService.validateRequest(invalidJson);

        assertFalse(result.isValid());
        assertFalse(result.getAnomalies().isEmpty());
    }

    @Test
    void whenNoMatchingModel_thenReturnsError() throws Exception {
        // Prepare test data
        String request = """
                {
                    "path": "/test",
                    "method": "GET",
                    "query_params": [],
                    "headers": [],
                    "body": []
                }""";

        // Configure mock
        when(requestValidator.validateRequest(any(JsonNode.class))).thenReturn(new HashMap<>());
        when(modelRepository.findByPathAndMethod(anyString(), anyString()))
                .thenReturn(Optional.empty());

        // Execute
        ValidationResultDTO result = validationService.validateRequest(request);

        // Verify
        assertFalse(result.isValid());
        assertTrue(result.getAnomalies().containsKey("error"));
        assertTrue(result.getAnomalies().get("error").contains("No model found"));
    }

    @Test
    void whenRequestValidationFails_thenReturnsErrors() throws Exception {
        // Prepare test data
        String request = """
                {
                    "path": "/test",
                    "method": "GET",
                    "query_params": [],
                    "headers": [],
                    "body": []
                }""";

        // Configure mock to return validation errors
        when(requestValidator.validateRequest(any(JsonNode.class)))
                .thenReturn(Collections.singletonMap("error", "Invalid request"));

        // Execute
        ValidationResultDTO result = validationService.validateRequest(request);

        // Verify
        assertFalse(result.isValid());
        assertFalse(result.getAnomalies().isEmpty());
    }

    @Test
    void whenParameterTypeMismatch_thenReturnsError() throws Exception {
        // Prepare test data
        String request = """
                {
                    "path": "/test",
                    "method": "GET",
                    "query_params": [
                        {
                            "name": "param1",
                            "value": "not-a-number"
                        }
                    ],
                    "headers": [],
                    "body": []
                }""";

        String modelJson = """
                {
                    "path": "/test",
                    "method": "GET",
                    "query_params": [
                        {
                            "name": "param1",
                            "types": ["Int"],
                            "required": true
                        }
                    ],
                    "headers": [],
                    "body": []
                }""";

        Model model = new Model("/test", "GET", modelJson);

        // Configure mocks
        when(requestValidator.validateRequest(any(JsonNode.class))).thenReturn(new HashMap<>());
        when(modelRepository.findByPathAndMethod(anyString(), anyString()))
                .thenReturn(Optional.of(model));
        when(typeValidator.isValidType(any(), eq("Int"))).thenReturn(false);

        // Execute
        ValidationResultDTO result = validationService.validateRequest(request);

        // Verify
        assertFalse(result.isValid());
        assertTrue(result.getAnomalies().containsKey("query_params.param1"));
    }

    @Test
    void whenMissingRequiredParameter_thenReturnsError() throws Exception {
        // Prepare test data
        String request = """
                {
                    "path": "/test",
                    "method": "GET",
                    "query_params": [],
                    "headers": [],
                    "body": []
                }""";

        String modelJson = """
                {
                    "path": "/test",
                    "method": "GET",
                    "query_params": [
                        {
                            "name": "required_param",
                            "types": ["String"],
                            "required": true
                        }
                    ],
                    "headers": [],
                    "body": []
                }""";

        Model model = new Model("/test", "GET", modelJson);

        // Configure mocks
        when(requestValidator.validateRequest(any(JsonNode.class))).thenReturn(new HashMap<>());
        when(modelRepository.findByPathAndMethod(anyString(), anyString()))
                .thenReturn(Optional.of(model));

        // Execute
        ValidationResultDTO result = validationService.validateRequest(request);

        // Verify
        assertFalse(result.isValid());
        assertTrue(result.getAnomalies().containsKey("query_params.required_param"));
        assertEquals("Required parameter is missing",
                result.getAnomalies().get("query_params.required_param"));
    }
}