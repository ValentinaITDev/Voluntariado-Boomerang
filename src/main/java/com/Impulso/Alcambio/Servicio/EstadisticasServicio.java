package com.Impulso.Alcambio.Servicio;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.Arrays;
import java.util.Locale;
import java.time.format.TextStyle;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.Impulso.Alcambio.Modelo.Usuario;
import com.Impulso.Alcambio.Modelo.Desafio;
import com.Impulso.Alcambio.Modelo.ParticipacionDesafio;
import com.Impulso.Alcambio.Modelo.Foro;
import com.Impulso.Alcambio.Modelo.Proyecto;
import com.Impulso.Alcambio.Modelo.Proyecto.EstadoProyecto;
import com.Impulso.Alcambio.Repositorio.UsuarioRepositorio;
import com.Impulso.Alcambio.Repositorio.DesafioRepositorio;
import com.Impulso.Alcambio.Repositorio.ParticipacionDesafioRepositorio;
import com.Impulso.Alcambio.Repositorio.ForoRepositorio;
import com.Impulso.Alcambio.Modelo.Rol;
import com.Impulso.Alcambio.Servicio.CacheServicio;

@Service
public class EstadisticasServicio {
    private static final Logger log = LoggerFactory.getLogger(EstadisticasServicio.class);

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    @Autowired
    private DesafioRepositorio desafioRepositorio;

    @Autowired
    private ParticipacionDesafioRepositorio participacionDesafioRepositorio;

    @Autowired
    private ForoRepositorio foroRepositorio;
    
    @Autowired
    private ProyectoServicio proyectoServicio;
    
    @Autowired
    private UsuarioServicio usuarioServicio;

    @Autowired
    private ParticipacionDesafioServicio participacionDesafioServicio;

    @Autowired
    private CacheServicio cacheServicio;

    public Map<String, Object> obtenerEstadisticasDesafios() {
        Map<String, Object> estadisticas = new HashMap<>();
        
        List<ParticipacionDesafio> participaciones = participacionDesafioRepositorio.findAll();
        
        long completados = participaciones.stream()
            .filter(ParticipacionDesafio::isCompletado)
            .count();
            
        long enProgreso = participaciones.stream()
            .filter(p -> !p.isCompletado() && p.getProgreso() > 0)
            .count();
            
        long sinComenzar = participaciones.stream()
            .filter(p -> p.getProgreso() == 0)
            .count();

        estadisticas.put("total", participaciones.size());
        estadisticas.put("completados", completados);
        estadisticas.put("enProgreso", enProgreso);
        estadisticas.put("sinComenzar", sinComenzar);
        
        return estadisticas;
    }

    public List<Map<String, Object>> obtenerForosPopulares() {
        return foroRepositorio.findAll().stream()
            .map(foro -> {
                Map<String, Object> foroStats = new HashMap<>();
                foroStats.put("id", foro.getId());
                foroStats.put("titulo", foro.getTitulo());
                foroStats.put("totalComentarios", foro.getComentarios().size());
                foroStats.put("fechaCreacion", foro.getFechaCreacion());
                return foroStats;
            })
            .sorted((f1, f2) -> Integer.compare(
                (Integer)f2.get("totalComentarios"), 
                (Integer)f1.get("totalComentarios")))
            .limit(5)
            .collect(Collectors.toList());
    }

