package com.proyecto.servicios.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Detalle del comprobante y ticket de pago emitido")
public class ComprobantePagoResponseDto {

    @Schema(description = "ID del registro de transacción interno", example = "1")
    private Long transaccionId;

    @Schema(description = "Número de cuenta origen", example = "0000000001")
    private String numeroCuenta;

    @Schema(description = "Identificador de transacción otorgado por el proveedor", example = "98273615")
    private String idTxProveedor;

    @Schema(description = "Número de autorización emitido por el proveedor", example = "AUT-772819")
    private String numAutorizacion;

    @Schema(description = "Código único de transacción (UPC)", example = "202610231345999")
    private String upc;

    @Schema(description = "Bandera de operación: true = Recarga, false = Servicio / Tarjeta de Regalo", example = "true")
    private Boolean bandera;

    @Schema(description = "Número de teléfono recargado (si aplica)", example = "5512345678")
    private String telefono;

    @Schema(description = "Referencia del servicio o recibo (si aplica)", example = "012345678901")
    private String referencia;

    @Schema(description = "Código PIN de activación para tarjetas de regalo / gift cards (Amazon, Netflix, Spotify, etc.)", example = "AMZ-9988-7766")
    private String pinGiftCard;

    @Schema(description = "Monto pagado", example = "100.00")
    private BigDecimal monto;

    @Schema(description = "Saldo restante en la cuenta bancaria del usuario", example = "1450.00")
    private BigDecimal nuevoSaldoCuenta;

    @Schema(description = "Código de respuesta del proveedor (01 = Éxito)", example = "01")
    private String codigoRespuesta;

    @Schema(description = "Mensaje devuelto por el proveedor", example = "Operacion realizada con exito")
    private String mensajeRespuesta;

    @Schema(description = "Estado interno de la transacción (EN_PROCESO, EXITOSA, RECHAZADA, PENDIENTE_CONCILIACION)", example = "EXITOSA")
    private String estadoTransaccion;

    @Schema(description = "Fecha y hora de la transacción")
    private LocalDateTime fechaTransaccion;
}
