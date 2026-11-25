package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.ApiResponse;
import com.proyecto.servicios.model.LoginRequestDto;
import com.proyecto.servicios.model.LoginResponseDto;
import com.proyecto.servicios.model.Recuperar2faRequestDto;
import com.proyecto.servicios.model.Verificar2faRequestDto;
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
@Tag(name = "Autenticación", description = "Inicio de sesión, verificación multifactor (2FA TOTP / OTP) y control de sesión única simultánea")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Paso 1: Validar credenciales e iniciar reto 2FA", description = "Valida correo y contraseña y genera el challengeToken temporal para verificación de segundo factor (Google Authenticator / OTP).")
    public ResponseEntity<ApiResponse<LoginResponseDto>> login(@Valid @RequestBody LoginRequestDto request) {
        LoginResponseDto response = authService.login(request);
        ApiResponse<LoginResponseDto> respuesta = ApiResponse.exitoso(
                "Credenciales válidas. Ingrese el código de segundo factor de autenticación (2FA).",
                response,
                HttpStatus.OK.value()
        );
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/verificar-2fa")
    @Operation(summary = "Paso 2: Verificar código 2FA y establecer sesión única", description = "Valida el código de Google Authenticator o código de respaldo, verifica exclusividad de sesión activa y emite el JWT con header kid válido.")
    public ResponseEntity<ApiResponse<LoginResponseDto>> verificar2fa(@Valid @RequestBody Verificar2faRequestDto request) {
        LoginResponseDto response = authService.verificar2fa(request);
        ApiResponse<LoginResponseDto> respuesta = ApiResponse.exitoso(
                "Autenticación de doble factor exitosa. Sesión iniciada.",
                response,
                HttpStatus.OK.value()
        );
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/recuperar-2fa")
    @Operation(summary = "Recuperación de acceso 2FA por correo", description = "Envía un código temporal de respaldo al correo electrónico del titular y opcionalmente devuelve la URI QR de Google Authenticator.")
    public ResponseEntity<ApiResponse<LoginResponseDto>> recuperar2fa(@Valid @RequestBody Recuperar2faRequestDto request) {
        LoginResponseDto response = authService.recuperar2fa(request);
        ApiResponse<LoginResponseDto> respuesta = ApiResponse.exitoso(
                "Se ha enviado un código de respaldo temporal a tu correo electrónico registrado.",
                response,
                HttpStatus.OK.value()
        );
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesión activa", description = "Finaliza e invalida inmediatamente la sesión activa del usuario en el servidor.")
    public ResponseEntity<ApiResponse<Void>> logout() {
        authService.logout();
        ApiResponse<Void> respuesta = ApiResponse.exitoso(
                "Sesión finalizada de manera segura.",
                null,
                HttpStatus.OK.value()
        );
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/sesion")
    @Operation(summary = "Consultar estado de la sesión activa", description = "Verifica que la sesión actual siga vigente como única sesión activa y devuelve los datos del titular y su cuenta principal.")
    public ResponseEntity<ApiResponse<LoginResponseDto>> obtenerEstadoSesion() {
        LoginResponseDto response = authService.obtenerEstadoSesionActual();
        ApiResponse<LoginResponseDto> respuesta = ApiResponse.exitoso(
                "Sesión activa verificada.",
                response,
                HttpStatus.OK.value()
        );
        return ResponseEntity.ok(respuesta);
    }
}
