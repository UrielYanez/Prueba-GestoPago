package com.proyecto.servicios.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.exception.BusinessRuleException;
import com.proyecto.servicios.model.gestopago.GestoPagoTxPayload;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;

@Component
@Slf4j
public class AesEncryptionUtil {

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";

    @Value("${gestopago.crypto.secret-key}")
    private String base64SecretKey;

    @Value("${gestopago.crypto.iv}")
    private String base64Iv;

    public AesEncryptionUtil() {
    }

    public AesEncryptionUtil(String base64SecretKey, String base64Iv) {
        this.base64SecretKey = base64SecretKey;
        this.base64Iv = base64Iv;
        init();
    }

    private SecretKeySpec keySpec;
    private IvParameterSpec ivSpec;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(base64SecretKey.trim());
            byte[] ivBytes = Base64.getDecoder().decode(base64Iv.trim());
            this.keySpec = new SecretKeySpec(keyBytes, "AES");
            this.ivSpec = new IvParameterSpec(ivBytes);
            log.info("AesEncryptionUtil inicializado correctamente con algoritmo {}", ALGORITHM);
        } catch (Exception e) {
            log.error("Error al inicializar llaves criptográficas de GestoPago: {}", e.getMessage(), e);
            throw new IllegalStateException("Configuración criptográfica inválida para GestoPago", e);
        }
    }

    public String encrypt(String plainText) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (GeneralSecurityException e) {
            log.error("Error al cifrar texto con AES: {}", e.getMessage(), e);
            throw new BusinessRuleException("Error al cifrar el payload de la transacción: " + e.getMessage());
        }
    }

    public String decrypt(String base64Encrypted) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(base64Encrypted.trim()));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            log.error("Error al descifrar texto con AES: {}", e.getMessage(), e);
            throw new BusinessRuleException("Error al descifrar respuesta: " + e.getMessage());
        }
    }

    public String encryptPayload(GestoPagoTxPayload payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            log.info("JSON payload antes de cifrado AES: {}", json);
            return encrypt(json);
        } catch (Exception e) {
            log.error("Error al serializar y cifrar GestoPagoTxPayload: {}", e.getMessage(), e);
            throw new BusinessRuleException("Error al procesar payload de transacción: " + e.getMessage());
        }
    }
}
