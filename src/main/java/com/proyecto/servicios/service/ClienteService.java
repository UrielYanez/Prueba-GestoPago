package com.proyecto.servicios.service;

import com.proyecto.servicios.model.*;

public interface ClienteService {
    ClienteResponseDto registrarCliente(ClienteRegistroDto dto);
    PaginatedResponse<ClienteResponseDto> buscar(ClienteBusquedaRequestDto dto);
    ClienteResponseDto actualizar(ClienteActualizacionDto dto);
    void desactivar(ClienteBajaRequestDto dto);
    void reactivar(ReactivacionDto dto);
}
