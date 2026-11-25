package com.proyecto.servicios.model.gestopago;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Petición de búsqueda, filtrado por categoría y paginación del catálogo de productos")
public class ProductoBusquedaRequestDto {

    @Schema(description = "Texto de búsqueda por nombre de producto o compañía", example = "Telcel")
    private String busqueda;

    @Schema(description = "Filtro por categoría (1=Tiempo Aire, 5=Servicios, 10=Gift Cards, 13=Telefonía Virtual)", example = "1")
    private String idCatTipoServicio;

    @Schema(description = "Filtro por identificador de servicio o compañía", example = "1")
    private String idServicio;

    @Schema(description = "Número de página (base 0)", example = "0")
    private Integer page;

    @Schema(description = "Tamaño de página", example = "20")
    private Integer size;
}

