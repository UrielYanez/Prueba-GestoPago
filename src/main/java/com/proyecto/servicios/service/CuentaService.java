package com.proyecto.servicios.service;

import com.proyecto.servicios.model.*;

public interface CuentaService {
    PaginatedResponse<CuentaResponseDto> buscar(CuentaBusquedaRequestDto dto);
    CuentaResponseDto crearCuenta(CuentaCreacionRequestDto dto);
    CuentaResponseDto actualizar(CuentaActualizacionDto dto);
}
