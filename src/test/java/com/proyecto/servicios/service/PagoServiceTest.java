package com.proyecto.servicios.service;

import com.proyecto.servicios.client.GestoPagoTransactionClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.entity.postgres.*;
import com.proyecto.servicios.exception.BusinessRuleException;
import com.proyecto.servicios.exception.ResourceNotFoundException;
import com.proyecto.servicios.model.ComprobantePagoResponseDto;
import com.proyecto.servicios.model.ProcesarPagoRequestDto;
import com.proyecto.servicios.model.SolicitudPinRequestDto;
import com.proyecto.servicios.repositorys.postgres.ConfirmacionPinRepository;
import com.proyecto.servicios.repositorys.postgres.CuentaRepository;
import com.proyecto.servicios.repositorys.postgres.TransaccionPagoRepository;
import com.proyecto.servicios.service.Impl.PagoServiceImpl;
import com.proyecto.servicios.util.AesEncryptionUtil;
import com.proyecto.servicios.repositorys.mongo.CatProductMongoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
        clienteMock = Cliente.builder()
                .id(1L)
                .nombre("Uriel")
                .apellidoPaterno("Aguayo")
                .correoElectronico("aguayomar77@gmail.com.com")
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
                .monto(new BigDecimal("1000.00")) // Saldo es 500
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

        when(cuentaRepository.findByNumeroCuenta("0000000001")).thenReturn(Optional.of(cuentaMock));
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
        assertEquals("01", resp.getCodigoRespuesta());
        assertEquals("AUT-771122", resp.getNumAutorizacion());
        assertEquals("884920", resp.getIdTxProveedor());
        assertEquals(new BigDecimal("400.00"), resp.getNuevoSaldoCuenta());
        assertEquals(new BigDecimal("400.00"), cuentaMock.getSaldo());
        assertEquals("5512345678", resp.getTelefono());
        assertTrue(resp.getBandera());
        assertTrue(pinMock.getUtilizado());

        verify(cuentaRepository).save(cuentaMock);
        verify(transaccionPagoRepository).save(any(TransaccionPago.class));
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
                .telefono(null) // En servicios/gift cards no es obligatorio
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

        when(cuentaRepository.findByNumeroCuenta("0000000001")).thenReturn(Optional.of(cuentaMock));
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
        assertEquals("01", resp.getCodigoRespuesta());
        assertEquals("AUT-993344", resp.getNumAutorizacion());
        assertEquals("AMZ-X789-Q456", resp.getPinGiftCard());
        assertFalse(resp.getBandera());
        assertEquals("AMAZON-GIFT-REF", resp.getReferencia());
        assertEquals(new BigDecimal("300.00"), resp.getNuevoSaldoCuenta()); // 500 - 200

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
                .telefono(null) // Teléfono nulo para recarga
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

        when(cuentaRepository.findByNumeroCuenta("0000000001")).thenReturn(Optional.of(cuentaMock));
        when(confirmacionPinRepository.findPinActivo(eq("0000000001"), any())).thenReturn(Optional.of(pinMock));
        when(passwordEncoder.matches("123456", "hashed-pin")).thenReturn(true);

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
                .referencia("") // Referencia vacía para servicio
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

        when(cuentaRepository.findByNumeroCuenta("0000000001")).thenReturn(Optional.of(cuentaMock));
        when(confirmacionPinRepository.findPinActivo(eq("0000000001"), any())).thenReturn(Optional.of(pinMock));
        when(passwordEncoder.matches("123456", "hashed-pin")).thenReturn(true);

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
                .pin("999999") // PIN incorrecto
                .build();

        ConfirmacionPin pinMock = ConfirmacionPin.builder()
                .id(5L)
                .cuenta(cuentaMock)
                .pinHash("hashed-pin")
                .intentos(0)
                .utilizado(false)
                .fechaExpiracion(LocalDateTime.now().plusMinutes(5))
                .build();

        when(cuentaRepository.findByNumeroCuenta("0000000001")).thenReturn(Optional.of(cuentaMock));
        when(confirmacionPinRepository.findPinActivo(eq("0000000001"), any())).thenReturn(Optional.of(pinMock));
        when(passwordEncoder.matches("999999", "hashed-pin")).thenReturn(false);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> pagoService.procesarPago(req));
        assertTrue(ex.getMessage().contains("PIN de seguridad es incorrecto"));
        assertEquals(1, pinMock.getIntentos());
        verify(cuentaRepository, never()).save(cuentaMock);
        verify(transactionClient, never()).sendTx(any(), any(), any(), any());
    }

    @Test
    void testProcesarPagoProveedorRechaza() {
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

        when(cuentaRepository.findByNumeroCuenta("0000000001")).thenReturn(Optional.of(cuentaMock));
        when(confirmacionPinRepository.findPinActivo(eq("0000000001"), any())).thenReturn(Optional.of(pinMock));
        when(passwordEncoder.matches("123456", "hashed-pin")).thenReturn(true);
        when(aesEncryptionUtil.encryptPayload(any())).thenReturn("signed-payload");
        when(gestoPagoTokenService.obtenerTokenActivo(83, "GPS83-TPV-17")).thenReturn(Optional.of(tokenMock));
        when(transactionClient.sendTx(anyString(), anyString(), anyString(), anyString())).thenReturn(xmlError);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> pagoService.procesarPago(req));
        assertTrue(ex.getMessage().contains("Saldo insuficiente en distribuidor"));
        // El saldo NO debe descontarse si el proveedor rechaza
        assertEquals(new BigDecimal("500.00"), cuentaMock.getSaldo());
        verify(transaccionPagoRepository, never()).save(any());
    }
}
