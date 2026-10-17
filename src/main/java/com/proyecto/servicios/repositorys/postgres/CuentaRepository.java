package com.proyecto.servicios.repositorys.postgres;

import com.proyecto.servicios.entity.postgres.Cuenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CuentaRepository extends JpaRepository<Cuenta, Long>, JpaSpecificationExecutor<Cuenta> {
    Optional<Cuenta> findByNumeroCuenta(String numeroCuenta);
}
