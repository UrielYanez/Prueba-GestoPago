package com.proyecto.servicios.client;

import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.model.gestopago.CatProductXmlDto;
import com.proyecto.servicios.service.GestoPagoTokenService;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;

import java.io.StringReader;

@Component
public class GestoPagoProductClient {
    private static final Logger log = LoggerFactory.getLogger(GestoPagoProductClient.class);
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${gestopago.auth.url}")
    private String baseUrl;

    @Value("${gestopago.service.getProductList}")
    private String endpointPath;

    @Value("${gestopago.auth.id-distribuidor}")
    private Integer idDistribuidor;

    @Value("${gestopago.auth.codigo-dispositivo}")
    private String codigoDispositivo;

    @Autowired
    private GestoPagoTokenService gestoPagoTokenService;

    @Autowired
    private GestoPagoProductFeignClient productFeignClient;

    public CatProductXmlDto getProductListXml() {
        String url = baseUrl + endpointPath;
        log.info("Iniciando invocación Feign a {}", url);
        try {
            // Obtenemos el token activo de base de datos o forzamos renovación inicial si aún no existe
            GestoPagoToken tokenEntity = gestoPagoTokenService.obtenerTokenActivo(idDistribuidor, codigoDispositivo)
                    .orElseGet(() -> {
                        log.info("No se encontró token activo de GestoPago en BD. Forzando autenticación...");
                        gestoPagoTokenService.renovarToken();
                        return gestoPagoTokenService.obtenerTokenActivo(idDistribuidor, codigoDispositivo)
                                .orElseThrow(() -> new RuntimeException("No fue posible obtener un token activo de GestoPago"));
                    });

            String bearerToken = tokenEntity.getToken();
            String xmlResponse = productFeignClient.getProductListXml("Bearer " + bearerToken);

            if (xmlResponse != null && !xmlResponse.isBlank()) {
                log.info("Fin de invocación Feign exitosa. Parseando XML...");
                JAXBContext context = JAXBContext.newInstance(CatProductXmlDto.class);
                Unmarshaller unmarshaller = context.createUnmarshaller();
                return (CatProductXmlDto) unmarshaller.unmarshal(new StringReader(xmlResponse));
            } else {
                log.warn("Respuesta vacía del servicio externo Feign");
                throw new RuntimeException("Respuesta vacía del servicio externo");
            }
        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("Error de autenticación 401 con servicio externo.");
            throw new RuntimeException("Error de autenticación con el proveedor externo", e);
        } catch (HttpClientErrorException.Forbidden e) {
            log.error("Error 403 Forbidden: token expirado o sin permisos.");
            throw new RuntimeException("Token expirado o denegado por el proveedor", e);
        } catch (ResourceAccessException e) {
            log.error("Timeout de comunicación con servicio externo: {}", e.getMessage());
            throw new RuntimeException("Timeout al conectar con el servicio externo", e);
        } catch (RestClientResponseException e) {
            log.error("Respuesta no exitosa HTTP {}: sin exponer secretos", e.getStatusCode());
            throw new RuntimeException("Falla en respuesta del servicio externo (HTTP " + e.getStatusCode() + ")", e);
        } catch (Exception e) {
            log.error("Error de comunicación genérico con servicio externo sin exponer secreto", e);
            throw new RuntimeException("Error de comunicación con el servicio externo", e);
        }
    }
}