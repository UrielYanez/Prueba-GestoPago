package com.proyecto.servicios.model;

import com.proyecto.servicios.entity.postgres.EstatusCuenta;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO para la búsqueda dinámica de cuentas bancarias vía @RequestBody.
 */
@Getter
@Setter
public class CuentaBusquedaRequestDto {

    private String numeroCuenta;
    private Long clienteId;
    private EstatusCuenta estatus;

    @Min(value = 0, message = "El número de página no puede ser menor a 0")
    private Integer page;

    @Min(value = 1, message = "El tamaño de página debe ser al menos 1")
    private Integer size;
}
