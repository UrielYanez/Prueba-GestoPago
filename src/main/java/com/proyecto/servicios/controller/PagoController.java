package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.ApiResponse;
import com.proyecto.servicios.model.ComprobantePagoResponseDto;
import com.proyecto.servicios.model.ConciliarPagoRequestDto;
import com.proyecto.servicios.model.ProcesarPagoRequestDto;
import com.proyecto.servicios.model.SolicitudPinRequestDto;
import com.proyecto.servicios.service.PagoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pagos")
@RequiredArgsConstructor
@Tag(name = "Pagos y Transacciones", description = "Procesamiento de pagos con GestoPago, confirmación de seguridad OTP (2FA) y conciliación de operaciones")
public class PagoController {

    private final PagoService pagoService;

    @PostMapping("/solicitar-pin")
    @Operation(
            summary = "Solicitar código PIN de confirmación por correo",
            description = "Valida la identidad del cliente, cuenta activa y saldo suficiente, genera un PIN de seguridad de 6 dígitos (válido por 5 minutos) y lo envía al correo del titular."
    )
    public ResponseEntity<ApiResponse<Void>> solicitarPin(@Valid @RequestBody SolicitudPinRequestDto request) {
        pagoService.solicitarPin(request);
        ApiResponse<Void> respuesta = ApiResponse.exitoso(
                "Código PIN de confirmación enviado exitosamente al correo registrado del titular.",
                null,
                HttpStatus.OK.value()
        );
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/procesar")
    @Operation(
            summary = "Procesar pago y enviar a GestoPago",
            description = "Verifica identidad del cliente, catálogo en MongoDB (anti-manipulación de montos), concurrencia con bloqueo pesimista, idempotencia, PIN OTP, cifra con AES-256-CBC y ejecuta el pago en 2 fases con GestoPago sendTx.do."
    )
    public ResponseEntity<ApiResponse<ComprobantePagoResponseDto>> procesarPago(@Valid @RequestBody ProcesarPagoRequestDto request) {
        ComprobantePagoResponseDto comprobante = pagoService.procesarPago(request);
        ApiResponse<ComprobantePagoResponseDto> respuesta = ApiResponse.exitoso(
                "Pago procesado exitosamente por GestoPago.",
                comprobante,
                HttpStatus.OK.value()
        );
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/conciliar")
    @Operation(
            summary = "Conciliar transacción pendiente por timeout (confirmTx.do)",
            description = "Consulta en GestoPago confirmTx.do el estado real de una transacción en PENDIENTE_CONCILIACION. Si fue aplicada (código 06), la consolida como EXITOSA; si no fue aplicada (código 70), la marca como RECHAZADA y reembolsa el saldo al cliente."
    )
    public ResponseEntity<ApiResponse<ComprobantePagoResponseDto>> conciliarPago(@Valid @RequestBody ConciliarPagoRequestDto request) {
        ComprobantePagoResponseDto comprobante = pagoService.conciliarPago(request);
        ApiResponse<ComprobantePagoResponseDto> respuesta = ApiResponse.exitoso(
                "Conciliación de transacción completada.",
                comprobante,
                HttpStatus.OK.value()
        );
        return ResponseEntity.ok(respuesta);
    }
}
