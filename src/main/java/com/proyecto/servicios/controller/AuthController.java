package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.ApiResponse;
import com.proyecto.servicios.model.LoginRequestDto;
import com.proyecto.servicios.model.LoginResponseDto;
import com.proyecto.servicios.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticación", description = "Endpoints para inicio de sesión y obtención de JWT")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * POST /auth/login
     * Recibe correo y password, valida credenciales y retorna un JWT dentro de ApiResponse uniforme.
     */
    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión", description = "Valida credenciales (correo y contraseña) y retorna el token JWT.")
    public ResponseEntity<ApiResponse<LoginResponseDto>> login(@Valid @RequestBody LoginRequestDto request) {
        LoginResponseDto response = authService.login(request);
        ApiResponse<LoginResponseDto> respuesta = ApiResponse.exitoso(
                "Autenticación exitosa.",
                response,
                HttpStatus.OK.value()
        );
        return ResponseEntity.ok(respuesta);
    }
}
