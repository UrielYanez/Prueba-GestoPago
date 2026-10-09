package com.proyecto.servicios.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.function.Function;

@Component
public class JwtUtil {

    @Value("${jwt.secret:clave-secreta-super-segura-de-256-bits-para-desarrollo-2024}")
    private String secretKey;

    @Value("${jwt.expiration:86400000}") // 24 horas en ms
    private long expirationMs;

    private Key getSigningKey() {
        byte[] keyBytes = Base64.getEncoder().encode(secretKey.getBytes());
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // Genera un token JWT para el correo dado
    public String generarToken(String correo) {
        return Jwts.builder()
                .setSubject(correo)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // Extrae el correo (subject) del token
    public String extraerCorreo(String token) {
        return extraerClaim(token, Claims::getSubject);
    }

    // Valida que el token pertenezca al correo dado y no haya expirado
    public boolean validarToken(String token, String correo) {
        final String correoToken = extraerCorreo(token);
        return correoToken.equals(correo) && !estaExpirado(token);
    }

    private boolean estaExpirado(String token) {
        return extraerClaim(token, Claims::getExpiration).before(new Date());
    }

    private <T> T extraerClaim(String token, Function<Claims, T> resolver) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return resolver.apply(claims);
    }
}
