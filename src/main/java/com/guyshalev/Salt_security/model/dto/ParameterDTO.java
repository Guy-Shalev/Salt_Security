package com.guyshalev.Salt_security.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class ParameterDTO {

    private String name;
    private List<String> types;
    private boolean required;

}
