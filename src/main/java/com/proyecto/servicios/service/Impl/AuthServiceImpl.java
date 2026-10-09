package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.config.JwtUtil;
import com.proyecto.servicios.entity.postgres.Usuario;
import com.proyecto.servicios.exception.BusinessRuleException;
import com.proyecto.servicios.exception.InvalidCredentialsException;
import com.proyecto.servicios.model.LoginRequestDto;
import com.proyecto.servicios.model.LoginResponseDto;
import com.proyecto.servicios.repositorys.postgres.UsuarioRepository;
import com.proyecto.servicios.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthServiceImpl(UsuarioRepository usuarioRepository,
                           PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public LoginResponseDto login(LoginRequestDto request) {
        // 1. Buscar al usuario por correo
        Usuario usuario = usuarioRepository.findByCorreo(request.getCorreo())
                .orElseThrow(() -> new InvalidCredentialsException("Credenciales incorrectas"));

        // 2. Validar que el usuario esté activo
        if (Boolean.FALSE.equals(usuario.getActivo())) {
            throw new BusinessRuleException("El usuario está desactivado. Contacte a su ejecutivo.");
        }

        // 3. Validar contraseña con BCrypt
        if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            throw new InvalidCredentialsException("Credenciales incorrectas");
        }

        // 4. Generar y retornar el token JWT
        String token = jwtUtil.generarToken(usuario.getCorreo());

        return LoginResponseDto.builder()
                .token(token)
                .tipo("Bearer")
                .correo(usuario.getCorreo())
                .expiraEn(86400) // 24 horas en segundos
                .build();
    }
}
