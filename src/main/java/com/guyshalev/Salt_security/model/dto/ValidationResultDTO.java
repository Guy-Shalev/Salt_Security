package com.guyshalev.Salt_security.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Setter
@Getter
public class ValidationResultDTO {
    private boolean valid;
    private Map<String, String> anomalies;

    public ValidationResultDTO(boolean valid, Map<String, String> anomalies) {
        this.valid = valid;
        this.anomalies = anomalies;
    }

}
