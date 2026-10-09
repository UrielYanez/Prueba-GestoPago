package com.proyecto.servicios.controller;

import com.proyecto.servicios.entity.postgres.EstatusCuenta;
import com.proyecto.servicios.model.CuentaActualizacionDto;
import com.proyecto.servicios.model.CuentaResponseDto;
import com.proyecto.servicios.service.CuentaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cuentas")
public class CuentaController {

    private final CuentaService cuentaService;

    public CuentaController(CuentaService cuentaService) {
        this.cuentaService = cuentaService;
    }

    /**
     * GET /cuentas/{numeroCuenta}
     * Consulta una cuenta por su número. Requiere JWT.
     */
    @GetMapping("/{numeroCuenta}")
    public ResponseEntity<CuentaResponseDto> obtenerPorNumeroCuenta(@PathVariable String numeroCuenta) {
        return ResponseEntity.ok(cuentaService.obtenerPorNumeroCuenta(numeroCuenta));
    }

    /**
     * GET /cuentas
     * Búsqueda flexible por cliente y/o estatus. Requiere JWT.
     */
    @GetMapping
    public ResponseEntity<List<CuentaResponseDto>> listarTodas(
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) EstatusCuenta estatus) {
        return ResponseEntity.ok(cuentaService.listarTodas(clienteId, estatus));
    }

    /**
     * POST /cuentas
     * Crear una cuenta asociada a un cliente.
     */
    @PostMapping
    public ResponseEntity<CuentaResponseDto> crearCuenta(@RequestParam Long clienteId) {
        return new ResponseEntity<>(cuentaService.crearCuenta(clienteId), org.springframework.http.HttpStatus.CREATED);
    }

    /**
     * PATCH /cuentas/{numeroCuenta}
     * Actualiza el saldo o estatus de una cuenta. No permite cambiar el numeroCuenta. Requiere JWT.
     */
    @PatchMapping("/{numeroCuenta}")
    public ResponseEntity<CuentaResponseDto> actualizar(
            @PathVariable String numeroCuenta,
            @RequestBody CuentaActualizacionDto dto) {
        return ResponseEntity.ok(cuentaService.actualizar(numeroCuenta, dto));
    }
}
