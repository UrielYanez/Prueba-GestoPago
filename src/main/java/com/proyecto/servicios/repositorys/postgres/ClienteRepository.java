package com.proyecto.servicios.repositorys.postgres;

import com.proyecto.servicios.entity.postgres.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long>, JpaSpecificationExecutor<Cliente> {
    Optional<Cliente> findByCurp(String curp);
    Optional<Cliente> findByRfc(String rfc);
    Optional<Cliente> findByCorreoElectronico(String correoElectronico);
    
    // Búsquedas flexibles por prefijo
    Iterable<Cliente> findByCurpStartingWith(String curp);
    Iterable<Cliente> findByRfcStartingWith(String rfc);
    
    // Búsquedas flexibles por nombre y apellidos
    Iterable<Cliente> findByNombreContainingIgnoreCase(String nombre);
    Iterable<Cliente> findByApellidoPaternoContainingIgnoreCase(String apellidoPaterno);
    Iterable<Cliente> findByApellidoMaternoContainingIgnoreCase(String apellidoMaterno);
}
