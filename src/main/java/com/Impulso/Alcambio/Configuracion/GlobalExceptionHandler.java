package com.Impulso.Alcambio.Configuracion;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Manejador global de excepciones para convertir todos los errores en respuestas JSON
 * en lugar de páginas HTML de error.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Maneja todas las excepciones no especificadas y las convierte en respuestas JSON
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGlobalException(HttpServletRequest request, Exception ex) {
        log.error("Excepción no controlada capturada: Request: {} raised {}", request.getRequestURL(), ex.getMessage(), ex);

        Map<String, Object> errorResponse = new HashMap<>();
        
        // Información básica del error
        errorResponse.put("error", ex.getClass().getSimpleName());
        errorResponse.put("mensaje", ex.getMessage());
        errorResponse.put("path", request.getRequestURL().toString());
        errorResponse.put("timestamp", System.currentTimeMillis());

        // Buscar la línea relevante en el stack trace
        String errorClass = "Desconocida";
        String errorMethod = "Desconocido";
        int errorLine = -1;

        StackTraceElement[] stackTrace = ex.getStackTrace();
        if (stackTrace != null && stackTrace.length > 0) {
            for (StackTraceElement element : stackTrace) {
                // Intentar encontrar la primera línea que pertenece a nuestro código base
                if (element.getClassName().startsWith("com.Impulso.Alcambio") && errorLine == -1) {
                    errorClass = element.getClassName();
                    errorMethod = element.getMethodName();
                    errorLine = element.getLineNumber();
                    break;
                }
            }
            
            // Si no encontramos una línea específica de nuestro paquete, tomamos la primera del stack
            if (errorLine == -1 && stackTrace.length > 0) {
                StackTraceElement topElement = stackTrace[0];
                errorClass = topElement.getClassName();
                errorMethod = topElement.getMethodName();
                errorLine = topElement.getLineNumber();
            }
        }

        // Añadir información de depuración en entornos no productivos
        Map<String, Object> debugInfo = new HashMap<>();
        debugInfo.put("clase", errorClass);
        debugInfo.put("metodo", errorMethod);
        debugInfo.put("linea", errorLine);
        errorResponse.put("debug", debugInfo);

        // Establecer las cabeceras para asegurar respuesta JSON
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        return new ResponseEntity<>(errorResponse, headers, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    /**
     * Maneja específicamente las excepciones de rutas no encontradas
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(HttpServletRequest request, NoHandlerFoundException ex) {
        log.warn("Recurso no encontrado: {}", request.getRequestURL());
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "NotFound");
        errorResponse.put("mensaje", "El recurso solicitado no existe");
        errorResponse.put("path", request.getRequestURL().toString());
        errorResponse.put("timestamp", System.currentTimeMillis());
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        return new ResponseEntity<>(errorResponse, headers, HttpStatus.NOT_FOUND);
    }
} 