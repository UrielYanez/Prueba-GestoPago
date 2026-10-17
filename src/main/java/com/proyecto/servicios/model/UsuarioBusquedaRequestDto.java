package com.proyecto.servicios.model;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO para la búsqueda dinámica de usuarios vía @RequestBody.
 */
@Getter
@Setter
public class UsuarioBusquedaRequestDto {

    private Long id;
    private Long clienteId;
    private String correo;
    private Boolean activo;

    @Min(value = 0, message = "El número de página no puede ser menor a 0")
    private Integer page;

    @Min(value = 1, message = "El tamaño de página debe ser al menos 1")
    private Integer size;
}
