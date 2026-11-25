package com.proyecto.servicios.service;

import com.proyecto.servicios.model.ComprobantePagoResponseDto;
import com.proyecto.servicios.model.ConciliarPagoRequestDto;
import com.proyecto.servicios.model.HistorialPagosRequestDto;
import com.proyecto.servicios.model.PaginatedResponse;
import com.proyecto.servicios.model.ProcesarPagoRequestDto;
import com.proyecto.servicios.model.SolicitudPinRequestDto;

public interface PagoService {

    void solicitarPin(SolicitudPinRequestDto request);

    ComprobantePagoResponseDto procesarPago(ProcesarPagoRequestDto request);

    ComprobantePagoResponseDto conciliarPago(ConciliarPagoRequestDto request);

    PaginatedResponse<ComprobantePagoResponseDto> consultarHistorialExitoso(HistorialPagosRequestDto request);
}
