package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.*;
import com.proyecto.servicios.service.ClienteService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para la gestión de Clientes.
 * Cumple estrictamente con las restricciones:
 * - Cero uso de @PathVariable y @RequestParam.
 * - Toda entrada de datos y criterios de búsqueda se reciben vía @RequestBody con @Valid.
 * - Respuestas uniformes utilizando ApiResponse<T>.
 */
@RestController
@RequestMapping("/clientes")
@Tag(name = "Clientes", description = "Gestión integral de clientes: Onboarding, búsqueda multicriterio dinámica paginada, actualización parcial, baja lógica y reactivación")
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
    public ResponseEntity<ApiResponse<ClienteResponseDto>> registrarCliente(@Valid @RequestBody ClienteRegistroDto dto) {
        ClienteResponseDto clienteCreado = clienteService.registrarCliente(dto);
        ApiResponse<ClienteResponseDto> respuesta = ApiResponse.exitoso(
                "Cliente registrado exitosamente y cuenta bancaria creada.",
                clienteCreado,
                HttpStatus.CREATED.value()
        );
        return new ResponseEntity<>(respuesta, HttpStatus.CREATED);
    }

    /**
     * POST /clientes/buscar
     * Único endpoint de consulta combinada y dinámica (paginada).
     * Recibe los criterios y paginación en el Request DTO mediante @RequestBody.
     */
    @PostMapping("/buscar")
    public ResponseEntity<ApiResponse<PaginatedResponse<ClienteResponseDto>>> buscar(
            @Valid @RequestBody ClienteBusquedaRequestDto dto) {
        PaginatedResponse<ClienteResponseDto> resultados = clienteService.buscar(dto);
        ApiResponse<PaginatedResponse<ClienteResponseDto>> respuesta = ApiResponse.exitoso(
                "Consulta de clientes realizada con éxito.",
                resultados,
                HttpStatus.OK.value()
        );
        return ResponseEntity.ok(respuesta);
    }

    /**
     * PATCH /clientes/actualizar
     * Actualización parcial vía @RequestBody (el ID viene dentro del DTO).
     * Impide modificar campos controlados por el servidor (CURP, RFC, número de cuenta, etc.).
     */
    @PatchMapping("/actualizar")
    public ResponseEntity<ApiResponse<ClienteResponseDto>> actualizar(
            @Valid @RequestBody ClienteActualizacionDto dto) {
        ClienteResponseDto clienteActualizado = clienteService.actualizar(dto);
        ApiResponse<ClienteResponseDto> respuesta = ApiResponse.exitoso(
                "Cliente actualizado exitosamente.",
                clienteActualizado,
                HttpStatus.OK.value()
        );
        return ResponseEntity.ok(respuesta);
    }

    /**
     * POST /clientes/desactivar
     * Baja lógica vía @RequestBody (el ID viene dentro del DTO).
     */
    @PostMapping("/desactivar")
    public ResponseEntity<ApiResponse<Void>> desactivar(
            @Valid @RequestBody ClienteBajaRequestDto dto) {
        clienteService.desactivar(dto);
        ApiResponse<Void> respuesta = ApiResponse.exitoso("Cliente desactivado exitosamente.");
        return ResponseEntity.ok(respuesta);
    }

    /**
     * POST /clientes/reactivar
     * Reactivación de cuenta vía @RequestBody.
     */
    @PostMapping("/reactivar")
    public ResponseEntity<ApiResponse<Void>> reactivar(
            @Valid @RequestBody ReactivacionDto dto) {
        clienteService.reactivar(dto);
        ApiResponse<Void> respuesta = ApiResponse.exitoso("Cuenta reactivada exitosamente. Ya puedes iniciar sesión nuevamente.");
        return ResponseEntity.ok(respuesta);
    }
}
