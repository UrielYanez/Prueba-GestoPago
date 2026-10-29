package com.proyecto.servicios.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Solicitud para conciliar una transacción pendiente con GestoPago (confirmTx.do)")
public class ConciliarPagoRequestDto {

    @NotBlank(message = "El código único de transacción (UPC) es obligatorio para la conciliación")
    @Schema(description = "Código único de operación (UPC) generado al enviar la transacción", example = "UPC1792804103921")
    private String upc;
}

