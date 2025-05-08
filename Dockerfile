FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app

# Copiar y configurar el archivo JAR
COPY target/Alcambio-0.0.1-SNAPSHOT.jar app.jar
RUN sh -c 'touch /app/app.jar'

# Configurar usuario no-root para mejorar seguridad
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Optimizaciones para la JVM
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
EXPOSE 8080