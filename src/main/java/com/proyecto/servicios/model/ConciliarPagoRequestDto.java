package com.proyecto.servicios.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Solicitud para conciliar una transacción pendiente con GestoPago (confirmTx.do)")
public class ConciliarPagoRequestDto {

    @Schema(description = "Código único de operación (UPC) generado al enviar la transacción", example = "UPC1792804103921")
    private String upc;

    @Schema(description = "Clave de idempotencia generada por el cliente cuando se desconoce el UPC por corte de red", example = "550e8400-e29b-41d4-a716-446655440000")
    private String idOperacionCliente;
}
