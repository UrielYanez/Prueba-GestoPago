package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.ApiResponse;
import com.proyecto.servicios.model.gestopago.CatProductJsonDto;
import com.proyecto.servicios.model.gestopago.ProductQueryRequest;
import com.proyecto.servicios.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Productos / GestoPago", description = "Consulta y sincronización de catálogos con la API externa de GestoPago")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/getProductList")
    @Operation(summary = "Obtener lista de productos desde MongoDB", description = "Consulta los productos almacenados en la base de datos MongoDB Atlas con su ficha técnica completa.")
    public ResponseEntity<ApiResponse<List<CatProductJsonDto>>> getProductListEndpoint() {
        List<CatProductJsonDto> productos = productService.getProductListFromMongo();
        ApiResponse<List<CatProductJsonDto>> respuesta = ApiResponse.exitoso(
                "Productos obtenidos exitosamente desde MongoDB.",
                productos,
                HttpStatus.OK.value()
        );
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/productos/sincronizar")
    @Operation(summary = "Sincronizar catálogo con MongoDB", description = "Descarga el catálogo desde la API externa de GestoPago y lo guarda en MongoDB Atlas con su ficha técnica completa.")
    public ResponseEntity<ApiResponse<List<CatProductJsonDto>>> sincronizarProductos() {
        productService.syncProductsToMongo();
        List<CatProductJsonDto> productos = productService.getProductListFromMongo();
        ApiResponse<List<CatProductJsonDto>> respuesta = ApiResponse.exitoso(
                "Sincronización completada exitosamente. Total de productos en MongoDB: " + productos.size(),
                productos,
                HttpStatus.OK.value()
        );
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/productos/detalle")
    @Operation(summary = "Obtener detalle o ficha técnica de un producto", description = "Busca en MongoDB Atlas la ficha técnica completa de un producto por su idProducto.")
    public ResponseEntity<ApiResponse<CatProductJsonDto>> obtenerDetalleProducto(@RequestBody ProductQueryRequest request) {
        if (request == null || request.getIdProducto() == null || request.getIdProducto().isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("El idProducto es requerido", HttpStatus.BAD_REQUEST.value()));
        }
        return productService.getProductById(request.getIdProducto().trim())
                .map(p -> ResponseEntity.ok(ApiResponse.exitoso("Ficha técnica obtenida exitosamente de MongoDB.", p, HttpStatus.OK.value())))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Producto no encontrado en catálogo de MongoDB con idProducto: " + request.getIdProducto(), HttpStatus.NOT_FOUND.value())));
    }
}