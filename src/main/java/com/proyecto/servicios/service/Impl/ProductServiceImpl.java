package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoProductClient;
import com.proyecto.servicios.entity.mongo.CatProductMongo;
import com.proyecto.servicios.model.PaginatedResponse;
import com.proyecto.servicios.model.gestopago.CatProductJsonDto;
import com.proyecto.servicios.model.gestopago.CatProductXmlDto;
import com.proyecto.servicios.model.gestopago.ProductStatusEnum;
import com.proyecto.servicios.model.gestopago.ProductoBusquedaRequestDto;
import com.proyecto.servicios.repositorys.mongo.CatProductMongoRepository;
import com.proyecto.servicios.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

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
    public boolean hasProductsInMongo() {
        return mongoRepository.count() > 0;
    }

    @Override
    public List<CatProductJsonDto> getProductListFromMongo() {
        return mongoRepository.findAll().stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    public PaginatedResponse<CatProductJsonDto> buscarProductosPaginado(ProductoBusquedaRequestDto request) {
        List<CatProductJsonDto> todos = getProductListFromMongo();
        ProductoBusquedaRequestDto filtro = request != null ? request : new ProductoBusquedaRequestDto();

        List<CatProductJsonDto> filtrados = todos.stream()
                .filter(p -> coincideCategoria(p, filtro.getIdCatTipoServicio()))
                .filter(p -> coincideServicio(p, filtro.getIdServicio()))
                .filter(p -> coincideBusqueda(p, filtro.getBusqueda()))
                .toList();

        int page = (filtro.getPage() != null && filtro.getPage() >= 0) ? filtro.getPage() : 0;
        int size = (filtro.getSize() != null && filtro.getSize() > 0) ? filtro.getSize() : 20;
        int totalElements = filtrados.size();
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);

        int fromIndex = Math.min(page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);
        List<CatProductJsonDto> paginaContenido = filtrados.subList(fromIndex, toIndex);

        return PaginatedResponse.<CatProductJsonDto>builder()
                .contenido(paginaContenido)
                .paginaActual(page)
                .tamanoPagina(size)
                .totalElementos(totalElements)
                .totalPaginas(totalPages)
                .esPrimera(page == 0)
                .esUltima(totalPages == 0 || page >= totalPages - 1)
                .build();
    }

    private boolean coincideCategoria(CatProductJsonDto p, String idCatTipoServicio) {
        if (idCatTipoServicio == null || idCatTipoServicio.isBlank()) {
            return true;
        }
        return idCatTipoServicio.trim().equals(p.getIdCatTipoServicio());
    }

    private boolean coincideServicio(CatProductJsonDto p, String idServicio) {
        if (idServicio == null || idServicio.isBlank()) {
            return true;
        }
        return idServicio.trim().equals(p.getIdServicio());
    }

    private boolean coincideBusqueda(CatProductJsonDto p, String busqueda) {
        if (busqueda == null || busqueda.isBlank()) {
            return true;
        }
        String term = busqueda.trim().toLowerCase(Locale.ROOT);
        String nombreProd = p.getProducto() != null ? p.getProducto().toLowerCase(Locale.ROOT) : "";
        String nombreServ = p.getServicio() != null ? p.getServicio().toLowerCase(Locale.ROOT) : "";
        return nombreProd.contains(term) || nombreServ.contains(term);
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
                .toList();
    }

    @Override
    public List<CatProductJsonDto> fetchProductListFromApi() {
        CatProductXmlDto xmlDto = client.getProductListXml();
        if (xmlDto == null || xmlDto.getProducts() == null) {
            return List.of();
        }
        return xmlDto.getProducts().stream()
                .map(this::mapXmlElementToDto)
                .toList();
    }

    private CatProductJsonDto mapXmlElementToDto(CatProductXmlDto.ProductXmlItem p) {
        Double precioParsed = parsearPrecio(p.getPrecio());
        return CatProductJsonDto.builder()
                .idProducto(p.getIdProducto())
                .idServicio(p.getIdServicio())
                .servicio(p.getServicio())
                .producto(p.getNombreProducto())
                .precio(precioParsed)
                .idCatTipoServicio(p.getIdCatTipoServicio())
                .nombreCategoria(resolverNombreCategoria(p.getIdCatTipoServicio()))
                .tipoFront(p.getTipoFront())
                .tipoOperacionSugerido(resolverTipoOperacionSugerido(p.getIdCatTipoServicio(), p.getTipoFront()))
                .hasDigitoVerificador(p.getHasDigitoVerificador())
                .showAyuda(p.getShowAyuda())
                .tipoReferencia(p.getTipoReferencia())
                .legend(p.getLegend())
                .status(ProductStatusEnum.ACTIVE)
                .build();
    }

    private Double parsearPrecio(String precioRaw) {
        if (precioRaw == null || precioRaw.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(precioRaw.trim());
        } catch (NumberFormatException ex) {
            log.debug("Precio no numérico recibido en catálogo: {}", precioRaw);
            return null;
        }
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
        ).toList();

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
                .nombreCategoria(resolverNombreCategoria(m.getIdCatTipoServicio()))
                .tipoFront(m.getTipoFront())
                .tipoOperacionSugerido(resolverTipoOperacionSugerido(m.getIdCatTipoServicio(), m.getTipoFront()))
                .hasDigitoVerificador(m.getHasDigitoVerificador())
                .showAyuda(m.getShowAyuda())
                .tipoReferencia(m.getTipoReferencia())
                .legend(m.getLegend())
                .status(m.getStatus() != null ? m.getStatus() : ProductStatusEnum.ACTIVE)
                .build();
    }

    private String resolverNombreCategoria(String idCatTipoServicio) {
        if (idCatTipoServicio == null) {
            return "Otros Servicios";
        }
        return switch (idCatTipoServicio.trim()) {
            case "1" -> "Tiempo Aire / Recargas";
            case "5" -> "Pago de Servicios";
            case "10" -> "Pines y Tarjetas de Regalo";
            case "13" -> "Telefonía Virtual";
            default -> "Otros Servicios";
        };
    }

    private String resolverTipoOperacionSugerido(String idCatTipoServicio, String tipoFront) {
        if ("10".equals(idCatTipoServicio)) {
            return "COMPRA_PIN";
        }
        if ("2".equals(tipoFront) || "5".equals(idCatTipoServicio)) {
            return "PAGO_SERVICIO";
        }
        return "RECARGA";
    }
}
