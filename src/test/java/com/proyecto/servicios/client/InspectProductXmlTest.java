package com.proyecto.servicios.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.util.AesEncryptionUtil;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Disabled("Prueba manual en vivo contra GestoPago")
public class InspectProductXmlTest {

    @Test
    void testSendTxLive() throws Exception {
        RestTemplate restTemplate = new RestTemplate();

        // 1. Authenticate
        String authUrl = "https://gestopago.portalventas.net/sistema/app/jwt-gp/authenticate/?idDistribuidor=83&codigoDispositivo=GPS83-TPV-17&password=12345678";
        ResponseEntity<Map> authResponse = restTemplate.postForEntity(authUrl, null, Map.class);
        String token = (String) authResponse.getBody().get("token");
        System.out.println("TOKEN OBTENIDO: " + (token != null ? token.substring(0, 20) + "..." : "null"));

        // 2. Prepare JSON payload with Integers
        Map<String, Object> payloadMap = new LinkedHashMap<>();
        payloadMap.put("idProducto", 381); // Integer
        payloadMap.put("idServicio", 124); // Integer
        payloadMap.put("telefono", "5512345678");
        payloadMap.put("horaLocal", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        payloadMap.put("upc", "UPC" + System.currentTimeMillis());
        payloadMap.put("unidad", "083");

        ObjectMapper mapper = new ObjectMapper();
        String jsonPayload = mapper.writeValueAsString(payloadMap);
        System.out.println("JSON PAYLOAD A CIFRAR: " + jsonPayload);

        // 3. Encrypt AES
        AesEncryptionUtil aesUtil = new AesEncryptionUtil("HPo7OLqB4Fkk4E2yGOtwqw8H5fHR9kNx67OR4g4UdlA=", "p5ldmBPdd/9pjC0bDC/nSg==");
        String signed = aesUtil.encrypt(jsonPayload);
        System.out.println("SIGNED: " + signed);

        // 4. Send to sendTx.do
        String sendTxUrl = "https://gestopago.portalventas.net/sistema/service/sendTx.do";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Bearer " + token);
        headers.set("X-API-Key", "e8248386b976dbec8aa742490df1efad36e1c4ec8663ee839556858e6e584f27");

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("random", String.valueOf(System.currentTimeMillis()));
        form.add("signed", signed);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);
        ResponseEntity<String> txResponse = restTemplate.postForEntity(sendTxUrl, request, String.class);

        System.out.println("=== RESPUESTA XML DE GESTOPAGO ===");
        System.out.println(txResponse.getBody());
    }
}
