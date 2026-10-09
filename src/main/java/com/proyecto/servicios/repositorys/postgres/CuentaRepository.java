package com.proyecto.servicios.repositorys.postgres;

import com.proyecto.servicios.entity.postgres.Cuenta;
import com.proyecto.servicios.entity.postgres.EstatusCuenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CuentaRepository extends JpaRepository<Cuenta, Long>, JpaSpecificationExecutor<Cuenta> {
    Optional<Cuenta> findByNumeroCuenta(String numeroCuenta);
    List<Cuenta> findByClienteId(Long clienteId);
    List<Cuenta> findByEstatus(EstatusCuenta estatus);
}
