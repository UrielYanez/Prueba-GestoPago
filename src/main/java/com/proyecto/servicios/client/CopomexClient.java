package com.proyecto.servicios.client;

import com.proyecto.servicios.model.CopomexResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "copomexClient", url = "https://api.copomex.com/query")
public interface CopomexClient {

    @GetMapping("/info_cp/{cp}")
    List<CopomexResponseDto> getInfoByCp(
            @PathVariable("cp") String cp,
            @RequestParam("token") String token
    );
}
