package com.proyecto.servicios.model;

import com.proyecto.servicios.entity.postgres.EstatusCuenta;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO para actualización parcial de cuenta (PATCH).
 * No incluye numeroCuenta ya que es inmutable.
 */
@Getter
@Setter
public class CuentaActualizacionDto {
    private BigDecimal saldo;
    private EstatusCuenta estatus;
}
