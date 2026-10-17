package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.postgres.Cliente;
import com.proyecto.servicios.entity.postgres.Usuario;
import com.proyecto.servicios.exception.BusinessRuleException;
import com.proyecto.servicios.exception.DuplicatedResourceException;
import com.proyecto.servicios.exception.ResourceNotFoundException;
import com.proyecto.servicios.model.PaginatedResponse;
import com.proyecto.servicios.model.UsuarioBusquedaRequestDto;
import com.proyecto.servicios.model.UsuarioRegistroDto;
import com.proyecto.servicios.model.UsuarioResponseDto;
import com.proyecto.servicios.repositorys.postgres.ClienteRepository;
import com.proyecto.servicios.repositorys.postgres.UsuarioRepository;
import com.proyecto.servicios.service.UsuarioService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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
    public PaginatedResponse<UsuarioResponseDto> buscar(UsuarioBusquedaRequestDto dto) {
        Specification<Usuario> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (dto.getId() != null) {
                predicates.add(cb.equal(root.get("id"), dto.getId()));
            }
            if (dto.getClienteId() != null) {
                predicates.add(cb.equal(root.get("cliente").get("id"), dto.getClienteId()));
            }
            if (dto.getCorreo() != null && !dto.getCorreo().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("correo")), "%" + dto.getCorreo().toLowerCase() + "%"));
            }
            if (dto.getActivo() != null) {
                predicates.add(cb.equal(root.get("activo"), dto.getActivo()));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        int page = (dto.getPage() != null && dto.getPage() >= 0) ? dto.getPage() : 0;
        int size = (dto.getSize() != null && dto.getSize() > 0) ? dto.getSize() : 10;
        Pageable pageable = PageRequest.of(page, size);

        Page<Usuario> pagina = usuarioRepository.findAll(spec, pageable);

        if (pagina.isEmpty()) {
            throw new ResourceNotFoundException("No se encontraron usuarios con los criterios de búsqueda proporcionados.");
        }

        List<UsuarioResponseDto> contenido = pagina.getContent().stream()
                .map(this::mapearAResponse)
                .collect(Collectors.toList());

        return PaginatedResponse.<UsuarioResponseDto>builder()
                .contenido(contenido)
                .paginaActual(pagina.getNumber())
                .tamanoPagina(pagina.getSize())
                .totalElementos(pagina.getTotalElements())
                .totalPaginas(pagina.getTotalPages())
                .esPrimera(pagina.isFirst())
                .esUltima(pagina.isLast())
                .build();
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
