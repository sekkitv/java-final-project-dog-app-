# syntax=docker/dockerfile:1

# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Copy Maven wrapper and POM first to cache dependencies
COPY .mvn/ .mvn/
COPY mvnw mvnw.cmd pom.xml ./
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

# Copy source and build the layered jar
COPY src/ src/
RUN ./mvnw -B clean package -DskipTests

# Extract layers for a smaller runtime image
RUN mkdir -p /extracted && \
    java -Djarmode=layertools -jar target/*.jar extract --destination /extracted

# ---- Runtime stage ----
FROM eclipse-temurin:17-jre
WORKDIR /app

# Persistent upload volume mount point (Railway volume mounts here)
RUN mkdir -p /app/uploads && chown -R 1000:1000 /app
VOLUME ["/app/uploads"]

COPY --from=build /extracted/dependencies/         ./
COPY --from=build /extracted/spring-boot-loader/   ./
COPY --from=build /extracted/snapshot-dependencies/ ./
COPY --from=build /extracted/application/          ./

EXPOSE 8080

ENV APP_UPLOAD_DIR=/app/uploads \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
