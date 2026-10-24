package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.ApiResponse;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador de respaldo para la ruta /error.
 * Garantiza que cualquier error a nivel de contenedor de servlets
 * mantenga el formato uniforme ApiResponse.
 */
@Hidden
@RestController
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public ResponseEntity<ApiResponse<Void>> handleError(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int statusCode = HttpStatus.NOT_FOUND.value();
        if (status != null) {
            try {
                statusCode = Integer.parseInt(status.toString());
            } catch (NumberFormatException ignored) {
            }
        }

        String mensaje;
        if (statusCode == HttpStatus.NOT_FOUND.value()) {
            mensaje = "La ruta o recurso solicitado no fue encontrado.";
        } else if (statusCode == HttpStatus.FORBIDDEN.value()) {
            mensaje = "Acceso denegado: No tienes los permisos necesarios para realizar esta acción.";
        } else if (statusCode == HttpStatus.UNAUTHORIZED.value()) {
            mensaje = "No autorizado: Se requiere un token JWT válido para acceder a este recurso.";
        } else if (statusCode == HttpStatus.METHOD_NOT_ALLOWED.value()) {
            mensaje = "El método HTTP no está permitido para este recurso.";
        } else {
            mensaje = "Ocurrió un error al procesar la solicitud.";
        }

        HttpStatus httpStatus = HttpStatus.resolve(statusCode);
        if (httpStatus == null) {
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        return new ResponseEntity<>(ApiResponse.error(mensaje, statusCode), httpStatus);
    }
}
