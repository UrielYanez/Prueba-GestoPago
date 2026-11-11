package com.proyecto.servicios.entity.postgres;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    private static final ZoneId ZONA_MX = ZoneId.of("America/Mexico_City");
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String correo;

    @Column(nullable = false)
    private String password;

    private Boolean activo;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now(ZONA_MX);
        fechaActualizacion = LocalDateTime.now(ZONA_MX);
    }

    @PreUpdate
    protected void onUpdate() {
        fechaActualizacion = LocalDateTime.now(ZONA_MX);
    }
}
