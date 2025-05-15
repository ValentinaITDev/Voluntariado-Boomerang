package com.Impulso.Alcambio.Controller;

import java.util.Map;
import java.util.List;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.Impulso.Alcambio.Servicio.EstadisticasServicio;

/**
 * Este controlador maneja todas las estadísticas del sistema
 * Proporciona datos para los gráficos, cuadros de mando y KPIs
 * que se muestran en los dashboards administrativos
 */
@RestController
@RequestMapping("/api")
@PreAuthorize("hasAuthority('ADMIN')")
public class EstadisticasController {

    @Autowired
    private EstadisticasServicio estadisticasServicio;

    // =================== ESTADÍSTICAS BÁSICAS ===================
    
    /**
     * Endpoint para obtener estadísticas de desafíos - usado en el panel de admin
     * Muestra métricas como tasa de completitud, participación, etc.
     */
    @GetMapping("/desafios/estadisticas")
    public ResponseEntity<Map<String, Object>> obtenerEstadisticasDesafios() {
        return ResponseEntity.ok(estadisticasServicio.obtenerEstadisticasDesafios());
    }

    /**
     * Devuelve los foros más activos - para la sección de "Tendencias" del panel
     * Ordenados por nivel de actividad para destacar los más populares
     */
    @GetMapping("/foros/estadisticas")
    public ResponseEntity<List<Map<String, Object>>> obtenerEstadisticasForos() {
        return ResponseEntity.ok(estadisticasServicio.obtenerForosPopulares());
    }

    /**
     * Proporciona datos sobre la actividad de usuarios en diferentes períodos
     * Lo usamos para gráficos de líneas en el dashboard administrativo
     */
    @GetMapping("/usuarios/estadisticas/actividad")
    public ResponseEntity<List<Map<String, Object>>> obtenerEstadisticasActividad() {
        return ResponseEntity.ok(estadisticasServicio.obtenerActividadUsuarios());
    }

    // =================== DASHBOARD DE ADMINISTRACIÓN ===================
    
    /**
     * Obtiene un resumen completo para el panel de administración.
     * Incluye todas las métricas y datos para las gráficas.
     */
    @GetMapping("/dashboard/resumen")
    public ResponseEntity<Map<String, Object>> obtenerResumenDashboard() {
        try {
            Map<String, Object> resumen = estadisticasServicio.obtenerResumenDashboard();
            Map<String, Object> resultado = procesarDatosResumen(resumen);
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            e.printStackTrace();
            // En caso de error, devolver datos mínimos para evitar errores en el frontend
            Map<String, Object> respuestaError = prepararDatosRespaldo(e.getMessage());
            return ResponseEntity.ok(respuestaError);
        }
    }
    
    /**
     * Endpoint exclusivo para el panel avanzado.
     * Devuelve datos completos para todas las gráficas.
     */
    @GetMapping("/dashboard/avanzado/datos")
    public ResponseEntity<Map<String, Object>> obtenerDatosPanelAvanzado() {
        try {
            // Obtener datos base
            Map<String, Object> datosBase = estadisticasServicio.obtenerResumenDashboard();
            
            // Complementar con datos adicionales específicos para el panel avanzado
            Map<String, Object> datosAvanzados = new HashMap<>(datosBase);
            
            // Obtener datos específicos para gráficas adicionales
            datosAvanzados.put("forosActivos", estadisticasServicio.obtenerForosPopulares());
            datosAvanzados.put("actividad", estadisticasServicio.obtenerActividadUsuarios());
            
            // Estandarizar formato de datos
            normalizarDatos(datosAvanzados);
            
            return ResponseEntity.ok(datosAvanzados);
        } catch (Exception e) {
            e.printStackTrace();
            // Respuesta de error estándar
            return ResponseEntity.ok(prepararDatosRespaldo(e.getMessage()));
        }
    }
    
