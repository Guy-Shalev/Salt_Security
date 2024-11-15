package com.guyshalev.Salt_security.service;

import com.guyshalev.Salt_security.dal.ModelRepository;
import com.guyshalev.Salt_security.mapper.ModelMapper;
import com.guyshalev.Salt_security.model.dto.ModelDTO;
import com.guyshalev.Salt_security.model.dto.RequestDTO;
import com.guyshalev.Salt_security.model.dto.RequestParameterDTO;
import com.guyshalev.Salt_security.model.dto.ValidationResultDTO;
import com.guyshalev.Salt_security.model.entity.Model;
import com.guyshalev.Salt_security.model.entity.Parameter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ValidationServiceTest {
    @Mock
    private ModelRepository modelRepository;

    @Mock
    private TypeValidator typeValidator;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private ValidationService validationService;

    private Model testModel;
    private RequestDTO testRequest;

    @BeforeEach
    void setUp() {
        // Setup test model
        testModel = new Model();
        testModel.setPath("/users/info");
        testModel.setMethod("GET");

        // Setup test request
        testRequest = new RequestDTO();
        testRequest.setPath("/users/info");
        testRequest.setMethod("GET");
    }

    @Test
    void saveModels_WithValidModels_ShouldSaveSuccessfully() {
        // Arrange
        List<ModelDTO> modelDTOs = Collections.singletonList(new ModelDTO());
        List<Model> models = Collections.singletonList(new Model());
        when(modelMapper.toEntityList(modelDTOs)).thenReturn(models);

        // Act
        validationService.saveModels(modelDTOs);

        // Assert
        verify(modelRepository).saveAll(models);
    }

    @Test
    void saveModels_WithEmptyList_ShouldNotCallRepository() {
        // Act
        validationService.saveModels(Collections.emptyList());

        // Assert
        verify(modelRepository, never()).saveAll(any());
    }

    @Test
    void validateRequest_WhenModelNotFound_ShouldReturnError() {
        // Arrange
        when(modelRepository.findByPathAndMethod(any(), any())).thenReturn(Optional.empty());

        // Act
        ValidationResultDTO result = validationService.validateRequest(testRequest);

        // Assert
        assertThat(result.isValid()).isFalse();
        assertThat(result.getAnomalies())
                .containsKey("global")
                .containsValue("No model found for path '/users/info' and method 'GET'");
    }

    @Test
    void validateRequest_WithNullRequest_ShouldReturnError() {
        // Act
        ValidationResultDTO result = validationService.validateRequest(null);

        // Assert
        assertThat(result.isValid()).isFalse();
        assertThat(result.getAnomalies())
                .containsKey("global")
                .containsValue("Request, path, and method cannot be null");
    }

    @Test
    void validateRequest_WithValidRequest_ShouldReturnSuccess() {
        // Arrange
        setupValidModel();
        setupValidRequest();
        when(modelRepository.findByPathAndMethod(testRequest.getPath(), testRequest.getMethod()))
                .thenReturn(Optional.of(testModel));
        when(typeValidator.isValidType(any(), any())).thenReturn(true);

        // Act
        ValidationResultDTO result = validationService.validateRequest(testRequest);

        // Assert
        assertThat(result.isValid()).isTrue();
        assertThat(result.getAnomalies()).isEmpty();
    }

    @Test
    void validateRequest_WithMissingRequiredParameter_ShouldReturnError() {
        // Arrange
        setupModelWithRequiredParameter();
        setupRequestWithMissingParameter();
        when(modelRepository.findByPathAndMethod(testRequest.getPath(), testRequest.getMethod()))
                .thenReturn(Optional.of(testModel));

        // Act
        ValidationResultDTO result = validationService.validateRequest(testRequest);

        // Assert
        assertThat(result.isValid()).isFalse();
        assertThat(result.getAnomalies())
                .containsKey("header.Authorization")
                .containsValue("Required parameter is missing");
    }

    @Test
    void validateRequest_WithInvalidParameterType_ShouldReturnError() {
        // Arrange
        setupModelWithAuthParameter();
        setupRequestWithInvalidAuthToken();
        when(modelRepository.findByPathAndMethod(testRequest.getPath(), testRequest.getMethod()))
                .thenReturn(Optional.of(testModel));
        when(typeValidator.isValidType(any(), eq("Auth-Token"))).thenReturn(false);

        // Act
        ValidationResultDTO result = validationService.validateRequest(testRequest);

        // Assert
        assertThat(result.isValid()).isFalse();
        assertThat(result.getAnomalies())
                .containsKey("header.Authorization")
                .containsValue("Value 'Invalid-Token' does not match any of the allowed types: Auth-Token");
    }

    @Test
    void validateRequest_WithUnexpectedParameter_ShouldReturnError() {
        // Arrange
        setupValidModel();
        setupRequestWithUnexpectedParameter();
        when(modelRepository.findByPathAndMethod(testRequest.getPath(), testRequest.getMethod()))
                .thenReturn(Optional.of(testModel));

        // Act
        ValidationResultDTO result = validationService.validateRequest(testRequest);

        // Assert
        assertThat(result.isValid()).isFalse();
        assertThat(result.getAnomalies())
                .containsKey("query.unexpected")
                .containsValue("Unexpected parameter");
    }

    @Test
    void validateRequest_WithMultipleValidationErrors_ShouldReturnAllErrors() {
        // Arrange
        setupModelWithMultipleParameters();
        setupRequestWithMultipleErrors();
        when(modelRepository.findByPathAndMethod(testRequest.getPath(), testRequest.getMethod()))
                .thenReturn(Optional.of(testModel));
        when(typeValidator.isValidType(any(), any())).thenReturn(false);

        // Act
        ValidationResultDTO result = validationService.validateRequest(testRequest);

        // Assert
        assertThat(result.isValid()).isFalse();
        assertThat(result.getAnomalies()).hasSize(2);
    }

    @Test
    void getAllModels_ShouldReturnMappedDTOs() {
        // Arrange
        List<Model> models = Collections.singletonList(new Model());
        List<ModelDTO> expected = Collections.singletonList(new ModelDTO());
        when(modelRepository.findAll()).thenReturn(models);
        when(modelMapper.toDTOList(models)).thenReturn(expected);

        // Act
        List<ModelDTO> result = validationService.getAllModels();

        // Assert
        assertThat(result).isEqualTo(expected);
        verify(modelRepository).findAll();
        verify(modelMapper).toDTOList(models);
    }

    private void setupValidModel() {
        Parameter authParam = new Parameter();
        authParam.setName("Authorization");
        authParam.setTypes(Collections.singletonList("Auth-Token"));
        authParam.setRequired(true);

        testModel.setHeaders(Collections.singletonList(authParam));
    }

    private void setupValidRequest() {
        RequestParameterDTO authParam = new RequestParameterDTO();
        authParam.setName("Authorization");
        authParam.setValue("Bearer valid-token");

        testRequest.setHeaders(Collections.singletonList(authParam));
    }

    private void setupModelWithRequiredParameter() {
        Parameter authParam = new Parameter();
        authParam.setName("Authorization");
        authParam.setTypes(Collections.singletonList("Auth-Token"));
        authParam.setRequired(true);

        testModel.setHeaders(Collections.singletonList(authParam));
    }

    private void setupRequestWithMissingParameter() {
        testRequest.setHeaders(Collections.emptyList());
    }

    private void setupModelWithAuthParameter() {
        Parameter authParam = new Parameter();
        authParam.setName("Authorization");
        authParam.setTypes(Collections.singletonList("Auth-Token"));
        authParam.setRequired(true);

        testModel.setHeaders(Collections.singletonList(authParam));
    }

    private void setupRequestWithInvalidAuthToken() {
        RequestParameterDTO authParam = new RequestParameterDTO();
        authParam.setName("Authorization");
        authParam.setValue("Invalid-Token");

        testRequest.setHeaders(Collections.singletonList(authParam));
    }

    private void setupRequestWithUnexpectedParameter() {
        RequestParameterDTO unexpectedParam = new RequestParameterDTO();
        unexpectedParam.setName("unexpected");
        unexpectedParam.setValue("value");

        testRequest.setQueryParams(Collections.singletonList(unexpectedParam));
    }

    private void setupModelWithMultipleParameters() {
        Parameter authParam = new Parameter();
        authParam.setName("Authorization");
        authParam.setTypes(Collections.singletonList("Auth-Token"));
        authParam.setRequired(true);

        Parameter boolParam = new Parameter();
        boolParam.setName("flag");
        boolParam.setTypes(Collections.singletonList("Boolean"));
        boolParam.setRequired(true);

        testModel.setHeaders(Arrays.asList(authParam, boolParam));
    }

    private void setupRequestWithMultipleErrors() {
        RequestParameterDTO authParam = new RequestParameterDTO();
        authParam.setName("Authorization");
        authParam.setValue("Invalid-Token");

        RequestParameterDTO invalidBoolParam = new RequestParameterDTO();
        invalidBoolParam.setName("flag");
        invalidBoolParam.setValue("not-a-boolean");

        testRequest.setHeaders(Arrays.asList(authParam, invalidBoolParam));
    }
}