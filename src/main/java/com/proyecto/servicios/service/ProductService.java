package com.proyecto.servicios.service;

import com.proyecto.servicios.model.gestopago.CatProductJsonDto;
import java.util.List;
import java.util.Optional;

public interface ProductService {
    // Consulta todo el catálogo desde MongoDB con ficha técnica completa
    List<CatProductJsonDto> getProductListFromMongo();

    // Consulta un producto específico por su idProducto desde MongoDB
    Optional<CatProductJsonDto> getProductById(String idProducto);

    // Consulta productos filtrados por idServicio desde MongoDB
    List<CatProductJsonDto> getProductsByServicio(String idServicio);

    // Consulta a la API externa de GestoPago (usado por el Cron y sincronización)
    List<CatProductJsonDto> fetchProductListFromApi();

    // Sincroniza y guarda el catálogo completo en MongoDB Atlas
    void syncProductsToMongo();
}