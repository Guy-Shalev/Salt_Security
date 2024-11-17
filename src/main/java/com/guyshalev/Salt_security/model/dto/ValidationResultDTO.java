package com.guyshalev.Salt_security.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
public class ValidationResultDTO {
    private boolean valid;
    private Map<String, String> anomalies;
}
