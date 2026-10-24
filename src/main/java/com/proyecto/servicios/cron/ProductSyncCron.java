package com.proyecto.servicios.cron;

import com.proyecto.servicios.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class ProductSyncCron {
    private static final Logger log = LoggerFactory.getLogger(ProductSyncCron.class);
    private final ProductService productService;

    public ProductSyncCron(ProductService productService) {
        this.productService = productService;
    }

    /**
     * Sincronización programada diaria a las 06:00 AM.
     */
    @Scheduled(cron = "0 0 6 * * *")
    public void syncDailyAt6Am() {
        log.info("Ejecutando cron programado de sincronización de productos a Mongo a las 06:00 AM");
        productService.syncProductsToMongo();
    }

    /**
     * Sincronización automática al iniciar el proyecto.
     * Se ejecuta en segundo plano (asíncrono) para no demorar el arranque del servidor web
     * y asegurar que los servicios de base de datos y autenticación de GestoPago estén 100% listos.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void syncOnStartup() {
        CompletableFuture.runAsync(() -> {
            try {
                // Pausa prudente de 2 segundos para asegurar que el token inicial ya quedó guardado en PostgreSQL
                Thread.sleep(2000);
                log.info("Iniciando sincronización automática de productos hacia MongoDB tras arranque completo...");
                productService.syncProductsToMongo();
                log.info("Sincronización inicial de productos hacia MongoDB finalizada con éxito.");
            } catch (Exception e) {
                log.error("No se pudo completar la sincronización inicial de productos: {}", e.getMessage());
            }
        });
    }
}