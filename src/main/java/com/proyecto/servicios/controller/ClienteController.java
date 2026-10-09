package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.ClienteActualizacionDto;
import com.proyecto.servicios.model.ClienteRegistroDto;
import com.proyecto.servicios.model.ClienteResponseDto;
import com.proyecto.servicios.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    /**
     * POST /clientes
     * Registro completo de un cliente (Onboarding). Ruta pública.
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> registrarCliente(@Valid @RequestBody ClienteRegistroDto dto) {
        clienteService.registrarCliente(dto);
        return new ResponseEntity<>(Map.of("mensaje", "Cliente registrado exitosamente y cuenta bancaria creada."), HttpStatus.CREATED);
    }

    /**
     * GET /clientes
     * Lista todos los clientes registrados, con la opción de filtrar por múltiples campos. Requiere JWT.
     */
    @GetMapping
    public ResponseEntity<List<ClienteResponseDto>> listarTodos(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String segundoNombre,
            @RequestParam(required = false) String apellidoPaterno,
            @RequestParam(required = false) String apellidoMaterno,
            @RequestParam(required = false) String curp,
            @RequestParam(required = false) String rfc,
            @RequestParam(required = false) String correo,
            @RequestParam(required = false) String numeroCuenta,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate fechaInicio,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate fechaFin) {
        return ResponseEntity.ok(clienteService.listarTodos(nombre, segundoNombre, apellidoPaterno, apellidoMaterno, curp, rfc, correo, numeroCuenta, activo, fechaInicio, fechaFin));
    }

    /**
     * GET /clientes/{id}
     * Obtiene un cliente por su ID. Requiere JWT.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ClienteResponseDto> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.obtenerPorId(id));
    }

    /**
     * PATCH /clientes/{id}
     * Actualización parcial. No permite modificar CURP, RFC ni numeroCuenta. Requiere JWT.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<ClienteResponseDto> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ClienteActualizacionDto dto) {
        return ResponseEntity.ok(clienteService.actualizar(id, dto));
    }

    /**
     * DELETE /clientes/{id}
     * Baja lógica: cambia activo=false en cliente, usuario y sus cuentas. Requiere JWT.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> desactivar(@PathVariable Long id) {
        clienteService.desactivar(id);
        return ResponseEntity.ok(Map.of("mensaje", "Cliente desactivado exitosamente."));
    }

    /**
     * POST /clientes/reactivar
     * Reactiva una cuenta inactiva. No requiere Token, pero requiere credenciales y datos de seguridad.
     */
    @PostMapping("/reactivar")
    public ResponseEntity<Map<String, String>> reactivar(@Valid @RequestBody com.proyecto.servicios.model.ReactivacionDto dto) {
        clienteService.reactivar(dto);
        return ResponseEntity.ok(Map.of("mensaje", "Cuenta reactivada exitosamente. Ya puedes iniciar sesión nuevamente."));
    }
}
