package com.proyecto.servicios.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuración centralizada de Swagger / OpenAPI 3.0 para la API de Servicios.
 * Proporciona:
 * - Soporte para autenticación JWT Bearer con botón global 'Authorize' en Swagger UI.
 * - Múltiples entornos/servidores configurados (Local, Alterno, Staging, Producción).
 * - Documentación detallada de arquitectura, estándares de diseño y endpoints.
 * - Categorización y ordenamiento de tags de negocio.
 */
@Configuration
public class OpenApi {

    public static final String SECURITY_SCHEME_NAME = "BearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                // 1. Información general y documentación del sistema
                .info(new Info()
                        .title("API RESTful de Administración Bancaria y GestoPago")
                        .version("1.0.0")
                        .description("""
                                ### Sistema Integral de Onboarding Bancario y Servicios GestoPago
                                
                                Esta API proporciona la infraestructura transaccional y administrativa para la gestión de clientes, cuentas bancarias, credenciales de usuario y catálogo de productos externos.
                                
                                #### Principios de Arquitectura y Diseño:
                                * **Cero `@PathVariable` y Cero `@RequestParam`:** Todos los endpoints reciben sus parámetros y criterios a través del `@RequestBody` con validaciones DTO (`@Valid`).
                                * **Búsquedas Dinámicas y Paginadas:** Filtros combinados multicriterio mediante JPA Criteria API y respuestas paginadas (`PaginatedResponse<T>`).
                                * **Estructura Uniforme de Respuestas:** Todas las peticiones exitosas y fallidas devuelven la estructura homogénea `ApiResponse<T>` con `exito`, `mensaje`, `datos`, `codigoEstado` y `marcaTiempo`.
                                * **Seguridad Centralizada con JWT:** Encriptación de contraseñas con **BCrypt** y autorización basada en tokens Bearer JWT stateless.
                                
                                #### Guía Rápida para Pruebas en Swagger UI:
                                1. Dirígete a la sección **Autenticación** y ejecuta `POST /auth/login` con tus credenciales.
                                2. Copia la cadena del token devuelta en el campo `datos.token`.
                                3. Haz clic en el botón verde **Authorize** (arriba a la derecha con el icono de candado).
                                4. Pega el token en el campo de texto (no es necesario escribir `Bearer`, Swagger lo añadirá automáticamente) y presiona **Authorize**.
                                5. ¡Listo! Todos los endpoints protegidos se ejecutarán autorizados.
                                """)
                        .contact(new Contact()
                                .name("Repositorio Base del Código")
                                .url("https://github.com/UrielYanez/Prueba-GestoPago")))
                
                // 2. Definición de múltiples servidores y entornos de ejecución
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8081")
                                .description("Servidor Local de Desarrollo (Puerto 8081 - Principal)"),
                        new Server()
                                .url("http://localhost:8080")
                                .description("Servidor Local Alternativo / Gateway (Puerto 8080)"),
                        new Server()
                                .url("https://api-dev.gestopago.com")
                                .description("Servidor de Pruebas / Staging (Ambiente Cloud de QA)"),
                        new Server()
                                .url("https://api.gestopago.com")
                                .description("Servidor de Producción")
                ))
                
                // 3. Organización y descripción de Tags
                .tags(List.of(
                        new Tag().name("Autenticación").description("Inicio de sesión, validación de credenciales y generación de tokens JWT"),
                        new Tag().name("Clientes").description("Gestión integral de clientes: Onboarding, búsqueda multicriterio dinámica paginada, actualización parcial, baja lógica y reactivación"),
                        new Tag().name("Cuentas").description("Administración de cuentas bancarias: apertura de cuentas secundarias, depósitos, retiros y balances"),
                        new Tag().name("Usuarios").description("Gestión de credenciales de acceso, usuarios independientes y consultas paginadas"),
                        new Tag().name("Productos / GestoPago").description("Consulta y sincronización de catálogos con la API externa de GestoPago"),
                        new Tag().name("Pagos y Transacciones").description("Procesamiento de pagos con GestoPago y confirmación de seguridad OTP (2FA) por correo")
                ))
                
                // 4. Esquema de seguridad JWT (habilita el botón Authorize)
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Ingrese el token JWT obtenido desde `/auth/login`. Formato: `[token]` (sin la palabra Bearer).")
                        )
                );
    }
}