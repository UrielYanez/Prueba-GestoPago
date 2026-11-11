package com.proyecto.servicios.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CopomexInfoResponse {
    private String cp;
    private String asentamiento; // colonia
    @JsonProperty("tipo_asentamiento")
    private String tipoAsentamiento;
    private String municipio;
    private String estado;
    private String ciudad;
    private String pais;
}
