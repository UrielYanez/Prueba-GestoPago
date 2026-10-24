package com.proyecto.servicios.config;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Slf4j
@Configuration
public class FlywayConfig {

    @Value("${spring.flyway.locations:classpath:db/migration}")
    private String[] locations;

    @Value("${spring.flyway.table:flyway_schema_history}")
    private String historyTable;

    @Value("${spring.flyway.schemas:public}")
    private String schema;

    @Bean(name = "flyway")
    public Flyway flyway(@Qualifier("sfDatasource") DataSource dataSource) {
        log.info("Iniciando migraciones Flyway en schema '{}'", schema);
        try (var conn = dataSource.getConnection(); var stmt = conn.createStatement()) {
            stmt.execute("DO $$ BEGIN " +
                    "IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'transacciones_pago' AND column_name = 'referencia_telefono') THEN " +
                    "    ALTER TABLE transacciones_pago ALTER COLUMN referencia_telefono DROP NOT NULL; " +
                    "END IF; " +
                    "END $$;");
            log.info("Columna referencia_telefono ajustada exitosamente en PostgreSQL.");
        } catch (Exception e) {
            log.warn("Nota al verificar columnas de transacciones_pago: {}", e.getMessage());
        }

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations(locations)
                .table(historyTable)
                .schemas(schema)
                .baselineOnMigrate(true)
                .baselineVersion("4")
                .cleanOnValidationError(true)
                .load();
        try {
            flyway.migrate();
        } catch (Exception e) {
            log.warn("Flyway migrate warning: {}", e.getMessage());
        }
        return flyway;
    }
}