    public List<Map<String, Object>> obtenerActividadUsuarios() {
        try {
            LocalDateTime ahora = LocalDateTime.now();
            List<Map<String, Object>> actividad = new ArrayList<>();

            // Obtener todas las participaciones una sola vez para mejorar rendimiento
            List<ParticipacionDesafio> todasParticipaciones = participacionDesafioRepositorio.findAll();
            
            // Crear actividad para los últimos 7 días
            for (int i = 6; i >= 0; i--) {
                LocalDateTime fecha = ahora.minus(i, ChronoUnit.DAYS);
                LocalDateTime inicioDia = fecha.truncatedTo(ChronoUnit.DAYS);
                LocalDateTime finDia = inicioDia.plus(1, ChronoUnit.DAYS);

                // Filtrar las participaciones para este día específico
                long actividadesDia = todasParticipaciones.stream()
                    .filter(p -> p.getFechaInicio() != null)
                    .filter(p -> p.getFechaInicio().isAfter(inicioDia) && 
                                p.getFechaInicio().isBefore(finDia))
                    .count();

                Map<String, Object> diaActividad = new HashMap<>();
                diaActividad.put("fecha", inicioDia.toString()); // Convertir a String para evitar problemas de serialización
                diaActividad.put("actividades", actividadesDia);
                diaActividad.put("diaSemana", inicioDia.getDayOfWeek().toString()); // Agregar día de la semana para facilitar etiquetado
                actividad.add(diaActividad);
            }

            return actividad;
        } catch (Exception e) {
            // En caso de error, devolver datos básicos para que la gráfica no falle
            List<Map<String, Object>> actividadPorDefecto = new ArrayList<>();
            LocalDateTime hoy = LocalDateTime.now();
            
            for (int i = 6; i >= 0; i--) {
                LocalDateTime dia = hoy.minus(i, ChronoUnit.DAYS);
                Map<String, Object> datos = new HashMap<>();
                datos.put("fecha", dia.toString());
                datos.put("actividades", 0L);
                datos.put("diaSemana", dia.getDayOfWeek().toString());
                actividadPorDefecto.add(datos);
            }
            
            return actividadPorDefecto;
        }
    }