    /**
     * Endpoint de diagnóstico para verificar posibles errores en el dashboard
     */
    @GetMapping("/debug/dashboard/resumen")
    public ResponseEntity<?> obtenerResumenDashboardDebug() {
        try {
            Map<String, Object> resumen = estadisticasServicio.obtenerResumenDashboard();
            return ResponseEntity.ok(resumen);
        } catch (Exception e) {
            Map<String, String> error = Map.of(
                "error", e.getMessage(),
                "causa", e.getCause() != null ? e.getCause().getMessage() : "Desconocida",
                "tipo", e.getClass().getName(),
                "stackTrace", e.getStackTrace().length > 0 ? e.getStackTrace()[0].toString() : "No disponible"
            );
            return ResponseEntity.status(500).body(error);
        }
    }
    
    // =================== MÉTODOS AUXILIARES ===================
    
    /**
     * Helper para obtener valores Long de un mapa de forma segura.
     */
    private Long getLongValueFromMap(Map<String, Object> map, String key, Long defaultValue) {
        if (map == null) return defaultValue;
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        } else if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                // Considerar loguear este error si ocurre frecuentemente
                return defaultValue;
            }
        }
        // Si se espera un Long y el valor es null pero la clave existe, y defaultValue es null, se retorna null.
        if (defaultValue == null && value == null && map.containsKey(key)) return null; 
        return defaultValue;
    }

    /**
     * Procesa los datos del resumen para asegurar consistencia
     */
    private Map<String, Object> procesarDatosResumen(Map<String, Object> resumen) {
        Map<String, Object> resultado = new HashMap<>();
        
        // Métricas principales (con valores por defecto en caso de ser nulos)
        Long totalVoluntarios = getLongValueFromMap(resumen, "totalVoluntarios", 0L);
        Long totalAdministradores = getLongValueFromMap(resumen, "totalAdministradores", 0L);
        Long totalUsuariosOriginal = getLongValueFromMap(resumen, "totalUsuarios", null); // Permitir null para detectar si no vino

        Long totalUsuariosCalculado;
        // Si el servicio ya proveyó un totalUsuarios > 0, usar ese.
        if (totalUsuariosOriginal != null && totalUsuariosOriginal > 0L) {
            totalUsuariosCalculado = totalUsuariosOriginal;
        } else {
            // Si no, o si era 0, calcularlo desde los componentes.
            // Esto asegura que si hay voluntarios o admins, el total no será 0 a menos que ambos sean 0.
            totalUsuariosCalculado = totalVoluntarios + totalAdministradores;
        }

        // Almacenar como valores primitivos
        resultado.put("totalUsuarios", totalUsuariosCalculado.longValue());
        resultado.put("totalVoluntarios", totalVoluntarios.longValue());
        resultado.put("totalAdministradores", totalAdministradores.longValue());

        // Métricas númericas como valores primitivos
        resultado.put("proyectosActivos", getLongValueFromMap(resumen, "proyectosActivos", 0L).longValue());
        resultado.put("desafiosCompletados", getLongValueFromMap(resumen, "desafiosCompletados", 0L).longValue());
        resultado.put("empresasParticipantes", getLongValueFromMap(resumen, "empresasParticipantes", 0L).longValue());
        
        // Datos de tendencias como valores primitivos
        Object tendenciaUsuarios = resumen.getOrDefault("tendenciaUsuarios", 0.0);
        Object tendenciaProyectos = resumen.getOrDefault("tendenciaProyectos", 0.0);
        Object tendenciaDesafios = resumen.getOrDefault("tendenciaDesafios", 0.0);
        Object tendenciaEmpresas = resumen.getOrDefault("tendenciaEmpresas", 0.0);
        
        resultado.put("tendenciaUsuarios", tendenciaUsuarios instanceof Number ? ((Number)tendenciaUsuarios).doubleValue() : 0.0);
        resultado.put("tendenciaProyectos", tendenciaProyectos instanceof Number ? ((Number)tendenciaProyectos).doubleValue() : 0.0);
        resultado.put("tendenciaDesafios", tendenciaDesafios instanceof Number ? ((Number)tendenciaDesafios).doubleValue() : 0.0);
        resultado.put("tendenciaEmpresas", tendenciaEmpresas instanceof Number ? ((Number)tendenciaEmpresas).doubleValue() : 0.0);
        
        // Datos para gráfica de desafíos como valores primitivos
        resultado.put("completados", getLongValueFromMap(resumen, "completados", 0L).longValue());
        resultado.put("enProgreso", getLongValueFromMap(resumen, "enProgreso", 0L).longValue());
        resultado.put("sinComenzar", getLongValueFromMap(resumen, "sinComenzar", 0L).longValue());
        
        // Datos para gráfica de proyectos por mes
        Object proyectosPorMesData = resumen.get("proyectosPorMes");
        if (proyectosPorMesData instanceof Map) {
            Map<String, Object> originalMap = (Map<String, Object>)proyectosPorMesData;
            Map<String, Long> proyectosPorMesPrimitivos = new HashMap<>();
            
            for (Map.Entry<String, Object> entry : originalMap.entrySet()) {
                if (entry.getValue() instanceof Number) {
                    proyectosPorMesPrimitivos.put(entry.getKey(), ((Number)entry.getValue()).longValue());
                } else {
                    try {
                        proyectosPorMesPrimitivos.put(entry.getKey(), Long.parseLong(entry.getValue().toString()));
                    } catch (Exception e) {
                        proyectosPorMesPrimitivos.put(entry.getKey(), 0L);
                    }
                }
            }
            resultado.put("proyectosPorMes", proyectosPorMesPrimitivos);
        } else {
            resultado.put("proyectosPorMes", generarDatosProyectosPorMesVacios());
        }
        
        // Datos de actividad - procesar valores como primitivos
        Object actividadData = resumen.get("actividad");
        if (actividadData instanceof List) {
            List<Map<String, Object>> actividad = (List<Map<String, Object>>)actividadData;
            List<Map<String, Object>> actividadPrimitiva = new ArrayList<>();
            
            for (Map<String, Object> item : actividad) {
                Map<String, Object> itemPrimitivo = new HashMap<>(item);
                if (itemPrimitivo.containsKey("actividades") && itemPrimitivo.get("actividades") instanceof Number) {
                    itemPrimitivo.put("actividades", ((Number)itemPrimitivo.get("actividades")).longValue());
                }
                actividadPrimitiva.add(itemPrimitivo);
            }
            resultado.put("actividad", actividadPrimitiva);
        } else {
            resultado.put("actividad", new ArrayList<>());
        }
        
        // Ranking de empresas - procesar valores como primitivos
        Object empresasRankingData = resumen.get("empresasRanking");
        if (empresasRankingData instanceof List) {
            List<Map<String, Object>> empresasRanking = (List<Map<String, Object>>)empresasRankingData;
            List<Map<String, Object>> empresasRankingPrimitivo = new ArrayList<>();
            
            for (Map<String, Object> empresa : empresasRanking) {
                Map<String, Object> empresaPrimitiva = new HashMap<>(empresa);
                // Procesar valores numéricos
                for (String clave : new String[]{"usuarios", "proyectos", "puntos"}) {
                    if (empresaPrimitiva.containsKey(clave) && empresaPrimitiva.get(clave) instanceof Number) {
                        Number valor = (Number) empresaPrimitiva.get(clave);
                        if (valor instanceof Integer || valor instanceof Long) {
                            empresaPrimitiva.put(clave, valor.longValue());
                        } else {
                            empresaPrimitiva.put(clave, valor.doubleValue());
                        }
                    }
                }
                empresasRankingPrimitivo.add(empresaPrimitiva);
            }
            resultado.put("empresasRanking", empresasRankingPrimitivo);
        } else {
            resultado.put("empresasRanking", new ArrayList<>());
        }

        // Foros activos - procesar valores como primitivos
        Object forosActivosData = resumen.get("forosActivos");
        if (forosActivosData instanceof List) {
            List<Map<String, Object>> forosActivos = (List<Map<String, Object>>)forosActivosData;
            List<Map<String, Object>> forosActivosPrimitivo = new ArrayList<>();
            
            for (Map<String, Object> foro : forosActivos) {
                Map<String, Object> foroPrimitivo = new HashMap<>(foro);
                if (foroPrimitivo.containsKey("totalComentarios") && foroPrimitivo.get("totalComentarios") instanceof Number) {
                    foroPrimitivo.put("totalComentarios", ((Number)foroPrimitivo.get("totalComentarios")).intValue());
                }
                forosActivosPrimitivo.add(foroPrimitivo);
            }
            resultado.put("forosActivos", forosActivosPrimitivo);
        } else {
            resultado.put("forosActivos", new ArrayList<>());
        }

        // Asegurar otros campos que el frontend podría esperar, si aplica
        // Ejemplo: desafíos por categoría
        Object desafiosCategoriasData = resumen.get("desafiosCategorias");
        if (desafiosCategoriasData instanceof Map) {
            Map<String, Object> categorias = (Map<String, Object>)desafiosCategoriasData;
            Map<String, Integer> categoriasPrimitivas = new HashMap<>();
            
            for (Map.Entry<String, Object> entry : categorias.entrySet()) {
                if (entry.getValue() instanceof Number) {
                    categoriasPrimitivas.put(entry.getKey(), ((Number)entry.getValue()).intValue());
                } else {
                    try {
                        categoriasPrimitivas.put(entry.getKey(), Integer.parseInt(entry.getValue().toString()));
                    } catch (Exception e) {
                        categoriasPrimitivas.put(entry.getKey(), 0);
                    }
                }
            }
            resultado.put("desafiosCategorias", categoriasPrimitivas);
        } else {
            resultado.put("desafiosCategorias", new HashMap<>());
        }
        
        return resultado;
    }

    /**
     * Genera datos de proyectos por mes vacíos para casos donde no hay datos disponibles
     */
    private Map<String, Long> generarDatosProyectosPorMesVacios() {
        Map<String, Long> proyectosPorMes = new HashMap<>();
        LocalDateTime ahora = LocalDateTime.now();
        for (int i = 5; i >= 0; i--) {
            LocalDateTime mes = ahora.minusMonths(i);
            String clave = mes.getYear() + "-" + String.format("%02d", mes.getMonthValue());
            proyectosPorMes.put(clave, 0L);
        }
        return proyectosPorMes;
    }
    
    /**
     * Prepara datos de respaldo en caso de error para evitar fallos en el frontend
     */
    private Map<String, Object> prepararDatosRespaldo(String errorMsg) {
        Map<String, Object> respuestaError = new HashMap<>();
        respuestaError.put("error", "Error al generar estadísticas: " + errorMsg);
        
        // Datos básicos con valor 0
        respuestaError.put("totalUsuarios", 0L);
        respuestaError.put("totalVoluntarios", 0L);
        respuestaError.put("totalAdministradores", 0L);
        respuestaError.put("proyectosActivos", 0L);
        respuestaError.put("desafiosCompletados", 0L);
        respuestaError.put("empresasParticipantes", 0L);
        
        // Datos de tendencia con valor 0.0
        respuestaError.put("tendenciaUsuarios", 0.0);
        respuestaError.put("tendenciaProyectos", 0.0);
        respuestaError.put("tendenciaDesafios", 0.0);
        respuestaError.put("tendenciaEmpresas", 0.0);
        
        // Datos específicos para gráficas de desafíos con valor 0
        respuestaError.put("completados", 0L);
        respuestaError.put("enProgreso", 0L);
        respuestaError.put("sinComenzar", 0L);
        
        // Estado de proyectos (vacío o con ceros)
        Map<String, Long> estadosProyecto = new HashMap<>();
        estadosProyecto.put("ACTIVO", 0L);
        estadosProyecto.put("COMPLETADO", 0L);
        estadosProyecto.put("EXPIRADO", 0L);
        estadosProyecto.put("CANCELADO", 0L);
        respuestaError.put("proyectosPorEstado", estadosProyecto);
        
        // Datos de actividad (lista vacía)
        respuestaError.put("actividad", new ArrayList<>());
        
        // Categorías de desafíos (mapa vacío)
        respuestaError.put("desafiosCategorias", new HashMap<>());
        
        // Foros activos (lista vacía)
        respuestaError.put("forosActivos", new ArrayList<>());
        
        // Datos de proyectos por mes (mapa vacío)
        respuestaError.put("proyectosPorMes", generarDatosProyectosPorMesVacios()); // Mantenemos la estructura de meses con 0
        
        // Datos de empresas (listas/mapas vacíos)
        respuestaError.put("empresasRanking", new ArrayList<>());
        respuestaError.put("participacionMensual", new HashMap<>());
        
        return respuestaError;
    }
    
    /**
     * Normaliza el formato de datos para evitar errores en el frontend
     */
    private void normalizarDatos(Map<String, Object> datos) {
        // Convertir strings a números donde sea necesario
        convertirANumero(datos, "totalUsuarios");
        convertirANumero(datos, "totalVoluntarios");
        convertirANumero(datos, "totalAdministradores");
        convertirANumero(datos, "proyectosActivos");
        convertirANumero(datos, "desafiosCompletados");
        convertirANumero(datos, "empresasParticipantes");
        
        // Asegurar que hay valores por defecto para las tendencias
        if (!datos.containsKey("tendenciaUsuarios")) datos.put("tendenciaUsuarios", 0.0);
        if (!datos.containsKey("tendenciaProyectos")) datos.put("tendenciaProyectos", 0.0);
        if (!datos.containsKey("tendenciaDesafios")) datos.put("tendenciaDesafios", 0.0);
        if (!datos.containsKey("tendenciaEmpresas")) datos.put("tendenciaEmpresas", 0.0);
        
        // Convertir objetos Long/Integer a primitivos para evitar problemas de serialización
        for (String clave : new String[] {"completados", "enProgreso", "sinComenzar"}) {
            if (datos.containsKey(clave) && datos.get(clave) instanceof Number) {
                datos.put(clave, ((Number)datos.get(clave)).longValue());
            }
        }
        
        // Procesar el mapa de proyectos por mes
        if (datos.containsKey("proyectosPorMes") && datos.get("proyectosPorMes") instanceof Map) {
            Map<String, Object> proyectosPorMes = (Map<String, Object>) datos.get("proyectosPorMes");
            Map<String, Long> proyectosPorMesPrimitivos = new HashMap<>();
            
            for (Map.Entry<String, Object> entry : proyectosPorMes.entrySet()) {
                if (entry.getValue() instanceof Number) {
                    proyectosPorMesPrimitivos.put(entry.getKey(), ((Number)entry.getValue()).longValue());
                } else if (entry.getValue() instanceof String) {
                    try {
                        proyectosPorMesPrimitivos.put(entry.getKey(), Long.parseLong((String)entry.getValue()));
                    } catch (NumberFormatException e) {
                        proyectosPorMesPrimitivos.put(entry.getKey(), 0L);
                    }
                } else {
                    proyectosPorMesPrimitivos.put(entry.getKey(), 0L);
                }
            }
            
            datos.put("proyectosPorMes", proyectosPorMesPrimitivos);
        }
        
        // Procesar listas de objetos
        procesarListaDeMaps(datos, "actividad", "actividades");
        procesarListaDeMaps(datos, "empresasRanking", "usuarios", "proyectos", "puntos");
        procesarListaDeMaps(datos, "forosActivos", "totalComentarios");
    }
    
    /**
     * Procesa una lista de mapas para convertir sus valores a primitivos
     */
    private void procesarListaDeMaps(Map<String, Object> datos, String claveLista, String... clavesValores) {
        if (datos.containsKey(claveLista) && datos.get(claveLista) instanceof List) {
            List<Map<String, Object>> lista = (List<Map<String, Object>>) datos.get(claveLista);
            
            for (Map<String, Object> item : lista) {
                for (String claveValor : clavesValores) {
                    if (item.containsKey(claveValor) && item.get(claveValor) instanceof Number) {
                        Number valor = (Number) item.get(claveValor);
                        // Determinar si es entero o decimal
                        if (valor instanceof Integer || valor instanceof Long) {
                            item.put(claveValor, valor.longValue());
                        } else {
                            item.put(claveValor, valor.doubleValue());
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Convierte un valor a número (long) en un mapa
     */
    private void convertirANumero(Map<String, Object> datos, String clave) {
        if (datos.containsKey(clave)) {
            Object valor = datos.get(clave);
            if (valor instanceof String) {
                try {
                    datos.put(clave, Long.parseLong((String)valor));
                } catch (NumberFormatException e) {
                    datos.put(clave, 0L);
                }
            } else if (valor instanceof Number) {
                // Asegurar que sea un tipo primitivo
                datos.put(clave, ((Number)valor).longValue());
            }
        } else {
            datos.put(clave, 0L);
        }
    }
} 