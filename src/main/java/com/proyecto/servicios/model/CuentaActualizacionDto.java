package com.proyecto.servicios.model;

import com.proyecto.servicios.entity.postgres.EstatusCuenta;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO para actualización de cuenta vía @RequestBody (PATCH).
 */
@Getter
@Setter
public class CuentaActualizacionDto {

    @NotBlank(message = "El número de cuenta es obligatorio")
    private String numeroCuenta;

    private BigDecimal saldo;
    private EstatusCuenta estatus;
}
