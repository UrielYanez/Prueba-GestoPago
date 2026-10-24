package com.proyecto.servicios.service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@org.junit.jupiter.api.Disabled("Test de contexto completo deshabilitado para pruebas unitarias rápidas")
@SpringBootTest
class EmailServiceTest {

    @Autowired
    private EmailService emailService;

    @Test
    void testEnviarPinTransaccion() {
        assertDoesNotThrow(() -> {
            emailService.enviarPinTransaccion(
                    "aguayomar77@gmail.com",
                    "Omar Aguayo",
                    "492815",
                    new BigDecimal("150.00"),
                    "Recarga Telcel 150"
            );
        });
    }
}
