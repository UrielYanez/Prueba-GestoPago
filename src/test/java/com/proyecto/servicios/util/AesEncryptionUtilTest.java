package com.proyecto.servicios.util;

import com.proyecto.servicios.model.gestopago.GestoPagoTxPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class AesEncryptionUtilTest {

    private AesEncryptionUtil aesUtil;

    @BeforeEach
    void setUp() throws Exception {
        aesUtil = new AesEncryptionUtil();

        Field keyField = AesEncryptionUtil.class.getDeclaredField("base64SecretKey");
        keyField.setAccessible(true);
        keyField.set(aesUtil, "HPo7OLqB4Fkk4E2yGOtwqw8H5fHR9kNx67OR4g4UdlA=");

        Field ivField = AesEncryptionUtil.class.getDeclaredField("base64Iv");
        ivField.setAccessible(true);
        ivField.set(aesUtil, "p5ldmBPdd/9pjC0bDC/nSg==");

        aesUtil.init();
    }

    @Test
    void testEncryptAndDecrypt() {
        String original = "Texto secreto de prueba para GestoPago 12345";
        String encrypted = aesUtil.encrypt(original);
        assertNotNull(encrypted);
        assertNotEquals(original, encrypted);

        String decrypted = aesUtil.decrypt(encrypted);
        assertEquals(original, decrypted);
    }

    @Test
    void testEncryptPayload() {
        GestoPagoTxPayload payload = GestoPagoTxPayload.builder()
                .idProducto(101)
                .idServicio(1)
                .referencia("5512345678")
                .montoPago(new BigDecimal("100.00"))
                .telefono("5512345678")
                .horaLocal("20261023134500")
                .upc("123456789012")
                .unidad("083")
                .build();

        String signed = aesUtil.encryptPayload(payload);
        assertNotNull(signed);
        assertFalse(signed.isBlank());

        String decryptedJson = aesUtil.decrypt(signed);
        assertTrue(decryptedJson.contains("101"));
        assertTrue(decryptedJson.contains("5512345678"));
        assertTrue(decryptedJson.contains("083"));
    }
}
