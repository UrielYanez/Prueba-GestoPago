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
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
public class ClienteServiceImpl implements ClienteService {

    private static final ZoneId ZONA_MX = ZoneId.of("America/Mexico_City");

    private final ClienteRepository clienteRepository;
    private final CuentaRepository cuentaRepository;
    private final UsuarioRepository usuarioRepository;
    private final CopomexClient copomexClient;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

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
        validarRequisitosRegistro(dto);
        autocompletarDireccionConCopomex(dto);

        // Mapear Usuario (con contraseña encriptada)
        Usuario usuario = Usuario.builder()
                .correo(dto.getCorreoElectronico())
                .password(passwordEncoder.encode(dto.getPassword()))
                .activo(true)
                .build();

        // Mapear Domicilio
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

        // Mapear Cliente
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

        // Crear Cuenta Bancaria con número único de 10 dígitos
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

    private void validarRequisitosRegistro(ClienteRegistroDto dto) {
        int edad = Period.between(dto.getFechaNacimiento(), LocalDate.now(ZONA_MX)).getYears();
        if (edad < 18) {
            throw new BusinessRuleException("El cliente debe ser mayor de edad (18 años o más).");
        }
        if (clienteRepository.findByCurp(dto.getCurp()).isPresent()) {
            throw new DuplicatedResourceException("Ya existe un cliente registrado con la CURP: " + dto.getCurp());
        }
        if (clienteRepository.findByRfc(dto.getRfc()).isPresent()) {
            throw new DuplicatedResourceException("Ya existe un cliente registrado con el RFC: " + dto.getRfc());
        }
        if (usuarioRepository.findByCorreo(dto.getCorreoElectronico()).isPresent()) {
            throw new DuplicatedResourceException("Ya existe un usuario registrado con el correo: " + dto.getCorreoElectronico());
        }
    }

    private void autocompletarDireccionConCopomex(ClienteRegistroDto dto) {
        if (!requiereAutocompletarDireccion(dto)) {
            return;
        }
        try {
            List<CopomexResponseDto> copomexData = copomexClient.getInfoByCp(dto.getCodigoPostal(), copomexToken);
            if (copomexData != null && !copomexData.isEmpty()) {
                aplicarDatosCopomexSiValido(dto, copomexData.get(0));
            }
        } catch (Exception ignored) {
            // Si la API externa no responde, no interrumpimos el registro
        }
    }

    private boolean requiereAutocompletarDireccion(ClienteRegistroDto dto) {
        return esTextoVacio(dto.getColonia()) || esTextoVacio(dto.getMunicipio()) || esTextoVacio(dto.getEstado());
    }

    private void aplicarDatosCopomexSiValido(ClienteRegistroDto dto, CopomexResponseDto match) {
        if (match.isError() || match.getResponse() == null) {
            return;
        }
        CopomexInfoResponse info = match.getResponse();
        if (esTextoVacio(dto.getColonia())) {
            dto.setColonia(info.getAsentamiento());
        }
        if (esTextoVacio(dto.getMunicipio())) {
            dto.setMunicipio(info.getMunicipio());
        }
        if (esTextoVacio(dto.getEstado())) {
            dto.setEstado(info.getEstado());
        }
    }

