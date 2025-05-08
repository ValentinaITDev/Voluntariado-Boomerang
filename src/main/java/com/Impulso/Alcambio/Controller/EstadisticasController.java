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
     * Procesa los datos del resumen para asegurar consistencia
     */
    private Map<String, Object> procesarDatosResumen(Map<String, Object> resumen) {
        // Procesar los datos para garantizar que todas las propiedades necesarias estén disponibles
        Map<String, Object> resultado = new HashMap<>();
        
        // Métricas principales (con valores por defecto en caso de ser nulos)
        resultado.put("totalUsuarios", resumen.getOrDefault("totalUsuarios", 0L));
        resultado.put("totalVoluntarios", resumen.getOrDefault("totalVoluntarios", 0L));
        resultado.put("totalAdministradores", resumen.getOrDefault("totalAdministradores", 0L));
        resultado.put("proyectosActivos", resumen.getOrDefault("proyectosActivos", 0L));
        resultado.put("desafiosCompletados", resumen.getOrDefault("desafiosCompletados", 0L));
        resultado.put("empresasParticipantes", resumen.getOrDefault("empresasParticipantes", 0L));
        
        // Datos de tendencias (con valores por defecto)
        resultado.put("tendenciaUsuarios", resumen.getOrDefault("tendenciaUsuarios", 0.0));
        resultado.put("tendenciaProyectos", resumen.getOrDefault("tendenciaProyectos", 0.0));
        resultado.put("tendenciaDesafios", resumen.getOrDefault("tendenciaDesafios", 0.0));
        resultado.put("tendenciaEmpresas", resumen.getOrDefault("tendenciaEmpresas", 0.0));
        
        // Datos para gráfica de desafíos
        resultado.put("completados", resumen.getOrDefault("completados", 0L));
        resultado.put("enProgreso", resumen.getOrDefault("enProgreso", 0L));
        resultado.put("sinComenzar", resumen.getOrDefault("sinComenzar", 0L));
        
        // Datos para gráfica de proyectos
        if (resumen.containsKey("proyectosPorEstado")) {
            resultado.put("proyectosPorEstado", resumen.get("proyectosPorEstado"));
        } else {
            Map<String, Long> estadosProyecto = new HashMap<>();
            estadosProyecto.put("ACTIVO", 0L);
            estadosProyecto.put("COMPLETADO", 0L);
            estadosProyecto.put("EXPIRADO", 0L);
            estadosProyecto.put("CANCELADO", 0L);
            resultado.put("proyectosPorEstado", estadosProyecto);
        }
        
        // Datos para gráfica de proyectos por mes
        if (resumen.containsKey("proyectosPorMes")) {
            resultado.put("proyectosPorMes", resumen.get("proyectosPorMes"));
        } else {
            resultado.put("proyectosPorMes", generarDatosProyectosPorMesVacios());
        }
        
        // Datos de actividad
        if (resumen.containsKey("actividad")) {
            resultado.put("actividad", resumen.get("actividad"));
        } else {
            List<Map<String, Object>> actividadUsuarios = estadisticasServicio.obtenerActividadUsuarios();
            resultado.put("actividad", actividadUsuarios);
        }
        
        // Datos para nuevas gráficas
        // Foros activos
        if (resumen.containsKey("forosActivos")) {
            resultado.put("forosActivos", resumen.get("forosActivos"));
        } else {
            List<Map<String, Object>> forosActivos = estadisticasServicio.obtenerForosPopulares();
            resultado.put("forosActivos", forosActivos);
        }
        
        // Desafíos por categoría
        if (resumen.containsKey("desafiosCategorias")) {
            resultado.put("desafiosCategorias", resumen.get("desafiosCategorias"));
        } else {
            // Obtener datos de categorías de desafíos
            Map<String, Object> desafiosStats = estadisticasServicio.obtenerEstadisticasDesafios();
            if (desafiosStats.containsKey("categorias")) {
                resultado.put("desafiosCategorias", desafiosStats.get("categorias"));
            } else {
                // Datos por defecto
                Map<String, Integer> defaultCategorias = new HashMap<>();
                defaultCategorias.put("Medioambiente", 3);
                defaultCategorias.put("Tecnología", 5);
                defaultCategorias.put("Social", 2);
                defaultCategorias.put("Educación", 4);
                resultado.put("desafiosCategorias", defaultCategorias);
            }
        }
        
        // Información de usuarios destacados
        if (resumen.containsKey("usuariosConMasProyectos")) {
            resultado.put("usuariosConMasProyectos", resumen.get("usuariosConMasProyectos"));
        }
        
        // Información de proyectos recientes
        if (resumen.containsKey("proyectosRecientes")) {
            resultado.put("proyectosRecientes", resumen.get("proyectosRecientes"));
        }
        
        // DATOS DE EMPRESAS
        resultado.put("empresasRanking", resumen.getOrDefault("empresasRanking", 
                                       estadisticasServicio.obtenerRankingEmpresas(null)));
        
        resultado.put("participacionMensual", resumen.getOrDefault("participacionMensual", 
                                      estadisticasServicio.obtenerParticipacionMensualPorEmpresa(null)));
        
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
        
        // Datos básicos
        respuestaError.put("totalUsuarios", 0L);
        respuestaError.put("totalVoluntarios", 0L);
        respuestaError.put("totalAdministradores", 0L);
        respuestaError.put("proyectosActivos", 0L);
        respuestaError.put("desafiosCompletados", 0L);
        respuestaError.put("empresasParticipantes", 0L);
        
        // Datos de tendencia
        respuestaError.put("tendenciaUsuarios", 0.0);
        respuestaError.put("tendenciaProyectos", 0.0);
        respuestaError.put("tendenciaDesafios", 0.0);
        respuestaError.put("tendenciaEmpresas", 0.0);
        
        // Datos específicos para gráficas de desafíos
        respuestaError.put("completados", 0L);
        respuestaError.put("enProgreso", 0L);
        respuestaError.put("sinComenzar", 0L);
        
        // Estado de proyectos
        Map<String, Long> estadosProyecto = new HashMap<>();
        estadosProyecto.put("ACTIVO", 0L);
        estadosProyecto.put("COMPLETADO", 0L);
        estadosProyecto.put("EXPIRADO", 0L);
        estadosProyecto.put("CANCELADO", 0L);
        respuestaError.put("proyectosPorEstado", estadosProyecto);
        
        // Datos de actividad dummy
        respuestaError.put("actividad", generarActividadDummy());
        
        // Datos para nuevas gráficas
        Map<String, Integer> defaultCategorias = new HashMap<>();
        defaultCategorias.put("Medioambiente", 1);
        defaultCategorias.put("Tecnología", 1);
        defaultCategorias.put("Social", 1);
        defaultCategorias.put("Educación", 1);
        respuestaError.put("desafiosCategorias", defaultCategorias);
        
        // Foros activos
        respuestaError.put("forosActivos", generarForosActivosDummy());
        
        // Datos de proyectos por mes dummy
        respuestaError.put("proyectosPorMes", generarDatosProyectosPorMesVacios());
        
        // Datos de empresas desde el servicio (con manejo de errores)
        try {
            // Intentar obtener datos reales de empresas
            respuestaError.put("empresasRanking", estadisticasServicio.obtenerRankingEmpresas(null));
            respuestaError.put("participacionMensual", estadisticasServicio.obtenerParticipacionMensualPorEmpresa(null));
        } catch (Exception e) {
            System.err.println("Error al obtener datos de empresas de respaldo: " + e.getMessage());
            // Si falla, usar los métodos locales como último recurso
            respuestaError.put("empresasRanking", generarDatosEmpresasRespaldo());
            respuestaError.put("participacionMensual", generarDatosParticipacionMensualRespaldo());
        }
        
        return respuestaError;
    }
    
    /**
     * Genera una lista dummy de foros activos para respaldo
     */
    private List<Map<String, Object>> generarForosActivosDummy() {
        List<Map<String, Object>> forosDefault = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Map<String, Object> foro = new HashMap<>();
            foro.put("nombre", "Foro " + (i+1));
            foro.put("comentarios", i+1);
            forosDefault.add(foro);
        }
        return forosDefault;
    }
    
    /**
     * Genera datos básicos de empresas como último recurso
     */
    private List<Map<String, Object>> generarDatosEmpresasRespaldo() {
        List<Map<String, Object>> empresasRespaldo = new ArrayList<>();
        
        // Al menos una empresa para evitar errores en UI
        Map<String, Object> empresa = new HashMap<>();
        empresa.put("nombre", "Empresa ejemplo");
        empresa.put("usuarios", 1);
        empresa.put("participaciones", 1);
        empresa.put("puntos", 10);
        empresasRespaldo.add(empresa);
        
        return empresasRespaldo;
    }
    
    /**
     * Genera datos básicos de participación mensual como último recurso
     */
    private Map<String, Map<String, Integer>> generarDatosParticipacionMensualRespaldo() {
        Map<String, Map<String, Integer>> participacionRespaldo = new LinkedHashMap<>();
        LocalDateTime ahora = LocalDateTime.now();
        
        // Generar datos para los últimos 6 meses con al menos una empresa
        for (int i = 5; i >= 0; i--) {
            LocalDateTime mes = ahora.minusMonths(i);
            String clave = mes.getYear() + "-" + String.format("%02d", mes.getMonthValue());
            
            Map<String, Integer> datosMes = new HashMap<>();
            datosMes.put("Empresa ejemplo", 1);
            
            participacionRespaldo.put(clave, datosMes);
        }
        
        return participacionRespaldo;
    }
    
    /**
     * Genera datos dummy de actividad para casos de error
     */
    private List<Map<String, Object>> generarActividadDummy() {
        List<Map<String, Object>> actividad = new ArrayList<>();
        LocalDateTime hoy = LocalDateTime.now();
        
        // Nombres para los días de la semana
        String[] diasSemana = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};
        
        for (int i = 6; i >= 0; i--) {
            LocalDateTime dia = hoy.minus(i, ChronoUnit.DAYS);
            Map<String, Object> datos = new HashMap<>();
            datos.put("fecha", diasSemana[dia.getDayOfWeek().getValue() - 1]);
            datos.put("actividades", 0);
            datos.put("diaSemana", dia.getDayOfWeek().toString());
            actividad.add(datos);
        }
        
        return actividad;
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
            }
        } else {
            datos.put(clave, 0L);
        }
    }
} 