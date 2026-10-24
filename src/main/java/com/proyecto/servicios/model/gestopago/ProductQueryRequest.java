package com.proyecto.servicios.model.gestopago;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Petición para consultar detalle de producto o servicio en catálogo")
public class ProductQueryRequest {

    @Schema(description = "Identificador del producto en GestoPago", example = "381")
    private String idProducto;

    @Schema(description = "Identificador de la compañía o servicio en GestoPago", example = "124")
    private String idServicio;
}
