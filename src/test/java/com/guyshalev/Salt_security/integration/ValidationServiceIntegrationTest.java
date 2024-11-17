package com.guyshalev.Salt_security.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guyshalev.Salt_security.model.dto.ModelDTO;
import com.guyshalev.Salt_security.model.dto.ValidationResultDTO;
import com.guyshalev.Salt_security.service.ValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for ValidationService.
 * Tests the complete flow of saving models and validating requests.
 */
@SpringBootTest
@Transactional
class ValidationServiceIntegrationTest {

    @Autowired
    private ValidationService validationService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() throws Exception {
        String validModel = """
                [{
                    "path": "/users/info",
                    "method": "GET",
                    "query_params": [
                        {
                            "name": "with_extra_data",
                            "types": ["Boolean"],
                            "required": false
                        },
                        {
                            "name": "user_id",
                            "types": ["Int", "UUID"],
                            "required": false
                        }
                    ],
                    "headers": [
                        {
                            "name": "Authorization",
                            "types": ["Auth-Token"],
                            "required": true
                        }
                    ],
                    "body": []
                }]""";
        validationService.saveModels(validModel);
    }

    @Test
    void whenSavingValidModel_thenSuccess() {
        List<ModelDTO> models = validationService.getAllModels();
        assertEquals(1, models.size());
        assertEquals("/users/info", models.get(0).getPath());
        assertEquals("GET", models.get(0).getMethod());
    }

    @Test
    void whenSavingInvalidModel_thenThrowsException() {
        String invalidModel = """
                [{
                    "path": "/users/info",
                    "method": "GET",
                    "query_params": [
                        {
                            "name": "param1",
                            "types": ["InvalidType"],
                            "required": false
                        }
                    ]
                }]""";

        assertThrows(IllegalArgumentException.class, () -> validationService.saveModels(invalidModel));
    }

    @Test
    void whenValidatingValidRequest_thenSuccess() throws Exception {
        String validRequest = """
                {
                    "path": "/users/info",
                    "method": "GET",
                    "query_params": [
                        {
                            "name": "with_extra_data",
                            "value": false
                        }
                    ],
                    "headers": [
                        {
                            "name": "Authorization",
                            "value": "Bearer abc123def456"
                        }
                    ],
                    "body": []
                }""";

        ValidationResultDTO result = validationService.validateRequest(validRequest);
        assertTrue(result.isValid());
        assertTrue(result.getAnomalies().isEmpty());
    }

    @Test
    void whenValidatingRequestWithMissingRequiredHeader_thenFails() throws Exception {
        String requestWithMissingHeader = """
                {
                    "path": "/users/info",
                    "method": "GET",
                    "query_params": [
                        {
                            "name": "with_extra_data",
                            "value": false
                        }
                    ],
                    "headers": [],
                    "body": []
                }""";

        ValidationResultDTO result = validationService.validateRequest(requestWithMissingHeader);
        assertFalse(result.isValid());
        assertTrue(result.getAnomalies().containsKey("headers.Authorization"));
        assertEquals("Required parameter is missing", result.getAnomalies().get("headers.Authorization"));
    }

    @Test
    void whenValidatingRequestWithInvalidType_thenFails() throws Exception {
        String requestWithInvalidType = """
                {
                    "path": "/users/info",
                    "method": "GET",
                    "query_params": [
                        {
                            "name": "with_extra_data",
                            "value": "not-a-boolean"
                        }
                    ],
                    "headers": [
                        {
                            "name": "Authorization",
                            "value": "Bearer abc123def456"
                        }
                    ],
                    "body": []
                }""";

        ValidationResultDTO result = validationService.validateRequest(requestWithInvalidType);
        assertFalse(result.isValid());
        assertTrue(result.getAnomalies().containsKey("query_params.with_extra_data"));
    }

    @Test
    void whenValidatingRequestWithInvalidAuthToken_thenFails() throws Exception {
        String requestWithInvalidToken = """
                {
                    "path": "/users/info",
                    "method": "GET",
                    "query_params": [],
                    "headers": [
                        {
                            "name": "Authorization",
                            "value": "InvalidToken"
                        }
                    ],
                    "body": []
                }""";

        ValidationResultDTO result = validationService.validateRequest(requestWithInvalidToken);
        assertFalse(result.isValid());
        assertTrue(result.getAnomalies().containsKey("headers.Authorization"));
    }

    @Test
    void whenValidatingRequestWithUnexpectedParameter_thenFails() throws Exception {
        String requestWithExtraParam = """
                {
                    "path": "/users/info",
                    "method": "GET",
                    "query_params": [
                        {
                            "name": "unexpected_param",
                            "value": "test"
                        }
                    ],
                    "headers": [
                        {
                            "name": "Authorization",
                            "value": "Bearer abc123def456"
                        }
                    ],
                    "body": []
                }""";

        ValidationResultDTO result = validationService.validateRequest(requestWithExtraParam);
        assertFalse(result.isValid());
        assertTrue(result.getAnomalies().containsKey("query_params.unexpected_param"));
        assertEquals("Unexpected parameter", result.getAnomalies().get("query_params.unexpected_param"));
    }

    @Test
    void whenValidatingRequestForNonExistentPath_thenFails() throws Exception {
        String requestWithWrongPath = """
                {
                    "path": "/non/existent",
                    "method": "GET",
                    "query_params": [],
                    "headers": [],
                    "body": []
                }""";

        ValidationResultDTO result = validationService.validateRequest(requestWithWrongPath);
        assertFalse(result.isValid());
        assertEquals(
                "No model found for path '/non/existent' and method 'GET'",
                result.getAnomalies().get("error")
        );
    }

    @Test
    void whenValidatingMalformedJson_thenFails() {
        String malformedJson = "{ invalid json }";

        ValidationResultDTO result = validationService.validateRequest(malformedJson);
        assertFalse(result.isValid());
        assertNotNull(result.getAnomalies().get("error"));
    }

    @Test
    void whenValidatingWithMultipleTypes_thenSucceeds() throws Exception {
        String requestWithUUID = """
                {
                    "path": "/users/info",
                    "method": "GET",
                    "query_params": [
                        {
                            "name": "user_id",
                            "value": "123e4567-e89b-12d3-a456-426614174000"
                        }
                    ],
                    "headers": [
                        {
                            "name": "Authorization",
                            "value": "Bearer abc123def456"
                        }
                    ],
                    "body": []
                }""";

        ValidationResultDTO result = validationService.validateRequest(requestWithUUID);
        assertTrue(result.isValid());
        assertTrue(result.getAnomalies().isEmpty());

        String requestWithInt = """
                {
                    "path": "/users/info",
                    "method": "GET",
                    "query_params": [
                        {
                            "name": "user_id",
                            "value": 12345
                        }
                    ],
                    "headers": [
                        {
                            "name": "Authorization",
                            "value": "Bearer abc123def456"
                        }
                    ],
                    "body": []
                }""";

        result = validationService.validateRequest(requestWithInt);
        assertTrue(result.isValid());
        assertTrue(result.getAnomalies().isEmpty());
    }
}