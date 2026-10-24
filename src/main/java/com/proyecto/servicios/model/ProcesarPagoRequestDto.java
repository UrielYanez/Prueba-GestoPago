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
@Schema(description = "Petición completa para procesar pago con confirmación OTP y bandera de operación")
public class ProcesarPagoRequestDto {

    @NotBlank(message = "El número de cuenta es obligatorio")
    @Schema(description = "Número de cuenta origen (10 dígitos)", example = "0000000001")
    private String numeroCuenta;

    @NotBlank(message = "El idServicio es obligatorio")
    @Schema(description = "Identificador del servicio en GestoPago", example = "1")
    private String idServicio;

    @NotBlank(message = "El idProducto es obligatorio")
    @Schema(description = "Identificador del producto en GestoPago", example = "101")
    private String idProducto;

    @NotNull(message = "El precio es obligatorio")
    @Positive(message = "El precio debe ser mayor a cero")
    @Schema(description = "Monto/Precio de la recarga o servicio", example = "100.00")
    private BigDecimal precio;

    @Schema(description = "Distribuidor / Unidad en GestoPago (por defecto 083)", example = "083")
    private String distribuidor;

    @NotNull(message = "La bandera es obligatoria: true para Recarga telefónica, false para Pago de Servicio / Tarjeta de Regalo")
    @Schema(description = "Bandera booleana: true = Recarga telefónica (requiere 'telefono'), false = Pago de servicio o tarjeta de regalo (requiere 'referencia')", example = "true")
    private Boolean bandera;

    @Schema(description = "Número de teléfono a 10 dígitos. Obligatorio si bandera = true (Recarga).", example = "5512345678")
    private String telefono;

    @Schema(description = "Referencia alfanumérica de cobro o recibo. Obligatoria si bandera = false (Servicio / Tarjeta de Regalo).", example = "012345678901")
    private String referencia;

    @NotBlank(message = "El PIN de confirmación es obligatorio")
    @Schema(description = "Código PIN de 6 dígitos recibido por correo electrónico", example = "849201")
    private String pin;
}
