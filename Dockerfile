FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Instalar dependencias necesarias
RUN apk add --no-cache maven

# Copiar pom.xml y descargar dependencias primero (capa separada para mejor caching)
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
COPY mvnw.cmd .
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -B

# Copiar código fuente y compilar
COPY src ./src
RUN ./mvnw package -DskipTests

# Verificar que el JAR se creó correctamente
RUN ls -la /app/target/

FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app

# Copiar el JAR desde la etapa de compilación
COPY --from=build /app/target/Alcambio-*.jar app.jar

# Configurar usuario no-root para mejorar seguridad
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Optimizaciones para la JVM
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
EXPOSE 8080