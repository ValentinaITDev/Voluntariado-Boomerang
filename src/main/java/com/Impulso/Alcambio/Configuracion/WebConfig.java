package com.Impulso.Alcambio.Configuracion;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Configurar acceso a la carpeta de imágenes de proyectos
        registry.addResourceHandler("/Proyectos/**")
                .addResourceLocations("file:src/main/resources/Proyectos/");
        
        // Mantener configuraciones predeterminadas
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
    }
} 