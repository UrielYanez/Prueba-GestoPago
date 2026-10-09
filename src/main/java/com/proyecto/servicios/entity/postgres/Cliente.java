package com.proyecto.servicios.entity.postgres;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "clientes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(name = "segundo_nombre")
    private String segundoNombre;

    @Column(name = "apellido_paterno", nullable = false)
    private String apellidoPaterno;

    @Column(name = "apellido_materno", nullable = false)
    private String apellidoMaterno;

    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    @Column(unique = true, nullable = false, length = 18)
    private String curp;

    @Column(unique = true, nullable = false, length = 13)
    private String rfc;

    @Enumerated(EnumType.STRING)
    private SexoEnum sexo;

    @Enumerated(EnumType.STRING)
    private NacionalidadEnum nacionalidad;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_civil")
    private EstadoCivilEnum estadoCivil;

    @Column(name = "correo_electronico", unique = true, nullable = false)
    private String correoElectronico;

    @Column(name = "telefono_movil", nullable = false)
    private String telefonoMovil;

    @Column(name = "telefono_alternativo")
    private String telefonoAlternativo;

    private String ocupacion;
    private String empresa;

    @Column(name = "ingreso_mensual")
    private java.math.BigDecimal ingresoMensual;

    private Boolean activo;

    @Column(name = "fecha_creacion")
    private java.time.LocalDateTime fechaCreacion;

    @PrePersist
    protected void onCreate() {
        if (fechaCreacion == null) {
            fechaCreacion = java.time.LocalDateTime.now();
        }
    }

    // Relaciones
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "domicilio_id", referencedColumnName = "id")
    private Domicilio domicilio;

    @OneToOne(mappedBy = "cliente", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Usuario usuario;

    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Cuenta> cuentas;
}
