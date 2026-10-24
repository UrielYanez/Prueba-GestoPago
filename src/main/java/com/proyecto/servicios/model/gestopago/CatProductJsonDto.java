package com.proyecto.servicios.model.gestopago;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Ficha técnica completa del producto o servicio de GestoPago")
public class CatProductJsonDto {

    @Schema(description = "Identificador único del producto en GestoPago", example = "381")
    private String idProducto;

    @Schema(description = "Identificador de la compañía/servicio en GestoPago", example = "124")
    private String idServicio;

    @Schema(description = "Nombre de la empresa o compañía proveedora", example = "Movistar")
    private String servicio;

    @Schema(description = "Nombre comercial del paquete o producto", example = "Recarga $100")
    private String producto;

    @Schema(description = "Precio o denominación oficial", example = "100.00")
    private Double precio;

    @Schema(description = "Categoría del servicio (1=Tiempo Aire, 5=Servicios, 10=Gift Card, 13=Telefonía Virtual)", example = "1")
    private String idCatTipoServicio;

    @Schema(description = "Tipo de interfaz/formulario frontend (1=Monto fijo/Recarga, 2=Pago de servicio monto abierto, 4=Consulta saldo)", example = "1")
    private String tipoFront;

    @Schema(description = "Indica si la referencia requiere cálculo o validación de dígito verificador", example = "false")
    private Boolean hasDigitoVerificador;

    @Schema(description = "Indica si debe mostrarse botón de ayuda/instrucciones al usuario", example = "true")
    private Boolean showAyuda;

    @Schema(description = "Tipo de referencia esperada (a=número telefónico, b=número de servicio, bc=código de barras)", example = "a")
    private String tipoReferencia;

    @Schema(description = "Instrucciones de cobro, leyenda o soporte para el cliente", example = "Recibe soporte marcando al *787...")
    private String legend;

    @Schema(description = "Estatus operativo del producto en catálogo", example = "ACTIVE")
    private ProductStatusEnum status;
}