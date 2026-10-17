package com.proyecto.servicios.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Envoltorio para resultados paginados de consultas dinámicas.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginatedResponse<T> {
    private List<T> contenido;
    private int paginaActual;
    private int tamanoPagina;
    private long totalElementos;
    private int totalPaginas;
    private boolean esPrimera;
    private boolean esUltima;
}
