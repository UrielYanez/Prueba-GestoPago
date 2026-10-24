package com.proyecto.servicios.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "gestoPagoProductFeignClient", url = "${gestopago.auth.url}")
public interface GestoPagoProductFeignClient {

    @GetMapping(value = "${gestopago.service.getProductList}", produces = {"application/xml", "text/xml"})
    String getProductListXml(@RequestHeader("Authorization") String bearerToken);
}
