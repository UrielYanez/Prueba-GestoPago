package com.proyecto.servicios.entity.postgres;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transacciones_pago")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransaccionPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private Cuenta cuenta;

    @Column(name = "id_servicio", nullable = false, length = 50)
    private String idServicio;

    @Column(name = "id_producto", nullable = false, length = 50)
    private String idProducto;

    @Column(length = 20)
    private String telefono;

    @Column(length = 100)
    private String referencia;

    @Column(nullable = false)
    @Builder.Default
    private Boolean bandera = true;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false, unique = true, length = 50)
    private String upc;

    @Column(nullable = false, length = 20)
    private String distribuidor;

    @Column(name = "id_tx_proveedor", length = 100)
    private String idTxProveedor;

    @Column(name = "num_autorizacion", length = 100)
    private String numAutorizacion;

    @Column(name = "codigo_respuesta", length = 20)
    private String codigoRespuesta;

    @Column(name = "mensaje_respuesta", length = 255)
    private String mensajeRespuesta;

    @Column(name = "saldo_proveedor", precision = 19, scale = 2)
    private BigDecimal saldoProveedor;

    @Column(name = "pin_gift_card", length = 250)
    private String pinGiftCard;

    @Column(name = "fecha_transaccion", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime fechaTransaccion = LocalDateTime.now();
}
