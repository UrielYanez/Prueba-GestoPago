package com.proyecto.servicios.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO para actualización parcial de cliente vía @RequestBody (PATCH/PUT).
 * No incluye CURP, RFC ni numeroCuenta, ya que esos campos son inmutables y controlados por el servidor.
 */
@Getter
@Setter
public class ClienteActualizacionDto {

    @NotNull(message = "El ID del cliente es obligatorio para la actualización")
    private Long id;

    private String nombre;
    private String segundoNombre;
    private String apellidoPaterno;
    private String apellidoMaterno;

    @Email(message = "El correo no tiene un formato válido")
    private String correoElectronico;

    @Pattern(regexp = "^\\d{10}$", message = "El teléfono móvil debe tener exactamente 10 dígitos")
    private String telefonoMovil;

    private String telefonoAlternativo;
    private String ocupacion;
    private String empresa;
    private BigDecimal ingresoMensual;

    // Campos de domicilio actualizables
    private String calle;
    private String numeroExterior;
    private String numeroInterior;
    private String colonia;
    private String municipio;
    private String estado;

    @Pattern(regexp = "^\\d{5}$", message = "El código postal debe tener exactamente 5 dígitos")
    private String codigoPostal;
}
