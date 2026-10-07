package com.proyecto.servicios.model.gestopago;

import jakarta.xml.bind.annotation.*;
import java.util.List;

@XmlRootElement(name = "RESPONSE")
@XmlAccessorType(XmlAccessType.FIELD)
public class CatProductXmlDto {

    @XmlElement(name = "PRODUCTOS")
    private ProductosWrapper productosWrapper;

    public List<ProductXmlItem> getProducts() {
        return productosWrapper != null ? productosWrapper.getProductList() : null;
    }
    public void setProducts(List<ProductXmlItem> products) {
        if (this.productosWrapper == null) this.productosWrapper = new ProductosWrapper();
        this.productosWrapper.setProductList(products);
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ProductosWrapper {
        @XmlElement(name = "producto")
        private List<ProductXmlItem> productList;

        public List<ProductXmlItem> getProductList() { return productList; }
        public void setProductList(List<ProductXmlItem> productList) { this.productList = productList; }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ProductXmlItem {
        @XmlAttribute(name = "servicio")
        private String servicio;

        @XmlAttribute(name = "producto")
        private String nombreProducto;

        @XmlAttribute(name = "idServicio")
        private String idServicio;

        @XmlAttribute(name = "idProducto")
        private String idProducto;

        @XmlAttribute(name = "idCatTipoServicio")
        private String idCatTipoServicio;

        @XmlAttribute(name = "precio")
        private String precio;

        @XmlElement(name = "legend")
        private String legend;

        public String getServicio() { return servicio; }
        public void setServicio(String servicio) { this.servicio = servicio; }

        public String getNombreProducto() { return nombreProducto; }
        public void setNombreProducto(String nombreProducto) { this.nombreProducto = nombreProducto; }

        public String getIdServicio() { return idServicio; }
        public void setIdServicio(String idServicio) { this.idServicio = idServicio; }

        public String getIdProducto() { return idProducto; }
        public void setIdProducto(String idProducto) { this.idProducto = idProducto; }

        public String getIdCatTipoServicio() { return idCatTipoServicio; }
        public void setIdCatTipoServicio(String idCatTipoServicio) { this.idCatTipoServicio = idCatTipoServicio; }

        public String getPrecio() { return precio; }
        public void setPrecio(String precio) { this.precio = precio; }

        public String getLegend() { return legend; }
        public void setLegend(String legend) { this.legend = legend; }
    }
}