    private boolean esTextoVacio(String valor) {
        return valor == null || valor.isBlank();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ÚNICO ENDPOINT DE BÚSQUEDA Y CONSULTA DINÁMICA (Paginada vía @RequestBody)
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<ClienteResponseDto> buscar(ClienteBusquedaRequestDto dto) {
        Specification<Cliente> spec = (root, query, cb) -> construirPredicadosBusqueda(dto, root, cb);

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

    private Predicate construirPredicadosBusqueda(ClienteBusquedaRequestDto dto, Root<Cliente> root, CriteriaBuilder cb) {
        List<Predicate> predicates = new ArrayList<>();
        agregarFiltroIgual(predicates, cb, root, "id", dto.getId());
        agregarFiltroIgual(predicates, cb, root, "activo", dto.getActivo());
        agregarFiltroContieneLower(predicates, cb, root, "nombre", dto.getNombre());
        agregarFiltroContieneLower(predicates, cb, root, "segundoNombre", dto.getSegundoNombre());
        agregarFiltroContieneLower(predicates, cb, root, "apellidoPaterno", dto.getApellidoPaterno());
        agregarFiltroContieneLower(predicates, cb, root, "apellidoMaterno", dto.getApellidoMaterno());
        agregarFiltroContieneLower(predicates, cb, root, "correoElectronico", dto.getCorreo());
        agregarFiltroPrefijoUpper(predicates, cb, root, "curp", dto.getCurp());
        agregarFiltroPrefijoUpper(predicates, cb, root, "rfc", dto.getRfc());
        agregarFiltroRangoFechas(predicates, cb, root, dto);
        agregarFiltroNumeroCuenta(predicates, cb, root, dto.getNumeroCuenta());
        return cb.and(predicates.toArray(new Predicate[0]));
    }

    private void agregarFiltroIgual(List<Predicate> predicates, CriteriaBuilder cb, Root<Cliente> root, String campo, Object valor) {
        if (valor != null) {
            predicates.add(cb.equal(root.get(campo), valor));
        }
    }

    private void agregarFiltroContieneLower(List<Predicate> predicates, CriteriaBuilder cb, Root<Cliente> root, String campo, String valor) {
        if (!esTextoVacio(valor)) {
            predicates.add(cb.like(cb.lower(root.get(campo)), "%" + valor.toLowerCase() + "%"));
        }
    }

    private void agregarFiltroPrefijoUpper(List<Predicate> predicates, CriteriaBuilder cb, Root<Cliente> root, String campo, String valor) {
        if (!esTextoVacio(valor)) {
            predicates.add(cb.like(cb.upper(root.get(campo)), valor.toUpperCase() + "%"));
        }
    }

    private void agregarFiltroRangoFechas(List<Predicate> predicates, CriteriaBuilder cb, Root<Cliente> root, ClienteBusquedaRequestDto dto) {
        if (dto.getFechaInicio() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("fechaCreacion"), dto.getFechaInicio().atStartOfDay()));
        }
        if (dto.getFechaFin() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("fechaCreacion"), dto.getFechaFin().atTime(23, 59, 59)));
        }
    }

    private void agregarFiltroNumeroCuenta(List<Predicate> predicates, CriteriaBuilder cb, Root<Cliente> root, String numeroCuenta) {
        if (!esTextoVacio(numeroCuenta)) {
            Join<Cliente, Cuenta> cuentaJoin = root.join("cuentas");
            predicates.add(cb.equal(cuentaJoin.get("numeroCuenta"), numeroCuenta));
        }
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

        actualizarDatosPersonales(cliente, dto);
        actualizarDatosDomicilio(cliente.getDomicilio(), dto);

        return mapearAResponse(clienteRepository.save(cliente));
    }

    private void actualizarDatosPersonales(Cliente cliente, ClienteActualizacionDto dto) {
        asignarSiNoNulo(dto.getNombre(), cliente::setNombre);
        asignarSiNoNulo(dto.getSegundoNombre(), cliente::setSegundoNombre);
        asignarSiNoNulo(dto.getApellidoPaterno(), cliente::setApellidoPaterno);
        asignarSiNoNulo(dto.getApellidoMaterno(), cliente::setApellidoMaterno);
        asignarSiNoNulo(dto.getCorreoElectronico(), cliente::setCorreoElectronico);
        asignarSiNoNulo(dto.getTelefonoMovil(), cliente::setTelefonoMovil);
        asignarSiNoNulo(dto.getTelefonoAlternativo(), cliente::setTelefonoAlternativo);
        asignarSiNoNulo(dto.getOcupacion(), cliente::setOcupacion);
        asignarSiNoNulo(dto.getEmpresa(), cliente::setEmpresa);
        asignarSiNoNulo(dto.getIngresoMensual(), cliente::setIngresoMensual);
    }

    private void actualizarDatosDomicilio(Domicilio dom, ClienteActualizacionDto dto) {
        if (dom == null) {
            return;
        }
        asignarSiNoNulo(dto.getCalle(), dom::setCalle);
        asignarSiNoNulo(dto.getNumeroExterior(), dom::setNumeroExterior);
        asignarSiNoNulo(dto.getNumeroInterior(), dom::setNumeroInterior);
        asignarSiNoNulo(dto.getColonia(), dom::setColonia);
        asignarSiNoNulo(dto.getMunicipio(), dom::setMunicipio);
        asignarSiNoNulo(dto.getEstado(), dom::setEstado);
        asignarSiNoNulo(dto.getCodigoPostal(), dom::setCodigoPostal);
    }

    private <T> void asignarSiNoNulo(T valor, Consumer<T> setter) {
        if (valor != null) {
            setter.accept(valor);
        }
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
        String cuentaStr = String.format("%010d", secureRandom.nextLong(10_000_000_000L));
        while (cuentaRepository.findByNumeroCuenta(cuentaStr).isPresent()) {
            cuentaStr = String.format("%010d", secureRandom.nextLong(10_000_000_000L));
        }
        return cuentaStr;
    }
}
