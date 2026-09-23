package com.proyecto.servicios.service;

import com.proyecto.servicios.model.gestopago.CatProductJsonDto;
import java.util.List;

public interface ProductService {
    List<CatProductJsonDto> getProductList();
    void syncProductsToMongo();
}