package com.proyecto.servicios.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "gestoPagoTransactionClient", url = "${gestopago.auth.url}")
public interface GestoPagoTransactionClient {

    @PostMapping(value = "${gestopago.service.sendTx}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    String sendTx(
            @RequestHeader("Authorization") String bearerToken,
            @RequestHeader("X-API-Key") String apiKey,
            @RequestParam("random") String random,
            @RequestParam("signed") String signed
    );
}
