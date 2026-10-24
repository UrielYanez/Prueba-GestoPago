package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoTransactionClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.entity.postgres.*;
import com.proyecto.servicios.exception.BusinessRuleException;
import com.proyecto.servicios.exception.ResourceNotFoundException;
import com.proyecto.servicios.model.ComprobantePagoResponseDto;
import com.proyecto.servicios.model.ProcesarPagoRequestDto;
import com.proyecto.servicios.model.SolicitudPinRequestDto;
import com.proyecto.servicios.model.gestopago.GestoPagoTxPayload;
import com.proyecto.servicios.model.gestopago.GestoPagoTxResponseXml;
import com.proyecto.servicios.repositorys.postgres.ConfirmacionPinRepository;
import com.proyecto.servicios.repositorys.postgres.CuentaRepository;
import com.proyecto.servicios.repositorys.postgres.TransaccionPagoRepository;
import com.proyecto.servicios.service.EmailService;
import com.proyecto.servicios.service.GestoPagoTokenService;
import com.proyecto.servicios.service.PagoService;
import com.proyecto.servicios.util.AesEncryptionUtil;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.proyecto.servicios.entity.mongo.CatProductMongo;
import com.proyecto.servicios.repositorys.mongo.CatProductMongoRepository;
import java.io.StringReader;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PagoServiceImpl implements PagoService {

    private final CuentaRepository cuentaRepository;
    private final ConfirmacionPinRepository confirmacionPinRepository;
    private final TransaccionPagoRepository transaccionPagoRepository;
    private final CatProductMongoRepository catProductMongoRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final AesEncryptionUtil aesEncryptionUtil;
    private final GestoPagoTransactionClient transactionClient;
    private final GestoPagoTokenService gestoPagoTokenService;

    @Value("${gestopago.auth.id-distribuidor:83}")
    private Integer idDistribuidor;

    @Value("${gestopago.auth.codigo-dispositivo:GPS83-TPV-17}")
    private String codigoDispositivo;

    @Value("${gestopago.tx.api-key:YSX1HpAFum4TpCecyFBxs4eIjAlbhKqK6fpcSQp8}")
    private String apiKey;

    @Value("${gestopago.tx.unidad:083}")
    private String defaultUnidad;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public void solicitarPin(SolicitudPinRequestDto request) {
        log.info("Solicitud de PIN para la cuenta: {}, monto: {}", request.getNumeroCuenta(), request.getMonto());

        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(request.getNumeroCuenta())
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta no encontrada con número: " + request.getNumeroCuenta()));

        if (cuenta.getEstatus() != EstatusCuenta.ACTIVA) {
            throw new BusinessRuleException("La cuenta no está activa. Estatus actual: " + cuenta.getEstatus());
        }

        if (cuenta.getSaldo().compareTo(request.getMonto()) < 0) {
            throw new BusinessRuleException("Saldo insuficiente en la cuenta para realizar esta operación.");
        }

        Cliente cliente = cuenta.getCliente();
        if (cliente == null || cliente.getCorreoElectronico() == null || cliente.getCorreoElectronico().isBlank()) {
            throw new BusinessRuleException("El titular de la cuenta no tiene un correo electrónico registrado para recibir el PIN.");
        }

        // Invalidar pines activos previos de esta cuenta
        confirmacionPinRepository.invalidarPinesAnteriores(cuenta.getId());

        // Generar PIN aleatorio de 6 dígitos numéricos
        int pinNumero = 100_000 + secureRandom.nextInt(900_000);
        String pinTexto = String.valueOf(pinNumero);

        // Guardar PIN hasheado con expiración de 5 minutos
        ConfirmacionPin nuevoPin = ConfirmacionPin.builder()
                .cuenta(cuenta)
                .pinHash(passwordEncoder.encode(pinTexto))
                .fechaExpiracion(LocalDateTime.now().plusMinutes(5))
                .utilizado(false)
                .intentos(0)
                .fechaCreacion(LocalDateTime.now())
                .build();

        confirmacionPinRepository.save(nuevoPin);
        log.info("PIN OTP generado para la cuenta {}: {}", cuenta.getNumeroCuenta(), pinTexto);

        // Enviar por correo al cliente
        emailService.enviarPinTransaccion(
                cliente.getCorreoElectronico(),
                cliente.getNombre() + (cliente.getApellidoPaterno() != null ? " " + cliente.getApellidoPaterno() : ""),
                pinTexto,
                request.getMonto(),
                request.getConcepto() != null ? request.getConcepto() : "Pago de Servicio"
        );
    }

    @Override
    @Transactional
    public ComprobantePagoResponseDto procesarPago(ProcesarPagoRequestDto request) {
        log.info("Procesando pago para cuenta: {}, producto: {}, servicio: {}, precio: {}",
                request.getNumeroCuenta(), request.getIdProducto(), request.getIdServicio(), request.getPrecio());

        // 1. Validar Cuenta
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(request.getNumeroCuenta())
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta no encontrada con número: " + request.getNumeroCuenta()));

        if (cuenta.getEstatus() != EstatusCuenta.ACTIVA) {
            throw new BusinessRuleException("La cuenta se encuentra inactiva o bloqueada. Estatus: " + cuenta.getEstatus());
        }

        // 2. Validar Saldo
        if (cuenta.getSaldo().compareTo(request.getPrecio()) < 0) {
            throw new BusinessRuleException(String.format("Saldo insuficiente. Saldo disponible: $%.2f, Monto requerido: $%.2f",
                    cuenta.getSaldo(), request.getPrecio()));
        }

        // 3. Validar PIN OTP
        ConfirmacionPin pinEntity = confirmacionPinRepository.findPinActivo(request.getNumeroCuenta(), LocalDateTime.now())
                .orElseThrow(() -> new BusinessRuleException("No se encontró un PIN de confirmación activo o ha expirado. Por favor solicite uno nuevo."));

        if (pinEntity.getIntentos() >= 3) {
            pinEntity.setUtilizado(true);
            confirmacionPinRepository.save(pinEntity);
            throw new BusinessRuleException("El PIN ha sido bloqueado por superar el límite de 3 intentos fallidos. Solicite un nuevo código.");
        }

        if (!passwordEncoder.matches(request.getPin().trim(), pinEntity.getPinHash())) {
            pinEntity.setIntentos(pinEntity.getIntentos() + 1);
            if (pinEntity.getIntentos() >= 3) {
                pinEntity.setUtilizado(true);
            }
            confirmacionPinRepository.save(pinEntity);
            int restantes = 3 - pinEntity.getIntentos();
            throw new BusinessRuleException("El código PIN de seguridad es incorrecto. Intentos restantes: " + restantes);
        }

        // Marcar PIN como utilizado inmediatamente para que no pueda reutilizarse
        pinEntity.setUtilizado(true);
        confirmacionPinRepository.save(pinEntity);

        // 4. Validar según la Bandera de Operación (true = Recarga, false = Servicio / Tarjeta de Regalo)
        if (request.getBandera() == null) {
            throw new BusinessRuleException("La bandera de tipo de operación es obligatoria (true para Recarga, false para Servicio/Tarjeta de Regalo).");
        }

        // Parsear IDs a enteros para GestoPago
        Integer idProductoInt;
        Integer idServicioInt;
        try {
            idProductoInt = Integer.parseInt(request.getIdProducto().trim());
            idServicioInt = Integer.parseInt(request.getIdServicio().trim());
        } catch (Exception e) {
            throw new BusinessRuleException("Los campos 'idProducto' e 'idServicio' deben ser identificadores numéricos válidos.");
        }

        String telefonoGestoPago;
        String referenciaGestoPago;
        java.math.BigDecimal montoGestoPago;

        // Consultar catálogo en MongoDB para conocer el tipo técnico de producto (Recarga, Servicio o Tarjeta de Regalo)
        Optional<CatProductMongo> productOpt = catProductMongoRepository.findByIdProducto(request.getIdProducto().trim());
        boolean isGiftCard = productOpt.map(p -> "10".equals(p.getIdCatTipoServicio()) || "11".equals(p.getIdCatTipoServicio())).orElse(false);

        if (Boolean.TRUE.equals(request.getBandera())) {
            // Modo Recarga Telefónica (En recargas el monto viene implícito en el idProducto y el destino es el teléfono)
            if (request.getTelefono() == null || !request.getTelefono().matches("^\\d{10}$")) {
                throw new BusinessRuleException("Para recargas telefónicas (bandera=true), el campo 'telefono' es obligatorio y debe tener exactamente 10 dígitos numéricos.");
            }
            telefonoGestoPago = request.getTelefono().trim();
            referenciaGestoPago = null; // No viaja en recargas para evitar rechazo 44
            montoGestoPago = null;      // No viaja en recargas para evitar rechazo 44
        } else if (isGiftCard) {
            // Modo Tarjeta de Regalo / PIN Electrónico (Amazon, Netflix, Spotify, etc.)
            // En GestoPago, idCatTipoServicio 10 y 11 fijan el teléfono en "#1111111111" y no llevan referencia ni montoPago
            log.info("Producto {} identificado como Tarjeta de Regalo / Entretenimiento (Cat: {}). Configurando formato GiftCard GestoPago ('#1111111111').",
                    request.getIdProducto(), productOpt.get().getIdCatTipoServicio());
            telefonoGestoPago = "#1111111111";
            referenciaGestoPago = null;
            montoGestoPago = null;
        } else {
            // Modo Pago de Servicio (CFE, Telmex, Izzi, etc.)
            if (request.getReferencia() == null || request.getReferencia().trim().isEmpty()) {
                throw new BusinessRuleException("Para pagos de servicios (bandera=false), el campo 'referencia' es obligatorio (número de recibo, contrato o código de barras).");
            }
            referenciaGestoPago = request.getReferencia().trim();
            // Si el cliente no proporciona teléfono en un servicio, GestoPago exige "1111111111"
            telefonoGestoPago = (request.getTelefono() != null && !request.getTelefono().isBlank())
                    ? request.getTelefono().trim()
                    : "1111111111";
            montoGestoPago = request.getPrecio();
        }

        // 5. Construir Payload para GestoPago
        String upc = "UPC" + System.currentTimeMillis();
        String horaLocal = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String unidad = (request.getDistribuidor() != null && !request.getDistribuidor().isBlank())
                ? request.getDistribuidor()
                : defaultUnidad;

        GestoPagoTxPayload payload = GestoPagoTxPayload.builder()
                .idProducto(idProductoInt)
                .idServicio(idServicioInt)
                .referencia(referenciaGestoPago)
                .telefono(telefonoGestoPago)
                .montoPago(montoGestoPago)
                .horaLocal(horaLocal)
                .upc(upc)
                .unidad(unidad)
                .build();

        // 6. Cifrado AES-256-CBC
        String signed = aesEncryptionUtil.encryptPayload(payload);
        String random = String.valueOf(System.currentTimeMillis());

        // 7. Obtener Token Bearer de GestoPago (con auto-renovación si no existe)
        GestoPagoToken tokenEntity = gestoPagoTokenService.obtenerTokenActivo(idDistribuidor, codigoDispositivo)
                .orElseGet(() -> {
                    log.info("Token GestoPago no encontrado en BD. Renovando antes de enviar transacción...");
                    gestoPagoTokenService.renovarToken();
                    return gestoPagoTokenService.obtenerTokenActivo(idDistribuidor, codigoDispositivo)
                            .orElseThrow(() -> new BusinessRuleException("No fue posible obtener un token de autenticación con el proveedor"));
                });

        // 8. Enviar Transacción vía Feign
        log.info("Enviando transacción sendTx a GestoPago para UPC: {} (Bandera: {}, Teléfono: {}, Referencia: {})",
                upc, request.getBandera(), telefonoGestoPago, referenciaGestoPago);
        String xmlResponse;
        try {
            xmlResponse = transactionClient.sendTx(
                    "Bearer " + tokenEntity.getToken(),
                    apiKey,
                    random,
                    signed
            );
            log.info("XML crudo retornado por GestoPago sendTx: {}", xmlResponse);
        } catch (Exception e) {
            log.error("Fallo de comunicación con GestoPago sendTx: {}", e.getMessage(), e);
            throw new BusinessRuleException("Error al comunicarse con el proveedor de pagos: " + e.getMessage());
        }

        // 9. Parsear Respuesta XML de GestoPago
        GestoPagoTxResponseXml responseXml = parseResponseXml(xmlResponse);
        log.info("Respuesta de GestoPago recibida. Código: {}, Mensaje: {}, ID_TX: {}, PIN GiftCard: {}",
                responseXml.getCodigo(), responseXml.getTexto(), responseXml.getIdTx(), responseXml.getPinGiftCard());

        // 10. Verificar Código de Respuesta (01 = Éxito)
        if (!responseXml.isExitoso()) {
            throw new BusinessRuleException("La transacción fue rechazada por el proveedor: "
                    + responseXml.getTexto() + " (Código: " + responseXml.getCodigo() + ")");
        }

        // 11. Descontar Saldo Atómicamente
        cuenta.setSaldo(cuenta.getSaldo().subtract(request.getPrecio()));
        cuentaRepository.save(cuenta);
        log.info("Saldo descontado exitosamente. Nuevo saldo en cuenta {}: ${}", cuenta.getNumeroCuenta(), cuenta.getSaldo());

        // 12. Registrar Transacción y Comprobante
        TransaccionPago transaccion = TransaccionPago.builder()
                .cuenta(cuenta)
                .idServicio(request.getIdServicio())
                .idProducto(request.getIdProducto())
                .telefono(request.getTelefono())
                .referencia(request.getReferencia())
                .bandera(request.getBandera())
                .monto(request.getPrecio())
                .upc(upc)
                .distribuidor(unidad)
                .idTxProveedor(responseXml.getIdTx())
                .numAutorizacion(responseXml.getNumAutorizacion())
                .codigoRespuesta(responseXml.getCodigo())
                .mensajeRespuesta(responseXml.getTexto())
                .saldoProveedor(responseXml.getSaldo())
                .pinGiftCard(responseXml.getPinGiftCard())
                .fechaTransaccion(LocalDateTime.now())
                .build();

        TransaccionPago savedTx = transaccionPagoRepository.save(transaccion);

        // 13. Enviar comprobante al correo del cliente (incluyendo PIN si es tarjeta de regalo)
        Cliente cliente = cuenta.getCliente();
        if (cliente != null && cliente.getCorreoElectronico() != null) {
            String detalleRef = Boolean.TRUE.equals(request.getBandera())
                    ? "Teléfono: " + request.getTelefono()
                    : "Referencia: " + request.getReferencia();

            emailService.enviarComprobantePago(
                    cliente.getCorreoElectronico(),
                    cliente.getNombre(),
                    responseXml.getNumAutorizacion(),
                    request.getPrecio(),
                    "Producto ID: " + request.getIdProducto(),
                    detalleRef,
                    upc,
                    responseXml.getPinGiftCard()
            );
        }

        // 14. Retornar Comprobante DTO
        return ComprobantePagoResponseDto.builder()
                .transaccionId(savedTx.getId())
                .numeroCuenta(cuenta.getNumeroCuenta())
                .idTxProveedor(responseXml.getIdTx())
                .numAutorizacion(responseXml.getNumAutorizacion())
                .upc(upc)
                .bandera(request.getBandera())
                .telefono(request.getTelefono())
                .referencia(request.getReferencia())
                .pinGiftCard(responseXml.getPinGiftCard())
                .monto(request.getPrecio())
                .nuevoSaldoCuenta(cuenta.getSaldo())
                .codigoRespuesta(responseXml.getCodigo())
                .mensajeRespuesta(responseXml.getTexto())
                .fechaTransaccion(savedTx.getFechaTransaccion())
                .build();
    }

    private GestoPagoTxResponseXml parseResponseXml(String xml) {
        if (xml == null || xml.isBlank()) {
            throw new BusinessRuleException("El proveedor respondió con una cadena vacía");
        }
        try {
            JAXBContext context = JAXBContext.newInstance(GestoPagoTxResponseXml.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            return (GestoPagoTxResponseXml) unmarshaller.unmarshal(new StringReader(xml));
        } catch (Exception e) {
            log.error("Error al parsear XML de respuesta de GestoPago: {}. XML: {}", e.getMessage(), xml);
            throw new BusinessRuleException("Respuesta inválida recibida del proveedor de pagos");
        }
    }
}
