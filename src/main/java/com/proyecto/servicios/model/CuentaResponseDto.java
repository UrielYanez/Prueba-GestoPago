package com.proyecto.servicios.model;

import com.proyecto.servicios.entity.postgres.EstatusCuenta;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class CuentaResponseDto {
    private Long id;
    private String numeroCuenta;
    private BigDecimal saldo;
    private EstatusCuenta estatus;
    // Datos básicos del cliente dueño de la cuenta
    private Long clienteId;
    private String nombreCliente;
}
