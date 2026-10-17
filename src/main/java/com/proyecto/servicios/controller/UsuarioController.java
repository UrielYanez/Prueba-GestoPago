package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.ApiResponse;
import com.proyecto.servicios.model.PaginatedResponse;
import com.proyecto.servicios.model.UsuarioBusquedaRequestDto;
import com.proyecto.servicios.model.UsuarioRegistroDto;
import com.proyecto.servicios.model.UsuarioResponseDto;
import com.proyecto.servicios.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para la administración de usuarios.
 * Cumple con las restricciones:
 * - Cero uso de @PathVariable y @RequestParam.
 * - Búsqueda dinámica vía @RequestBody con Request DTO.
 * - Respuestas uniformes mediante ApiResponse<T>.
 */
@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /**
     * POST /usuarios/buscar
     * Único endpoint de búsqueda dinámica y paginada para usuarios.
     */
    @PostMapping("/buscar")
    public ResponseEntity<ApiResponse<PaginatedResponse<UsuarioResponseDto>>> buscar(
            @Valid @RequestBody UsuarioBusquedaRequestDto dto) {
        PaginatedResponse<UsuarioResponseDto> resultados = usuarioService.buscar(dto);
        ApiResponse<PaginatedResponse<UsuarioResponseDto>> respuesta = ApiResponse.exitoso(
                "Consulta de usuarios realizada con éxito.",
                resultados,
                HttpStatus.OK.value()
        );
        return ResponseEntity.ok(respuesta);
    }

    /**
     * POST /usuarios
     * Agrega un usuario asociado a un cliente.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<UsuarioResponseDto>> agregarUsuario(
            @Valid @RequestBody UsuarioRegistroDto dto) {
        UsuarioResponseDto usuarioCreado = usuarioService.agregarUsuario(dto);
        ApiResponse<UsuarioResponseDto> respuesta = ApiResponse.exitoso(
                "Usuario creado exitosamente.",
                usuarioCreado,
                HttpStatus.CREATED.value()
        );
        return new ResponseEntity<>(respuesta, HttpStatus.CREATED);
    }
}
