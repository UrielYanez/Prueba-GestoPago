package com.proyecto.servicios.model.gestopago;

public class CatProductJsonDto {
    private String id;
    private String name;
    private ProductStatusEnum status;

    public CatProductJsonDto(String id, String name, ProductStatusEnum status) {
        this.id = id;
        this.name = name;
        this.status = status;
    }
    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public ProductStatusEnum getStatus() { return status; }
    public void setStatus(ProductStatusEnum status) { this.status = status; }
}