package com.proyecto.servicios.model;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO para la baja lógica de un cliente vía @RequestBody (sin @PathVariable).
 */
@Getter
@Setter
public class ClienteBajaRequestDto {

    @NotNull(message = "El ID del cliente es obligatorio para la baja lógica")
    private Long id;
}
