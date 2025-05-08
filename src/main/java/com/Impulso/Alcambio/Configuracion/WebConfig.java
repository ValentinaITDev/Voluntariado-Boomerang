package com.Impulso.Alcambio.Configuracion;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Configurar acceso a la carpeta de imágenes de proyectos
        registry.addResourceHandler("/Proyectos/**")
                .addResourceLocations("file:src/main/resources/Proyectos/");
        
        // Configuración explícita para CSS y JS
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/");
                
        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/");
                
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/");
        
        // Mantener configuraciones predeterminadas para otros recursos estáticos
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
    }
    
    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        // Configurar tipos MIME explícitamente
        configurer
            .mediaType("css", MediaType.valueOf("text/css"))
            .mediaType("js", MediaType.valueOf("application/javascript"))
            .mediaType("html", MediaType.TEXT_HTML)
            .mediaType("png", MediaType.IMAGE_PNG)
            .mediaType("jpg", MediaType.IMAGE_JPEG)
            .mediaType("jpeg", MediaType.IMAGE_JPEG);
    }
} 