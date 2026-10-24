package com.proyecto.servicios.repositorys.mongo;

import com.proyecto.servicios.entity.mongo.CatProductMongo;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CatProductMongoRepository extends MongoRepository<CatProductMongo, String> {
    Optional<CatProductMongo> findByIdProducto(String idProducto);
    List<CatProductMongo> findByIdServicio(String idServicio);
    List<CatProductMongo> findByIdCatTipoServicio(String idCatTipoServicio);
}