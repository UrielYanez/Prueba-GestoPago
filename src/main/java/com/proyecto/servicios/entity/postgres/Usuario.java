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

    @Column(name = "session_id_activo", length = 100)
    private String sessionIdActivo;

    @Column(name = "plataforma_activa", length = 20)
    private String plataformaActiva;

    @Column(name = "fecha_inicio_sesion")
    private LocalDateTime fechaInicioSesion;

    @Column(name = "fecha_expiracion_sesion")
    private LocalDateTime fechaExpiracionSesion;

    @Column(name = "totp_secret", length = 64)
    private String totpSecret;

    @Column(name = "totp_habilitado")
    @Builder.Default
    private Boolean totpHabilitado = false;

    @Column(name = "ultimo_codigo_2fa_usado", length = 10)
    private String ultimoCodigo2faUsado;

    @Column(name = "codigo_recuperacion_hash")
    private String codigoRecuperacionHash;

    @Column(name = "expiracion_codigo_recuperacion")
    private LocalDateTime expiracionCodigoRecuperacion;

    @Column(name = "intentos_fallidos_auth")
    @Builder.Default
    private Integer intentosFallidosAuth = 0;

    @Column(name = "bloqueado_hasta")
    private LocalDateTime bloqueadoHasta;

    @Column(name = "ultimo_envio_2fa")
    private LocalDateTime ultimoEnvio2fa;

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
