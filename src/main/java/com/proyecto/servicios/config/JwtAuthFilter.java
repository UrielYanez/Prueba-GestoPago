package com.proyecto.servicios.config;

import com.proyecto.servicios.entity.postgres.Usuario;
import com.proyecto.servicios.repositorys.postgres.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Optional;

@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    public static final String ATTR_AUTH_ERROR = "AUTH_ERROR_MESSAGE";
    private static final ZoneId ZONA_MX = ZoneId.of("America/Mexico_City");

    private final JwtUtil jwtUtil;
    private final UsuarioRepository usuarioRepository;

    public JwtAuthFilter(JwtUtil jwtUtil, UsuarioRepository usuarioRepository) {
        this.jwtUtil = jwtUtil;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(7);
        autenticarTokenSiEsValido(token, request);
        filterChain.doFilter(request, response);
    }

    private void autenticarTokenSiEsValido(String token, HttpServletRequest request) {
        try {
            String correo = jwtUtil.extraerCorreo(token);
            if (correo == null || SecurityContextHolder.getContext().getAuthentication() != null) {
                return;
            }
            if (!jwtUtil.validarToken(token, correo)) {
                request.setAttribute(ATTR_AUTH_ERROR, "Token JWT inválido, expirado o de propósito no autorizado.");
                return;
            }
            Optional<Usuario> usuarioOpt = usuarioRepository.findByCorreo(correo);
            if (usuarioOpt.isEmpty()) {
                return;
            }
            Usuario usuario = usuarioOpt.get();
            if (!esSesionActivaValida(usuario, token, request)) {
                return;
            }
            SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_USER");
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(correo, null, Collections.singletonList(authority));
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
        } catch (Exception e) {
            log.warn("Token JWT inválido o expirado: {}", e.getMessage());
            request.setAttribute(ATTR_AUTH_ERROR, "La sesión ha expirado o el token es inválido.");
        }
    }

    private boolean esSesionActivaValida(Usuario usuario, String token, HttpServletRequest request) {
        if (Boolean.FALSE.equals(usuario.getActivo())) {
            request.setAttribute(ATTR_AUTH_ERROR, "La cuenta de usuario se encuentra inactiva.");
            return false;
        }
        String sessionIdUsuario = usuario.getSessionIdActivo();
        String sessionIdToken = jwtUtil.extraerSessionId(token);

        if (sessionIdUsuario != null && !sessionIdUsuario.equals(sessionIdToken)) {
            request.setAttribute(ATTR_AUTH_ERROR, "Tu sesión fue finalizada porque iniciaste sesión en otro dispositivo o cerraste sesión.");
            return false;
        }
        if (usuario.getFechaExpiracionSesion() != null
                && usuario.getFechaExpiracionSesion().isBefore(LocalDateTime.now(ZONA_MX))) {
            request.setAttribute(ATTR_AUTH_ERROR, "Tu sesión ha expirado. Por favor inicia sesión nuevamente.");
            return false;
        }
        return true;
    }
}
