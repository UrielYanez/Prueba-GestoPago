package com.proyecto.servicios.repositorys.postgres;

import com.proyecto.servicios.entity.postgres.EstadoTransaccion;
import com.proyecto.servicios.entity.postgres.TransaccionPago;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

@Repository
public interface TransaccionPagoRepository extends JpaRepository<TransaccionPago, Long> {
    Optional<TransaccionPago> findByUpc(String upc);
    Optional<TransaccionPago> findByIdTxProveedor(String idTxProveedor);
    Optional<TransaccionPago> findByIdOperacionCliente(String idOperacionCliente);

    @Query("SELECT t FROM TransaccionPago t LEFT JOIN FETCH t.cuenta c LEFT JOIN FETCH c.cliente WHERE t.upc = :upc")
    Optional<TransaccionPago> findByUpcWithCuentaAndCliente(@Param("upc") String upc);

    @Query("SELECT t FROM TransaccionPago t LEFT JOIN FETCH t.cuenta c LEFT JOIN FETCH c.cliente WHERE t.idOperacionCliente = :idOperacionCliente")
    Optional<TransaccionPago> findByIdOperacionClienteWithCuentaAndCliente(@Param("idOperacionCliente") String idOperacionCliente);

    @Query(
            value = "SELECT t FROM TransaccionPago t JOIN FETCH t.cuenta c JOIN FETCH c.cliente cl " +
                    "WHERE LOWER(cl.correoElectronico) = LOWER(:correo) " +
                    "AND t.estado = :estado " +
                    "AND (:numeroCuenta IS NULL OR c.numeroCuenta = :numeroCuenta) " +
                    "ORDER BY t.fechaTransaccion DESC",
            countQuery = "SELECT COUNT(t) FROM TransaccionPago t JOIN t.cuenta c JOIN c.cliente cl " +
                    "WHERE LOWER(cl.correoElectronico) = LOWER(:correo) " +
                    "AND t.estado = :estado " +
                    "AND (:numeroCuenta IS NULL OR c.numeroCuenta = :numeroCuenta)"
    )
    Page<TransaccionPago> findHistorialPorCorreoCliente(
            @Param("correo") String correo,
            @Param("numeroCuenta") String numeroCuenta,
            @Param("estado") EstadoTransaccion estado,
            Pageable pageable
    );

    @Query("SELECT COUNT(t) > 0 FROM TransaccionPago t WHERE t.cuenta.numeroCuenta = :numeroCuenta " +
           "AND t.idProducto = :idProducto AND t.monto = :monto " +
           "AND ((:telefono IS NULL AND t.telefono IS NULL) OR t.telefono = :telefono) " +
           "AND ((:referencia IS NULL AND t.referencia IS NULL) OR t.referencia = :referencia) " +
           "AND t.estado IN :estados AND t.fechaTransaccion >= :desde")
    boolean existeTransaccionDuplicadaReciente(
            @Param("numeroCuenta") String numeroCuenta,
            @Param("idProducto") String idProducto,
            @Param("monto") BigDecimal monto,
            @Param("telefono") String telefono,
            @Param("referencia") String referencia,
            @Param("estados") Collection<EstadoTransaccion> estados,
            @Param("desde") LocalDateTime desde
    );
}
