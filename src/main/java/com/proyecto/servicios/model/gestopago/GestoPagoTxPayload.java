package com.proyecto.servicios.model.gestopago;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GestoPagoTxPayload {
    private Integer idProducto;
    private Integer idServicio;
    private String referencia;
    private BigDecimal montoPago;
    private String telefono;
    private String horaLocal; // Formato yyyyMMddHHmmss
    private String upc;       // Código único correlativo
    private String unidad;    // Distribuidor (e.g. 083)
}
