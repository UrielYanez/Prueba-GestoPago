package com.proyecto.servicios.service;

import com.proyecto.servicios.model.ComprobantePagoResponseDto;
import com.proyecto.servicios.model.ConciliarPagoRequestDto;
import com.proyecto.servicios.model.ProcesarPagoRequestDto;
import com.proyecto.servicios.model.SolicitudPinRequestDto;

public interface PagoService {

    /**
     * Genera un código OTP de 6 dígitos aleatorio, lo guarda encriptado (BCrypt) con expiración de 5 minutos,
     * y lo envía al correo del titular de la cuenta.
     */
    void solicitarPin(SolicitudPinRequestDto request);

    /**
     * Valida identidad, titularidad de cuenta, catálogo de productos, paridad de precios, idempotencia,
     * bloqueo pesimista de saldo, PIN OTP, envía la transacción a sendTx.do de GestoPago en esquema de 2 fases
     * y gestiona la respuesta o compensación.
     */
    ComprobantePagoResponseDto procesarPago(ProcesarPagoRequestDto request);

    /**
     * Concilia el estado de una transacción que quedó en PENDIENTE_CONCILIACION por timeout invocando confirmTx.do
     * de GestoPago con los mismos parámetros de la solicitud original.
     */
    ComprobantePagoResponseDto conciliarPago(ConciliarPagoRequestDto request);
}
