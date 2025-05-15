package com.Impulso.Alcambio.Configuracion;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;

import lombok.extern.slf4j.Slf4j;

/**
 * Configuración de Redis para el sistema de caché.
 * Esta clase configura la conexión a Redis y el RedisTemplate para operaciones de caché.
 * 
 * NOTA: Esta aplicación utiliza Redis como sistema de almacenamiento en caché
 * para mejorar el rendimiento y la escalabilidad.
 */
@Configuration
@EnableCaching
@Slf4j
public class RedisConfig {

    @Value("${spring.redis.url:redis://localhost:6379}")
    private String redisUrl;
    
    @Value("${spring.redis.timeout:2000}")
    private int timeout;

    /**
     * Configura la fábrica de conexiones Jedis para Redis
     * 
     * @return JedisConnectionFactory configurado
     */
    @Bean
    public JedisConnectionFactory jedisConnectionFactory() {
        try {
            log.info("Configurando conexión a Redis con URL: {}", redisUrl);
            
            // Configuración por defecto que usa propiedades de application.properties
            JedisConnectionFactory factory = new JedisConnectionFactory();
            
            // Configurar timeout si es necesario
            if (timeout > 0) {
                factory.setTimeout(timeout);
            }
            
            return factory;
        } catch (Exception e) {
            log.error("Error al configurar la conexión a Redis: {}", e.getMessage(), e);
            // Devolver una configuración básica como fallback
            return new JedisConnectionFactory();
        }
    }

    /**
     * Crea y configura un ObjectMapper para la serialización JSON
     * que maneja correctamente fechas y propiedades
     * 
     * @return ObjectMapper configurado para Redis
     */
    @Bean
    public ObjectMapper redisObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        
        // Registrar módulo para manejar fechas de Java 8
        objectMapper.registerModule(new JavaTimeModule());
        
        // Desactivar serialización de fechas como timestamps
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // Configurar visibilidad para facilitar serialización
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        
        return objectMapper;
    }

    /**
     * Configura el RedisTemplate con serializadores apropiados
     * 
     * @return RedisTemplate configurado para operaciones de caché
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        try {
            RedisTemplate<String, Object> template = new RedisTemplate<>();
            template.setConnectionFactory(jedisConnectionFactory());
            
            // Crear serializador JSON con ObjectMapper personalizado
            Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = 
                new Jackson2JsonRedisSerializer<>(Object.class);
            jackson2JsonRedisSerializer.setObjectMapper(redisObjectMapper());
            
            // Configurar serializadores
            template.setKeySerializer(new StringRedisSerializer());
            template.setValueSerializer(jackson2JsonRedisSerializer);
            template.setHashKeySerializer(new StringRedisSerializer());
            template.setHashValueSerializer(jackson2JsonRedisSerializer);
            
            // Inicializar el template
            template.afterPropertiesSet();
            
            log.info("RedisTemplate configurado correctamente para operaciones de caché");
            return template;
        } catch (Exception e) {
            log.error("Error al configurar RedisTemplate: {}", e.getMessage(), e);
            // En caso de error, devolver un template con configuración básica
            RedisTemplate<String, Object> fallbackTemplate = new RedisTemplate<>();
            fallbackTemplate.setConnectionFactory(jedisConnectionFactory());
            fallbackTemplate.setKeySerializer(new StringRedisSerializer());
            fallbackTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
            fallbackTemplate.afterPropertiesSet();
            return fallbackTemplate;
        }
    }
} 