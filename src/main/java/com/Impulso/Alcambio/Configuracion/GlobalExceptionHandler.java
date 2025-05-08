package com.Impulso.Alcambio.Configuracion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// import org.springframework.ui.Model; // Ya no necesitamos Model aquí
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest; // Asegúrate de usar la importación correcta para tu versión de Spring Boot

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    // Eliminamos el parámetro Model model
    public ModelAndView handleGlobalException(HttpServletRequest request, Exception ex) {
        log.error("Excepción no controlada capturada: Request: {} raised {}", request.getRequestURL(), ex.getMessage(), ex);

        ModelAndView mav = new ModelAndView();
        
        // Información básica del error - Añadir directamente a mav
        mav.addObject("errorMessage", ex.getMessage());
        mav.addObject("errorType", ex.getClass().getSimpleName());
        mav.addObject("requestUrl", request.getRequestURL().toString());

        // Buscar la línea relevante en el stack trace
        String errorClass = "Desconocida";
        String errorMethod = "Desconocido";
        int errorLine = -1;
        String stackTraceSnippet = "No disponible";

        StackTraceElement[] stackTrace = ex.getStackTrace();
        if (stackTrace != null && stackTrace.length > 0) {
            StringBuilder snippetBuilder = new StringBuilder();
            int count = 0;
            for (StackTraceElement element : stackTrace) {
                // Intentar encontrar la primera línea que pertenece a nuestro código base
                if (element.getClassName().startsWith("com.Impulso.Alcambio") && errorLine == -1) {
                    errorClass = element.getClassName();
                    errorMethod = element.getMethodName();
                    errorLine = element.getLineNumber();
                }
                // Crear un snippet de las primeras líneas del stack trace
                if (count < 5) { // Mostrar las primeras 5 líneas
                    snippetBuilder.append(element.toString()).append("\n");
                    count++;
                }
            }
            stackTraceSnippet = snippetBuilder.toString();
        }
        
        // Si no encontramos una línea específica de nuestro paquete, tomamos la primera del stack
        if (errorLine == -1 && stackTrace != null && stackTrace.length > 0) {
             StackTraceElement topElement = stackTrace[0];
             errorClass = topElement.getClassName();
             errorMethod = topElement.getMethodName();
             errorLine = topElement.getLineNumber();
        }

        // Añadir detalles directamente a mav
        mav.addObject("errorClass", errorClass);
        mav.addObject("errorMethod", errorMethod);
        mav.addObject("errorLine", errorLine);
        mav.addObject("stackTraceSnippet", stackTraceSnippet);

        mav.setViewName("error"); // Nombre de la plantilla Thymeleaf (error.html)
        return mav;
    }
} 