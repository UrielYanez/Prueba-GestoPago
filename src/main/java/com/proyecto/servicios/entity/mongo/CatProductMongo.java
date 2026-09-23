package com.proyecto.servicios.entity.mongo;

import com.proyecto.servicios.model.gestopago.ProductStatusEnum;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "cat_products")
public class CatProductMongo {
    @Id
    private String id;
    private String productId;
    private String name;
    private ProductStatusEnum status;

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public ProductStatusEnum getStatus() { return status; }
    public void setStatus(ProductStatusEnum status) { this.status = status; }
}