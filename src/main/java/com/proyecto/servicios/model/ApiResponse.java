package com.proyecto.servicios.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Estructura estándar y uniforme para todas las respuestas exitosas y errores de la API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean exito;
    private String mensaje;
    private T datos;
    private int codigoEstado;
    
    @Builder.Default
    private LocalDateTime marcaTiempo = LocalDateTime.now();

    public static <T> ApiResponse<T> exitoso(String mensaje, T datos, int codigoEstado) {
        return ApiResponse.<T>builder()
                .exito(true)
                .mensaje(mensaje)
                .datos(datos)
                .codigoEstado(codigoEstado)
                .marcaTiempo(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> exitoso(String mensaje, T datos) {
        return exitoso(mensaje, datos, 200);
    }

    public static <T> ApiResponse<T> exitoso(String mensaje) {
        return exitoso(mensaje, null, 200);
    }

    public static <T> ApiResponse<T> error(String mensaje, T detalles, int codigoEstado) {
        return ApiResponse.<T>builder()
                .exito(false)
                .mensaje(mensaje)
                .datos(detalles)
                .codigoEstado(codigoEstado)
                .marcaTiempo(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> error(String mensaje, int codigoEstado) {
        return error(mensaje, null, codigoEstado);
    }
}
