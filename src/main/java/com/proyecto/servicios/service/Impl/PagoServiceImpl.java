package com.proyecto.servicios.service.Impl;

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
import com.proyecto.servicios.model.gestopago.GestoPagoTxPayload;
import com.proyecto.servicios.model.gestopago.GestoPagoTxResponseXml;
import com.proyecto.servicios.model.gestopago.ProductStatusEnum;
import com.proyecto.servicios.repositorys.mongo.CatProductMongoRepository;
import com.proyecto.servicios.repositorys.postgres.ConfirmacionPinRepository;
import com.proyecto.servicios.repositorys.postgres.CuentaRepository;
import com.proyecto.servicios.repositorys.postgres.TransaccionPagoRepository;
import com.proyecto.servicios.repositorys.postgres.UsuarioRepository;
import com.proyecto.servicios.service.EmailService;
import com.proyecto.servicios.service.GestoPagoTokenService;
import com.proyecto.servicios.service.PagoService;
import com.proyecto.servicios.util.AesEncryptionUtil;
import feign.RetryableException;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.StringReader;
import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Slf4j
public class PagoServiceImpl implements PagoService {

    private static final ZoneId ZONA_MX = ZoneId.of("America/Mexico_City");
    private static final BigDecimal MONTO_MINIMO_SERVICIO_VARIABLE = new BigDecimal("5.00");
    private static final BigDecimal MONTO_MAXIMO_SERVICIO_VARIABLE = new BigDecimal("25000.00");
    private static final int VENTANA_IDEMPOTENCIA_SEGUNDOS = 60;
    private static final Set<String> CATEGORIAS_RECARGA_TELEFONICA = Set.of("1", "2", "3", "4", "6", "12", "13", "17");

    private final CuentaRepository cuentaRepository;
    private final ConfirmacionPinRepository confirmacionPinRepository;
    private final TransaccionPagoRepository transaccionPagoRepository;
    private final CatProductMongoRepository catProductMongoRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final AesEncryptionUtil aesEncryptionUtil;
    private final GestoPagoTransactionClient transactionClient;
    private final GestoPagoTokenService gestoPagoTokenService;
    private final UsuarioRepository usuarioRepository;

    private TransactionTemplate transactionTemplate;

