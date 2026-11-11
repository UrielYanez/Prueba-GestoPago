package com.proyecto.servicios.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CopomexResponseDto {
    private boolean error;
    @JsonProperty("code_error")
    private int codeError;
    @JsonProperty("error_message")
    private String errorMessage;
    private CopomexInfoResponse response;
}
