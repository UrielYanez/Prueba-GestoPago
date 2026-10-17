package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.ApiResponse;
import com.proyecto.servicios.model.EliminaPersonaRequest;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.PersonaResponse;
import com.proyecto.servicios.model.PersonasRequest;
import com.proyecto.servicios.service.PersonaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class PersonaController {

    private final PersonaService personaService;

    public PersonaController(PersonaService personaService) {
        this.personaService = personaService;
    }

    @PostMapping(value = "/personas", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PersonaResponse>> crearUser(@Valid @RequestBody PersonasRequest personasRequest) {
        PersonaResponse response = personaService.creaPersona(personasRequest);
        return ResponseEntity.ok(ApiResponse.exitoso("Persona creada exitosamente.", response, HttpStatus.OK.value()));
    }

    @PutMapping(value = "/personasActualiza", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<GenericResponse>> actualizarPersona(@Valid @RequestBody PersonasRequest personasRequest) {
        GenericResponse response = personaService.actualizaPersona(personasRequest);
        return ResponseEntity.ok(ApiResponse.exitoso("Persona actualizada exitosamente.", response, HttpStatus.OK.value()));
    }

    @DeleteMapping(value = "/personasElimina", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<GenericResponse>> eliminarPersona(@Valid @RequestBody EliminaPersonaRequest personasRequest) {
        GenericResponse response = personaService.eliminaPersona(personasRequest);
        return ResponseEntity.ok(ApiResponse.exitoso("Persona eliminada exitosamente.", response, HttpStatus.OK.value()));
    }
}