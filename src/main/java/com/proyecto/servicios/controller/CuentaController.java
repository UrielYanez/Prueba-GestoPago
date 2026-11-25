package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.*;
import com.proyecto.servicios.service.CuentaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cuentas")
@Tag(name = "Cuentas", description = "Administración de cuentas bancarias: apertura de cuentas secundarias, depósitos, retiros y balances")
public class CuentaController {

    private final CuentaService cuentaService;

    public CuentaController(CuentaService cuentaService) {
        this.cuentaService = cuentaService;
    }

    @PostMapping("/buscar")
    @Operation(summary = "Buscar cuentas paginadas", description = "Consulta dinámica y paginada por criterios (número de cuenta, clienteId, estatus).")
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

    @PostMapping("/mis-cuentas")
    @Operation(summary = "Obtener cuentas del usuario autenticado", description = "Devuelve todas las cuentas y saldos asociados al usuario autenticado mediante su JWT.")
    public ResponseEntity<ApiResponse<List<CuentaResponseDto>>> obtenerMisCuentas() {
        List<CuentaResponseDto> cuentas = cuentaService.obtenerMisCuentas();
        ApiResponse<List<CuentaResponseDto>> respuesta = ApiResponse.exitoso(
                "Cuentas del usuario obtenidas con éxito.",
                cuentas,
                HttpStatus.OK.value()
        );
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping
    @Operation(summary = "Crear cuenta bancaria", description = "Crea una cuenta asociada a un cliente.")
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

    @PatchMapping("/actualizar")
    @Operation(summary = "Actualizar cuenta bancaria", description = "Actualiza el saldo o estatus de una cuenta.")
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
