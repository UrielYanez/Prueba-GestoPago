package com.proyecto.servicios.model;

import lombok.Data;

@Data
public class CopomexResponseDto {
    private boolean error;
    private int code_error;
    private String error_message;
    private CopomexInfoResponse response;
}
