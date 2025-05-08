package com.Impulso.Alcambio.Servicio;

import java.util.concurrent.TimeUnit;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * Servicio para manejar el almacenamiento en caché con Redis.
 * Proporciona métodos para guardar, recuperar y eliminar objetos de la caché.
 */
@Service
@Slf4j
public class CacheServicio {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    /**
     * Guarda un objeto en la caché
     * 
     * @param key Clave bajo la cual se almacena el objeto
     * @param value Objeto a almacenar
     */
    public void guardar(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            log.debug("Objeto almacenado en caché con clave: {}", key);
        } catch (Exception e) {
            log.error("Error al guardar en caché: {}", e.getMessage());
        }
    }
    
    /**
     * Guarda un objeto en la caché con un tiempo de expiración
     * 
     * @param key Clave bajo la cual se almacena el objeto
     * @param value Objeto a almacenar
     * @param timeout Tiempo de expiración
     * @param unit Unidad de tiempo
     */
    public void guardarConExpiracion(String key, Object value, long timeout, TimeUnit unit) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
            log.debug("Objeto almacenado en caché con clave: {} y expiración: {} {}", key, timeout, unit);
        } catch (Exception e) {
            log.error("Error al guardar en caché con expiración: {}", e.getMessage());
        }
    }
    
    /**
     * Recupera un objeto de la caché
     * 
     * @param key Clave del objeto a recuperar
     * @return El objeto almacenado o null si no existe
     */
    public Object obtener(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                log.debug("Objeto recuperado de caché con clave: {}", key);
            } else {
                log.debug("No se encontró objeto en caché con clave: {}", key);
            }
            return value;
        } catch (Exception e) {
            log.error("Error al obtener de caché: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Elimina un objeto de la caché
     * 
     * @param key Clave del objeto a eliminar
     * @return true si se eliminó correctamente, false en caso contrario
     */
    public boolean eliminar(String key) {
        try {
            Boolean result = redisTemplate.delete(key);
            log.debug("Eliminación de caché para clave {}: {}", key, result);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Error al eliminar de caché: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Verifica si una clave existe en la caché
     * 
     * @param key Clave a verificar
     * @return true si la clave existe, false en caso contrario
     */
    public boolean existe(String key) {
        try {
            Boolean result = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Error al verificar existencia en caché: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Elimina todas las claves que coincidan con un patrón
     * 
     * @param pattern Patrón de claves a eliminar (ej: "foro:*")
     * @return Número de claves eliminadas
     */
    public long eliminarPorPatron(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                Long deletedCount = redisTemplate.delete(keys);
                log.debug("Eliminadas {} claves con patrón: {}", deletedCount, pattern);
                return deletedCount != null ? deletedCount : 0;
            }
            return 0;
        } catch (Exception e) {
            log.error("Error al eliminar claves con patrón {}: {}", pattern, e.getMessage());
            return 0;
        }
    }
    
    /**
     * Cuenta el número de claves que coinciden con un patrón
     * 
     * @param pattern Patrón de claves a contar (ej: "desafio:*")
     * @return Número de claves que coinciden con el patrón
     */
    public long contarClaves(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            return keys != null ? keys.size() : 0;
        } catch (Exception e) {
            log.error("Error al contar claves con patrón {}: {}", pattern, e.getMessage());
            return 0;
        }
    }
} 