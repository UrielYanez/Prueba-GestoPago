package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.config.JwtUtil;
import com.proyecto.servicios.entity.postgres.Cliente;
import com.proyecto.servicios.entity.postgres.Cuenta;
import com.proyecto.servicios.entity.postgres.EstatusCuenta;
import com.proyecto.servicios.entity.postgres.Usuario;
import com.proyecto.servicios.exception.ActiveSessionConflictException;
import com.proyecto.servicios.exception.BusinessRuleException;
import com.proyecto.servicios.exception.InvalidCredentialsException;
import com.proyecto.servicios.exception.ResourceNotFoundException;
import com.proyecto.servicios.model.LoginRequestDto;
import com.proyecto.servicios.model.LoginResponseDto;
import com.proyecto.servicios.model.Recuperar2faRequestDto;
import com.proyecto.servicios.model.Verificar2faRequestDto;
import com.proyecto.servicios.repositorys.postgres.CuentaRepository;
import com.proyecto.servicios.repositorys.postgres.UsuarioRepository;
import com.proyecto.servicios.service.AuthService;
import com.proyecto.servicios.service.EmailService;
import com.proyecto.servicios.util.TotpUtil;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private static final ZoneId ZONA_MX = ZoneId.of("America/Mexico_City");
    private static final int MAX_INTENTOS_FALLIDOS = 5;
    private static final int MINUTOS_BLOQUEO = 5;
    private static final int SEGUNDOS_COOLDOWN_2FA = 60;

    private final UsuarioRepository usuarioRepository;
    private final CuentaRepository cuentaRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TotpUtil totpUtil;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthServiceImpl(
            UsuarioRepository usuarioRepository,
            CuentaRepository cuentaRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            TotpUtil totpUtil,
            EmailService emailService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.cuentaRepository = cuentaRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.totpUtil = totpUtil;
        this.emailService = emailService;
    }

    @Override
    @Transactional(noRollbackFor = {InvalidCredentialsException.class, BusinessRuleException.class})
    public LoginResponseDto login(LoginRequestDto request) {
        Usuario usuario = usuarioRepository.findByCorreo(request.getCorreo().trim())
                .orElseThrow(() -> new InvalidCredentialsException("Credenciales incorrectas"));

        validarCuentaActivaYSinBloqueo(usuario);

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            registrarIntentoFallido(usuario);
            throw new InvalidCredentialsException("Credenciales incorrectas");
        }

        asegurarSecretoTotp(usuario);
        usuario.setIntentosFallidosAuth(0);
        usuario.setBloqueadoHasta(null);
        usuarioRepository.save(usuario);

        String plataforma = normalizarPlataforma(request.getPlataforma());
        String challengeToken = jwtUtil.generarChallengeToken(usuario.getCorreo(), plataforma);
        boolean totpConfigurado = Boolean.TRUE.equals(usuario.getTotpHabilitado());
        String otpAuthUri = totpUtil.construirOtpAuthUri(usuario.getCorreo(), usuario.getTotpSecret());

        return LoginResponseDto.builder()
                .correo(usuario.getCorreo())
                .plataforma(plataforma)
                .requiere2fa(true)
                .challengeToken(challengeToken)
                .totpConfigurado(totpConfigurado)
                .otpAuthUri(otpAuthUri)
                .secretManual(usuario.getTotpSecret())
                .build();
    }

    @Override
    @Transactional(noRollbackFor = {InvalidCredentialsException.class, BusinessRuleException.class, ActiveSessionConflictException.class})
    public LoginResponseDto verificar2fa(Verificar2faRequestDto request) {
        validarChallengeToken(request.getChallengeToken());

        String correo = jwtUtil.extraerCorreo(request.getChallengeToken());
        String plataforma = normalizarPlataforma(jwtUtil.extraerPlataforma(request.getChallengeToken()));

        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new InvalidCredentialsException("Usuario no encontrado para el reto 2FA"));

        validarCuentaActivaYSinBloqueo(usuario);
        String codigo = request.getCodigo2fa().trim();

        if (codigo.equals(usuario.getUltimoCodigo2faUsado())) {
            throw new BusinessRuleException("Este código 2FA ya fue utilizado recientemente. Espera al siguiente código temporal.");
        }

        if (!esCodigoSegundoFactorValido(usuario, codigo)) {
            registrarIntentoFallido(usuario);
            int restantes = Math.max(0, MAX_INTENTOS_FALLIDOS - usuario.getIntentosFallidosAuth());
            throw new InvalidCredentialsException("Código de verificación 2FA inválido o expirado. Intentos restantes: " + restantes);
        }

        LocalDateTime ahora = LocalDateTime.now(ZONA_MX);
        if (tieneSesionActivaVigente(usuario, ahora) && !Boolean.TRUE.equals(request.getForzarCierreSesion())) {
            String platActiva = usuario.getPlataformaActiva() != null ? usuario.getPlataformaActiva() : "OTRO_DISPOSITIVO";
            throw new ActiveSessionConflictException(
                    "Ya existe una sesión activa iniciada en " + platActiva + ". ¿Deseas finalizar la sesión anterior para continuar?",
                    platActiva,
                    usuario.getFechaInicioSesion()
            );
        }

        String nuevoSessionId = UUID.randomUUID().toString();
        long expiraEnSegundos = jwtUtil.getExpirationSeconds();

        usuario.setSessionIdActivo(nuevoSessionId);
        usuario.setPlataformaActiva(plataforma);
        usuario.setFechaInicioSesion(ahora);
        usuario.setFechaExpiracionSesion(ahora.plusSeconds(expiraEnSegundos));
        usuario.setTotpHabilitado(true);
        usuario.setUltimoCodigo2faUsado(codigo);
        usuario.setCodigoRecuperacionHash(null);
        usuario.setExpiracionCodigoRecuperacion(null);
        usuario.setIntentosFallidosAuth(0);
        usuario.setBloqueadoHasta(null);
        usuarioRepository.save(usuario);

        String accessToken = jwtUtil.generarTokenSesion(usuario.getCorreo(), nuevoSessionId, plataforma);
        return construirRespuestaSesionActiva(usuario, accessToken, nuevoSessionId, plataforma, expiraEnSegundos);
    }

    @Override
    @Transactional
    public LoginResponseDto recuperar2fa(Recuperar2faRequestDto request) {
        validarChallengeToken(request.getChallengeToken());
        String correo = jwtUtil.extraerCorreo(request.getChallengeToken());

        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        validarCuentaActivaYSinBloqueo(usuario);
        LocalDateTime ahora = LocalDateTime.now(ZONA_MX);

        if (usuario.getUltimoEnvio2fa() != null
                && usuario.getUltimoEnvio2fa().plusSeconds(SEGUNDOS_COOLDOWN_2FA).isAfter(ahora)) {
            throw new BusinessRuleException("Por favor espera al menos 60 segundos antes de solicitar otro código de recuperación.");
        }

        asegurarSecretoTotp(usuario);
        int codigoNum = 100_000 + secureRandom.nextInt(900_000);
        String codigoTexto = String.valueOf(codigoNum);

        usuario.setCodigoRecuperacionHash(passwordEncoder.encode(codigoTexto));
        usuario.setExpiracionCodigoRecuperacion(ahora.plusMinutes(5));
        usuario.setUltimoEnvio2fa(ahora);
        usuarioRepository.save(usuario);

        String nombre = obtenerNombreCliente(usuario.getCliente());
        emailService.enviarPinTransaccion(
                usuario.getCorreo(),
                nombre,
                codigoTexto,
                BigDecimal.ZERO,
                "Recuperación de Acceso 2FA (Inicio de Sesión)"
        );

        boolean mostrarQr = Boolean.TRUE.equals(request.getMostrarQr());
        String otpAuthUri = mostrarQr ? totpUtil.construirOtpAuthUri(usuario.getCorreo(), usuario.getTotpSecret()) : null;

        return LoginResponseDto.builder()
                .correo(usuario.getCorreo())
                .requiere2fa(true)
                .challengeToken(request.getChallengeToken())
                .totpConfigurado(usuario.getTotpHabilitado())
                .otpAuthUri(otpAuthUri)
                .secretManual(mostrarQr ? usuario.getTotpSecret() : null)
                .build();
    }

    @Override
    @Transactional
    public void logout() {
        Usuario usuario = obtenerUsuarioAutenticado();
        usuario.setSessionIdActivo(null);
        usuario.setPlataformaActiva(null);
        usuario.setFechaExpiracionSesion(null);
        usuarioRepository.save(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponseDto obtenerEstadoSesionActual() {
        Usuario usuario = obtenerUsuarioAutenticado();
        return construirRespuestaSesionActiva(
                usuario,
                null,
                usuario.getSessionIdActivo(),
                usuario.getPlataformaActiva(),
                jwtUtil.getExpirationSeconds()
        );
    }

    private void validarChallengeToken(String challengeToken) {
        try {
            if (!jwtUtil.validarChallengeToken(challengeToken)) {
                throw new InvalidCredentialsException("El token de verificación 2FA es inválido o ha expirado. Inicia sesión nuevamente.");
            }
        } catch (Exception e) {
            throw new InvalidCredentialsException("El token de verificación 2FA es inválido o ha expirado. Inicia sesión nuevamente.");
        }
    }

    private void validarCuentaActivaYSinBloqueo(Usuario usuario) {
        if (Boolean.FALSE.equals(usuario.getActivo())) {
            throw new BusinessRuleException("El usuario está desactivado. Contacte a su ejecutivo.");
        }
        LocalDateTime ahora = LocalDateTime.now(ZONA_MX);
        if (usuario.getBloqueadoHasta() != null && usuario.getBloqueadoHasta().isAfter(ahora)) {
            throw new BusinessRuleException("Demasiados intentos fallidos. Tu cuenta está bloqueada temporalmente por seguridad; intenta de nuevo en unos minutos.");
        }
    }

    private void registrarIntentoFallido(Usuario usuario) {
        int intentos = (usuario.getIntentosFallidosAuth() != null ? usuario.getIntentosFallidosAuth() : 0) + 1;
        usuario.setIntentosFallidosAuth(intentos);
        if (intentos >= MAX_INTENTOS_FALLIDOS) {
            usuario.setBloqueadoHasta(LocalDateTime.now(ZONA_MX).plusMinutes(MINUTOS_BLOQUEO));
        }
        usuarioRepository.save(usuario);
    }

    private void asegurarSecretoTotp(Usuario usuario) {
        if (usuario.getTotpSecret() == null || usuario.getTotpSecret().isBlank()) {
            usuario.setTotpSecret(totpUtil.generarSecretoBase32());
            usuario.setTotpHabilitado(false);
        }
    }

    private boolean esCodigoSegundoFactorValido(Usuario usuario, String codigo) {
        if (totpUtil.verificarCodigo(usuario.getTotpSecret(), codigo)) {
            return true;
        }
        LocalDateTime ahora = LocalDateTime.now(ZONA_MX);
        return usuario.getCodigoRecuperacionHash() != null
                && usuario.getExpiracionCodigoRecuperacion() != null
                && usuario.getExpiracionCodigoRecuperacion().isAfter(ahora)
                && passwordEncoder.matches(codigo, usuario.getCodigoRecuperacionHash());
    }

    private boolean tieneSesionActivaVigente(Usuario usuario, LocalDateTime ahora) {
        return usuario.getSessionIdActivo() != null
                && !usuario.getSessionIdActivo().isBlank()
                && (usuario.getFechaExpiracionSesion() == null || usuario.getFechaExpiracionSesion().isAfter(ahora));
    }

    private String normalizarPlataforma(String plataforma) {
        if (plataforma == null || plataforma.isBlank()) {
            return "WEB";
        }
        String upper = plataforma.trim().toUpperCase();
        return ("MOVIL".equals(upper) || "MOBILE".equals(upper)) ? "MOVIL" : "WEB";
    }

    private Usuario obtenerUsuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || "anonymousUser".equals(auth.getPrincipal())) {
            throw new InvalidCredentialsException("No existe una sesión autenticada activa.");
        }
        return usuarioRepository.findByCorreo(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado."));
    }

    private LoginResponseDto construirRespuestaSesionActiva(
            Usuario usuario,
            String token,
            String sessionId,
            String plataforma,
            long expiraEn
    ) {
        Cliente cliente = usuario.getCliente();
        Long clienteId = cliente != null ? cliente.getId() : null;
        String nombreCliente = obtenerNombreCliente(cliente);
        String numeroCuenta = null;
        BigDecimal saldo = null;

        if (clienteId != null && cuentaRepository != null) {
            List<Cuenta> cuentas = cuentaRepository.findByClienteId(clienteId);
            Cuenta principal = cuentas.stream()
                    .filter(c -> c.getEstatus() == EstatusCuenta.ACTIVA)
                    .findFirst()
                    .orElse(cuentas.isEmpty() ? null : cuentas.get(0));
            if (principal != null) {
                numeroCuenta = principal.getNumeroCuenta();
                saldo = principal.getSaldo();
            }
        }

        return LoginResponseDto.builder()
                .token(token)
                .tipo(token != null ? "Bearer" : null)
                .correo(usuario.getCorreo())
                .expiraEn(expiraEn)
                .sessionId(sessionId)
                .plataforma(plataforma)
                .clienteId(clienteId)
                .nombreCliente(nombreCliente)
                .numeroCuentaPrincipal(numeroCuenta)
                .saldoDisponible(saldo)
                .requiere2fa(false)
                .totpConfigurado(usuario.getTotpHabilitado())
                .build();
    }

    private String obtenerNombreCliente(Cliente cliente) {
        if (cliente == null) {
            return "Cliente";
        }
        String apellido = cliente.getApellidoPaterno() != null ? " " + cliente.getApellidoPaterno() : "";
        return cliente.getNombre() + apellido;
    }
}
