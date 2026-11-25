package com.proyecto.servicios.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
public class Verificar2faRequestDto {

    @NotBlank(message = "El token de reto (challengeToken) es obligatorio")
    private String challengeToken;

    @NotBlank(message = "El código de verificación 2FA es obligatorio")
    @Pattern(regexp = "^\\d{6}$", message = "El código 2FA debe contener exactamente 6 dígitos numéricos")
    private String codigo2fa;

    private Boolean forzarCierreSesion;
}