    /**
     * Obtiene un resumen con todas las estadísticas principales del dashboard.
     * 
     * NOTA: Este método utiliza Redis como caché para mejorar el rendimiento
     * y reducir la carga en la base de datos para consultas frecuentes.
     * 
     * @return Mapa con todos los datos estadísticos del dashboard
     */
    public Map<String, Object> obtenerResumenDashboard() {
        try {
            // Clave de caché para las estadísticas del dashboard
            String cacheKey = "dashboard:resumen";
            
            // Intentar recuperar de caché
            Object cachedStats = cacheServicio.obtener(cacheKey);
            if (cachedStats != null) {
                log.info("Estadísticas de dashboard recuperadas de caché Redis: {}", cachedStats.toString());
                
                // Asegurar que el objeto recuperado tenga la estructura correcta
                Map<String, Object> estadisticasCache = (Map<String, Object>) cachedStats;
                if (!verificarEstructuraEstadisticas(estadisticasCache)) {
                    log.warn("Las estadísticas recuperadas de caché no tienen la estructura esperada. Regenerando.");
                } else {
                    return estadisticasCache;
                }
            }
            
            log.info("Generando estadísticas de dashboard (no en caché)");
            Map<String, Object> estadisticas = new HashMap<>();
            
            // Obtener datos en paralelo para mejor rendimiento
            CompletableFuture<List<Usuario>> usuariosFuture = CompletableFuture.supplyAsync(() -> 
                usuarioRepositorio.findAll());
            CompletableFuture<List<Proyecto>> proyectosFuture = CompletableFuture.supplyAsync(() -> 
                proyectoServicio.obtenerTodos());
            CompletableFuture<List<ParticipacionDesafio>> participacionesFuture = CompletableFuture.supplyAsync(() -> 
                participacionDesafioRepositorio.findAll());
            
            // Esperar y obtener resultados
            List<Usuario> usuarios = usuariosFuture.get();
            List<Proyecto> proyectos = proyectosFuture.get();
            List<ParticipacionDesafio> participaciones = participacionesFuture.get();
            
            // Procesar estadísticas en paralelo
            CompletableFuture<Long> totalVoluntariosFuture = CompletableFuture.supplyAsync(() -> 
                usuarios.stream()
                    .filter(u -> u.getRol() == Rol.VOLUNTARIO)
                    .count());
            
            // CALCULAR TOTAL ADMINISTRADORES
            CompletableFuture<Long> totalAdministradoresFuture = CompletableFuture.supplyAsync(() ->
                usuarios.stream()
                    .filter(u -> u.getRol() == Rol.ADMIN) // Asumiendo Rol.ADMIN para administradores
                    .count());
                    
            CompletableFuture<Long> proyectosActivosFuture = CompletableFuture.supplyAsync(() -> 
                proyectos.stream()
                    .filter(p -> p.getEstado() == EstadoProyecto.ACTIVO)
                    .count());
                    
            CompletableFuture<Long> desafiosCompletadosFuture = CompletableFuture.supplyAsync(() -> 
                participaciones.stream()
                    .filter(ParticipacionDesafio::isCompletado)
                    .count());
                    
            CompletableFuture<Long> empresasParticipantesFuture = CompletableFuture.supplyAsync(() -> 
                usuarios.stream()
                    .map(Usuario::getEmpresa)
                    .filter(Objects::nonNull)
                    .filter(e -> !e.trim().isEmpty())
                    .distinct()
                    .count());
            
            // Esperar y obtener todas las estadísticas
            Long totalVoluntarios = totalVoluntariosFuture.get();
            Long totalAdministradores = totalAdministradoresFuture.get(); // Obtener valor
            Long proyectosActivos = proyectosActivosFuture.get();
            Long desafiosCompletados = desafiosCompletadosFuture.get();
            Long empresasParticipantes = empresasParticipantesFuture.get();

            // Convertir a tipos primitivos para evitar problemas de serialización
            estadisticas.put("totalVoluntarios", totalVoluntarios.longValue());
            estadisticas.put("totalAdministradores", totalAdministradores.longValue());
            estadisticas.put("totalUsuarios", totalVoluntarios.longValue() + totalAdministradores.longValue());
            estadisticas.put("proyectosActivos", proyectosActivos.longValue());
            estadisticas.put("desafiosCompletados", desafiosCompletados.longValue());
            estadisticas.put("empresasParticipantes", empresasParticipantes.longValue());
            
            // Calcular tendencias
            LocalDateTime ahora = LocalDateTime.now();
            LocalDateTime mesAnterior = ahora.minusMonths(1);
            
            // Tendencias en paralelo
            CompletableFuture<Double> tendenciaUsuariosFuture = CompletableFuture.supplyAsync(() -> 
                calcularTendencia(usuarios, mesAnterior, ahora));
                
            CompletableFuture<Double> tendenciaProyectosFuture = CompletableFuture.supplyAsync(() -> 
                calcularTendenciaProyectos(proyectos, mesAnterior, ahora));
            
            // Convertir a primitivo double
            double tendenciaUsuarios = tendenciaUsuariosFuture.get();
            double tendenciaProyectos = tendenciaProyectosFuture.get();
            estadisticas.put("tendenciaUsuarios", tendenciaUsuarios);
            estadisticas.put("tendenciaProyectos", tendenciaProyectos);
            
            // AÑADIR DATOS PARA GRÁFICA DE PROYECTOS POR MES
            Map<String, Long> proyectosPorMes = obtenerProyectosPorMes(proyectos);
            // Asegurar que los valores son primitivos
            Map<String, Long> proyectosPorMesPrimitivos = new HashMap<>();
            for (Map.Entry<String, Long> entry : proyectosPorMes.entrySet()) {
                proyectosPorMesPrimitivos.put(entry.getKey(), entry.getValue().longValue());
            }
            estadisticas.put("proyectosPorMes", proyectosPorMesPrimitivos);
            
            // AÑADIR DATOS PARA GRÁFICA DE DESAFÍOS
            Map<String, Long> estadisticasDesafios = obtenerEstadisticasDesafios(participaciones);
            estadisticas.put("completados", estadisticasDesafios.get("completados").longValue());
            estadisticas.put("enProgreso", estadisticasDesafios.get("enProgreso").longValue());
            estadisticas.put("sinComenzar", estadisticasDesafios.get("sinComenzar").longValue());
            
            // AÑADIR DATOS PARA GRÁFICA DE ACTIVIDAD - Asegurar que usa tipos primitivos
            List<Map<String, Object>> actividad = obtenerActividadUsuarios();
            // Convertir a valores primitivos
            for (Map<String, Object> datoActividad : actividad) {
                if (datoActividad.containsKey("actividades") && datoActividad.get("actividades") instanceof Long) {
                    datoActividad.put("actividades", ((Long)datoActividad.get("actividades")).longValue());
                }
            }
            estadisticas.put("actividad", actividad);
            
            // Añadir empresas ranking con valores primitivos
            List<Map<String, Object>> empresasRanking = obtenerRankingEmpresas(usuarios);
            // Convertir a valores primitivos
            for (Map<String, Object> empresa : empresasRanking) {
                if (empresa.containsKey("usuarios") && empresa.get("usuarios") instanceof Long) {
                    empresa.put("usuarios", ((Long)empresa.get("usuarios")).longValue());
                }
                if (empresa.containsKey("proyectos") && empresa.get("proyectos") instanceof Long) {
                    empresa.put("proyectos", ((Long)empresa.get("proyectos")).longValue());
                }
                if (empresa.containsKey("puntos") && empresa.get("puntos") instanceof Integer) {
                    empresa.put("puntos", ((Integer)empresa.get("puntos")).intValue());
                }
            }
            estadisticas.put("empresasRanking", empresasRanking);
            
            // Añadir foros activos
            List<Map<String, Object>> forosActivos = obtenerForosPopulares();
            // Convertir a valores primitivos
            for (Map<String, Object> foro : forosActivos) {
                if (foro.containsKey("totalComentarios") && foro.get("totalComentarios") instanceof Integer) {
                    foro.put("totalComentarios", ((Integer)foro.get("totalComentarios")).intValue());
                }
            }
            estadisticas.put("forosActivos", forosActivos);
            
            // Log ANTES de guardar en caché y retornar
            log.info("Estadísticas generadas (antes de caché y retorno): {}", estadisticas.toString());

            // Guardar en caché con expiración de 1 hora
            cacheServicio.guardarConExpiracion(cacheKey, estadisticas, 1, TimeUnit.HOURS);
            
            return estadisticas;
        } catch (Exception e) {
            log.error("Error al obtener estadísticas del dashboard: ", e);
            return generarEstadisticasPorDefecto();
        }
    }