    @Autowired(required = false)
    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        if (transactionManager != null) {
            this.transactionTemplate = new TransactionTemplate(transactionManager);
            this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        }
    }

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

        validarCuentaYClienteAutorizado(cuenta);

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
                .fechaExpiracion(LocalDateTime.now(ZONA_MX).plusMinutes(5))
                .utilizado(false)
                .intentos(0)
                .fechaCreacion(LocalDateTime.now(ZONA_MX))
                .build();

        confirmacionPinRepository.save(nuevoPin);
        log.info("PIN OTP generado y almacenado de forma segura para la cuenta {}: [PROTEGIDO]", cuenta.getNumeroCuenta());

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
    public ComprobantePagoResponseDto procesarPago(ProcesarPagoRequestDto request) {
        log.info("Iniciando procesamiento de pago para cuenta: {}, producto: {}, servicio: {}, monto: {}",
                request.getNumeroCuenta(), request.getIdProducto(), request.getIdServicio(), request.getPrecio());

        validarBanderaObligatoria(request);
        int idProductoInt = parsearIdentificadorNumerico(request.getIdProducto());
        int idServicioInt = parsearIdentificadorNumerico(request.getIdServicio());

        boolean isGiftCard = validarCatalogoYDeterminarGiftCard(request);

        String upc = "UPC" + System.currentTimeMillis();
        String horaLocal = LocalDateTime.now(ZONA_MX).format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String unidad = obtenerUnidadDistribuidor(request.getDistribuidor());

        GestoPagoTxPayload payload = construirPayloadPago(
                request, idProductoInt, idServicioInt, isGiftCard, horaLocal, upc, unidad
        );

        TransaccionPago txReservada = ejecutarEnTransaccion(() -> reservarSaldoYRegistrarEnProceso(
                request, upc, horaLocal, unidad
        ));

        String signed = aesEncryptionUtil.encryptPayload(payload);
        String random = String.valueOf(System.currentTimeMillis());
        GestoPagoToken tokenEntity = obtenerTokenGestoPago();

        String xmlResponse = invocarSendTxConManejoDeErrores(
                request, txReservada, upc, random, signed, tokenEntity.getToken()
        );

        GestoPagoTxResponseXml responseXml = parsearXmlOCompensar(request, txReservada, xmlResponse);

        log.info("Resultado de GestoPago para UPC {}. Código: {}, Mensaje: {}, ID_TX: {}, PIN GiftCard: {}",
                upc,
                responseXml.getCodigo(),
                responseXml.getTexto(),
                responseXml.getIdTx(),
                responseXml.getPinGiftCard() != null ? "[PROTEGIDO]" : "N/A");

        return resolverResultadoSendTx(request, txReservada, upc, responseXml);
    }

    private void validarBanderaObligatoria(ProcesarPagoRequestDto request) {
        if (request.getBandera() == null) {
            throw new BusinessRuleException("La bandera de tipo de operación es obligatoria (true para Recarga, false para Servicio/Tarjeta de Regalo).");
        }
    }

    private int parsearIdentificadorNumerico(String valor) {
        try {
            return Integer.parseInt(valor.trim());
        } catch (Exception e) {
            throw new BusinessRuleException("Los campos 'idProducto' e 'idServicio' deben ser identificadores numéricos válidos.", e);
        }
    }

    private boolean validarCatalogoYDeterminarGiftCard(ProcesarPagoRequestDto request) {
        Optional<CatProductMongo> productOpt = catProductMongoRepository.findByIdProducto(request.getIdProducto().trim());
        if (productOpt.isPresent()) {
            validarProductoYPrecioContraCatalogo(productOpt.get(), request);
        } else if (catProductMongoRepository.count() > 0) {
            throw new ResourceNotFoundException("El producto con ID '" + request.getIdProducto() + "' no existe en el catálogo autorizado de GestoPago.");
        }

        return productOpt
                .map(p -> "10".equals(p.getIdCatTipoServicio()) || "11".equals(p.getIdCatTipoServicio()))
                .orElse(false);
    }

    private String obtenerUnidadDistribuidor(String distribuidor) {
        return (distribuidor != null && !distribuidor.isBlank()) ? distribuidor.trim() : defaultUnidad;
    }

    private GestoPagoTxPayload construirPayloadPago(
            ProcesarPagoRequestDto request,
            int idProductoInt,
            int idServicioInt,
            boolean isGiftCard,
            String horaLocal,
            String upc,
            String unidad
    ) {
        String telefonoGestoPago;
        String referenciaGestoPago;
        BigDecimal montoGestoPago;

        if (Boolean.TRUE.equals(request.getBandera())) {
            if (request.getTelefono() == null || !request.getTelefono().matches("^\\d{10}$")) {
                throw new BusinessRuleException("Para recargas telefónicas (bandera=true), el campo 'telefono' es obligatorio y debe tener exactamente 10 dígitos numéricos.");
            }
            telefonoGestoPago = request.getTelefono().trim();
            referenciaGestoPago = null;
            montoGestoPago = null;
        } else if (isGiftCard) {
            log.info("Producto {} identificado como Tarjeta de Regalo / Entretenimiento. Configurando formato GiftCard GestoPago.",
                    request.getIdProducto());
            telefonoGestoPago = "#1111111111";
            referenciaGestoPago = null;
            montoGestoPago = null;
        } else {
            if (request.getReferencia() == null || request.getReferencia().trim().isEmpty()) {
                throw new BusinessRuleException("Para pagos de servicios (bandera=false), el campo 'referencia' es obligatorio (número de recibo, contrato o código de barras).");
            }
            referenciaGestoPago = request.getReferencia().trim();
            telefonoGestoPago = (request.getTelefono() != null && !request.getTelefono().isBlank())
                    ? request.getTelefono().trim()
                    : "1111111111";
            montoGestoPago = request.getPrecio();
        }

        return GestoPagoTxPayload.builder()
                .idProducto(idProductoInt)
                .idServicio(idServicioInt)
                .referencia(referenciaGestoPago)
                .telefono(telefonoGestoPago)
                .montoPago(montoGestoPago)
                .horaLocal(horaLocal)
                .upc(upc)
                .unidad(unidad)
                .build();
    }

    private String invocarSendTxConManejoDeErrores(
            ProcesarPagoRequestDto request,
            TransaccionPago txReservada,
            String upc,
            String random,
            String signed,
            String token
    ) {
        log.info("Enviando transacción sendTx a GestoPago para UPC: {} (Bandera: {})", upc, request.getBandera());
        try {
            String xmlResponse = transactionClient.sendTx("Bearer " + token, apiKey, random, signed);
            log.info("Respuesta XML recibida de GestoPago para UPC {}: {}", upc, sanitizarXmlParaLog(xmlResponse));
            return xmlResponse;
        } catch (Exception e) {
            if (esExcepcionDeTimeout(e)) {
                log.error("Timeout detectado en sendTx para UPC: {}. Marcando transacción como PENDIENTE_CONCILIACION sin reembolso prematuro.", upc);
                ejecutarEnTransaccion(() -> marcarPendienteConciliacion(
                        txReservada,
                        "82",
                        "Tiempo de espera agotado al comunicar con el proveedor. Pendiente de conciliación."
                ));
                throw new ProviderTimeoutException(
                        "Tiempo de espera agotado al procesar con el proveedor. La operación con UPC '" + upc
                                + "' quedó en estado PENDIENTE_CONCILIACION. Por seguridad no reintente el pago sin antes verificar su estado en /pagos/conciliar.",
                        e
                );
            }
            log.error("Fallo de comunicación con GestoPago sendTx para UPC {}: {}", upc, e.getMessage());
            ejecutarEnTransaccion(() -> compensarSaldoYRechazarTransaccion(
                    txReservada,
                    request.getNumeroCuenta(),
                    request.getPrecio(),
                    "ERR_COM",
                    "Error de comunicación con el proveedor"
            ));
            throw new BusinessRuleException("Error al comunicarse con el proveedor de pagos: " + e.getMessage(), e);
        }
    }

    private GestoPagoTxResponseXml parsearXmlOCompensar(
            ProcesarPagoRequestDto request,
            TransaccionPago txReservada,
            String xmlResponse
    ) {
        try {
            return parseResponseXml(xmlResponse);
        } catch (BusinessRuleException ex) {
            ejecutarEnTransaccion(() -> compensarSaldoYRechazarTransaccion(
                    txReservada,
                    request.getNumeroCuenta(),
                    request.getPrecio(),
                    "ERR_XML",
                    "Respuesta XML inválida del proveedor"
            ));
            throw ex;
        }
    }

    private ComprobantePagoResponseDto resolverResultadoSendTx(
            ProcesarPagoRequestDto request,
            TransaccionPago txReservada,
            String upc,
            GestoPagoTxResponseXml responseXml
    ) {
        if (responseXml.isTimeoutProveedor()) {
            log.warn("GestoPago reportó código 82 (Timeout) para UPC: {}. Pasando a PENDIENTE_CONCILIACION.", upc);
            ejecutarEnTransaccion(() -> marcarPendienteConciliacion(
                    txReservada,
                    responseXml.getCodigo(),
                    responseXml.getTexto()
            ));
            throw new ProviderTimeoutException(
                    "El proveedor reportó tiempo de espera agotado (Código: 82). La transacción con UPC '" + upc
                            + "' quedó en estado PENDIENTE_CONCILIACION. Consulte /pagos/conciliar para verificar si fue aplicada."
            );
        }

        if (!responseXml.isExitoso()) {
            ejecutarEnTransaccion(() -> compensarSaldoYRechazarTransaccion(
                    txReservada,
                    request.getNumeroCuenta(),
                    request.getPrecio(),
                    responseXml.getCodigo(),
                    responseXml.getTexto()
            ));
            String mensajeUsuario = mapearMensajeRechazoProveedor(responseXml.getCodigo(), responseXml.getTexto());
            throw new BusinessRuleException(mensajeUsuario);
        }

        TransaccionPago savedTx = ejecutarEnTransaccion(() -> confirmarTransaccionExitosa(txReservada, responseXml));
        enviarCorreoComprobanteSiAplica(savedTx);
        return construirComprobanteDto(savedTx);
    }

    @Override
    @Transactional
    public ComprobantePagoResponseDto conciliarPago(ConciliarPagoRequestDto request) {
        String upc = request.getUpc().trim();
        log.info("Iniciando conciliación de pago con GestoPago (confirmTx.do) para UPC: {}", upc);

        TransaccionPago transaccion = transaccionPagoRepository.findByUpcWithCuentaAndCliente(upc)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró ninguna transacción registrada con el UPC: " + upc));

        Cuenta cuenta = transaccion.getCuenta();
        validarCuentaYClienteAutorizado(cuenta);

        if (transaccion.getEstado() == EstadoTransaccion.EXITOSA) {
            log.info("La transacción con UPC {} ya se encuentra en estado EXITOSA.", upc);
            return construirComprobanteDto(transaccion);
        }

        if (transaccion.getEstado() == EstadoTransaccion.RECHAZADA) {
            throw new BusinessRuleException("La transacción con UPC '" + upc + "' ya fue conciliada previamente como RECHAZADA y el saldo fue reintegrado a la cuenta.");
        }

        GestoPagoTxPayload payload = construirPayloadConciliacion(transaccion);
        String signed = aesEncryptionUtil.encryptPayload(payload);
        String random = String.valueOf(System.currentTimeMillis());
        GestoPagoToken tokenEntity = obtenerTokenGestoPago();

        String xmlResponse;
        try {
            xmlResponse = transactionClient.confirmTx(
                    "Bearer " + tokenEntity.getToken(),
                    apiKey,
                    random,
                    signed
            );
            log.info("Respuesta XML de confirmTx.do para UPC {}: {}", upc, sanitizarXmlParaLog(xmlResponse));
        } catch (Exception e) {
            log.error("Error al consultar confirmTx.do para UPC {}: {}", upc, e.getMessage());
            throw new ProviderTimeoutException("No fue posible obtener respuesta del servicio de conciliación confirmTx.do. Intente más tarde.", e);
        }

        GestoPagoTxResponseXml responseXml = parseResponseXml(xmlResponse);

        // Código 06 o 01 en confirmTx: La transacción SÍ fue aplicada exitosamente por el proveedor
        if (responseXml.isConfirmadoExitoso()) {
            log.info("Conciliación exitosa (Código {}) para UPC {}: La operación sí fue aplicada por GestoPago.", responseXml.getCodigo(), upc);
            TransaccionPago savedTx = ejecutarEnTransaccion(() -> confirmarTransaccionExitosa(transaccion, responseXml));
            enviarCorreoComprobanteSiAplica(savedTx);
            return construirComprobanteDto(savedTx);
        }

        // Código 70 en confirmTx: La transacción NO fue aplicada por el proveedor -> Reembolsar saldo
        if (responseXml.isNoAplicada()) {
            log.info("Conciliación completada (Código 70) para UPC {}: La operación NO fue aplicada por GestoPago. Reintegrando saldo a cuenta {}.",
                    upc, cuenta.getNumeroCuenta());
            TransaccionPago rejectedTx = ejecutarEnTransaccion(() -> compensarSaldoYRechazarTransaccion(
                    transaccion,
                    cuenta.getNumeroCuenta(),
                    transaccion.getMonto(),
                    responseXml.getCodigo(),
                    responseXml.getTexto()
            ));
            return construirComprobanteDto(rejectedTx);
        }

        throw new BusinessRuleException("El proveedor aún no reporta un estado definitivo para el UPC '" + upc + "': "
                + responseXml.getTexto() + " (Código: " + responseXml.getCodigo() + ")");
    }

    private GestoPagoTxPayload construirPayloadConciliacion(TransaccionPago transaccion) {
        boolean isGiftCard = catProductMongoRepository.findByIdProducto(transaccion.getIdProducto())
                .map(p -> "10".equals(p.getIdCatTipoServicio()) || "11".equals(p.getIdCatTipoServicio()))
                .orElse(false);

        String telefonoGestoPago;
        String referenciaGestoPago;
        BigDecimal montoGestoPago;

        if (Boolean.TRUE.equals(transaccion.getBandera())) {
            telefonoGestoPago = transaccion.getTelefono();
            referenciaGestoPago = null;
            montoGestoPago = null;
        } else if (isGiftCard) {
            telefonoGestoPago = "#1111111111";
            referenciaGestoPago = null;
            montoGestoPago = null;
        } else {
            referenciaGestoPago = transaccion.getReferencia();
            telefonoGestoPago = (transaccion.getTelefono() != null && !transaccion.getTelefono().isBlank())
                    ? transaccion.getTelefono().trim()
                    : "1111111111";
            montoGestoPago = transaccion.getMonto();
        }

        String horaLocal = (transaccion.getHoraLocal() != null && !transaccion.getHoraLocal().isBlank())
                ? transaccion.getHoraLocal()
                : transaccion.getFechaTransaccion().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        return GestoPagoTxPayload.builder()
                .idProducto(Integer.parseInt(transaccion.getIdProducto().trim()))
                .idServicio(Integer.parseInt(transaccion.getIdServicio().trim()))
                .referencia(referenciaGestoPago)
                .telefono(telefonoGestoPago)
                .montoPago(montoGestoPago)
                .horaLocal(horaLocal)
                .upc(transaccion.getUpc())
                .unidad(transaccion.getDistribuidor())
                .build();
    }

    /**
     * Valida en el catálogo de MongoDB que el producto esté activo, que el idServicio coincida
     * y que el monto enviado no haya sido manipulado.
     */
    private void validarProductoYPrecioContraCatalogo(CatProductMongo catProduct, ProcesarPagoRequestDto request) {
        if (catProduct.getStatus() != null && catProduct.getStatus() != ProductStatusEnum.ACTIVE) {
            throw new BusinessRuleException("El producto '" + catProduct.getProducto() + "' no se encuentra activo o disponible para venta.");
        }

        if (catProduct.getIdServicio() != null && !catProduct.getIdServicio().trim().equals(request.getIdServicio().trim())) {
            throw new BusinessRuleException(String.format(
                    "Inconsistencia de catálogo: El idServicio enviado (%s) no corresponde al servicio oficial (%s) del producto '%s'.",
                    request.getIdServicio(), catProduct.getIdServicio(), catProduct.getProducto()
            ));
        }

        validarBanderaContraCategoria(catProduct, request.getBandera());
        validarMontoContraCatalogo(catProduct, request.getPrecio());
    }

    private void validarBanderaContraCategoria(CatProductMongo catProduct, Boolean bandera) {
        String catTipo = catProduct.getIdCatTipoServicio();
        boolean noEsRecarga = "2".equals(catProduct.getTipoFront()) || "10".equals(catTipo) || "11".equals(catTipo);
        if (Boolean.TRUE.equals(bandera) && noEsRecarga) {
            throw new BusinessRuleException("El producto '" + catProduct.getProducto() + "' no corresponde a una recarga telefónica (bandera=true).");
        }
        if (Boolean.FALSE.equals(bandera) && catTipo != null && CATEGORIAS_RECARGA_TELEFONICA.contains(catTipo.trim())) {
            throw new BusinessRuleException("El producto '" + catProduct.getProducto() + "' es una recarga telefónica y debe enviarse con bandera=true.");
        }
    }

    private void validarMontoContraCatalogo(CatProductMongo catProduct, BigDecimal precioSolicitado) {
        boolean esMontoVariable = "2".equals(catProduct.getTipoFront())
                || (catProduct.getPrecio() != null && Double.compare(catProduct.getPrecio(), 0.0) == 0);

        if (esMontoVariable) {
            BigDecimal minPermitido = (catProduct.getPrecio() != null && catProduct.getPrecio() > 0)
                    ? BigDecimal.valueOf(catProduct.getPrecio())
                    : MONTO_MINIMO_SERVICIO_VARIABLE;
            if (precioSolicitado.compareTo(minPermitido) < 0 || precioSolicitado.compareTo(MONTO_MAXIMO_SERVICIO_VARIABLE) > 0) {
                throw new BusinessRuleException(String.format(
                        "El monto solicitado ($%.2f) para el servicio '%s' está fuera de los límites permitidos ($%.2f - $%.2f).",
                        precioSolicitado, catProduct.getProducto(), minPermitido, MONTO_MAXIMO_SERVICIO_VARIABLE
                ));
            }
            return;
        }

        if (catProduct.getPrecio() != null && catProduct.getPrecio() > 0) {
            BigDecimal precioOficial = BigDecimal.valueOf(catProduct.getPrecio());
            if (precioSolicitado.compareTo(precioOficial) != 0) {
                throw new BusinessRuleException(String.format(
                        "Monto inválido o manipulado: El precio enviado ($%.2f) no coincide con el precio oficial en catálogo ($%.2f) para '%s'.",
                        precioSolicitado, precioOficial, catProduct.getProducto()
                ));
            }
        }
    }

    /**
     * Fase 1: Bloqueo pesimista de la cuenta, validación de identidad, idempotencia, PIN OTP y reserva del saldo.
     */
    private TransaccionPago reservarSaldoYRegistrarEnProceso(ProcesarPagoRequestDto request, String upc, String horaLocal, String unidad) {
        Cuenta cuenta = obtenerCuentaConBloqueoPesimista(request.getNumeroCuenta());

        validarCuentaYClienteAutorizado(cuenta);

        // Validar ventana de idempotencia (evitar cargos duplicados en los últimos 60 segundos)
        String telNorm = (request.getTelefono() != null && !request.getTelefono().isBlank()) ? request.getTelefono().trim() : null;
        String refNorm = (request.getReferencia() != null && !request.getReferencia().isBlank()) ? request.getReferencia().trim() : null;
        boolean duplicada = transaccionPagoRepository.existeTransaccionDuplicadaReciente(
                cuenta.getNumeroCuenta(),
                request.getIdProducto().trim(),
                request.getPrecio(),
                telNorm,
                refNorm,
                List.of(EstadoTransaccion.EN_PROCESO, EstadoTransaccion.EXITOSA, EstadoTransaccion.PENDIENTE_CONCILIACION),
                LocalDateTime.now(ZONA_MX).minusSeconds(VENTANA_IDEMPOTENCIA_SEGUNDOS)
        );

        if (duplicada) {
            throw new DuplicatedResourceException(
                    "Operación duplicada detectada: Ya existe una transacción idéntica en curso o procesada en los últimos "
                            + VENTANA_IDEMPOTENCIA_SEGUNDOS + " segundos para esta cuenta y destino."
            );
        }

        // Validar Saldo Suficiente
        if (cuenta.getSaldo().compareTo(request.getPrecio()) < 0) {
            throw new BusinessRuleException(String.format(
                    "Saldo insuficiente. Saldo disponible: $%.2f, Monto requerido: $%.2f",
                    cuenta.getSaldo(), request.getPrecio()
            ));
        }

        // Validar PIN OTP
        ConfirmacionPin pinEntity = confirmacionPinRepository.findPinActivo(request.getNumeroCuenta(), LocalDateTime.now(ZONA_MX))
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

        // Quemar PIN OTP inmediatamente para que no pueda reutilizarse
        pinEntity.setUtilizado(true);
        confirmacionPinRepository.save(pinEntity);

        // Descontar/reservar el saldo bajo el bloqueo pesimista
        cuenta.setSaldo(cuenta.getSaldo().subtract(request.getPrecio()));
        cuentaRepository.save(cuenta);
        log.info("Saldo reservado exitosamente en cuenta {}. Saldo tras reserva: ${}", cuenta.getNumeroCuenta(), cuenta.getSaldo());

        // Registrar transacción en estado EN_PROCESO
        TransaccionPago transaccion = TransaccionPago.builder()
                .cuenta(cuenta)
                .idServicio(request.getIdServicio().trim())
                .idProducto(request.getIdProducto().trim())
                .telefono(telNorm)
                .referencia(refNorm)
                .bandera(request.getBandera())
                .monto(request.getPrecio())
                .upc(upc)
                .horaLocal(horaLocal)
                .distribuidor(unidad)
                .estado(EstadoTransaccion.EN_PROCESO)
                .fechaTransaccion(LocalDateTime.now(ZONA_MX))
                .build();

        TransaccionPago guardada = transaccionPagoRepository.save(transaccion);
        return guardada != null ? guardada : transaccion;
    }

    /**
     * Fase 3A: Consolida la transacción como EXITOSA tras recibir código 01 (o 06 en confirmTx).
     */
    private TransaccionPago confirmarTransaccionExitosa(TransaccionPago transaccion, GestoPagoTxResponseXml responseXml) {
        TransaccionPago tx = (transaccion.getId() != null)
                ? transaccionPagoRepository.findById(transaccion.getId()).orElse(transaccion)
                : transaccion;
        tx.setEstado(EstadoTransaccion.EXITOSA);
        tx.setIdTxProveedor(responseXml.getIdTx());
        tx.setNumAutorizacion(responseXml.getNumAutorizacion());
        tx.setCodigoRespuesta(responseXml.getCodigo());
        tx.setMensajeRespuesta(responseXml.getTexto());
        tx.setSaldoProveedor(responseXml.getSaldo());
        tx.setPinGiftCard(responseXml.getPinGiftCard());
        TransaccionPago saved = transaccionPagoRepository.save(tx);
        return saved != null ? saved : tx;
    }

    /**
     * Fase 3B: Reintegra el saldo reservado a la cuenta y marca la transacción como RECHAZADA.
     */
    private TransaccionPago compensarSaldoYRechazarTransaccion(
            TransaccionPago transaccion,
            String numeroCuenta,
            BigDecimal montoReembolso,
            String codigo,
            String mensaje
    ) {
        Cuenta cuenta = obtenerCuentaConBloqueoPesimista(numeroCuenta);
        cuenta.setSaldo(cuenta.getSaldo().add(montoReembolso));
        cuentaRepository.save(cuenta);
        log.info("Compensación aplicada: Reintegrados ${} a la cuenta {}. Saldo restaurado: ${}",
                montoReembolso, cuenta.getNumeroCuenta(), cuenta.getSaldo());

        transaccion.setCuenta(cuenta);
        transaccion.setEstado(EstadoTransaccion.RECHAZADA);
        transaccion.setCodigoRespuesta(codigo);
        transaccion.setMensajeRespuesta(mensaje);
        TransaccionPago saved = transaccionPagoRepository.save(transaccion);
        return saved != null ? saved : transaccion;
    }

    /**
     * Fase 3C: Marca la transacción como PENDIENTE_CONCILIACION sin liberar el saldo hasta confirmar con confirmTx.do.
     */
    private TransaccionPago marcarPendienteConciliacion(TransaccionPago transaccion, String codigo, String mensaje) {
        transaccion.setEstado(EstadoTransaccion.PENDIENTE_CONCILIACION);
        transaccion.setCodigoRespuesta(codigo);
        transaccion.setMensajeRespuesta(mensaje);
        TransaccionPago saved = transaccionPagoRepository.save(transaccion);
        return saved != null ? saved : transaccion;
    }

    /**
     * Obtiene la cuenta aplicando bloqueo pesimista (SELECT ... FOR UPDATE) para evitar condiciones de carrera.
     */
    private Cuenta obtenerCuentaConBloqueoPesimista(String numeroCuenta) {
        Optional<Cuenta> cuentaLocked = cuentaRepository.findByNumeroCuentaForUpdate(numeroCuenta);
        if (cuentaLocked.isPresent()) {
            return cuentaLocked.get();
        }
        return cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta no encontrada con número: " + numeroCuenta));
    }

    /**
     * Valida que la cuenta esté activa, que el cliente titular esté activo y que el usuario autenticado
     * en el contexto de seguridad (JWT) sea el legítimo titular de la cuenta.
     */
    private void validarCuentaYClienteAutorizado(Cuenta cuenta) {
        if (cuenta.getEstatus() != EstatusCuenta.ACTIVA) {
            throw new BusinessRuleException("La cuenta se encuentra inactiva o bloqueada. Estatus: " + cuenta.getEstatus());
        }

        Cliente cliente = cuenta.getCliente();
        if (cliente != null && Boolean.FALSE.equals(cliente.getActivo())) {
            throw new BusinessRuleException("El cliente titular de la cuenta se encuentra inactivo.");
        }

        Optional<String> correoAutenticadoOpt = obtenerCorreoAutenticado();
        if (correoAutenticadoOpt.isEmpty() || cliente == null) {
            return;
        }

        String correoAutenticado = correoAutenticadoOpt.get();
        if (!esTitularAutorizado(cliente, correoAutenticado)) {
            log.warn("Intento de operación no autorizada: El usuario '{}' intentó operar sobre la cuenta '{}' de otro titular.",
                    correoAutenticado, cuenta.getNumeroCuenta());
            throw new UnauthorizedOperationException("Acceso denegado: No tienes autorización para realizar operaciones sobre la cuenta solicitada.");
        }
    }

    private Optional<String> obtenerCorreoAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null || "anonymousUser".equals(auth.getPrincipal())) {
            return Optional.empty();
        }
        String correo = auth.getName();
        return (correo != null && !correo.isBlank()) ? Optional.of(correo) : Optional.empty();
    }

    private boolean esTitularAutorizado(Cliente cliente, String correoAutenticado) {
        if (cliente.getCorreoElectronico() != null && cliente.getCorreoElectronico().equalsIgnoreCase(correoAutenticado)) {
            return true;
        }
        if (usuarioRepository == null) {
            return false;
        }
        return usuarioRepository.findByCorreo(correoAutenticado)
                .map(usuario -> {
                    if (Boolean.FALSE.equals(usuario.getActivo())) {
                        throw new UnauthorizedOperationException("El usuario autenticado se encuentra inactivo.");
                    }
                    return usuario.getCliente() != null
                            && usuario.getCliente().getId() != null
                            && usuario.getCliente().getId().equals(cliente.getId());
                })
                .orElse(false);
    }

    private GestoPagoToken obtenerTokenGestoPago() {
        return gestoPagoTokenService.obtenerTokenActivo(idDistribuidor, codigoDispositivo)
                .orElseGet(() -> {
                    log.info("Token GestoPago no encontrado en BD. Renovando antes de enviar transacción...");
                    gestoPagoTokenService.renovarToken();
                    return gestoPagoTokenService.obtenerTokenActivo(idDistribuidor, codigoDispositivo)
                            .orElseThrow(() -> new BusinessRuleException("No fue posible obtener un token de autenticación con el proveedor"));
                });
    }

    private void enviarCorreoComprobanteSiAplica(TransaccionPago tx) {
        try {
            Cliente titular = obtenerClienteTitularSeguro(tx.getCuenta());
            if (titular != null && titular.getCorreoElectronico() != null && !titular.getCorreoElectronico().isBlank()) {
                emailService.enviarComprobantePago(titular.getCorreoElectronico(), titular.getNombre(), tx);
                log.info("Comprobante de pago enviado exitosamente por correo a: {}", titular.getCorreoElectronico());
            } else {
                log.warn("No se pudo obtener el correo del titular para la transacción UPC: {}", tx.getUpc());
            }
        } catch (Exception e) {
            log.error("No fue posible enviar el comprobante de pago por correo al titular: {}", e.getMessage());
        }
    }

    private Cliente obtenerClienteTitularSeguro(Cuenta cuenta) {
        if (cuenta == null) {
            return null;
        }
        Cliente cl = extraerClienteDirecto(cuenta);
        if (cl != null && cl.getCorreoElectronico() != null) {
            return cl;
        }
        if (cuenta.getId() != null) {
            return cuentaRepository.findById(cuenta.getId())
                    .map(Cuenta::getCliente)
                    .orElse(null);
        }
        return null;
    }

    private Cliente extraerClienteDirecto(Cuenta cuenta) {
        try {
            return cuenta.getCliente();
        } catch (Exception e) {
            log.debug("Cliente en estado lazy/detached, se consultará desde repositorio: {}", e.getMessage());
            return null;
        }
    }

    private ComprobantePagoResponseDto construirComprobanteDto(TransaccionPago tx) {
        String numCuenta = null;
        BigDecimal saldo = null;
        try {
            if (tx.getCuenta() != null) {
                numCuenta = tx.getCuenta().getNumeroCuenta();
                saldo = tx.getCuenta().getSaldo();
            }
        } catch (Exception ex) {
            log.warn("Cuenta en estado lazy/detached al construir comprobante, consultando BD: {}", ex.getMessage());
            if (tx.getCuenta() != null && tx.getCuenta().getId() != null) {
                Cuenta c = cuentaRepository.findById(tx.getCuenta().getId()).orElse(null);
                if (c != null) {
                    numCuenta = c.getNumeroCuenta();
                    saldo = c.getSaldo();
                }
            }
        }

        String codigoSanitizado = sanitizarCodigoRespuesta(tx.getCodigoRespuesta());
        String mensajeSanitizado = sanitizarMensajeRespuesta(tx.getMensajeRespuesta(), tx.getCodigoRespuesta());

        return ComprobantePagoResponseDto.builder()
                .transaccionId(tx.getId())
                .numeroCuenta(numCuenta)
                .idTxProveedor(tx.getIdTxProveedor())
                .numAutorizacion(tx.getNumAutorizacion())
                .upc(tx.getUpc())
                .bandera(tx.getBandera())
                .telefono(tx.getTelefono())
                .referencia(tx.getReferencia())
                .pinGiftCard(tx.getPinGiftCard())
                .monto(tx.getMonto())
                .nuevoSaldoCuenta(saldo)
                .codigoRespuesta(codigoSanitizado)
                .mensajeRespuesta(mensajeSanitizado)
                .estadoTransaccion(tx.getEstado() != null ? tx.getEstado().name() : EstadoTransaccion.EXITOSA.name())
                .fechaTransaccion(tx.getFechaTransaccion())
                .build();
    }

    private String sanitizarCodigoRespuesta(String codigo) {
        if ("01".equals(codigo)) {
            return "APROBADO";
        }
        if ("06".equals(codigo)) {
            return "CONCILIADO_APROBADO";
        }
        if ("70".equals(codigo)) {
            return "RECHAZADO";
        }
        return (codigo != null && !codigo.isBlank()) ? codigo : "APROBADO";
    }

    private String sanitizarMensajeRespuesta(String mensaje, String codigo) {
        if ("01".equals(codigo) || "06".equals(codigo)) {
            return "Operación realizada con éxito";
        }
        if ("70".equals(codigo)) {
            return "Operación rechazada por el proveedor. El saldo fue reembolsado a su cuenta.";
        }
        if (mensaje != null && !mensaje.isBlank()) {
            return mensaje;
        }
        return "Operación procesada";
    }

    private String mapearMensajeRechazoProveedor(String codigo, String texto) {
        if ("-1".equals(codigo)) {
            return "La operación fue declinada por la compañía proveedora (número telefónico o referencia no reconocida por la red del operador). El saldo retenido ha sido reembolsado íntegramente a su cuenta.";
        }
        if ("55".equals(codigo)) {
            return "La referencia o número de servicio no fue encontrada por el proveedor o no presenta saldo pendiente. El saldo retenido ha sido reembolsado íntegramente a su cuenta.";
        }
        if ("70".equals(codigo)) {
            return "La transacción no fue aplicada por el proveedor de servicios. El saldo retenido ha sido reembolsado íntegramente a su cuenta.";
        }
        if (texto != null && !texto.trim().isEmpty()) {
            return "La transacción fue declinada por el proveedor: " + texto.trim() + ". El saldo retenido ha sido reembolsado íntegramente a su cuenta.";
        }
        return "La transacción no pudo ser completada por la red del proveedor de servicios. El saldo retenido ha sido reembolsado íntegramente a su cuenta.";
    }

    private <T> T ejecutarEnTransaccion(Supplier<T> action) {
        if (transactionTemplate != null) {
            return transactionTemplate.execute(status -> action.get());
        }
        return action.get();
    }

    private boolean esExcepcionDeTimeout(Throwable e) {
        Throwable actual = e;
        while (actual != null) {
            if (actual instanceof SocketTimeoutException
                    || actual instanceof TimeoutException
                    || actual instanceof RetryableException) {
                return true;
            }
            String msg = actual.getMessage();
            if (msg != null && (msg.toLowerCase().contains("timed out") || msg.toLowerCase().contains("timeout"))) {
                return true;
            }
            actual = actual.getCause();
        }
        return false;
    }

    private String sanitizarXmlParaLog(String xml) {
        if (xml == null) {
            return "null";
        }
        return xml.replaceAll("(?i)<PIN>.*?</PIN>", "<PIN>[PROTEGIDO]</PIN>");
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
            log.error("Error al parsear XML de respuesta de GestoPago: {}", e.getMessage());
            throw new BusinessRuleException("Respuesta inválida recibida del proveedor de pagos");
        }
    }
}
