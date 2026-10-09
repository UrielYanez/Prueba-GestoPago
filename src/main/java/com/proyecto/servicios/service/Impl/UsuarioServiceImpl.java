package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.postgres.Cliente;
import com.proyecto.servicios.entity.postgres.Usuario;
import com.proyecto.servicios.exception.BusinessRuleException;
import com.proyecto.servicios.exception.DuplicatedResourceException;
import com.proyecto.servicios.exception.ResourceNotFoundException;
import com.proyecto.servicios.model.UsuarioRegistroDto;
import com.proyecto.servicios.model.UsuarioResponseDto;
import com.proyecto.servicios.repositorys.postgres.ClienteRepository;
import com.proyecto.servicios.repositorys.postgres.UsuarioRepository;
import com.proyecto.servicios.service.UsuarioService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioServiceImpl(UsuarioRepository usuarioRepository, ClienteRepository clienteRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioResponseDto> filtrarUsuarios(String correo, Boolean activo) {
        org.springframework.data.jpa.domain.Specification<Usuario> spec = (root, query, criteriaBuilder) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
            if (correo != null && !correo.isBlank()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("correo")), "%" + correo.toLowerCase() + "%"));
            }
            if (activo != null) {
                predicates.add(criteriaBuilder.equal(root.get("activo"), activo));
            }
            return criteriaBuilder.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        List<Usuario> resultados = usuarioRepository.findAll(spec);
        if (resultados.isEmpty()) {
            throw new ResourceNotFoundException("No se encontraron usuarios para tu búsqueda.");
        }

        return resultados.stream().map(this::mapearAResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UsuarioResponseDto agregarUsuario(UsuarioRegistroDto dto) {
        if (usuarioRepository.findByCorreo(dto.getCorreo()).isPresent()) {
            throw new DuplicatedResourceException("Ya existe un usuario registrado con el correo: " + dto.getCorreo());
        }

        Cliente cliente = clienteRepository.findById(dto.getClienteId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró un cliente con ID: " + dto.getClienteId()));

        if (cliente.getUsuario() != null) {
            throw new BusinessRuleException("El cliente ya tiene un usuario asociado.");
        }

        Usuario usuario = Usuario.builder()
                .correo(dto.getCorreo())
                .password(passwordEncoder.encode(dto.getPassword()))
                .activo(true)
                .cliente(cliente)
                .build();

        usuario = usuarioRepository.save(usuario);
        
        // Actualizamos la relación inversa para el contexto actual
        cliente.setUsuario(usuario);
        clienteRepository.save(cliente);

        return mapearAResponse(usuario);
    }

    private UsuarioResponseDto mapearAResponse(Usuario u) {
        Long clienteId = null;
        if (u.getCliente() != null) {
            clienteId = u.getCliente().getId();
        }
        return UsuarioResponseDto.builder()
                .id(u.getId())
                .clienteId(clienteId)
                .correo(u.getCorreo())
                .activo(u.getActivo())
                .fechaCreacion(u.getFechaCreacion())
                .fechaActualizacion(u.getFechaActualizacion())
                .build();
    }
}
