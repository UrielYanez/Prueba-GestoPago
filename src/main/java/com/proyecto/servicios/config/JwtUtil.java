package com.proyecto.servicios.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import java.util.function.Function;

@Component
public class JwtUtil {

    public static final String PURPOSE_ACCESS = "ACCESS_TOKEN";
    public static final String PURPOSE_CHALLENGE = "2FA_CHALLENGE";
    private static final String CLAIM_PURPOSE = "purpose";
    private static final String CLAIM_PLATAFORMA = "plataforma";
    private static final String HEADER_KID = "kid";

    @Value("${jwt.secret:clave-secreta-super-segura-de-256-bits-para-desarrollo-2024}")
    private String secretKey;

    @Value("${jwt.expiration:86400000}")
    private long expirationMs;

    @Value("${jwt.challenge-expiration:180000}")
    private long challengeExpirationMs;

    @Value("${jwt.kid:gestopago-jwt-key-2026-v1}")
    private String keyId;

    private Key getSigningKey() {
        byte[] keyBytes = Base64.getEncoder().encode(secretKey.getBytes(StandardCharsets.UTF_8));
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generarToken(String correo) {
        return generarTokenSesion(correo, UUID.randomUUID().toString(), "WEB");
    }

    public String generarTokenSesion(String correo, String sessionId, String plataforma) {
        Instant ahora = Instant.now();
        Instant expiracion = ahora.plusMillis(expirationMs);
        return Jwts.builder()
                .setHeaderParam(HEADER_KID, keyId)
                .setSubject(correo)
                .setId(sessionId)
                .claim(CLAIM_PURPOSE, PURPOSE_ACCESS)
                .claim(CLAIM_PLATAFORMA, plataforma != null ? plataforma : "WEB")
                .claim(Claims.ISSUED_AT, ahora.getEpochSecond())
                .claim(Claims.EXPIRATION, expiracion.getEpochSecond())
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generarChallengeToken(String correo, String plataforma) {
        Instant ahora = Instant.now();
        Instant expiracion = ahora.plusMillis(challengeExpirationMs > 0 ? challengeExpirationMs : 180000L);
        return Jwts.builder()
                .setHeaderParam(HEADER_KID, keyId)
                .setSubject(correo)
                .setId(UUID.randomUUID().toString())
                .claim(CLAIM_PURPOSE, PURPOSE_CHALLENGE)
                .claim(CLAIM_PLATAFORMA, plataforma != null ? plataforma : "MOVIL")
                .claim(Claims.ISSUED_AT, ahora.getEpochSecond())
                .claim(Claims.EXPIRATION, expiracion.getEpochSecond())
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extraerCorreo(String token) {
        return extraerClaim(token, Claims::getSubject);
    }

    public String extraerSessionId(String token) {
        return extraerClaim(token, Claims::getId);
    }

    public String extraerProposito(String token) {
        return extraerClaim(token, claims -> claims.get(CLAIM_PURPOSE, String.class));
    }

    public String extraerPlataforma(String token) {
        return extraerClaim(token, claims -> claims.get(CLAIM_PLATAFORMA, String.class));
    }

    public boolean tieneKidValido(String token) {
        Jws<Claims> jws = parsearJws(token);
        Object headerKid = jws.getHeader().get(HEADER_KID);
        String expectedKid = keyId != null ? keyId : "gestopago-jwt-key-2026-v1";
        return headerKid == null || expectedKid.equals(headerKid.toString());
    }

    public boolean validarToken(String token, String correo) {
        final String correoToken = extraerCorreo(token);
        String proposito = extraerProposito(token);
        boolean propositoValido = proposito == null || PURPOSE_ACCESS.equals(proposito);
        return correoToken.equals(correo) && propositoValido && tieneKidValido(token) && !estaExpirado(token);
    }

    public boolean validarChallengeToken(String token) {
        String proposito = extraerProposito(token);
        return PURPOSE_CHALLENGE.equals(proposito) && tieneKidValido(token) && !estaExpirado(token);
    }

    public long getExpirationSeconds() {
        return expirationMs > 0 ? expirationMs / 1000 : 86400L;
    }

    private boolean estaExpirado(String token) {
        Number expEpochSeconds = extraerClaim(token, claims -> claims.get(Claims.EXPIRATION, Number.class));
        return expEpochSeconds == null || Instant.ofEpochSecond(expEpochSeconds.longValue()).isBefore(Instant.now());
    }

    private <T> T extraerClaim(String token, Function<Claims, T> resolver) {
        Claims claims = parsearJws(token).getBody();
        return resolver.apply(claims);
    }

    private Jws<Claims> parsearJws(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token);
    }
}