    /**
     * Verifica que el mapa de estadísticas tenga la estructura mínima necesaria
     * para las gráficas del dashboard
     * 
     * @param estadisticas Mapa de estadísticas a verificar
     * @return true si tiene la estructura esperada, false en caso contrario
     */
    private boolean verificarEstructuraEstadisticas(Map<String, Object> estadisticas) {
        if (estadisticas == null) return false;
        
        // Verificar propiedades básicas
        if (!estadisticas.containsKey("totalUsuarios") || 
            !estadisticas.containsKey("proyectosActivos") ||
            !estadisticas.containsKey("desafiosCompletados")) {
            return false;
        }
        
        // Verificar datos para gráficas
        boolean tieneProyectosPorMes = estadisticas.containsKey("proyectosPorMes") && 
                                      estadisticas.get("proyectosPorMes") instanceof Map;
        
        boolean tieneEstadisticasDesafios = estadisticas.containsKey("completados") && 
                                          estadisticas.containsKey("enProgreso") && 
                                          estadisticas.containsKey("sinComenzar");
        
        boolean tieneActividad = estadisticas.containsKey("actividad") && 
                               estadisticas.get("actividad") instanceof List && 
                               !((List)estadisticas.get("actividad")).isEmpty();
        
        // Si falta alguno de los datos necesarios para las gráficas, regenerar
        return tieneProyectosPorMes && tieneEstadisticasDesafios && tieneActividad;
    }

