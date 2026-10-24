package com.proyecto.servicios.service;

import com.proyecto.servicios.model.ComprobantePagoResponseDto;
import com.proyecto.servicios.model.ProcesarPagoRequestDto;
import com.proyecto.servicios.model.SolicitudPinRequestDto;

public interface PagoService {

    /**
     * Genera un código OTP de 6 dígitos aleatorio, lo guarda encriptado (BCrypt) con expiración de 5 minutos,
     * y lo envía al correo del titular de la cuenta.
     */
    void solicitarPin(SolicitudPinRequestDto request);

    /**
     * Valida cuenta activa, saldo suficiente, verifica el código PIN ingresado, cifra el payload con AES-256-CBC,
     * envía la transacción a sendTx.do de GestoPago, descuenta saldo si el código es 01, y registra el comprobante.
     */
    ComprobantePagoResponseDto procesarPago(ProcesarPagoRequestDto request);
}
