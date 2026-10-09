package com.proyecto.servicios.config;

import com.proyecto.servicios.entity.postgres.Usuario;
import com.proyecto.servicios.repositorys.postgres.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

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
        // 1. Extraer el header Authorization
        final String authHeader = request.getHeader("Authorization");
        log.info("authHeader: {}", authHeader != null ? "Presente" : "Nulo");

        // 2. Si no hay token o no empieza con "Bearer ", continuar sin autenticar
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.info("No hay token o no empieza con Bearer");
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Extraer el token (quitar "Bearer ")
        final String token = authHeader.substring(7);
        String correo;

        try {
            correo = jwtUtil.extraerCorreo(token);
            log.info("Correo extraido del token: {}", correo);
        } catch (Exception e) {
            log.warn("Token JWT inválido o expirado: {}", e.getMessage());
            filterChain.doFilter(request, response);
            return;
        }

        log.info("Current Auth is null? {}", SecurityContextHolder.getContext().getAuthentication() == null);
        // 4. Si se extrajo el correo y no hay sesión activa aún, autenticar
        if (correo != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            Optional<Usuario> usuarioOpt = usuarioRepository.findByCorreo(correo);
            log.info("Usuario presente en BD: {}", usuarioOpt.isPresent());

            if (usuarioOpt.isPresent()) {
                boolean tokenValid = jwtUtil.validarToken(token, correo);
                boolean isActivo = Boolean.TRUE.equals(usuarioOpt.get().getActivo());
                log.info("Token valido: {}, Usuario activo: {}", tokenValid, isActivo);
                
                if (tokenValid && isActivo) {
                    // Asignamos un rol genérico (ROLE_USER) para que Spring Security sepa que sí tiene permisos
                    org.springframework.security.core.authority.SimpleGrantedAuthority authority = 
                            new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER");
                    
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(correo, null, java.util.Collections.singletonList(authority));
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Registrar en el contexto de seguridad
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.info("Autenticacion seteada en SecurityContextHolder");
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
