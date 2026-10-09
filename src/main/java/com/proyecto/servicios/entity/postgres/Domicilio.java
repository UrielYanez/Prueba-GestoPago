package com.proyecto.servicios.entity.postgres;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "domicilios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Domicilio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String calle;
    @Column(name = "numero_exterior")
    private String numeroExterior;
    @Column(name = "numero_interior")
    private String numeroInterior;
    private String colonia;
    private String municipio;
    private String estado;
    @Column(name = "codigo_postal")
    private String codigoPostal;
    
    @Enumerated(EnumType.STRING)
    private PaisEnum pais;
}
