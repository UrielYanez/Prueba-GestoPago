package com.proyecto.servicios.service;

import com.proyecto.servicios.model.PaginatedResponse;
import com.proyecto.servicios.model.gestopago.CatProductJsonDto;
import com.proyecto.servicios.model.gestopago.ProductoBusquedaRequestDto;

import java.util.List;
import java.util.Optional;

public interface ProductService {

    List<CatProductJsonDto> getProductListFromMongo();

    PaginatedResponse<CatProductJsonDto> buscarProductosPaginado(ProductoBusquedaRequestDto request);

    Optional<CatProductJsonDto> getProductById(String idProducto);

    List<CatProductJsonDto> getProductsByServicio(String idServicio);

    List<CatProductJsonDto> fetchProductListFromApi();

    void syncProductsToMongo();

    boolean hasProductsInMongo();
}
