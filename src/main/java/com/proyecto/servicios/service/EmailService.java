package com.proyecto.servicios.service;

import java.math.BigDecimal;

public interface EmailService {

    /**
     * Envía el código de confirmación (PIN / OTP) al correo del cliente.
     *
     * @param destinatario Correo electrónico del cliente.
     * @param nombreCliente Nombre o razón social del cliente.
     * @param pin Código OTP de 6 dígitos.
     * @param monto Monto de la transacción a confirmar.
     * @param producto Nombre o identificador del producto a pagar.
     */
    void enviarPinTransaccion(String destinatario, String nombreCliente, String pin, BigDecimal monto, String producto);

    /**
     * Envía el comprobante / ticket de pago exitoso al cliente (incluyendo PIN si es tarjeta de regalo).
     */
    void enviarComprobantePago(String destinatario, String nombreCliente, String autorizacion, BigDecimal monto, String producto, String referencia, String upc, String pinGiftCard);
}
