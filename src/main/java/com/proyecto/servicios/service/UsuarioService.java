package com.proyecto.servicios.service;

import com.proyecto.servicios.model.UsuarioRegistroDto;
import com.proyecto.servicios.model.UsuarioResponseDto;
import java.util.List;

public interface UsuarioService {
    List<UsuarioResponseDto> filtrarUsuarios(String correo, Boolean activo);
    UsuarioResponseDto agregarUsuario(UsuarioRegistroDto dto);
}
