package com.proyecto.servicios.service;

import com.proyecto.servicios.model.ClienteActualizacionDto;
import com.proyecto.servicios.model.ClienteRegistroDto;
import com.proyecto.servicios.model.ClienteResponseDto;

import java.util.List;

public interface ClienteService {
    void registrarCliente(ClienteRegistroDto dto);
    List<ClienteResponseDto> listarTodos(String nombre, String segundoNombre, String apellidoPaterno, String apellidoMaterno, 
                                         String curp, String rfc, String correo, String numeroCuenta, Boolean activo, 
                                         java.time.LocalDate fechaInicio, java.time.LocalDate fechaFin);
    ClienteResponseDto obtenerPorId(Long id);
    ClienteResponseDto actualizar(Long id, ClienteActualizacionDto dto);
    void desactivar(Long id);
    void reactivar(com.proyecto.servicios.model.ReactivacionDto dto);
}
