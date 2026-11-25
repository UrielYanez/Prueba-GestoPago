package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoAuthClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.mapper.GestoPagoTokenMapper;
import com.proyecto.servicios.model.gestopago.GestoPagoAuthResponse;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoTokenRepository;
import com.proyecto.servicios.service.GestoPagoTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

@Service
@Slf4j
public class GestoPagoTokenServiceImpl implements GestoPagoTokenService {

    private static final ZoneId ZONA_MX = ZoneId.of("America/Mexico_City");

    private final GestoPagoAuthClient gestoPagoAuthClient;
    private final GestoPagoTokenRepository tokenRepository;
    private final GestoPagoTokenMapper tokenMapper;

    @Value("${gestopago.auth.id-distribuidor}")
    private Integer idDistribuidor;

    @Value("${gestopago.auth.codigo-dispositivo}")
    private String codigoDispositivo;

    @Value("${gestopago.auth.password}")
    private String password;

    @Value("${gestopago.auth.token-validity-hours:23}")
    private long tokenValidityHours = 23;

    public GestoPagoTokenServiceImpl(GestoPagoAuthClient gestoPagoAuthClient,
                                     GestoPagoTokenRepository tokenRepository,
                                     GestoPagoTokenMapper tokenMapper) {
        this.gestoPagoAuthClient = gestoPagoAuthClient;
        this.tokenRepository = tokenRepository;
        this.tokenMapper = tokenMapper;
    }

    @Override
    @Scheduled(fixedRateString = "${gestopago.auth.refresh-rate-ms:3600000}", initialDelay = 0)
    public void renovarToken() {
        Optional<GestoPagoToken> tokenActual = tokenRepository
                .findByIdDistribuidorAndCodigoDispositivo(idDistribuidor, codigoDispositivo);
        if (tokenActual.isPresent() && esTokenVigente(tokenActual.get())) {
            log.info("Token GestoPago vigente en BD (actualizado: {}). Se reutiliza para respetar el límite de 1 token por día.",
                    tokenActual.get().getFechaActualizacion());
            return;
        }

        log.info("Renovando token GestoPago para distribuidor={}", idDistribuidor);
        try {
            GestoPagoAuthResponse response = gestoPagoAuthClient.authenticate(
                    idDistribuidor, codigoDispositivo, password);

            if (response == null || response.getToken() == null) {
                log.error("La respuesta de GestoPago no contiene token");
                return;
            }

            LocalDateTime ahoraMx = LocalDateTime.now(ZONA_MX);
            GestoPagoToken tokenEntity = tokenRepository
                    .findByIdDistribuidorAndCodigoDispositivo(idDistribuidor, codigoDispositivo)
                    .map(existing -> {
                        tokenMapper.updateEntity(response, existing);
                        existing.setActivo(true);
                        existing.setFechaActualizacion(ahoraMx);
                        return existing;
                    })
                    .orElseGet(() -> {
                        GestoPagoToken nuevo = tokenMapper.toEntity(response);
                        nuevo.setIdDistribuidor(idDistribuidor);
                        nuevo.setCodigoDispositivo(codigoDispositivo);
                        nuevo.setActivo(true);
                        nuevo.setFechaActualizacion(ahoraMx);
                        return nuevo;
                    });

            tokenRepository.save(tokenEntity);
            log.info("Token GestoPago renovado correctamente");

        } catch (Exception e) {
            log.error("Error al renovar token GestoPago: {}", e.getMessage(), e);
        }
    }

    @Override
    public Optional<GestoPagoToken> obtenerTokenActivo(Integer idDistribuidor, String codigoDispositivo) {
        return tokenRepository.findByIdDistribuidorAndCodigoDispositivo(idDistribuidor, codigoDispositivo)
                .filter(token -> esTokenValido(token, true));
    }

    private boolean esTokenVigente(GestoPagoToken tokenEntity) {
        return esTokenValido(tokenEntity, false);
    }

    private boolean esTokenValido(GestoPagoToken tokenEntity, boolean permitirSinFecha) {
        if (tokenEntity == null
                || Boolean.FALSE.equals(tokenEntity.getActivo())
                || tokenEntity.getToken() == null
                || tokenEntity.getToken().isBlank()) {
            return false;
        }
        LocalDateTime fechaRef = tokenEntity.getFechaActualizacion() != null
                ? tokenEntity.getFechaActualizacion()
                : tokenEntity.getFechaCreacion();
        if (fechaRef == null) {
            return permitirSinFecha;
        }
        long vigenciaHoras = tokenValidityHours > 0 ? tokenValidityHours : 23;
        return fechaRef.plusHours(vigenciaHoras).isAfter(LocalDateTime.now(ZONA_MX));
    }
}