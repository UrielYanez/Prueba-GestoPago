package com.proyecto.servicios.service;

import com.proyecto.servicios.config.JwtUtil;
import com.proyecto.servicios.entity.postgres.Cliente;
import com.proyecto.servicios.entity.postgres.Cuenta;
import com.proyecto.servicios.entity.postgres.EstatusCuenta;
import com.proyecto.servicios.entity.postgres.Usuario;
import com.proyecto.servicios.exception.ActiveSessionConflictException;
import com.proyecto.servicios.exception.BusinessRuleException;
import com.proyecto.servicios.model.LoginRequestDto;
import com.proyecto.servicios.model.LoginResponseDto;
import com.proyecto.servicios.model.Verificar2faRequestDto;
import com.proyecto.servicios.repositorys.postgres.CuentaRepository;
import com.proyecto.servicios.repositorys.postgres.UsuarioRepository;
import com.proyecto.servicios.service.Impl.AuthServiceImpl;
import com.proyecto.servicios.util.TotpUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TotpAndAuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private EmailService emailService;

    private TotpUtil totpUtil;
    private JwtUtil jwtUtil;
    private PasswordEncoder passwordEncoder;
    private AuthServiceImpl authService;
    private Usuario usuarioMock;

    @BeforeEach
    void setUp() throws Exception {
        totpUtil = new TotpUtil();
        jwtUtil = new JwtUtil();
        passwordEncoder = new BCryptPasswordEncoder();

        Field secretField = JwtUtil.class.getDeclaredField("secretKey");
        secretField.setAccessible(true);
        secretField.set(jwtUtil, "EstaEsUnaClaveSecretaMuyLargaYSeguraParaFirmarTokensJWTEnElProyectoServicios2026");

        Field expField = JwtUtil.class.getDeclaredField("expirationMs");
        expField.setAccessible(true);
        expField.set(jwtUtil, 3600000L);

        Field challengeExpField = JwtUtil.class.getDeclaredField("challengeExpirationMs");
        challengeExpField.setAccessible(true);
        challengeExpField.set(jwtUtil, 180000L);

        Field kidField = JwtUtil.class.getDeclaredField("keyId");
        kidField.setAccessible(true);
        kidField.set(jwtUtil, "gestopago-jwt-key-2026-v1");

        authService = new AuthServiceImpl(
                usuarioRepository,
                cuentaRepository,
                passwordEncoder,
                jwtUtil,
                totpUtil,
                emailService
        );

        Cliente cliente = Cliente.builder()
                .id(1L)
                .nombre("Uriel")
                .apellidoPaterno("Aguayo")
                .correoElectronico("aguayomar77@gmail.com")
                .activo(true)
                .build();

        usuarioMock = Usuario.builder()
                .id(10L)
                .correo("aguayomar77@gmail.com")
                .password(passwordEncoder.encode("Secret123!"))
                .activo(true)
                .cliente(cliente)
                .totpSecret(totpUtil.generarSecretoBase32())
                .totpHabilitado(true)
                .intentosFallidosAuth(0)
                .build();
    }

    @Test
    void testTotpGenerationAndVerification() {
        String secret = totpUtil.generarSecretoBase32();
        assertNotNull(secret);
        assertTrue(secret.length() >= 32);

        String code = totpUtil.generarCodigoActual(secret);
        assertEquals(6, code.length());
        assertTrue(totpUtil.verificarCodigo(secret, code));
        assertFalse(totpUtil.verificarCodigo(secret, "000000".equals(code) ? "999999" : "000000"));
    }

    @Test
    void testLoginPaso1GeneraChallenge2fa() {
        when(usuarioRepository.findByCorreo("aguayomar77@gmail.com")).thenReturn(Optional.of(usuarioMock));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        LoginRequestDto req = new LoginRequestDto();
        req.setCorreo("aguayomar77@gmail.com");
        req.setPassword("Secret123!");
        req.setPlataforma("MOVIL");

        LoginResponseDto resp = authService.login(req);

        assertTrue(resp.getRequiere2fa());
        assertNotNull(resp.getChallengeToken());
        assertNotNull(resp.getOtpAuthUri());
        assertNotNull(resp.getSecretManual());
        assertNull(resp.getToken());
        assertEquals("2FA_CHALLENGE", jwtUtil.extraerProposito(resp.getChallengeToken()));
    }

    @Test
    void testVerificar2faExitosoYBloqueoDeSesionesSimultaneasCon409() {
        when(usuarioRepository.findByCorreo("aguayomar77@gmail.com")).thenReturn(Optional.of(usuarioMock));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(cuentaRepository.findByClienteId(1L)).thenReturn(List.of(
                Cuenta.builder().id(1L).numeroCuenta("0000000001").saldo(new BigDecimal("500.00")).estatus(EstatusCuenta.ACTIVA).build()
        ));

        String challengeMovil = jwtUtil.generarChallengeToken("aguayomar77@gmail.com", "MOVIL");
        String codigoTotp = totpUtil.generarCodigoActual(usuarioMock.getTotpSecret());

        Verificar2faRequestDto reqMovil = Verificar2faRequestDto.builder()
                .challengeToken(challengeMovil)
                .codigo2fa(codigoTotp)
                .forzarCierreSesion(false)
                .build();

        LoginResponseDto sesionMovil = authService.verificar2fa(reqMovil);
        assertNotNull(sesionMovil.getToken());
        assertEquals("MOVIL", sesionMovil.getPlataforma());
        assertEquals("0000000001", sesionMovil.getNumeroCuentaPrincipal());
        assertEquals(sesionMovil.getSessionId(), usuarioMock.getSessionIdActivo());

        Verificar2faRequestDto intentoReuso = Verificar2faRequestDto.builder()
                .challengeToken(challengeMovil)
                .codigo2fa(codigoTotp)
                .forzarCierreSesion(true)
                .build();
        assertThrows(BusinessRuleException.class, () -> authService.verificar2fa(intentoReuso));

        usuarioMock.setUltimoCodigo2faUsado("000000");
        String challengeWeb = jwtUtil.generarChallengeToken("aguayomar77@gmail.com", "WEB");

        Verificar2faRequestDto reqWebSinForzar = Verificar2faRequestDto.builder()
                .challengeToken(challengeWeb)
                .codigo2fa(codigoTotp)
                .forzarCierreSesion(false)
                .build();

        assertThrows(ActiveSessionConflictException.class, () -> authService.verificar2fa(reqWebSinForzar));

        Verificar2faRequestDto reqWebForzando = Verificar2faRequestDto.builder()
                .challengeToken(challengeWeb)
                .codigo2fa(codigoTotp)
                .forzarCierreSesion(true)
                .build();

        LoginResponseDto sesionWeb = authService.verificar2fa(reqWebForzando);
        assertNotNull(sesionWeb.getToken());
        assertEquals("WEB", sesionWeb.getPlataforma());
        assertNotEquals(sesionMovil.getSessionId(), sesionWeb.getSessionId());
        assertEquals(sesionWeb.getSessionId(), usuarioMock.getSessionIdActivo());
    }
}
