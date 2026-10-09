package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.postgres.EstatusCuenta;
import com.proyecto.servicios.model.CuentaActualizacionDto;
import com.proyecto.servicios.model.CuentaResponseDto;

import java.util.List;

public interface CuentaService {
    CuentaResponseDto obtenerPorNumeroCuenta(String numeroCuenta);
    List<CuentaResponseDto> listarTodas(Long clienteId, EstatusCuenta estatus);
    CuentaResponseDto crearCuenta(Long clienteId);
    CuentaResponseDto actualizar(String numeroCuenta, CuentaActualizacionDto dto);
}