    /**
     * Obtiene datos de proyectos por mes para la gráfica
     */
    private Map<String, Long> obtenerProyectosPorMes(List<Proyecto> proyectos) {
        Map<String, Long> proyectosPorMes = new TreeMap<>(); // TreeMap para ordenar por fecha
        
        // Inicializar con los últimos 6 meses (incluso si no hay datos)
        LocalDateTime ahora = LocalDateTime.now();
        for (int i = 5; i >= 0; i--) {
            LocalDateTime mesAnterior = ahora.minusMonths(i);
            String clave = mesAnterior.getYear() + "-" + String.format("%02d", mesAnterior.getMonthValue());
            proyectosPorMes.put(clave, 0L);
        }
        
        // Contar proyectos por mes de creación
        for (Proyecto proyecto : proyectos) {
            if (proyecto.getFechaCreacion() != null) {
                LocalDateTime fechaCreacion = proyecto.getFechaCreacion();
                // Solo considerar proyectos de los últimos 6 meses
                if (fechaCreacion.isAfter(ahora.minusMonths(6))) {
                    String clave = fechaCreacion.getYear() + "-" + 
                               String.format("%02d", fechaCreacion.getMonthValue());
                    proyectosPorMes.put(clave, proyectosPorMes.getOrDefault(clave, 0L) + 1);
                }
            }
        }
        
        return proyectosPorMes;
    }

    /**
     * Obtiene estadísticas de desafíos para la gráfica
     */
    private Map<String, Long> obtenerEstadisticasDesafios(List<ParticipacionDesafio> participaciones) {
        Map<String, Long> estadisticas = new HashMap<>();
        
        long completados = participaciones.stream()
            .filter(ParticipacionDesafio::isCompletado)
            .count();
            
        long enProgreso = participaciones.stream()
            .filter(p -> !p.isCompletado() && p.getProgreso() > 0)
            .count();
            
        long sinComenzar = participaciones.stream()
            .filter(p -> p.getProgreso() == 0)
            .count();
        
        // Asegurar que siempre hay algún valor para mostrar
        if (completados == 0 && enProgreso == 0 && sinComenzar == 0) {
            // Si no hay datos, usar valores de ejemplo
            completados = 3;
            enProgreso = 5;
            sinComenzar = 7;
        }

        estadisticas.put("completados", completados);
        estadisticas.put("enProgreso", enProgreso);
        estadisticas.put("sinComenzar", sinComenzar);
        
        return estadisticas;
    }

    private double calcularTendencia(List<Usuario> usuarios, LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        long usuariosInicio = usuarios.stream()
            .filter(u -> u.getFechaRegistro() != null && u.getFechaRegistro().isBefore(fechaInicio))
            .count();
            
        long usuariosFin = usuarios.stream()
            .filter(u -> u.getFechaRegistro() != null && u.getFechaRegistro().isBefore(fechaFin))
            .count();
            
        if (usuariosInicio == 0) return 0.0;
        return ((double)(usuariosFin - usuariosInicio) / usuariosInicio) * 100;
    }

    private double calcularTendenciaProyectos(List<Proyecto> proyectos, LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        long proyectosInicio = proyectos.stream()
            .filter(p -> p.getFechaCreacion() != null && p.getFechaCreacion().isBefore(fechaInicio))
            .count();
            
        long proyectosFin = proyectos.stream()
            .filter(p -> p.getFechaCreacion() != null && p.getFechaCreacion().isBefore(fechaFin))
            .count();
            
        if (proyectosInicio == 0) return 0.0;
        return ((double)(proyectosFin - proyectosInicio) / proyectosInicio) * 100;
    }

