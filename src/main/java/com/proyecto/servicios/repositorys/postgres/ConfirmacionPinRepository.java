package com.proyecto.servicios.repositorys.postgres;

import com.proyecto.servicios.entity.postgres.ConfirmacionPin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ConfirmacionPinRepository extends JpaRepository<ConfirmacionPin, Long> {

    @Query("SELECT p FROM ConfirmacionPin p WHERE p.cuenta.numeroCuenta = :numeroCuenta AND p.utilizado = false AND p.fechaExpiracion > :ahora ORDER BY p.fechaCreacion DESC LIMIT 1")
    Optional<ConfirmacionPin> findPinActivo(@Param("numeroCuenta") String numeroCuenta, @Param("ahora") LocalDateTime ahora);

    @Modifying
    @Query("UPDATE ConfirmacionPin p SET p.utilizado = true WHERE p.cuenta.id = :cuentaId AND p.utilizado = false")
    void invalidarPinesAnteriores(@Param("cuentaId") Long cuentaId);
}
