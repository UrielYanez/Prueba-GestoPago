package com.proyecto.servicios.model;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO para la creación de cuenta bancaria vía @RequestBody.
 */
@Getter
@Setter
public class CuentaCreacionRequestDto {

    @NotNull(message = "El ID del cliente es obligatorio para crear la cuenta")
    private Long clienteId;
}