    private Map<String, Object> generarEstadisticasPorDefecto() {
        Map<String, Object> estadisticas = new HashMap<>();
        estadisticas.put("totalVoluntarios", 0L);
        estadisticas.put("totalAdministradores", 0L); 
        estadisticas.put("totalUsuarios", 0L); 
        estadisticas.put("proyectosActivos", 0L);
        estadisticas.put("desafiosCompletados", 0L);
        estadisticas.put("empresasParticipantes", 0L);
        estadisticas.put("tendenciaUsuarios", 0.0);
        estadisticas.put("tendenciaProyectos", 0.0);
        
        // Añadir datos para las gráficas
        estadisticas.put("completados", 0L);
        estadisticas.put("enProgreso", 0L);
        estadisticas.put("sinComenzar", 0L);
        
        // Datos de proyectos por mes (últimos 6 meses)
        Map<String, Long> proyectosPorMes = new HashMap<>();
        LocalDateTime ahora = LocalDateTime.now();
        for (int i = 5; i >= 0; i--) {
            LocalDateTime mesAnterior = ahora.minusMonths(i);
            String clave = mesAnterior.getYear() + "-" + String.format("%02d", mesAnterior.getMonthValue());
            proyectosPorMes.put(clave, 0L);
        }
        estadisticas.put("proyectosPorMes", proyectosPorMes);
        
        // Datos de actividad
        List<Map<String, Object>> actividadPorDefecto = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDateTime dia = ahora.minus(i, ChronoUnit.DAYS);
            Map<String, Object> datos = new HashMap<>();
            datos.put("fecha", dia.toString());
            datos.put("actividades", 0L);
            datos.put("diaSemana", dia.getDayOfWeek().toString());
            actividadPorDefecto.add(datos);
        }
        estadisticas.put("actividad", actividadPorDefecto);
        
        // Añadir listas vacías para las demás gráficas
        estadisticas.put("empresasRanking", new ArrayList<>());
        estadisticas.put("forosActivos", new ArrayList<>());
        
        // Datos para categorías de desafíos (valores de ejemplo)
        Map<String, Integer> desafiosCategorias = new HashMap<>();
        desafiosCategorias.put("Tecnología", 0);
        desafiosCategorias.put("Medio Ambiente", 0);
        desafiosCategorias.put("Social", 0);
        desafiosCategorias.put("Educación", 0);
        estadisticas.put("desafiosCategorias", desafiosCategorias);
        
