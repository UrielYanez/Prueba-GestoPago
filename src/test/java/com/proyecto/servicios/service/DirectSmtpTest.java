package com.proyecto.servicios.service;

import com.proyecto.servicios.service.Impl.EmailServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class DirectSmtpTest {

    @org.junit.jupiter.api.Disabled("Prueba de conexión real con Gmail SMTP verificada con éxito")
    @Test
    void testConexionDirectaSmtp() {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost("smtp.gmail.com");
        sender.setPort(587);
        sender.setUsername("aguayomar77@gmail.com");
        sender.setPassword("fzqhuihcljnelccu");

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");

        EmailServiceImpl service = new EmailServiceImpl(sender);
        try {
            Field field = EmailServiceImpl.class.getDeclaredField("remitente");
            field.setAccessible(true);
            field.set(service, "aguayomar77@gmail.com");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        assertDoesNotThrow(() -> {
            service.enviarPinTransaccion(
                    "aguayomar77@gmail.com",
                    "Omar Aguayo",
                    "849201",
                    new BigDecimal("100.00"),
                    "Prueba de Verificación Fase 1"
            );
        });
    }
}
