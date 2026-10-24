package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.exception.BusinessRuleException;
import com.proyecto.servicios.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String remitente;

    @Override
    public void enviarPinTransaccion(String destinatario, String nombreCliente, String pin, BigDecimal monto, String producto) {
        log.info("Preparando envío de PIN de confirmación para cliente: {} al correo: {}", nombreCliente, destinatario);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(remitente);
            helper.setTo(destinatario);
            helper.setSubject("Código de Seguridad para Confirmación de Pago - GestoPago");

            String htmlContent = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f4f6f9; margin: 0; padding: 20px; }
                        .container { max-width: 580px; margin: 0 auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.08); }
                        .header { background: linear-gradient(135deg, #1e3a8a, #3b82f6); color: #ffffff; padding: 30px 20px; text-align: center; }
                        .header h1 { margin: 0; font-size: 24px; font-weight: 600; }
                        .content { padding: 30px 25px; color: #334155; line-height: 1.6; }
                        .pin-box { background: #f1f5f9; border: 2px dashed #3b82f6; border-radius: 8px; padding: 20px; text-align: center; margin: 25px 0; }
                        .pin-code { font-size: 36px; font-weight: 700; letter-spacing: 8px; color: #1e3a8a; }
                        .details { background: #f8fafc; border-left: 4px solid #3b82f6; padding: 12px 16px; margin: 15px 0; border-radius: 4px; }
                        .warning { font-size: 13px; color: #dc2626; margin-top: 15px; font-weight: 500; }
                        .footer { background: #f8fafc; padding: 15px; text-align: center; font-size: 12px; color: #94a3b8; border-top: 1px solid #e2e8f0; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>Confirmación de Transacción</h1>
                        </div>
                        <div class="content">
                            <p>Estimado(a) <strong>%s</strong>,</p>
                            <p>Se ha solicitado una transacción en su cuenta. Para autorizar la operación, ingrese el siguiente código de seguridad:</p>
                            
                            <div class="pin-box">
                                <span class="pin-code">%s</span>
                            </div>

                            <div class="details">
                                <p style="margin: 4px 0;"><strong>Concepto/Producto:</strong> %s</p>
                                <p style="margin: 4px 0;"><strong>Monto:</strong> $%s MXN</p>
                            </div>

                            <p class="warning">⚠️ Este código es confidencial, de un solo uso y expirará en <strong>5 minutos</strong>. Si no solicitó esta operación, por favor contacte a soporte de inmediato.</p>
                        </div>
                        <div class="footer">
                            <p>© %d GestoPago / Servicios Financieros. Todos los derechos reservados.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(
                    nombreCliente != null ? nombreCliente : "Cliente",
                    pin,
                    producto != null ? producto : "Pago de servicio",
                    monto != null ? monto.toPlainString() : "0.00",
                    java.time.Year.now().getValue()
            );

            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Correo de PIN enviado exitosamente a: {}", destinatario);

        } catch (MessagingException | org.springframework.mail.MailException e) {
            log.error("Error al enviar correo de PIN a {}: {}", destinatario, e.getMessage(), e);
            throw new BusinessRuleException("No se pudo enviar el código de confirmación por correo: " + e.getMessage());
        }
    }

    @Override
    public void enviarComprobantePago(String destinatario, String nombreCliente, String autorizacion, BigDecimal monto, String producto, String referencia, String upc, String pinGiftCard) {
        log.info("Enviando comprobante de pago exitoso a: {}", destinatario);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(remitente);
            helper.setTo(destinatario);
            helper.setSubject("Comprobante de Pago Exitoso - GestoPago");

            String fechaHora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));

            String pinBoxHtml = "";
            if (pinGiftCard != null && !pinGiftCard.isBlank()) {
                pinBoxHtml = """
                    <div style="background: #fffbeb; border: 2px dashed #f59e0b; padding: 15px; border-radius: 8px; text-align: center; margin: 20px 0;">
                        <p style="margin: 0; font-size: 13px; color: #92400e; font-weight: 700; text-transform: uppercase;">PIN / Código de Canje de Tarjeta de Regalo:</p>
                        <p style="margin: 8px 0 0 0; font-size: 26px; font-weight: 800; color: #b45309; letter-spacing: 4px;">{{pin}}</p>
                    </div>
                    """.replace("{{pin}}", pinGiftCard);
            }

            String htmlContent = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f4f6f9; margin: 0; padding: 20px; }
                        .container { max-width: 580px; margin: 0 auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.08); }
                        .header { background: linear-gradient(135deg, #059669, #10b981); color: #ffffff; padding: 30px 20px; text-align: center; }
                        .header h1 { margin: 0; font-size: 24px; font-weight: 600; }
                        .content { padding: 30px 25px; color: #334155; line-height: 1.6; }
                        .receipt-table { width: 100%; border-collapse: collapse; margin: 20px 0; }
                        .receipt-table td { padding: 10px 12px; border-bottom: 1px solid #e2e8f0; font-size: 14px; }
                        .receipt-table td.label { font-weight: 600; color: #64748b; width: 45%; }
                        .receipt-table td.value { font-weight: 500; color: #0f172a; text-align: right; }
                        .total-row td { font-size: 16px; font-weight: 700; color: #059669; border-top: 2px solid #059669; }
                        .footer { background: #f8fafc; padding: 15px; text-align: center; font-size: 12px; color: #94a3b8; border-top: 1px solid #e2e8f0; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>¡Pago Realizado con Éxito!</h1>
                        </div>
                        <div class="content">
                            <p>Estimado(a) <strong>{{nombreCliente}}</strong>,</p>
                            <p>Le confirmamos que su transacción ha sido procesada y autorizada correctamente.</p>
                            
                            {{pinBoxHtml}}

                            <table class="receipt-table">
                                <tr>
                                    <td class="label">No. Autorización:</td>
                                    <td class="value"><strong>{{autorizacion}}</strong></td>
                                </tr>
                                <tr>
                                    <td class="label">Producto / Servicio:</td>
                                    <td class="value">{{producto}}</td>
                                </tr>
                                <tr>
                                    <td class="label">Referencia / Teléfono:</td>
                                    <td class="value">{{referencia}}</td>
                                </tr>
                                <tr>
                                    <td class="label">Código Único (UPC):</td>
                                    <td class="value">{{upc}}</td>
                                </tr>
                                <tr>
                                    <td class="label">Fecha y Hora:</td>
                                    <td class="value">{{fechaHora}}</td>
                                </tr>
                                <tr class="total-row">
                                    <td class="label">Monto Pagado:</td>
                                    <td class="value">${{monto}} MXN</td>
                                </tr>
                            </table>

                            <p style="font-size: 13px; color: #64748b;">Conserve este correo como su comprobante digital de operación.</p>
                        </div>
                        <div class="footer">
                            <p>© {{anio}} GestoPago / Servicios Financieros. Todos los derechos reservados.</p>
                        </div>
                    </div>
                </body>
                </html>
                """
                .replace("{{nombreCliente}}", nombreCliente != null ? nombreCliente : "Cliente")
                .replace("{{pinBoxHtml}}", pinBoxHtml)
                .replace("{{autorizacion}}", autorizacion != null ? autorizacion : "N/A")
                .replace("{{producto}}", producto != null ? producto : "Servicio")
                .replace("{{referencia}}", referencia != null ? referencia : "N/A")
                .replace("{{upc}}", upc != null ? upc : "N/A")
                .replace("{{fechaHora}}", fechaHora)
                .replace("{{monto}}", monto != null ? monto.toPlainString() : "0.00")
                .replace("{{anio}}", String.valueOf(java.time.Year.now().getValue()));

            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Comprobante enviado exitosamente a: {}", destinatario);

        } catch (MessagingException | org.springframework.mail.MailException e) {
            log.warn("No se pudo enviar el comprobante de pago por correo a {}: {}", destinatario, e.getMessage());
        }
    }
}
