package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.gestopago.CatProductJsonDto;
import com.proyecto.servicios.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Product Controller", description = "Endpoints para consulta de productos externos")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/getProductList")
    @Operation(summary = "Obtener lista de productos del servicio externo")
    public ResponseEntity<List<CatProductJsonDto>> getProductListEndpoint() {
        return ResponseEntity.ok(productService.getProductList());
    }
}