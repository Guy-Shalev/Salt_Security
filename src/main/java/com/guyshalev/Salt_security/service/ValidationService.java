package com.guyshalev.Salt_security.service;

import com.guyshalev.Salt_security.dal.ModelRepository;
import com.guyshalev.Salt_security.mapper.ModelMapper;
import com.guyshalev.Salt_security.model.dto.ModelDTO;
import com.guyshalev.Salt_security.model.dto.RequestDTO;
import com.guyshalev.Salt_security.model.dto.RequestParameterDTO;
import com.guyshalev.Salt_security.model.dto.ValidationResultDTO;
import com.guyshalev.Salt_security.model.entity.Model;
import com.guyshalev.Salt_security.model.entity.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class ValidationService {

    private final ModelRepository modelRepository;
    private final TypeValidator typeValidator;
    private final ModelMapper modelMapper;

    public ValidationService(ModelRepository modelRepository,
                             TypeValidator typeValidator,
                             ModelMapper modelMapper) {
        this.modelRepository = modelRepository;
        this.typeValidator = typeValidator;
        this.modelMapper = modelMapper;
    }

    @Transactional
    public void saveModels(List<ModelDTO> modelDTOs) {
        if (CollectionUtils.isEmpty(modelDTOs)) {
            log.warn("Attempted to save empty or null model list");
            return;
        }
        List<Model> models = modelMapper.toEntityList(modelDTOs);
        modelRepository.saveAll(models);
    }

    @Transactional(readOnly = true)
    public ValidationResultDTO validateRequest(RequestDTO request) {
        if (request == null || request.getPath() == null || request.getMethod() == null) {
            return new ValidationResultDTO(false,
                    Map.of("global", "Request, path, and method cannot be null"));
        }

        Optional<Model> modelOpt = modelRepository.findByPathAndMethod(request.getPath(), request.getMethod());

        if (modelOpt.isEmpty()) {
            return new ValidationResultDTO(false,
                    Map.of("global", String.format("No model found for path '%s' and method '%s'",
                            request.getPath(), request.getMethod())));
        }

        Model model = modelOpt.get();
        if (log.isDebugEnabled()) {
            log.debug("Validating request for path: {}, method: {}", request.getPath(), request.getMethod());
        }

        return validateRequestAgainstModel(request, model);
    }

    private ValidationResultDTO validateRequestAgainstModel(RequestDTO request, Model model) {
        Map<String, String> anomalies = new HashMap<>();

        validateParameterGroup(request.getQueryParams(), model.getQueryParams(), "query", anomalies);
        validateParameterGroup(request.getHeaders(), model.getHeaders(), "header", anomalies);
        validateParameterGroup(request.getBody(), model.getBody(), "body", anomalies);

        return new ValidationResultDTO(anomalies.isEmpty(), anomalies);
    }

    private void validateParameterGroup(List<RequestParameterDTO> requestParams,
                                        List<Parameter> modelParams,
                                        String location,
                                        Map<String, String> anomalies) {
        // Create maps for efficient lookup
        Map<String, RequestParameterDTO> requestParamMap = createParamMap(requestParams);
        Map<String, Parameter> modelParamMap = createParamMap(modelParams);

        // Validate required parameters and types
        validateRequiredAndTypes(requestParamMap, modelParamMap, location, anomalies);

        // Check for unexpected parameters
        validateUnexpectedParams(requestParamMap, modelParamMap, location, anomalies);
    }

    private <T> Map<String, T> createParamMap(List<T> params) {
        Map<String, T> paramMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(params)) {
            params.forEach(param -> {
                String name = param instanceof RequestParameterDTO ?
                        ((RequestParameterDTO) param).getName() :
                        ((Parameter) param).getName();
                paramMap.put(name, param);
            });
        }
        return paramMap;
    }

    private void validateRequiredAndTypes(Map<String, RequestParameterDTO> requestParamMap,
                                          Map<String, Parameter> modelParamMap,
                                          String location,
                                          Map<String, String> anomalies) {
        modelParamMap.forEach((paramName, modelParam) -> {
            RequestParameterDTO requestParam = requestParamMap.get(paramName);

            // Check required parameters
            if (modelParam.isRequired() && requestParam == null) {
                anomalies.put(location + "." + paramName, "Required parameter is missing");
                return;
            }

            // Validate value if parameter is present
            if (requestParam != null) {
                validateParameterValue(requestParam, modelParam, location, anomalies);
            }
        });
    }

    private void validateParameterValue(RequestParameterDTO requestParam,
                                        Parameter modelParam,
                                        String location,
                                        Map<String, String> anomalies) {
        boolean isValid = modelParam.getTypes().stream()
                .anyMatch(type -> typeValidator.isValidType(requestParam.getValue(), type));

        if (!isValid) {
            anomalies.put(
                    location + "." + requestParam.getName(),
                    String.format(
                            "Value '%s' does not match any of the allowed types: %s",
                            requestParam.getValue(),
                            String.join(", ", modelParam.getTypes())
                    )
            );
        }
    }

    private void validateUnexpectedParams(Map<String, RequestParameterDTO> requestParamMap,
                                          Map<String, Parameter> modelParamMap,
                                          String location,
                                          Map<String, String> anomalies) {
        requestParamMap.forEach((name, param) -> {
            if (!modelParamMap.containsKey(name)) {
                anomalies.put(location + "." + name, "Unexpected parameter");
            }
        });
    }

    @Transactional(readOnly = true)
    public List<ModelDTO> getAllModels() {
        return modelMapper.toDTOList(modelRepository.findAll());
    }
}
