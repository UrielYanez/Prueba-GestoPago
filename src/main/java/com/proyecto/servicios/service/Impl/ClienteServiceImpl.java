package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.CopomexClient;
import com.proyecto.servicios.entity.postgres.*;
import com.proyecto.servicios.exception.BusinessRuleException;
import com.proyecto.servicios.exception.DuplicatedResourceException;
import com.proyecto.servicios.exception.ResourceNotFoundException;
import com.proyecto.servicios.model.*;
import com.proyecto.servicios.repositorys.postgres.ClienteRepository;
import com.proyecto.servicios.repositorys.postgres.CuentaRepository;
import com.proyecto.servicios.repositorys.postgres.UsuarioRepository;
import com.proyecto.servicios.service.ClienteService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClienteServiceImpl implements ClienteService {

    private final ClienteRepository clienteRepository;
    private final CuentaRepository cuentaRepository;
    private final UsuarioRepository usuarioRepository;
    private final CopomexClient copomexClient;
    private final PasswordEncoder passwordEncoder;

    @org.springframework.beans.factory.annotation.Value("${copomex.api.token:pruebas}")
    private String copomexToken;

    public ClienteServiceImpl(ClienteRepository clienteRepository, CuentaRepository cuentaRepository,
                              UsuarioRepository usuarioRepository, CopomexClient copomexClient,
                              PasswordEncoder passwordEncoder) {
        this.clienteRepository = clienteRepository;
        this.cuentaRepository = cuentaRepository;
        this.usuarioRepository = usuarioRepository;
        this.copomexClient = copomexClient;
        this.passwordEncoder = passwordEncoder;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // REGISTRO (Onboarding)
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public ClienteResponseDto registrarCliente(ClienteRegistroDto dto) {
        // 1. Validar Edad (Mayor de 18 años)
        int edad = Period.between(dto.getFechaNacimiento(), LocalDate.now()).getYears();
        if (edad < 18) {
            throw new BusinessRuleException("El cliente debe ser mayor de edad (18 años o más).");
        }

        // 2. Validar Duplicados
        if (clienteRepository.findByCurp(dto.getCurp()).isPresent()) {
            throw new DuplicatedResourceException("Ya existe un cliente registrado con la CURP: " + dto.getCurp());
        }
        if (clienteRepository.findByRfc(dto.getRfc()).isPresent()) {
            throw new DuplicatedResourceException("Ya existe un cliente registrado con el RFC: " + dto.getRfc());
        }
        if (usuarioRepository.findByCorreo(dto.getCorreoElectronico()).isPresent()) {
            throw new DuplicatedResourceException("Ya existe un usuario registrado con el correo: " + dto.getCorreoElectronico());
        }

        // 3. Autocompletar con Copomex si no se enviaron datos completos de dirección
        if (dto.getColonia() == null || dto.getColonia().isBlank() ||
            dto.getMunicipio() == null || dto.getMunicipio().isBlank() ||
            dto.getEstado() == null || dto.getEstado().isBlank()) {
            try {
                List<CopomexResponseDto> copomexData = copomexClient.getInfoByCp(dto.getCodigoPostal(), copomexToken);
                if (copomexData != null && !copomexData.isEmpty()) {
                    CopomexResponseDto firstMatch = copomexData.get(0);
                    if (!firstMatch.isError()) {
                        if (dto.getColonia() == null || dto.getColonia().isBlank()) dto.setColonia(firstMatch.getResponse().getAsentamiento());
                        if (dto.getMunicipio() == null || dto.getMunicipio().isBlank()) dto.setMunicipio(firstMatch.getResponse().getMunicipio());
                        if (dto.getEstado() == null || dto.getEstado().isBlank()) dto.setEstado(firstMatch.getResponse().getEstado());
                    }
                }
            } catch (Exception ignored) {
                // Si la API externa no responde, no interrumpimos el registro
            }
        }

        // 4. Mapear Usuario (con contraseña encriptada)
        Usuario usuario = Usuario.builder()
                .correo(dto.getCorreoElectronico())
                .password(passwordEncoder.encode(dto.getPassword()))
                .activo(true)
                .build();

        // 5. Mapear Domicilio
        Domicilio domicilio = Domicilio.builder()
                .calle(dto.getCalle())
                .numeroExterior(dto.getNumeroExterior())
                .numeroInterior(dto.getNumeroInterior())
                .colonia(dto.getColonia())
                .municipio(dto.getMunicipio())
                .estado(dto.getEstado())
                .codigoPostal(dto.getCodigoPostal())
                .pais(dto.getPais())
                .build();

        // 6. Mapear Cliente
        Cliente cliente = Cliente.builder()
                .nombre(dto.getNombre())
                .segundoNombre(dto.getSegundoNombre())
                .apellidoPaterno(dto.getApellidoPaterno())
                .apellidoMaterno(dto.getApellidoMaterno())
                .fechaNacimiento(dto.getFechaNacimiento())
                .curp(dto.getCurp())
                .rfc(dto.getRfc())
                .sexo(dto.getSexo())
                .nacionalidad(dto.getNacionalidad())
                .estadoCivil(dto.getEstadoCivil())
                .correoElectronico(dto.getCorreoElectronico())
                .telefonoMovil(dto.getTelefonoMovil())
                .telefonoAlternativo(dto.getTelefonoAlternativo())
                .ocupacion(dto.getOcupacion())
                .empresa(dto.getEmpresa())
                .ingresoMensual(dto.getIngresoMensual())
                .activo(true)
                .usuario(usuario)
                .domicilio(domicilio)
                .build();

        usuario.setCliente(cliente);
        cliente = clienteRepository.save(cliente);

        // 7. Crear Cuenta Bancaria con número único de 10 dígitos
        String numeroCuentaUnico = generarNumeroCuentaUnico();
        Cuenta cuenta = Cuenta.builder()
                .numeroCuenta(numeroCuentaUnico)
                .saldo(BigDecimal.ZERO)
                .estatus(EstatusCuenta.ACTIVA)
                .cliente(cliente)
                .build();
        cuentaRepository.save(cuenta);

        return mapearAResponse(cliente);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ÚNICO ENDPOINT DE BÚSQUEDA Y CONSULTA DINÁMICA (Paginada vía @RequestBody)
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<ClienteResponseDto> buscar(ClienteBusquedaRequestDto dto) {
        Specification<Cliente> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (dto.getId() != null) {
                predicates.add(cb.equal(root.get("id"), dto.getId()));
            }
            if (dto.getNombre() != null && !dto.getNombre().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("nombre")), "%" + dto.getNombre().toLowerCase() + "%"));
            }
            if (dto.getSegundoNombre() != null && !dto.getSegundoNombre().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("segundoNombre")), "%" + dto.getSegundoNombre().toLowerCase() + "%"));
            }
            if (dto.getApellidoPaterno() != null && !dto.getApellidoPaterno().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("apellidoPaterno")), "%" + dto.getApellidoPaterno().toLowerCase() + "%"));
            }
            if (dto.getApellidoMaterno() != null && !dto.getApellidoMaterno().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("apellidoMaterno")), "%" + dto.getApellidoMaterno().toLowerCase() + "%"));
            }
            if (dto.getCurp() != null && !dto.getCurp().isBlank()) {
                predicates.add(cb.like(cb.upper(root.get("curp")), dto.getCurp().toUpperCase() + "%"));
            }
            if (dto.getRfc() != null && !dto.getRfc().isBlank()) {
                predicates.add(cb.like(cb.upper(root.get("rfc")), dto.getRfc().toUpperCase() + "%"));
            }
            if (dto.getCorreo() != null && !dto.getCorreo().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("correoElectronico")), "%" + dto.getCorreo().toLowerCase() + "%"));
            }
            if (dto.getActivo() != null) {
                predicates.add(cb.equal(root.get("activo"), dto.getActivo()));
            }
            if (dto.getFechaInicio() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("fechaCreacion"), dto.getFechaInicio().atStartOfDay()));
            }
            if (dto.getFechaFin() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("fechaCreacion"), dto.getFechaFin().atTime(23, 59, 59)));
            }
            if (dto.getNumeroCuenta() != null && !dto.getNumeroCuenta().isBlank()) {
                jakarta.persistence.criteria.Join<Cliente, Cuenta> cuentaJoin = root.join("cuentas");
                predicates.add(cb.equal(cuentaJoin.get("numeroCuenta"), dto.getNumeroCuenta()));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        // Paginación flexible (valores por defecto 0 y 10)
        int page = (dto.getPage() != null && dto.getPage() >= 0) ? dto.getPage() : 0;
        int size = (dto.getSize() != null && dto.getSize() > 0) ? dto.getSize() : 10;
        Pageable pageable = PageRequest.of(page, size);

        Page<Cliente> paginaResultados = clienteRepository.findAll(spec, pageable);

        if (paginaResultados.isEmpty()) {
            throw new ResourceNotFoundException("No se encontraron clientes con los criterios de búsqueda proporcionados.");
        }

        List<ClienteResponseDto> contenido = paginaResultados.getContent().stream()
                .map(this::mapearAResponse)
                .collect(Collectors.toList());

        return PaginatedResponse.<ClienteResponseDto>builder()
                .contenido(contenido)
                .paginaActual(paginaResultados.getNumber())
                .tamanoPagina(paginaResultados.getSize())
                .totalElementos(paginaResultados.getTotalElements())
                .totalPaginas(paginaResultados.getTotalPages())
                .esPrimera(paginaResultados.isFirst())
                .esUltima(paginaResultados.isLast())
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ACTUALIZACIÓN PARCIAL (Vía @RequestBody)
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public ClienteResponseDto actualizar(ClienteActualizacionDto dto) {
        Cliente cliente = clienteRepository.findById(dto.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró un cliente con ID: " + dto.getId()));

        if (Boolean.FALSE.equals(cliente.getActivo())) {
            throw new BusinessRuleException("No se puede modificar un cliente inactivo.");
        }

        // Modificar solo campos permitidos que no sean nulos
        if (dto.getNombre() != null)            cliente.setNombre(dto.getNombre());
        if (dto.getSegundoNombre() != null)     cliente.setSegundoNombre(dto.getSegundoNombre());
        if (dto.getApellidoPaterno() != null)   cliente.setApellidoPaterno(dto.getApellidoPaterno());
        if (dto.getApellidoMaterno() != null)   cliente.setApellidoMaterno(dto.getApellidoMaterno());
        if (dto.getCorreoElectronico() != null) cliente.setCorreoElectronico(dto.getCorreoElectronico());
        if (dto.getTelefonoMovil() != null)     cliente.setTelefonoMovil(dto.getTelefonoMovil());
        if (dto.getTelefonoAlternativo() != null) cliente.setTelefonoAlternativo(dto.getTelefonoAlternativo());
        if (dto.getOcupacion() != null)         cliente.setOcupacion(dto.getOcupacion());
        if (dto.getEmpresa() != null)           cliente.setEmpresa(dto.getEmpresa());
        if (dto.getIngresoMensual() != null)    cliente.setIngresoMensual(dto.getIngresoMensual());

        // Actualizar domicilio si viene
        if (cliente.getDomicilio() != null) {
            Domicilio dom = cliente.getDomicilio();
            if (dto.getCalle() != null)          dom.setCalle(dto.getCalle());
            if (dto.getNumeroExterior() != null) dom.setNumeroExterior(dto.getNumeroExterior());
            if (dto.getNumeroInterior() != null) dom.setNumeroInterior(dto.getNumeroInterior());
            if (dto.getColonia() != null)        dom.setColonia(dto.getColonia());
            if (dto.getMunicipio() != null)      dom.setMunicipio(dto.getMunicipio());
            if (dto.getEstado() != null)         dom.setEstado(dto.getEstado());
            if (dto.getCodigoPostal() != null)   dom.setCodigoPostal(dto.getCodigoPostal());
        }

        return mapearAResponse(clienteRepository.save(cliente));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BAJA LÓGICA (Vía @RequestBody)
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public void desactivar(ClienteBajaRequestDto dto) {
        Cliente cliente = clienteRepository.findById(dto.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró un cliente con ID: " + dto.getId()));

        if (Boolean.FALSE.equals(cliente.getActivo())) {
            throw new BusinessRuleException("El cliente ya se encuentra inactivo.");
        }

        cliente.setActivo(false);
        if (cliente.getUsuario() != null) {
            cliente.getUsuario().setActivo(false);
        }
        if (cliente.getCuentas() != null) {
            cliente.getCuentas().forEach(c -> c.setEstatus(EstatusCuenta.INACTIVA));
        }

        clienteRepository.save(cliente);
    }

    @Override
    @Transactional
    public void reactivar(ReactivacionDto dto) {
        Cliente cliente = clienteRepository.findByCorreoElectronico(dto.getCorreo())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró un cliente con el correo proporcionado."));

        if (Boolean.TRUE.equals(cliente.getActivo())) {
            throw new BusinessRuleException("El cliente ya se encuentra activo.");
        }

        if (cliente.getUsuario() == null || !passwordEncoder.matches(dto.getPassword(), cliente.getUsuario().getPassword())) {
            throw new com.proyecto.servicios.exception.InvalidCredentialsException("La contraseña es incorrecta.");
        }

        if (!cliente.getCurp().equalsIgnoreCase(dto.getCurp()) ||
            !cliente.getRfc().equalsIgnoreCase(dto.getRfc()) ||
            !cliente.getFechaNacimiento().equals(dto.getFechaNacimiento())) {
            throw new BusinessRuleException("Los datos de seguridad (CURP, RFC o Fecha de Nacimiento) no coinciden.");
        }

        cliente.setActivo(true);
        if (cliente.getUsuario() != null) {
            cliente.getUsuario().setActivo(true);
        }
        if (cliente.getCuentas() != null) {
            cliente.getCuentas().forEach(c -> c.setEstatus(EstatusCuenta.ACTIVA));
        }

        clienteRepository.save(cliente);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────
    private ClienteResponseDto mapearAResponse(Cliente c) {
        ClienteResponseDto.ClienteResponseDtoBuilder builder = ClienteResponseDto.builder()
                .id(c.getId())
                .nombre(c.getNombre())
                .segundoNombre(c.getSegundoNombre())
                .apellidoPaterno(c.getApellidoPaterno())
                .apellidoMaterno(c.getApellidoMaterno())
                .fechaNacimiento(c.getFechaNacimiento())
                .curp(c.getCurp())
                .rfc(c.getRfc())
                .sexo(c.getSexo())
                .nacionalidad(c.getNacionalidad())
                .estadoCivil(c.getEstadoCivil())
                .correoElectronico(c.getCorreoElectronico())
                .telefonoMovil(c.getTelefonoMovil())
                .telefonoAlternativo(c.getTelefonoAlternativo())
                .ocupacion(c.getOcupacion())
                .empresa(c.getEmpresa())
                .ingresoMensual(c.getIngresoMensual())
                .activo(c.getActivo());

        if (c.getDomicilio() != null) {
            Domicilio d = c.getDomicilio();
            builder.calle(d.getCalle())
                   .numeroExterior(d.getNumeroExterior())
                   .numeroInterior(d.getNumeroInterior())
                   .colonia(d.getColonia())
                   .municipio(d.getMunicipio())
                   .estado(d.getEstado())
                   .codigoPostal(d.getCodigoPostal());
        }

        if (c.getCuentas() != null) {
            builder.cuentas(c.getCuentas().stream().map(cu ->
                    CuentaResponseDto.builder()
                            .id(cu.getId())
                            .numeroCuenta(cu.getNumeroCuenta())
                            .saldo(cu.getSaldo())
                            .estatus(cu.getEstatus())
                            .clienteId(c.getId())
                            .nombreCliente(c.getNombre() + " " + c.getApellidoPaterno())
                            .build()
            ).collect(Collectors.toList()));
        }

        return builder.build();
    }

    private String generarNumeroCuentaUnico() {
        String cuentaStr = String.format("%010d", (long) (Math.random() * 10000000000L));
        while (cuentaRepository.findByNumeroCuenta(cuentaStr).isPresent()) {
            cuentaStr = String.format("%010d", (long) (Math.random() * 10000000000L));
        }
        return cuentaStr;
    }
}
