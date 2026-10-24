package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.*;
import com.proyecto.servicios.service.CuentaService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para la gestión de cuentas bancarias.
 * Cumple con las restricciones:
 * - Cero uso de @PathVariable y @RequestParam.
 * - Toda entrada de datos mediante @RequestBody y @Valid.
 * - Respuestas uniformes mediante ApiResponse<T>.
 */
@RestController
@RequestMapping("/cuentas")
@Tag(name = "Cuentas", description = "Administración de cuentas bancarias: apertura de cuentas secundarias, depósitos, retiros y balances")
public class CuentaController {

    private final CuentaService cuentaService;

    public CuentaController(CuentaService cuentaService) {
        this.cuentaService = cuentaService;
    }

    /**
     * POST /cuentas/buscar
     * Consulta dinámica y paginada por criterios (número de cuenta, clienteId, estatus).
     */
    @PostMapping("/buscar")
    public ResponseEntity<ApiResponse<PaginatedResponse<CuentaResponseDto>>> buscar(
            @Valid @RequestBody CuentaBusquedaRequestDto dto) {
        PaginatedResponse<CuentaResponseDto> resultados = cuentaService.buscar(dto);
        ApiResponse<PaginatedResponse<CuentaResponseDto>> respuesta = ApiResponse.exitoso(
                "Consulta de cuentas realizada con éxito.",
                resultados,
                HttpStatus.OK.value()
        );
        return ResponseEntity.ok(respuesta);
    }

    /**
     * POST /cuentas
     * Crear una cuenta asociada a un cliente vía @RequestBody.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CuentaResponseDto>> crearCuenta(
            @Valid @RequestBody CuentaCreacionRequestDto dto) {
        CuentaResponseDto cuentaCreada = cuentaService.crearCuenta(dto);
        ApiResponse<CuentaResponseDto> respuesta = ApiResponse.exitoso(
                "Cuenta bancaria creada exitosamente.",
                cuentaCreada,
                HttpStatus.CREATED.value()
        );
        return new ResponseEntity<>(respuesta, HttpStatus.CREATED);
    }

    /**
     * PATCH /cuentas/actualizar
     * Actualiza el saldo o estatus de una cuenta vía @RequestBody.
     */
    @PatchMapping("/actualizar")
    public ResponseEntity<ApiResponse<CuentaResponseDto>> actualizar(
            @Valid @RequestBody CuentaActualizacionDto dto) {
        CuentaResponseDto cuentaActualizada = cuentaService.actualizar(dto);
        ApiResponse<CuentaResponseDto> respuesta = ApiResponse.exitoso(
                "Cuenta bancaria actualizada exitosamente.",
                cuentaActualizada,
                HttpStatus.OK.value()
        );
        return ResponseEntity.ok(respuesta);
    }
}
