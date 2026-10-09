# Etapa 1: Construcción (Build)
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Copiar los archivos necesarios de Gradle
COPY gradle /app/gradle
COPY gradlew /app/
COPY build.gradle /app/
COPY settings.gradle /app/

# Dar permisos de ejecución al wrapper de Gradle
RUN chmod +x gradlew

# Descargar las dependencias (optimiza el caché de Docker)
RUN ./gradlew dependencies --no-daemon

# Copiar el código fuente
COPY src /app/src

# Compilar el proyecto saltándose las pruebas para que sea más rápido
RUN ./gradlew clean build -x test --no-daemon

# Etapa 2: Ejecución (Run)
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Exponer el puerto en el que corre tu app (según application.properties)
EXPOSE 8081

# Copiar el JAR generado de la etapa de construcción
COPY --from=builder /app/build/libs/*.jar app.jar

# Comando para ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]
