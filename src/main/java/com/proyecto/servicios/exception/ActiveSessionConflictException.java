package com.proyecto.servicios.exception;

import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class ActiveSessionConflictException extends RuntimeException {

    private final String plataformaActiva;
    private final LocalDateTime fechaInicioSesion;

    public ActiveSessionConflictException(String message, String plataformaActiva, LocalDateTime fechaInicioSesion) {
        super(message);
        this.plataformaActiva = plataformaActiva;
        this.fechaInicioSesion = fechaInicioSesion;
    }
}

