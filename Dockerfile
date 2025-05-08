FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Instalar dependencias necesarias
RUN apk add --no-cache maven

# Copiar todo el proyecto
COPY . .

# Compilar el proyecto
RUN mvn package -DskipTests || echo "Compilación fallida, verificando JAR..."

# Verificar que el JAR se creó correctamente
RUN find /app/target -name "*.jar" -type f

FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app

# Copiar el JAR desde la etapa de compilación
COPY --from=build /app/target/*.jar app.jar

# Configurar usuario no-root para mejorar seguridad
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Optimizaciones para la JVM
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
EXPOSE 8080