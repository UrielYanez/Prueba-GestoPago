package com.proyecto.servicios.exception;

import com.proyecto.servicios.model.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

/**
 * Manejador global centralizado de excepciones y validaciones (@RestControllerAdvice).
 * Garantiza respuestas con estructura uniforme (ApiResponse) y previene la fuga de trazas técnicas.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. Intercepta los errores de validación de los Request DTOs (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errores.put(error.getField(), error.getDefaultMessage());
        }
        ApiResponse<Map<String, String>> response = ApiResponse.error(
                "Errores de validación en la solicitud.",
                errores,
                HttpStatus.BAD_REQUEST.value()
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // 2. Recursos duplicados (ej. CURP, RFC, Correo ya registrados)
    @ExceptionHandler(DuplicatedResourceException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicatedResourceException(DuplicatedResourceException ex) {
        ApiResponse<Void> response = ApiResponse.error(ex.getMessage(), HttpStatus.CONFLICT.value());
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    // 3. Reglas de negocio no cumplidas (ej. menor de edad, cliente inactivo)
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessRuleException(BusinessRuleException ex) {
        ApiResponse<Void> response = ApiResponse.error(ex.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY.value());
        return new ResponseEntity<>(response, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    // 4. Recursos no encontrados (ej. ID inexistente, búsqueda sin coincidencias)
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        ApiResponse<Void> response = ApiResponse.error(ex.getMessage(), HttpStatus.NOT_FOUND.value());
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    // 5. Credenciales inválidas
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidCredentialsException(InvalidCredentialsException ex) {
        ApiResponse<Void> response = ApiResponse.error(ex.getMessage(), HttpStatus.UNAUTHORIZED.value());
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    // 6. Tipos de datos incompatibles en parámetros
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String parametro = ex.getName();
        String valorEnviado = ex.getValue() != null ? ex.getValue().toString() : "null";
        String mensaje = "El valor '" + valorEnviado + "' para el parámetro '" + parametro + "' no tiene el formato requerido.";
        ApiResponse<Void> response = ApiResponse.error(mensaje, HttpStatus.BAD_REQUEST.value());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // 7. Cuerpo JSON mal formado o tipos de datos incompatibles (sin exponer nombres de clases ni trazas técnicas)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Error de lectura/deserialización de JSON: {}", ex.getMessage());
        ApiResponse<Void> response = ApiResponse.error(
                "El cuerpo de la solicitud (JSON) es inválido, está mal formado o contiene tipos de datos incompatibles.",
                HttpStatus.BAD_REQUEST.value()
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // 8. Método HTTP no soportado
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        ApiResponse<Void> response = ApiResponse.error(
                "El método HTTP " + ex.getMethod() + " no está soportado para este endpoint.",
                HttpStatus.METHOD_NOT_ALLOWED.value()
        );
        return new ResponseEntity<>(response, HttpStatus.METHOD_NOT_ALLOWED);
    }

    // 9. Captura genérica de cualquier error inesperado para evitar fugas de SQL o stacktraces
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Excepción inesperada no controlada: {}", ex.getMessage(), ex);
        ApiResponse<Void> response = ApiResponse.error(
                "Ocurrió un error interno en el servidor. Por favor intenta más tarde.",
                HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
