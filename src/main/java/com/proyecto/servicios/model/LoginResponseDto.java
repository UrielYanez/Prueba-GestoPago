package com.proyecto.servicios.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginResponseDto {
    private String token;
    private String tipo;
    private String correo;
    private Long expiraEn;
    private String sessionId;
    private String plataforma;
    private Long clienteId;
    private String nombreCliente;
    private String numeroCuentaPrincipal;
    private BigDecimal saldoDisponible;
    private Boolean requiere2fa;
    private String challengeToken;
    private Boolean totpConfigurado;
    private String otpAuthUri;
    private String secretManual;
}
