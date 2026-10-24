package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.ApiResponse;
import com.proyecto.servicios.model.ComprobantePagoResponseDto;
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
@Tag(name = "Pagos y Transacciones", description = "Procesamiento de pagos con GestoPago y confirmación de seguridad OTP (2FA) por correo")
public class PagoController {

    private final PagoService pagoService;

    @PostMapping("/solicitar-pin")
    @Operation(
            summary = "Solicitar código PIN de confirmación por correo",
            description = "Valida que la cuenta esté activa y tenga saldo suficiente, genera un PIN de seguridad de 6 dígitos (válido por 5 minutos) y lo envía al correo del cliente."
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
            description = "Verifica la cuenta activa, saldo disponible, valida el PIN OTP ingresado, cifra el payload con AES-256-CBC, realiza el envío a GestoPago sendTx.do, descuenta el saldo atómicamente si el código es 01 y genera el comprobante."
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
}
