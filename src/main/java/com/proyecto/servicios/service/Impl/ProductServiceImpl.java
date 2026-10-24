package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoProductClient;
import com.proyecto.servicios.entity.mongo.CatProductMongo;
import com.proyecto.servicios.model.gestopago.CatProductJsonDto;
import com.proyecto.servicios.model.gestopago.CatProductXmlDto;
import com.proyecto.servicios.model.gestopago.ProductStatusEnum;
import com.proyecto.servicios.repositorys.mongo.CatProductMongoRepository;
import com.proyecto.servicios.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProductServiceImpl implements ProductService {

    private final GestoPagoProductClient client;
    private final CatProductMongoRepository mongoRepository;

    public ProductServiceImpl(GestoPagoProductClient client, CatProductMongoRepository mongoRepository) {
        this.client = client;
        this.mongoRepository = mongoRepository;
    }

    @Override
    public List<CatProductJsonDto> getProductListFromMongo() {
        return mongoRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<CatProductJsonDto> getProductById(String idProducto) {
        if (idProducto == null || idProducto.isBlank()) {
            return Optional.empty();
        }
        return mongoRepository.findByIdProducto(idProducto.trim())
                .map(this::mapToDto);
    }

    @Override
    public List<CatProductJsonDto> getProductsByServicio(String idServicio) {
        if (idServicio == null || idServicio.isBlank()) {
            return List.of();
        }
        return mongoRepository.findByIdServicio(idServicio.trim()).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<CatProductJsonDto> fetchProductListFromApi() {
        CatProductXmlDto xmlDto = client.getProductListXml();
        if (xmlDto == null || xmlDto.getProducts() == null) {
            return List.of();
        }
        return xmlDto.getProducts().stream().map(p -> {
            Double precioParsed = null;
            if (p.getPrecio() != null && !p.getPrecio().isBlank()) {
                try {
                    precioParsed = Double.parseDouble(p.getPrecio().trim());
                } catch (NumberFormatException ignored) {}
            }
            return CatProductJsonDto.builder()
                    .idProducto(p.getIdProducto())
                    .idServicio(p.getIdServicio())
                    .servicio(p.getServicio())
                    .producto(p.getNombreProducto())
                    .precio(precioParsed)
                    .idCatTipoServicio(p.getIdCatTipoServicio())
                    .tipoFront(p.getTipoFront())
                    .hasDigitoVerificador(p.getHasDigitoVerificador())
                    .showAyuda(p.getShowAyuda())
                    .tipoReferencia(p.getTipoReferencia())
                    .legend(p.getLegend())
                    .status(ProductStatusEnum.ACTIVE)
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public void syncProductsToMongo() {
        List<CatProductJsonDto> list = fetchProductListFromApi();
        if (list == null || list.isEmpty()) {
            log.warn("El catálogo devuelto por GestoPago está vacío o no pudo descargarse. Se conservan los datos previos en Mongo.");
            return;
        }

        mongoRepository.deleteAll();

        List<CatProductMongo> mongoList = list.stream().map(dto ->
                CatProductMongo.builder()
                        .idProducto(dto.getIdProducto())
                        .idServicio(dto.getIdServicio())
                        .servicio(dto.getServicio())
                        .producto(dto.getProducto())
                        .precio(dto.getPrecio())
                        .idCatTipoServicio(dto.getIdCatTipoServicio())
                        .tipoFront(dto.getTipoFront())
                        .hasDigitoVerificador(dto.getHasDigitoVerificador())
                        .showAyuda(dto.getShowAyuda())
                        .tipoReferencia(dto.getTipoReferencia())
                        .legend(dto.getLegend())
                        .status(dto.getStatus())
                        .build()
        ).collect(Collectors.toList());

        mongoRepository.saveAll(mongoList);
        log.info("Sincronización a MongoDB exitosa. Total de productos guardados con ficha completa: {}", mongoList.size());
    }

    private CatProductJsonDto mapToDto(CatProductMongo m) {
        return CatProductJsonDto.builder()
                .idProducto(m.getIdProducto())
                .idServicio(m.getIdServicio())
                .servicio(m.getServicio())
                .producto(m.getProducto())
                .precio(m.getPrecio())
                .idCatTipoServicio(m.getIdCatTipoServicio())
                .tipoFront(m.getTipoFront())
                .hasDigitoVerificador(m.getHasDigitoVerificador())
                .showAyuda(m.getShowAyuda())
                .tipoReferencia(m.getTipoReferencia())
                .legend(m.getLegend())
                .status(m.getStatus() != null ? m.getStatus() : ProductStatusEnum.ACTIVE)
                .build();
    }
}