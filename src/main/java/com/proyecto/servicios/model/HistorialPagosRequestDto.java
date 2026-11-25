package com.proyecto.servicios.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Petición para consultar el historial paginado de compras exitosas del usuario autenticado")
public class HistorialPagosRequestDto {

    @Schema(description = "Filtro opcional por número de cuenta del usuario", example = "0000000001")
    private String numeroCuenta;

    @Schema(description = "Número de página (base 0)", example = "0")
    private Integer page;

    @Schema(description = "Tamaño de página", example = "10")
    private Integer size;
}

