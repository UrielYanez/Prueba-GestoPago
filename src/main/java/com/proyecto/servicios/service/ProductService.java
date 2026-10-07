package com.proyecto.servicios.service;

import com.proyecto.servicios.model.gestopago.CatProductJsonDto;
import java.util.List;

public interface ProductService {
    // Método para que el controlador lea rápido desde MongoDB
    List<CatProductJsonDto> getProductListFromMongo();
    
    // Método que consulta a la API de GestoPago (usado por el Cron)
    List<CatProductJsonDto> fetchProductListFromApi();
    
    void syncProductsToMongo();
}