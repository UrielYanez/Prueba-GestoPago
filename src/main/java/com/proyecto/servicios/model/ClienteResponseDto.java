package com.proyecto.servicios.model;

import com.proyecto.servicios.entity.postgres.EstadoCivilEnum;
import com.proyecto.servicios.entity.postgres.NacionalidadEnum;
import com.proyecto.servicios.entity.postgres.SexoEnum;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
public class ClienteResponseDto {
    private Long id;
    private String nombre;
    private String segundoNombre;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private LocalDate fechaNacimiento;
    private String curp;
    private String rfc;
    private SexoEnum sexo;
    private NacionalidadEnum nacionalidad;
    private EstadoCivilEnum estadoCivil;
    private String correoElectronico;
    private String telefonoMovil;
    private String telefonoAlternativo;
    private String ocupacion;
    private String empresa;
    private BigDecimal ingresoMensual;
    private Boolean activo;

    // Datos del domicilio
    private String calle;
    private String numeroExterior;
    private String numeroInterior;
    private String colonia;
    private String municipio;
    private String estado;
    private String codigoPostal;

    // Cuentas asociadas
    private List<CuentaResponseDto> cuentas;
}
