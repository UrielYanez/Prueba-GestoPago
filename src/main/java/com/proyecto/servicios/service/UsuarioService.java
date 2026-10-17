package com.proyecto.servicios.service;

import com.proyecto.servicios.model.PaginatedResponse;
import com.proyecto.servicios.model.UsuarioBusquedaRequestDto;
import com.proyecto.servicios.model.UsuarioRegistroDto;
import com.proyecto.servicios.model.UsuarioResponseDto;

public interface UsuarioService {
    PaginatedResponse<UsuarioResponseDto> buscar(UsuarioBusquedaRequestDto dto);
    UsuarioResponseDto agregarUsuario(UsuarioRegistroDto dto);
}
