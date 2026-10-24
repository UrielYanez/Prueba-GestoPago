package com.proyecto.servicios.repositorys.postgres;

import com.proyecto.servicios.entity.postgres.TransaccionPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransaccionPagoRepository extends JpaRepository<TransaccionPago, Long> {
    Optional<TransaccionPago> findByUpc(String upc);
    Optional<TransaccionPago> findByIdTxProveedor(String idTxProveedor);
}
