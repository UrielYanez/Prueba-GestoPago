package com.proyecto.servicios.service;

import com.proyecto.servicios.model.LoginRequestDto;
import com.proyecto.servicios.model.LoginResponseDto;

public interface AuthService {
    LoginResponseDto login(LoginRequestDto request);
}
