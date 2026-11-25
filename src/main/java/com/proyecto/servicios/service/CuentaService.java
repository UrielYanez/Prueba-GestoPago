package com.proyecto.servicios.service;

import com.proyecto.servicios.model.*;

import java.util.List;

public interface CuentaService {
    PaginatedResponse<CuentaResponseDto> buscar(CuentaBusquedaRequestDto dto);
    List<CuentaResponseDto> obtenerMisCuentas();
    CuentaResponseDto crearCuenta(CuentaCreacionRequestDto dto);
    CuentaResponseDto actualizar(CuentaActualizacionDto dto);
}
