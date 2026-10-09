package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.postgres.Cuenta;
import com.proyecto.servicios.entity.postgres.EstatusCuenta;
import com.proyecto.servicios.exception.BusinessRuleException;
import com.proyecto.servicios.exception.ResourceNotFoundException;
import com.proyecto.servicios.model.CuentaActualizacionDto;
import com.proyecto.servicios.model.CuentaResponseDto;
import com.proyecto.servicios.repositorys.postgres.CuentaRepository;
import com.proyecto.servicios.service.CuentaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public CuentaResponseDto obtenerPorNumeroCuenta(String numeroCuenta) {
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró una cuenta con número: " + numeroCuenta));
        return mapearAResponse(cuenta);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CuentaResponseDto> listarTodas(Long clienteId, EstatusCuenta estatus) {
        org.springframework.data.jpa.domain.Specification<Cuenta> spec = (root, query, criteriaBuilder) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
            if (clienteId != null) {
                predicates.add(criteriaBuilder.equal(root.get("cliente").get("id"), clienteId));
            }
            if (estatus != null) {
                predicates.add(criteriaBuilder.equal(root.get("estatus"), estatus));
            }
            return criteriaBuilder.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        List<Cuenta> resultados = cuentaRepository.findAll(spec);

        if (resultados.isEmpty()) {
            throw new ResourceNotFoundException("No se encontraron cuentas para tu búsqueda.");
        }

        return resultados.stream().map(this::mapearAResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CuentaResponseDto crearCuenta(Long clienteId) {
        com.proyecto.servicios.entity.postgres.Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró un cliente con ID: " + clienteId));

        if (Boolean.FALSE.equals(cliente.getActivo())) {
            throw new BusinessRuleException("No se pueden crear cuentas para clientes inactivos.");
        }

        String numeroCuentaUnico = String.format("%010d", (long) (Math.random() * 10000000000L));
        while (cuentaRepository.findByNumeroCuenta(numeroCuentaUnico).isPresent()) {
            numeroCuentaUnico = String.format("%010d", (long) (Math.random() * 10000000000L));
        }

        Cuenta cuenta = Cuenta.builder()
                .numeroCuenta(numeroCuentaUnico)
                .saldo(java.math.BigDecimal.ZERO)
                .estatus(EstatusCuenta.ACTIVA)
                .cliente(cliente)
                .build();

        return mapearAResponse(cuentaRepository.save(cuenta));
    }

    @Override
    @Transactional
    public CuentaResponseDto actualizar(String numeroCuenta, CuentaActualizacionDto dto) {
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró una cuenta con número: " + numeroCuenta));

        // Validar que no se meta un saldo negativo
        if (dto.getSaldo() != null && dto.getSaldo().signum() < 0) {
            throw new BusinessRuleException("El saldo no puede ser negativo.");
        }

        if (dto.getSaldo() != null)  cuenta.setSaldo(dto.getSaldo());
        if (dto.getEstatus() != null) cuenta.setEstatus(dto.getEstatus());

        return mapearAResponse(cuentaRepository.save(cuenta));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPER
    // ─────────────────────────────────────────────────────────────────────────
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
