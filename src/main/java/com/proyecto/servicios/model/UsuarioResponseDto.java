package com.proyecto.servicios.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UsuarioResponseDto {
    private Long id;
    private Long clienteId;
    private String correo;
    private Boolean activo;
    private java.time.LocalDateTime fechaCreacion;
    private java.time.LocalDateTime fechaActualizacion;
}
