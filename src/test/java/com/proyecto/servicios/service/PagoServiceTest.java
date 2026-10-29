package com.proyecto.servicios.service;

import com.proyecto.servicios.client.GestoPagoTransactionClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.entity.mongo.CatProductMongo;
import com.proyecto.servicios.entity.postgres.*;
import com.proyecto.servicios.exception.BusinessRuleException;
import com.proyecto.servicios.exception.DuplicatedResourceException;
import com.proyecto.servicios.exception.ProviderTimeoutException;
import com.proyecto.servicios.exception.ResourceNotFoundException;
import com.proyecto.servicios.exception.UnauthorizedOperationException;
import com.proyecto.servicios.model.ComprobantePagoResponseDto;
import com.proyecto.servicios.model.ConciliarPagoRequestDto;
import com.proyecto.servicios.model.ProcesarPagoRequestDto;
import com.proyecto.servicios.model.SolicitudPinRequestDto;
import com.proyecto.servicios.model.gestopago.ProductStatusEnum;
import com.proyecto.servicios.repositorys.mongo.CatProductMongoRepository;
import com.proyecto.servicios.repositorys.postgres.ConfirmacionPinRepository;
import com.proyecto.servicios.repositorys.postgres.CuentaRepository;
import com.proyecto.servicios.repositorys.postgres.TransaccionPagoRepository;
import com.proyecto.servicios.repositorys.postgres.UsuarioRepository;
import com.proyecto.servicios.service.Impl.PagoServiceImpl;
import com.proyecto.servicios.util.AesEncryptionUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PagoServiceTest {

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private ConfirmacionPinRepository confirmacionPinRepository;

    @Mock
    private TransaccionPagoRepository transaccionPagoRepository;

    @Mock
    private CatProductMongoRepository catProductMongoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AesEncryptionUtil aesEncryptionUtil;

    @Mock
    private GestoPagoTransactionClient transactionClient;

    @Mock
    private GestoPagoTokenService gestoPagoTokenService;

    @InjectMocks
    private PagoServiceImpl pagoService;

    private Cuenta cuentaMock;
    private Cliente clienteMock;

    @BeforeEach
    void setUp() throws Exception {
        SecurityContextHolder.clearContext();

        clienteMock = Cliente.builder()
                .id(1L)
                .nombre("Uriel")
                .apellidoPaterno("Aguayo")
                .correoElectronico("aguayomar77@gmail.com")
                .activo(true)
                .build();

        cuentaMock = Cuenta.builder()
                .id(10L)
                .numeroCuenta("0000000001")
                .saldo(new BigDecimal("500.00"))
                .estatus(EstatusCuenta.ACTIVA)
                .cliente(clienteMock)
                .build();

        Field apiKeyField = PagoServiceImpl.class.getDeclaredField("apiKey");
        apiKeyField.setAccessible(true);
        apiKeyField.set(pagoService, "test-api-key");

        Field unidadField = PagoServiceImpl.class.getDeclaredField("defaultUnidad");
        unidadField.setAccessible(true);
        unidadField.set(pagoService, "083");

        Field distField = PagoServiceImpl.class.getDeclaredField("idDistribuidor");
        distField.setAccessible(true);
        distField.set(pagoService, 83);

        Field devField = PagoServiceImpl.class.getDeclaredField("codigoDispositivo");
        devField.setAccessible(true);
        devField.set(pagoService, "GPS83-TPV-17");

        Field usuarioRepoField = PagoServiceImpl.class.getDeclaredField("usuarioRepository");
        usuarioRepoField.setAccessible(true);
        usuarioRepoField.set(pagoService, usuarioRepository);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testSolicitarPinExitoso() {
        SolicitudPinRequestDto req = SolicitudPinRequestDto.builder()
                .numeroCuenta("0000000001")
                .monto(new BigDecimal("100.00"))
                .concepto("Recarga")
                .build();

        when(cuentaRepository.findByNumeroCuenta("0000000001")).thenReturn(Optional.of(cuentaMock));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-pin");

        assertDoesNotThrow(() -> pagoService.solicitarPin(req));

        verify(confirmacionPinRepository).invalidarPinesAnteriores(10L);
        verify(confirmacionPinRepository).save(any(ConfirmacionPin.class));
        verify(emailService).enviarPinTransaccion(eq(clienteMock.getCorreoElectronico()), contains(clienteMock.getNombre()), anyString(), eq(new BigDecimal("100.00")), eq("Recarga"));
    }

    @Test
    void testSolicitarPinCuentaInactiva() {
        cuentaMock.setEstatus(EstatusCuenta.INACTIVA);
        SolicitudPinRequestDto req = SolicitudPinRequestDto.builder()
                .numeroCuenta("0000000001")
                .monto(new BigDecimal("100.00"))
                .build();

        when(cuentaRepository.findByNumeroCuenta("0000000001")).thenReturn(Optional.of(cuentaMock));

        assertThrows(BusinessRuleException.class, () -> pagoService.solicitarPin(req));
        verify(confirmacionPinRepository, never()).save(any());
    }

    @Test
    void testSolicitarPinSaldoInsuficiente() {
        SolicitudPinRequestDto req = SolicitudPinRequestDto.builder()
                .numeroCuenta("0000000001")
                .monto(new BigDecimal("1000.00"))
                .build();

        when(cuentaRepository.findByNumeroCuenta("0000000001")).thenReturn(Optional.of(cuentaMock));

        assertThrows(BusinessRuleException.class, () -> pagoService.solicitarPin(req));
    }

    @Test
    void testProcesarPagoRecargaExitosa() {
        ProcesarPagoRequestDto req = ProcesarPagoRequestDto.builder()
                .numeroCuenta("0000000001")
                .idServicio("1")
                .idProducto("101")
                .precio(new BigDecimal("100.00"))
                .bandera(true)
                .telefono("5512345678")
                .referencia(null)
                .pin("123456")
                .build();

        ConfirmacionPin pinMock = ConfirmacionPin.builder()
                .id(5L)
                .cuenta(cuentaMock)
                .pinHash("hashed-pin")
                .intentos(0)
                .utilizado(false)
                .fechaExpiracion(LocalDateTime.now().plusMinutes(5))
                .build();

        GestoPagoToken tokenMock = new GestoPagoToken();
        tokenMock.setToken("jwt-gestopago-token");

        String xmlExito = """
                <RESPONSE>
                    <ID_TX>884920</ID_TX>
                    <NUM_AUTORIZACION>AUT-771122</NUM_AUTORIZACION>
                    <SALDO>14000.00</SALDO>
                    <MENSAJE>
                        <CODIGO>01</CODIGO>
                        <TEXTO>Operacion realizada con exito</TEXTO>
                    </MENSAJE>
                </RESPONSE>
                """;

        when(cuentaRepository.findByNumeroCuentaForUpdate("0000000001")).thenReturn(Optional.of(cuentaMock));
        when(confirmacionPinRepository.findPinActivo(eq("0000000001"), any())).thenReturn(Optional.of(pinMock));
        when(passwordEncoder.matches("123456", "hashed-pin")).thenReturn(true);
        when(aesEncryptionUtil.encryptPayload(any())).thenReturn("signed-encrypted-payload");
        when(gestoPagoTokenService.obtenerTokenActivo(83, "GPS83-TPV-17")).thenReturn(Optional.of(tokenMock));
        when(transactionClient.sendTx(anyString(), anyString(), anyString(), anyString())).thenReturn(xmlExito);
        when(transaccionPagoRepository.save(any(TransaccionPago.class))).thenAnswer(i -> {
            TransaccionPago tx = i.getArgument(0);
            tx.setId(99L);
            return tx;
        });

        ComprobantePagoResponseDto resp = pagoService.procesarPago(req);

        assertNotNull(resp);
        assertEquals("APROBADO", resp.getCodigoRespuesta());
        assertEquals("EXITOSA", resp.getEstadoTransaccion());
        assertEquals("AUT-771122", resp.getNumAutorizacion());
        assertEquals("884920", resp.getIdTxProveedor());
        assertEquals(new BigDecimal("400.00"), resp.getNuevoSaldoCuenta());
        assertEquals(new BigDecimal("400.00"), cuentaMock.getSaldo());
        assertEquals("5512345678", resp.getTelefono());
        assertTrue(resp.getBandera());
        assertTrue(pinMock.getUtilizado());

        verify(cuentaRepository).findByNumeroCuentaForUpdate("0000000001");
        verify(cuentaRepository).save(cuentaMock);
        verify(transaccionPagoRepository, atLeastOnce()).save(any(TransaccionPago.class));
        verify(emailService).enviarComprobantePago(eq(clienteMock.getCorreoElectronico()), contains(clienteMock.getNombre()), eq("AUT-771122"), eq(new BigDecimal("100.00")), anyString(), contains("5512345678"), anyString(), isNull());
    }

    @Test
    void testProcesarPagoServicioGiftCardExitoso() {
        ProcesarPagoRequestDto req = ProcesarPagoRequestDto.builder()
                .numeroCuenta("0000000001")
                .idServicio("2")
                .idProducto("202")
                .precio(new BigDecimal("200.00"))
                .bandera(false)
                .telefono(null)
                .referencia("AMAZON-GIFT-REF")
                .pin("123456")
                .build();

        ConfirmacionPin pinMock = ConfirmacionPin.builder()
                .id(6L)
                .cuenta(cuentaMock)
                .pinHash("hashed-pin")
                .intentos(0)
                .utilizado(false)
                .fechaExpiracion(LocalDateTime.now().plusMinutes(5))
                .build();

        GestoPagoToken tokenMock = new GestoPagoToken();
        tokenMock.setToken("jwt-gestopago-token");

        String xmlGiftCard = """
                <RESPONSE>
                    <ID_TX>993812</ID_TX>
                    <NUM_AUTORIZACION>AUT-993344</NUM_AUTORIZACION>
                    <SALDO>13800.00</SALDO>
                    <PIN>AMZ-X789-Q456</PIN>
                    <MENSAJE>
                        <CODIGO>01</CODIGO>
                        <TEXTO>Operacion realizada con exito</TEXTO>
                    </MENSAJE>
                </RESPONSE>
                """;

        when(cuentaRepository.findByNumeroCuentaForUpdate("0000000001")).thenReturn(Optional.of(cuentaMock));
        when(confirmacionPinRepository.findPinActivo(eq("0000000001"), any())).thenReturn(Optional.of(pinMock));
        when(passwordEncoder.matches("123456", "hashed-pin")).thenReturn(true);
        when(aesEncryptionUtil.encryptPayload(any())).thenReturn("signed-encrypted-payload");
        when(gestoPagoTokenService.obtenerTokenActivo(83, "GPS83-TPV-17")).thenReturn(Optional.of(tokenMock));
        when(transactionClient.sendTx(anyString(), anyString(), anyString(), anyString())).thenReturn(xmlGiftCard);
        when(transaccionPagoRepository.save(any(TransaccionPago.class))).thenAnswer(i -> {
            TransaccionPago tx = i.getArgument(0);
            tx.setId(100L);
            return tx;
        });

        ComprobantePagoResponseDto resp = pagoService.procesarPago(req);

        assertNotNull(resp);
        assertEquals("APROBADO", resp.getCodigoRespuesta());
        assertEquals("EXITOSA", resp.getEstadoTransaccion());
        assertEquals("AUT-993344", resp.getNumAutorizacion());
        assertEquals("AMZ-X789-Q456", resp.getPinGiftCard());
        assertFalse(resp.getBandera());
        assertEquals("AMAZON-GIFT-REF", resp.getReferencia());
        assertEquals(new BigDecimal("300.00"), resp.getNuevoSaldoCuenta());

        verify(emailService).enviarComprobantePago(eq(clienteMock.getCorreoElectronico()), contains(clienteMock.getNombre()), eq("AUT-993344"), eq(new BigDecimal("200.00")), anyString(), contains("AMAZON-GIFT-REF"), anyString(), eq("AMZ-X789-Q456"));
    }

    @Test
    void testProcesarPagoRecargaSinTelefonoLanzaError() {
        ProcesarPagoRequestDto req = ProcesarPagoRequestDto.builder()
                .numeroCuenta("0000000001")
                .idServicio("1")
                .idProducto("101")
                .precio(new BigDecimal("100.00"))
                .bandera(true)
                .telefono(null)
                .pin("123456")
                .build();

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> pagoService.procesarPago(req));
        assertTrue(ex.getMessage().contains("campo 'telefono' es obligatorio"));
        verify(transactionClient, never()).sendTx(any(), any(), any(), any());
    }

    @Test
    void testProcesarPagoServicioSinReferenciaLanzaError() {
        ProcesarPagoRequestDto req = ProcesarPagoRequestDto.builder()
                .numeroCuenta("0000000001")
                .idServicio("1")
                .idProducto("101")
                .precio(new BigDecimal("100.00"))
                .bandera(false)
                .referencia("")
                .pin("123456")
                .build();

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> pagoService.procesarPago(req));
        assertTrue(ex.getMessage().contains("campo 'referencia' es obligatorio"));
        verify(transactionClient, never()).sendTx(any(), any(), any(), any());
    }

    @Test
    void testProcesarPagoPinIncorrecto() {
        ProcesarPagoRequestDto req = ProcesarPagoRequestDto.builder()
                .numeroCuenta("0000000001")
                .idServicio("1")
                .idProducto("101")
                .precio(new BigDecimal("100.00"))
                .bandera(true)
                .telefono("5512345678")
                .pin("999999")
                .build();

        ConfirmacionPin pinMock = ConfirmacionPin.builder()
                .id(5L)
                .cuenta(cuentaMock)
                .pinHash("hashed-pin")
                .intentos(0)
                .utilizado(false)
                .fechaExpiracion(LocalDateTime.now().plusMinutes(5))
                .build();

        when(cuentaRepository.findByNumeroCuentaForUpdate("0000000001")).thenReturn(Optional.of(cuentaMock));
        when(confirmacionPinRepository.findPinActivo(eq("0000000001"), any())).thenReturn(Optional.of(pinMock));
        when(passwordEncoder.matches("999999", "hashed-pin")).thenReturn(false);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> pagoService.procesarPago(req));
        assertTrue(ex.getMessage().contains("PIN de seguridad es incorrecto"));
        assertEquals(1, pinMock.getIntentos());
        verify(cuentaRepository, never()).save(cuentaMock);
        verify(transactionClient, never()).sendTx(any(), any(), any(), any());
    }

    @Test
    void testProcesarPagoProveedorRechazaYCompensaSaldo() {
        ProcesarPagoRequestDto req = ProcesarPagoRequestDto.builder()
                .numeroCuenta("0000000001")
                .idServicio("1")
                .idProducto("101")
                .precio(new BigDecimal("100.00"))
                .bandera(true)
                .telefono("5512345678")
                .pin("123456")
                .build();

        ConfirmacionPin pinMock = ConfirmacionPin.builder()
                .id(5L)
                .cuenta(cuentaMock)
                .pinHash("hashed-pin")
                .intentos(0)
                .utilizado(false)
                .fechaExpiracion(LocalDateTime.now().plusMinutes(5))
                .build();

        GestoPagoToken tokenMock = new GestoPagoToken();
        tokenMock.setToken("jwt-gestopago-token");

        String xmlError = """
                <RESPONSE>
                    <ID_TX></ID_TX>
                    <NUM_AUTORIZACION></NUM_AUTORIZACION>
                    <SALDO>0.00</SALDO>
                    <MENSAJE>
                        <CODIGO>02</CODIGO>
                        <TEXTO>Saldo insuficiente en distribuidor</TEXTO>
                    </MENSAJE>
                </RESPONSE>
                """;

        when(cuentaRepository.findByNumeroCuentaForUpdate("0000000001")).thenReturn(Optional.of(cuentaMock));
        when(confirmacionPinRepository.findPinActivo(eq("0000000001"), any())).thenReturn(Optional.of(pinMock));
        when(passwordEncoder.matches("123456", "hashed-pin")).thenReturn(true);
        when(aesEncryptionUtil.encryptPayload(any())).thenReturn("signed-payload");
        when(gestoPagoTokenService.obtenerTokenActivo(83, "GPS83-TPV-17")).thenReturn(Optional.of(tokenMock));
        when(transactionClient.sendTx(anyString(), anyString(), anyString(), anyString())).thenReturn(xmlError);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> pagoService.procesarPago(req));
        assertTrue(ex.getMessage().contains("Saldo insuficiente en distribuidor"));
        // El saldo reservado es compensado/reintegrado íntegramente tras el rechazo del proveedor
        assertEquals(new BigDecimal("500.00"), cuentaMock.getSaldo());
    }

    @Test
    void testProcesarPagoMontoManipuladoLanzaError() {
        CatProductMongo movistar100 = CatProductMongo.builder()
                .idProducto("381")
                .idServicio("124")
                .servicio("Movistar")
                .producto("Movistar $100")
                .precio(100.0)
                .idCatTipoServicio("1")
                .tipoFront("1")
                .status(ProductStatusEnum.ACTIVE)
                .build();

        ProcesarPagoRequestDto req = ProcesarPagoRequestDto.builder()
                .numeroCuenta("0000000001")
                .idServicio("124")
                .idProducto("381")
                .precio(new BigDecimal("10.00")) // Monto manipulado: envía $10 en lugar de $100
                .bandera(true)
                .telefono("5512345678")
                .pin("123456")
                .build();

        when(catProductMongoRepository.findByIdProducto("381")).thenReturn(Optional.of(movistar100));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> pagoService.procesarPago(req));
        assertTrue(ex.getMessage().contains("Monto inválido o manipulado"));
        verify(transactionClient, never()).sendTx(any(), any(), any(), any());
    }

    @Test
    void testProcesarPagoProductoInexistenteEnCatalogoLanza404() {
        ProcesarPagoRequestDto req = ProcesarPagoRequestDto.builder()
                .numeroCuenta("0000000001")
                .idServicio("124")
                .idProducto("999999")
                .precio(new BigDecimal("100.00"))
                .bandera(true)
                .telefono("5512345678")
                .pin("123456")
                .build();

        when(catProductMongoRepository.findByIdProducto("999999")).thenReturn(Optional.empty());
        when(catProductMongoRepository.count()).thenReturn(901L);

        assertThrows(ResourceNotFoundException.class, () -> pagoService.procesarPago(req));
        verify(transactionClient, never()).sendTx(any(), any(), any(), any());
    }

    @Test
    void testProcesarPagoUsuarioNoAutorizadoSobreCuentaAjenaLanza403() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "intruso@correo.com",
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        Cliente clienteIntruso = Cliente.builder().id(99L).correoElectronico("intruso@correo.com").activo(true).build();
        Usuario usuarioIntruso = Usuario.builder().id(99L).correo("intruso@correo.com").activo(true).cliente(clienteIntruso).build();

        when(cuentaRepository.findByNumeroCuentaForUpdate("0000000001")).thenReturn(Optional.of(cuentaMock));
        when(usuarioRepository.findByCorreo("intruso@correo.com")).thenReturn(Optional.of(usuarioIntruso));

        ProcesarPagoRequestDto req = ProcesarPagoRequestDto.builder()
                .numeroCuenta("0000000001")
                .idServicio("124")
                .idProducto("381")
                .precio(new BigDecimal("100.00"))
                .bandera(true)
                .telefono("5512345678")
                .pin("123456")
                .build();

        assertThrows(UnauthorizedOperationException.class, () -> pagoService.procesarPago(req));
        verify(transactionClient, never()).sendTx(any(), any(), any(), any());
    }

    @Test
    void testProcesarPagoDuplicadoEnVentanaIdempotenciaLanza409() {
        ProcesarPagoRequestDto req = ProcesarPagoRequestDto.builder()
                .numeroCuenta("0000000001")
                .idServicio("124")
                .idProducto("381")
                .precio(new BigDecimal("100.00"))
                .bandera(true)
                .telefono("5512345678")
                .pin("123456")
                .build();

        when(cuentaRepository.findByNumeroCuentaForUpdate("0000000001")).thenReturn(Optional.of(cuentaMock));
        when(transaccionPagoRepository.existeTransaccionDuplicadaReciente(
                eq("0000000001"), eq("381"), eq(new BigDecimal("100.00")), eq("5512345678"), isNull(), anyCollection(), any()
        )).thenReturn(true);

        assertThrows(DuplicatedResourceException.class, () -> pagoService.procesarPago(req));
        verify(transactionClient, never()).sendTx(any(), any(), any(), any());
    }

    @Test
    void testProcesarPagoTimeoutMarcaPendienteConciliacionSinReembolsoPrematuro() {
        ProcesarPagoRequestDto req = ProcesarPagoRequestDto.builder()
                .numeroCuenta("0000000001")
                .idServicio("124")
                .idProducto("381")
                .precio(new BigDecimal("100.00"))
                .bandera(true)
                .telefono("5512345678")
                .pin("123456")
                .build();

        ConfirmacionPin pinMock = ConfirmacionPin.builder()
                .id(5L)
                .cuenta(cuentaMock)
                .pinHash("hashed-pin")
                .intentos(0)
                .utilizado(false)
                .fechaExpiracion(LocalDateTime.now().plusMinutes(5))
                .build();

        GestoPagoToken tokenMock = new GestoPagoToken();
        tokenMock.setToken("jwt-gestopago-token");

        when(cuentaRepository.findByNumeroCuentaForUpdate("0000000001")).thenReturn(Optional.of(cuentaMock));
        when(confirmacionPinRepository.findPinActivo(eq("0000000001"), any())).thenReturn(Optional.of(pinMock));
        when(passwordEncoder.matches("123456", "hashed-pin")).thenReturn(true);
        when(aesEncryptionUtil.encryptPayload(any())).thenReturn("signed-payload");
        when(gestoPagoTokenService.obtenerTokenActivo(83, "GPS83-TPV-17")).thenReturn(Optional.of(tokenMock));
        when(transactionClient.sendTx(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Read timed out", new SocketTimeoutException("Read timed out")));

        ProviderTimeoutException ex = assertThrows(ProviderTimeoutException.class, () -> pagoService.procesarPago(req));
        assertTrue(ex.getMessage().contains("PENDIENTE_CONCILIACION"));
        // El saldo permanece reservado ($400.00) hasta que confirmTx.do determine si se aplicó o no
        assertEquals(new BigDecimal("400.00"), cuentaMock.getSaldo());
    }

    @Test
    void testConciliarPagoNoAplicadoCodigo70ReembolsaSaldo() {
        cuentaMock.setSaldo(new BigDecimal("400.00")); // Saldo tras la reserva inicial
        TransaccionPago txPendiente = TransaccionPago.builder()
                .id(77L)
                .cuenta(cuentaMock)
                .idServicio("124")
                .idProducto("381")
                .telefono("5512345678")
                .bandera(true)
                .monto(new BigDecimal("100.00"))
                .upc("UPC123456789")
                .horaLocal("20261029120000")
                .distribuidor("083")
                .estado(EstadoTransaccion.PENDIENTE_CONCILIACION)
                .fechaTransaccion(LocalDateTime.now())
                .build();

        GestoPagoToken tokenMock = new GestoPagoToken();
        tokenMock.setToken("jwt-gestopago-token");

        String xmlConfirmNoAplicada = """
                <RESPONSE>
                    <NUM_AUTORIZACION>0</NUM_AUTORIZACION>
                    <MENSAJE>
                        <CODIGO>70</CODIGO>
                        <TEXTO>La transaccion no fue aplicada</TEXTO>
                        <ID_TX>-1</ID_TX>
                    </MENSAJE>
                </RESPONSE>
                """;

        when(transaccionPagoRepository.findByUpcWithCuentaAndCliente("UPC123456789")).thenReturn(Optional.of(txPendiente));
        when(cuentaRepository.findByNumeroCuentaForUpdate("0000000001")).thenReturn(Optional.of(cuentaMock));
        when(aesEncryptionUtil.encryptPayload(any())).thenReturn("signed-confirm");
        when(gestoPagoTokenService.obtenerTokenActivo(83, "GPS83-TPV-17")).thenReturn(Optional.of(tokenMock));
        when(transactionClient.confirmTx(anyString(), anyString(), anyString(), anyString())).thenReturn(xmlConfirmNoAplicada);

        ConciliarPagoRequestDto req = ConciliarPagoRequestDto.builder().upc("UPC123456789").build();
        ComprobantePagoResponseDto resp = pagoService.conciliarPago(req);

        assertEquals("RECHAZADA", resp.getEstadoTransaccion());
        assertEquals("RECHAZADO", resp.getCodigoRespuesta());
        // Saldo restaurado a $500.00
        assertEquals(new BigDecimal("500.00"), cuentaMock.getSaldo());
        assertEquals(new BigDecimal("500.00"), resp.getNuevoSaldoCuenta());
    }

    @Test
    void testConciliarPagoAplicadoExitosamenteCodigo06() {
        cuentaMock.setSaldo(new BigDecimal("400.00"));
        TransaccionPago txPendiente = TransaccionPago.builder()
                .id(78L)
                .cuenta(cuentaMock)
                .idServicio("124")
                .idProducto("381")
                .telefono("5512345678")
                .bandera(true)
                .monto(new BigDecimal("100.00"))
                .upc("UPC987654321")
                .horaLocal("20261029120000")
                .distribuidor("083")
                .estado(EstadoTransaccion.PENDIENTE_CONCILIACION)
                .fechaTransaccion(LocalDateTime.now())
                .build();

        GestoPagoToken tokenMock = new GestoPagoToken();
        tokenMock.setToken("jwt-gestopago-token");

        String xmlConfirmExitosa = """
                <RESPONSE>
                    <NUM_AUTORIZACION>100024988</NUM_AUTORIZACION>
                    <MENSAJE>
                        <CODIGO>06</CODIGO>
                        <TEXTO>La transaccion fue aplicada exitosamente. Autorizacion [100024988] idTicket[291437188]</TEXTO>
                        <ID_TX>291437188</ID_TX>
                    </MENSAJE>
                </RESPONSE>
                """;

        when(transaccionPagoRepository.findByUpcWithCuentaAndCliente("UPC987654321")).thenReturn(Optional.of(txPendiente));
        when(aesEncryptionUtil.encryptPayload(any())).thenReturn("signed-confirm");
        when(gestoPagoTokenService.obtenerTokenActivo(83, "GPS83-TPV-17")).thenReturn(Optional.of(tokenMock));
        when(transactionClient.confirmTx(anyString(), anyString(), anyString(), anyString())).thenReturn(xmlConfirmExitosa);

        ConciliarPagoRequestDto req = ConciliarPagoRequestDto.builder().upc("UPC987654321").build();
        ComprobantePagoResponseDto resp = pagoService.conciliarPago(req);

        assertEquals("EXITOSA", resp.getEstadoTransaccion());
        assertEquals("CONCILIADO_APROBADO", resp.getCodigoRespuesta());
        assertEquals("100024988", resp.getNumAutorizacion());
        assertEquals("291437188", resp.getIdTxProveedor());
        assertEquals(new BigDecimal("400.00"), resp.getNuevoSaldoCuenta());
    }
}
