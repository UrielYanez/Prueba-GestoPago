package com.proyecto.servicios.util;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Instant;

@Component
public class TotpUtil {

    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final String HMAC_ALGO = "HmacSHA1";
    private static final int SECRET_BYTES = 20;
    private static final int TIME_STEP_SECONDS = 30;
    private static final int CODE_DIGITS = 6;
    private static final int DIGITS_POWER = 1_000_000;
    private static final String ISSUER = "GestoPago";

    private final SecureRandom secureRandom = new SecureRandom();

    public String generarSecretoBase32() {
        byte[] buffer = new byte[SECRET_BYTES];
        secureRandom.nextBytes(buffer);
        return encodeBase32(buffer);
    }

    public String construirOtpAuthUri(String correo, String secretBase32) {
        String cuenta = URLEncoder.encode(ISSUER + ":" + correo, StandardCharsets.UTF_8).replace("+", "%20");
        String issuerEnc = URLEncoder.encode(ISSUER, StandardCharsets.UTF_8);
        return String.format(
                "otpauth://totp/%s?secret=%s&issuer=%s&algorithm=SHA1&digits=%d&period=%d",
                cuenta, secretBase32, issuerEnc, CODE_DIGITS, TIME_STEP_SECONDS
        );
    }

    public boolean verificarCodigo(String secretBase32, String codigoIngresado) {
        if (secretBase32 == null || secretBase32.isBlank() || codigoIngresado == null || !codigoIngresado.matches("^\\d{6}$")) {
            return false;
        }
        byte[] keyBytes = decodeBase32(secretBase32);
        long ventanaActual = Instant.now().getEpochSecond() / TIME_STEP_SECONDS;

        for (int desfase = -1; desfase <= 1; desfase++) {
            String generado = generarCodigoEnVentana(keyBytes, ventanaActual + desfase);
            if (codigoIngresado.equals(generado)) {
                return true;
            }
        }
        return false;
    }

    public String generarCodigoActual(String secretBase32) {
        byte[] keyBytes = decodeBase32(secretBase32);
        long ventanaActual = Instant.now().getEpochSecond() / TIME_STEP_SECONDS;
        return generarCodigoEnVentana(keyBytes, ventanaActual);
    }

    @SuppressWarnings("java:S4790")
    private String generarCodigoEnVentana(byte[] keyBytes, long timeWindow) {
        try {
            byte[] data = ByteBuffer.allocate(8).putLong(timeWindow).array();
            SecretKeySpec signKey = new SecretKeySpec(keyBytes, HMAC_ALGO);
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(signKey);
            byte[] hash = mac.doFinal(data);

            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);

            int otp = binary % DIGITS_POWER;
            return String.format("%06d", otp);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Error al calcular código TOTP", e);
        }
    }

    private String encodeBase32(byte[] data) {
        StringBuilder result = new StringBuilder((data.length * 8 + 4) / 5);
        int buffer = data[0];
        int next = 1;
        int bitsLeft = 8;

        while (bitsLeft > 0 || next < data.length) {
            if (bitsLeft < 5) {
                if (next < data.length) {
                    buffer <<= 8;
                    buffer |= (data[next++] & 0xFF);
                    bitsLeft += 8;
                } else {
                    int pad = 5 - bitsLeft;
                    buffer <<= pad;
                    bitsLeft += pad;
                }
            }
            int index = 0x1F & (buffer >> (bitsLeft - 5));
            bitsLeft -= 5;
            result.append(BASE32_ALPHABET.charAt(index));
        }
        return result.toString();
    }

    private byte[] decodeBase32(String base32) {
        String clean = base32.trim().replace("=", "").replace(" ", "").toUpperCase();
        byte[] result = new byte[clean.length() * 5 / 8];
        int buffer = 0;
        int bitsLeft = 0;
        int index = 0;

        for (int i = 0; i < clean.length(); i++) {
            int val = BASE32_ALPHABET.indexOf(clean.charAt(i));
            if (val < 0) {
                continue;
            }
            buffer = (buffer << 5) | val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                result[index++] = (byte) ((buffer >> (bitsLeft - 8)) & 0xFF);
                bitsLeft -= 8;
            }
        }
        return result;
    }
}

