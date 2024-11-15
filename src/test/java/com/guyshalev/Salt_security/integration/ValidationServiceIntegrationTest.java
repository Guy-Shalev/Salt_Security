package com.guyshalev.Salt_security.integration;

import com.guyshalev.Salt_security.model.dto.*;
import com.guyshalev.Salt_security.service.ValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ValidationServiceIntegrationTest {

    @Autowired
    private ValidationService validationService;

    private ModelDTO testModel;
    private RequestDTO testRequest;

    @BeforeEach
    void setUp() {
        // Create a test model
        testModel = createTestModel();
    }

    private ModelDTO createTestModel() {
        ModelDTO model = new ModelDTO();
        model.setPath("/users/info");
        model.setMethod("GET");

        // Add query parameters
        ParameterDTO queryParam = new ParameterDTO();
        queryParam.setName("with_extra_data");
        queryParam.setTypes(Collections.singletonList("Boolean"));
        queryParam.setRequired(false);
        model.setQueryParams(Collections.singletonList(queryParam));

        // Add headers
        ParameterDTO headerParam = new ParameterDTO();
        headerParam.setName("Authorization");
        headerParam.setTypes(Collections.singletonList("Auth-Token"));
        headerParam.setRequired(true);
        model.setHeaders(Collections.singletonList(headerParam));

        model.setBody(Collections.emptyList());
        return model;
    }

    private RequestDTO createValidRequest() {
        RequestDTO request = new RequestDTO();
        request.setPath("/users/info");
        request.setMethod("GET");

        // Add query parameter
        RequestParameterDTO queryParam = new RequestParameterDTO();
        queryParam.setName("with_extra_data");
        queryParam.setValue(false);
        request.setQueryParams(Collections.singletonList(queryParam));

        // Add header
        RequestParameterDTO headerParam = new RequestParameterDTO();
        headerParam.setName("Authorization");
        headerParam.setValue("Bearer abc123");
        request.setHeaders(Collections.singletonList(headerParam));

        request.setBody(Collections.emptyList());
        return request;
    }

    private ModelDTO createAnotherTestModel() {
        ModelDTO model = new ModelDTO();
        model.setPath("/users/create");
        model.setMethod("POST");

        // Add body parameters
        ParameterDTO emailParam = new ParameterDTO();
        emailParam.setName("email");
        emailParam.setTypes(Collections.singletonList("Email"));
        emailParam.setRequired(true);

        ParameterDTO nameParam = new ParameterDTO();
        nameParam.setName("name");
        nameParam.setTypes(Collections.singletonList("String"));
        nameParam.setRequired(true);

        model.setBody(Arrays.asList(emailParam, nameParam));
        model.setQueryParams(Collections.emptyList());
        model.setHeaders(Collections.emptyList());
        return model;
    }

    private RequestDTO createRequestWithInvalidParameterType() {
        RequestDTO request = createValidRequest();
        request.getQueryParams().get(0).setValue("invalid-boolean");
        return request;
    }

    private RequestDTO createRequestMissingRequiredParameter() {
        RequestDTO request = new RequestDTO();
        request.setPath("/users/info");
        request.setMethod("GET");
        request.setQueryParams(Collections.emptyList());
        request.setHeaders(Collections.emptyList());
        request.setBody(Collections.emptyList());
        return request;
    }

    private RequestDTO createRequestWithUnexpectedParameter() {
        RequestDTO request = createValidRequest();
        RequestParameterDTO unexpectedParam = new RequestParameterDTO();
        unexpectedParam.setName("unexpected_param");
        unexpectedParam.setValue("value");
        request.setQueryParams(Arrays.asList(request.getQueryParams().get(0), unexpectedParam));
        return request;
    }

    @Test
    void fullValidationFlow_ValidRequest_ShouldSucceed() {
        // Save model
        validationService.saveModels(Collections.singletonList(testModel));

        // Create valid request
        RequestDTO request = createValidRequest();

        // Validate request
        ValidationResultDTO result = validationService.validateRequest(request);

        // Assert
        assertThat(result.isValid()).isTrue();
        assertThat(result.getAnomalies()).isEmpty();
    }

    private RequestDTO createInvalidRequest() {
        RequestDTO request = createValidRequest();
        RequestParameterDTO queryParam = request.getQueryParams().get(0);
        queryParam.setValue("not-a-boolean");
        return request;
    }

    @Test
    void fullValidationFlow_InvalidRequest_ShouldFail() {
        // Save model
        validationService.saveModels(Collections.singletonList(testModel));

        // Create invalid request
        RequestDTO request = createInvalidRequest();

        // Validate request
        ValidationResultDTO result = validationService.validateRequest(request);

        // Assert
        assertThat(result.isValid()).isFalse();
        assertThat(result.getAnomalies()).isNotEmpty();
    }

    @Test
    void saveAndRetrieveModels_ShouldMatch() {
        // Create multiple models
        List<ModelDTO> models = Arrays.asList(
                createTestModel(),
                createAnotherTestModel()
        );

        // Save models
        validationService.saveModels(models);

        // Retrieve models
        List<ModelDTO> retrievedModels = validationService.getAllModels();

        // Assert
        assertThat(retrievedModels).hasSameSizeAs(models);
        assertThat(retrievedModels)
                .extracting(ModelDTO::getPath, ModelDTO::getMethod)
                .containsExactlyInAnyOrderElementsOf(
                        models.stream()
                                .map(m -> tuple(m.getPath(), m.getMethod()))
                                .collect(Collectors.toList())
                );

        // Additional assertions for parameters
        for (ModelDTO originalModel : models) {
            ModelDTO retrievedModel = retrievedModels.stream()
                    .filter(m -> m.getPath().equals(originalModel.getPath())
                            && m.getMethod().equals(originalModel.getMethod()))
                    .findFirst()
                    .orElseThrow();

            // Compare parameters
            assertThat(retrievedModel.getQueryParams()).hasSameSizeAs(originalModel.getQueryParams());
            assertThat(retrievedModel.getHeaders()).hasSameSizeAs(originalModel.getHeaders());
            assertThat(retrievedModel.getBody()).hasSameSizeAs(originalModel.getBody());
        }
    }

    @Test
    void validateRequest_WithMissingModel_ShouldFail() {
        RequestDTO request = createValidRequest();
        ValidationResultDTO result = validationService.validateRequest(request);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getAnomalies())
                .containsKey("global")
                .containsValue(String.format("No model found for path '%s' and method '%s'",
                        request.getPath(), request.getMethod()));
    }

    @Test
    void validateRequest_WithMissingRequiredParameter_ShouldFail() {
        // Save model
        validationService.saveModels(Collections.singletonList(testModel));

        // Create request missing required parameter
        RequestDTO request = createRequestMissingRequiredParameter();

        // Validate
        ValidationResultDTO result = validationService.validateRequest(request);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getAnomalies())
                .containsKey("header.Authorization");
    }

    @Test
    void validateRequest_WithInvalidParameterType_ShouldFail() {
        // Save model
        validationService.saveModels(Collections.singletonList(testModel));

        // Create request with invalid parameter type
        RequestDTO request = createRequestWithInvalidParameterType();

        // Validate
        ValidationResultDTO result = validationService.validateRequest(request);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getAnomalies())
                .containsKey("query.with_extra_data");
    }

    @Test
    void validateRequest_WithUnexpectedParameter_ShouldFail() {
        // Save model
        validationService.saveModels(Collections.singletonList(testModel));

        // Create request with unexpected parameter
        RequestDTO request = createRequestWithUnexpectedParameter();

        // Validate
        ValidationResultDTO result = validationService.validateRequest(request);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getAnomalies())
                .containsKey("query.unexpected_param");
    }
}
