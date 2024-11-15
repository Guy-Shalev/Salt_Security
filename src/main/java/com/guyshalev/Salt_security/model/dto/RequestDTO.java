package com.guyshalev.Salt_security.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class RequestDTO {

    private String path;
    private String method;
    @JsonProperty("query_params")
    private List<RequestParameterDTO> queryParams;
    private List<RequestParameterDTO> headers;
    private List<RequestParameterDTO> body;

}
