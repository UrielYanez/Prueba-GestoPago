package com.proyecto.servicios.model;

import lombok.Data;

@Data
public class CopomexInfoResponse {
    private String cp;
    private String asentamiento; // colonia
    private String tipo_asentamiento;
    private String municipio;
    private String estado;
    private String ciudad;
    private String pais;
}
