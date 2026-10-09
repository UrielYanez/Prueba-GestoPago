package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.UsuarioRegistroDto;
import com.proyecto.servicios.model.UsuarioResponseDto;
import com.proyecto.servicios.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /**
     * GET /usuarios/filtro
     * Buscar usuarios por correo o estatus. Requiere JWT.
     */
    @GetMapping("/filtro")
    public ResponseEntity<List<UsuarioResponseDto>> filtrarUsuarios(
            @RequestParam(required = false) String correo,
            @RequestParam(required = false) Boolean activo) {
        return ResponseEntity.ok(usuarioService.filtrarUsuarios(correo, activo));
    }

    /**
     * PUT /usuarios/agregar
     * Agrega un usuario (asociado a un cliente).
     */
    @PutMapping("/agregar")
    public ResponseEntity<UsuarioResponseDto> agregarUsuario(@Valid @RequestBody UsuarioRegistroDto dto) {
        return new ResponseEntity<>(usuarioService.agregarUsuario(dto), HttpStatus.CREATED);
    }
}
