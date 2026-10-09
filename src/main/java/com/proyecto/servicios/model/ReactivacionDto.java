package com.proyecto.servicios.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ReactivacionDto {
    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El formato del correo es inválido")
    private String correo;

    @NotBlank(message = "La contraseña es obligatoria")
    private String password;

    @NotBlank(message = "La CURP es obligatoria")
    private String curp;

    @NotBlank(message = "El RFC es obligatorio")
    private String rfc;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    private LocalDate fechaNacimiento;
}
