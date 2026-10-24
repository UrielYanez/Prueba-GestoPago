package com.proyecto.servicios.entity.mongo;

import com.proyecto.servicios.model.gestopago.ProductStatusEnum;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "cat_products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatProductMongo {

    @Id
    private String id;

    private String idProducto;
    private String idServicio;
    private String servicio;
    private String producto;
    private Double precio;
    private String idCatTipoServicio;
    private String tipoFront;
    private Boolean hasDigitoVerificador;
    private Boolean showAyuda;
    private String tipoReferencia;
    private String legend;
    private ProductStatusEnum status;
}