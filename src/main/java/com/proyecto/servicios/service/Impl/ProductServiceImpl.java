package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoProductClient;
import com.proyecto.servicios.entity.mongo.CatProductMongo;
import com.proyecto.servicios.model.gestopago.CatProductJsonDto;
import com.proyecto.servicios.model.gestopago.CatProductXmlDto;
import com.proyecto.servicios.model.gestopago.ProductStatusEnum;
import com.proyecto.servicios.repositorys.mongo.CatProductMongoRepository;
import com.proyecto.servicios.service.ProductService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    private final GestoPagoProductClient client;
    private final CatProductMongoRepository mongoRepository;

    public ProductServiceImpl(GestoPagoProductClient client, CatProductMongoRepository mongoRepository) {
        this.client = client;
        this.mongoRepository = mongoRepository;
    }

    @Override
    public List<CatProductJsonDto> getProductList() {
        CatProductXmlDto xmlDto = client.getProductListXml();
        if (xmlDto == null || xmlDto.getProducts() == null) {
            return List.of();
        }
        return xmlDto.getProducts().stream().map(p -> {
            String idFinal = p.getIdProducto() != null ? p.getIdProducto() : p.getIdServicio();
            String nombreFinal = p.getNombreProducto() != null ? p.getNombreProducto() : p.getServicio();
            return new CatProductJsonDto(idFinal, nombreFinal, ProductStatusEnum.ACTIVE);
        }).collect(Collectors.toList());
    }

    @Override
    public void syncProductsToMongo() {
        List<CatProductJsonDto> list = getProductList();
        mongoRepository.deleteAll();
        List<CatProductMongo> mongoList = list.stream().map(dto -> {
            CatProductMongo m = new CatProductMongo();
            m.setProductId(dto.getId());
            m.setName(dto.getName());
            m.setStatus(dto.getStatus());
            return m;
        }).collect(Collectors.toList());
        mongoRepository.saveAll(mongoList);
    }
}