package com.proyecto.servicios.repositorys.mongo;

import com.proyecto.servicios.entity.mongo.CatProductMongo;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CatProductMongoRepository extends MongoRepository<CatProductMongo, String> {
}