package com.proyecto.servicios.model;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Request DTO para el endpoint único de búsqueda y consulta combinada de clientes.
 * Recibe los criterios mediante @RequestBody sin usar @RequestParam ni @PathVariable.
 */
@Getter
@Setter
public class ClienteBusquedaRequestDto {

    private Long id;
    private String nombre;
    private String segundoNombre;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private String curp;
    private String rfc;
    private String correo;
    private String numeroCuenta;
    private Boolean activo;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;

    @Min(value = 0, message = "El número de página no puede ser menor a 0")
    private Integer page;

    @Min(value = 1, message = "El tamaño de página debe ser al menos 1")
    private Integer size;
}
