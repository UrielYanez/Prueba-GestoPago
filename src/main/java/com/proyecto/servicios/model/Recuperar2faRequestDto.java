package com.proyecto.servicios.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Recuperar2faRequestDto {

    @NotBlank(message = "El token de reto (challengeToken) es obligatorio")
    private String challengeToken;

    private Boolean mostrarQr;
}