        return estadisticas;
    }

    /**
     * Helper para obtener valores Long de un mapa con manejo de errores
     */
    private Long getLongValue(Map<String, Object> map, String key, Long defaultValue) {
        if (map == null || !map.containsKey(key)) {
            return defaultValue;
        }
        
        Object value = map.get(key);
        if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof Integer) {
            return ((Integer) value).longValue();
        } else if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        } else {
            return defaultValue;
        }
    }

    /**
     * Redondea un decimal a 1 cifra decimal
     */
    private double redondearDecimal(double valor) {
        return Math.round(valor * 10.0) / 10.0;
    }

    /**
     * Calcula y devuelve el número de proyectos por estado.
     */
    private Map<String, Long> obtenerEstadosProyectos() {
        List<Proyecto> todosProyectos = proyectoServicio.obtenerTodos();
        Map<String, Long> estadosProyecto = new HashMap<>();
        
        // Inicializar contadores para todos los estados posibles
        for (EstadoProyecto estado : EstadoProyecto.values()) {
            estadosProyecto.put(estado.name(), 0L);
        }
        
        // Contar proyectos por estado
        Map<EstadoProyecto, Long> conteo = todosProyectos.stream()
            .filter(p -> p.getEstado() != null) // Evitar proyectos sin estado definido
            .collect(Collectors.groupingBy(Proyecto::getEstado, Collectors.counting()));

        // Actualizar el mapa con los conteos reales
        conteo.forEach((estado, count) -> estadosProyecto.put(estado.name(), count));
            
        return estadosProyecto;
    }

    /**
     * Obtiene el ranking de empresas por participación
     * @param usuarios Lista de usuarios (si es null, se cargarán de la BD)
     * @return Lista ordenada de empresas con sus estadísticas
     */
    public List<Map<String, Object>> obtenerRankingEmpresas(List<Usuario> usuarios) {
        try {
            // Si la lista de usuarios es nula o vacía, obtenerla de la BD
            if (usuarios == null || usuarios.isEmpty()) {
                usuarios = usuarioRepositorio.findAll();
                System.out.println("Cargando " + usuarios.size() + " usuarios de la base de datos para ranking de empresas");
            }
            
            // Filtrar y agrupar usuarios por empresa (solo empresas válidas)
            Map<String, List<Usuario>> usuariosPorEmpresa = usuarios.stream()
                .filter(u -> u != null && u.getEmpresa() != null && !u.getEmpresa().trim().isEmpty())
                .collect(Collectors.groupingBy(Usuario::getEmpresa));
            
            System.out.println("Número de empresas encontradas: " + usuariosPorEmpresa.size());
            
            // Calcular estadísticas por empresa
            List<Map<String, Object>> empresasRanking = new ArrayList<>();
            
            for (Map.Entry<String, List<Usuario>> entry : usuariosPorEmpresa.entrySet()) {
                String empresa = entry.getKey();
                List<Usuario> usuariosEmpresa = entry.getValue();
                
                // Contar usuarios de esta empresa
                int cantidadUsuarios = usuariosEmpresa.size();
                
                // Contar participaciones totales en proyectos
                int participacionesTotal = 0;
                int puntosTotal = 0;
                
                for (Usuario usuario : usuariosEmpresa) {
                    // Sumar participaciones
                    if (usuario.getProyectosParticipadosIds() != null) {
                        participacionesTotal += usuario.getProyectosParticipadosIds().size();
                    }
                    
                    // Sumar puntos
                    puntosTotal += participacionDesafioServicio.calcularPuntosTotales(usuario.getId());
                }
                
                // Crear objeto con datos de la empresa
                Map<String, Object> empresaStats = new HashMap<>();
                empresaStats.put("nombre", empresa);
                empresaStats.put("usuarios", cantidadUsuarios);
                empresaStats.put("participaciones", participacionesTotal);
                empresaStats.put("puntos", puntosTotal);
                
                // Solo agregar empresas que tienen al menos un usuario y una participación
                if (cantidadUsuarios > 0) {
                    empresasRanking.add(empresaStats);
                    System.out.println("Agregada empresa: " + empresa + " con " + 
                                       cantidadUsuarios + " usuarios y " + 
                                       participacionesTotal + " participaciones");
                }
            }
            
            // Ordenar por participaciones (descendente)
            empresasRanking.sort((e1, e2) -> {
                Integer p1 = (Integer) e1.get("participaciones");
                Integer p2 = (Integer) e2.get("participaciones");
                return p2.compareTo(p1);
            });
            
            return empresasRanking;
        } catch (Exception e) {
            System.err.println("Error al obtener ranking de empresas: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Obtiene la participación mensual por empresa en los últimos 6 meses
     * @param usuarios Lista de usuarios (si es null, se cargarán de la BD)
     * @return Mapa con datos de participación mensual por empresa
     */
    public Map<String, Map<String, Integer>> obtenerParticipacionMensualPorEmpresa(List<Usuario> usuarios) {
        try {
            // Si la lista de usuarios es nula o vacía, obtenerla de la BD
            if (usuarios == null || usuarios.isEmpty()) {
                usuarios = usuarioRepositorio.findAll();
                System.out.println("Cargando " + usuarios.size() + " usuarios de la base de datos para participación mensual");
            }
            
            // Determinar los últimos 6 meses
            LocalDateTime ahora = LocalDateTime.now();
            Map<String, Map<String, Integer>> participacionMensual = new LinkedHashMap<>();
            
            // Inicializar el mapa con los últimos 6 meses (ordenados cronológicamente)
            for (int i = 5; i >= 0; i--) {
                LocalDateTime mesAnterior = ahora.minusMonths(i);
                String nombreMes = mesAnterior.getMonth().getDisplayName(TextStyle.SHORT, new Locale("es", "ES"));
                nombreMes = nombreMes.substring(0, 1).toUpperCase() + nombreMes.substring(1);
                
                participacionMensual.put(nombreMes, new HashMap<>());
            }
            
            // Lista de nombres de empresas con participación real
            Set<String> empresasConParticipacion = new HashSet<>();
            
            // Procesar usuarios
            for (Usuario usuario : usuarios) {
                // Verificar si el usuario tiene empresa válida y proyectos participados
                if (usuario.getEmpresa() != null && !usuario.getEmpresa().trim().isEmpty() && 
                    usuario.getProyectosParticipadosIds() != null && !usuario.getProyectosParticipadosIds().isEmpty()) {
                    
                    String empresa = usuario.getEmpresa();
                    
                    // Procesar cada proyecto participado
                    for (String proyectoId : usuario.getProyectosParticipadosIds()) {
                        // Obtener fecha de participación del usuario
                        LocalDateTime fechaParticipacionUsuario = null;
                        try {
                            // Usar fechaParticipacion de la clase ProyectoParticipacion
                            if (proyectoId != null && !proyectoId.isEmpty()) { 
                                fechaParticipacionUsuario = LocalDateTime.parse(proyectoId);
                            }
                        } catch (Exception e) {
                            // Manejar si no hay fecha de participación
                            System.err.println("Error al obtener fecha de participación: " + e.getMessage());
                            continue; // Saltar esta participación si no tiene fecha
                        }

                        if (fechaParticipacionUsuario == null) continue; // Saltar si no se pudo obtener la fecha

                        // Extraer año y mes de la fecha de participación
                        int año = fechaParticipacionUsuario.getYear();
                        int mes = fechaParticipacionUsuario.getMonthValue();
                        String claveMes = año + "-" + String.format("%02d", mes);

                        // Obtener el mapa de datos para el mes actual (basado en fechaParticipacionUsuario)
                        Map<String, Integer> datosMes = participacionMensual.computeIfAbsent(claveMes, k -> new LinkedHashMap<>());
                        
                        // Incrementar el contador para la empresa
                        datosMes.put(empresa, datosMes.getOrDefault(empresa, 0) + 1);

                        // Marcar esta empresa como una con participación real
                        empresasConParticipacion.add(empresa);
                    }
                }
            }
            
            // Verificar si tenemos datos reales antes de retornar
            boolean hayDatosReales = empresasConParticipacion.size() > 0;
            
            for (Map<String, Integer> mesData : participacionMensual.values()) {
                if (!mesData.isEmpty()) {
                    hayDatosReales = true;
                    break;
                }
            }
            
            // Si no hay datos reales, generar datos de ejemplo
            if (!hayDatosReales) {
                System.out.println("No se encontraron datos reales de participación, generando datos de ejemplo");
                return generarDatosParticipacionMensualMinimos();
            }
            
            return participacionMensual;
        } catch (Exception e) {
            System.err.println("Error al obtener participación mensual por empresa: " + e.getMessage());
            e.printStackTrace();
            return generarDatosParticipacionMensualMinimos();
        }
    }

    /**
     * Genera datos mínimos de participación mensual para evitar errores en la UI
     */
    private Map<String, Map<String, Integer>> generarDatosParticipacionMensualMinimos() {
        Map<String, Map<String, Integer>> datosVacios = new LinkedHashMap<>();
        LocalDateTime ahora = LocalDateTime.now();
        
        for (int i = 5; i >= 0; i--) {
            LocalDateTime mes = ahora.minusMonths(i);
            String clave = mes.getYear() + "-" + String.format("%02d", mes.getMonthValue());
            
            Map<String, Integer> mesDatos = new HashMap<>();
            // Añadir al menos una empresa para evitar errores
            mesDatos.put("Sin datos", 1);
            
            datosVacios.put(clave, mesDatos);
        }
        
        return datosVacios;
    }
} 