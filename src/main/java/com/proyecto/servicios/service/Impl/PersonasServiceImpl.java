package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.sf.Personas;
import com.proyecto.servicios.exception.ResourceNotFoundException;
import com.proyecto.servicios.model.EliminaPersonaRequest;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.PersonaResponse;
import com.proyecto.servicios.model.PersonasRequest;
import com.proyecto.servicios.repositorys.sf.PersonasRepository;
import com.proyecto.servicios.service.PersonaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class PersonasServiceImpl implements PersonaService {

    private final PersonasRepository personasRepository;

    public PersonasServiceImpl(PersonasRepository personasRepository) {
        this.personasRepository = personasRepository;
    }

    @Override
    public PersonaResponse creaPersona(PersonasRequest personasRequest) {
        PersonaResponse person = new PersonaResponse();
        Personas persona = new Personas();
        persona.setNombre(personasRequest.getNombre());
        persona.setApellidoMaterno(personasRequest.getApellidoMaterno());
        persona.setApellidoP(personasRequest.getApellidoP());
        personasRepository.save(persona);
        person.setCodigo(0);
        person.setMensaje("Exito");
        BeanUtils.copyProperties(persona, person);
        return person;
    }

    @Override
    public GenericResponse eliminaPersona(EliminaPersonaRequest eliminaPersonaRequest) {
        Optional<Personas> existePersona = personasRepository.findByNombre(eliminaPersonaRequest.getNombre());
        if (existePersona.isEmpty()) {
            throw new ResourceNotFoundException("La persona no existe");
        }
        personasRepository.delete(existePersona.get());
        GenericResponse genericResponse = new GenericResponse();
        genericResponse.setCodigo(0);
        genericResponse.setMensaje("La persona ha sido eliminada correctamente");
        return genericResponse;
    }

    @Override
    public GenericResponse actualizaPersona(PersonasRequest personasRequest) {
        Optional<Personas> existePersona = personasRepository.findByNombre(personasRequest.getNombre());
        if (existePersona.isEmpty()) {
            throw new ResourceNotFoundException("La persona no existe");
        }
        Personas personaActualiza = existePersona.get();
        personaActualiza.setApellidoP(personasRequest.getApellidoP());
        personaActualiza.setApellidoMaterno(personasRequest.getApellidoMaterno());
        personasRepository.save(personaActualiza);
        GenericResponse genericResponse = new GenericResponse();
        genericResponse.setCodigo(0);
        genericResponse.setMensaje("La persona ha sido actualizada correctamente");
        return genericResponse;
    }
}