package com.proyecto.servicios.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Petición para generar y enviar PIN de confirmación por correo (2FA)")
public class SolicitudPinRequestDto {

    @NotBlank(message = "El número de cuenta es obligatorio")
    @Schema(description = "Número de cuenta origen (10 dígitos)", example = "0000000001")
    private String numeroCuenta;

    @NotNull(message = "El monto es obligatorio")
    @Positive(message = "El monto debe ser mayor a cero")
    @Schema(description = "Monto de la transacción a confirmar", example = "100.00")
    private BigDecimal monto;

    @Schema(description = "Nombre o descripción del producto/servicio a pagar", example = "Recarga Telcel 100")
    private String concepto;
}
