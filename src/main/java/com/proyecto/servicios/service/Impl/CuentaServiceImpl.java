package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.postgres.Cuenta;
import com.proyecto.servicios.entity.postgres.EstatusCuenta;
import com.proyecto.servicios.exception.BusinessRuleException;
import com.proyecto.servicios.exception.ResourceNotFoundException;
import com.proyecto.servicios.model.*;
import com.proyecto.servicios.repositorys.postgres.CuentaRepository;
import com.proyecto.servicios.service.CuentaService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CuentaServiceImpl implements CuentaService {

    private final CuentaRepository cuentaRepository;
    private final com.proyecto.servicios.repositorys.postgres.ClienteRepository clienteRepository;

    public CuentaServiceImpl(CuentaRepository cuentaRepository, com.proyecto.servicios.repositorys.postgres.ClienteRepository clienteRepository) {
        this.cuentaRepository = cuentaRepository;
        this.clienteRepository = clienteRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<CuentaResponseDto> buscar(CuentaBusquedaRequestDto dto) {
        Specification<Cuenta> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (dto.getNumeroCuenta() != null && !dto.getNumeroCuenta().isBlank()) {
                predicates.add(cb.equal(root.get("numeroCuenta"), dto.getNumeroCuenta()));
            }
            if (dto.getClienteId() != null) {
                predicates.add(cb.equal(root.get("cliente").get("id"), dto.getClienteId()));
            }
            if (dto.getEstatus() != null) {
                predicates.add(cb.equal(root.get("estatus"), dto.getEstatus()));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        int page = (dto.getPage() != null && dto.getPage() >= 0) ? dto.getPage() : 0;
        int size = (dto.getSize() != null && dto.getSize() > 0) ? dto.getSize() : 10;
        Pageable pageable = PageRequest.of(page, size);

        Page<Cuenta> pagina = cuentaRepository.findAll(spec, pageable);

        if (pagina.isEmpty()) {
            throw new ResourceNotFoundException("No se encontraron cuentas con los criterios de búsqueda proporcionados.");
        }

        List<CuentaResponseDto> contenido = pagina.getContent().stream()
                .map(this::mapearAResponse)
                .collect(Collectors.toList());

        return PaginatedResponse.<CuentaResponseDto>builder()
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
    public CuentaResponseDto crearCuenta(CuentaCreacionRequestDto dto) {
        com.proyecto.servicios.entity.postgres.Cliente cliente = clienteRepository.findById(dto.getClienteId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró un cliente con ID: " + dto.getClienteId()));

        if (Boolean.FALSE.equals(cliente.getActivo())) {
            throw new BusinessRuleException("No se pueden crear cuentas para clientes inactivos.");
        }

        String numeroCuentaUnico = String.format("%010d", (long) (Math.random() * 10000000000L));
        while (cuentaRepository.findByNumeroCuenta(numeroCuentaUnico).isPresent()) {
            numeroCuentaUnico = String.format("%010d", (long) (Math.random() * 10000000000L));
        }

        Cuenta cuenta = Cuenta.builder()
                .numeroCuenta(numeroCuentaUnico)
                .saldo(BigDecimal.ZERO)
                .estatus(EstatusCuenta.ACTIVA)
                .cliente(cliente)
                .build();

        return mapearAResponse(cuentaRepository.save(cuenta));
    }

    @Override
    @Transactional
    public CuentaResponseDto actualizar(CuentaActualizacionDto dto) {
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(dto.getNumeroCuenta())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró una cuenta con número: " + dto.getNumeroCuenta()));

        if (dto.getSaldo() != null && dto.getSaldo().signum() < 0) {
            throw new BusinessRuleException("El saldo no puede ser negativo.");
        }

        if (dto.getSaldo() != null)  cuenta.setSaldo(dto.getSaldo());
        if (dto.getEstatus() != null) cuenta.setEstatus(dto.getEstatus());

        return mapearAResponse(cuentaRepository.save(cuenta));
    }

    private CuentaResponseDto mapearAResponse(Cuenta c) {
        String nombreCliente = "";
        Long clienteId = null;
        if (c.getCliente() != null) {
            clienteId = c.getCliente().getId();
            nombreCliente = c.getCliente().getNombre() + " " + c.getCliente().getApellidoPaterno();
        }
        return CuentaResponseDto.builder()
                .id(c.getId())
                .numeroCuenta(c.getNumeroCuenta())
                .saldo(c.getSaldo())
                .estatus(c.getEstatus())
                .clienteId(clienteId)
                .nombreCliente(nombreCliente)
                .build();
    }
}
