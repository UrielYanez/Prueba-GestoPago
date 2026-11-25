package com.proyecto.servicios.service;

import com.proyecto.servicios.model.LoginRequestDto;
import com.proyecto.servicios.model.LoginResponseDto;
import com.proyecto.servicios.model.Recuperar2faRequestDto;
import com.proyecto.servicios.model.Verificar2faRequestDto;

public interface AuthService {
    LoginResponseDto login(LoginRequestDto request);
    LoginResponseDto verificar2fa(Verificar2faRequestDto request);
    LoginResponseDto recuperar2fa(Recuperar2faRequestDto request);
    void logout();
    LoginResponseDto obtenerEstadoSesionActual();
}
