package com.Impulso.Alcambio.Servicio;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.function.Predicate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Lazy;
import org.springframework.transaction.annotation.Transactional;

import com.Impulso.Alcambio.Modelo.Desafio;
import com.Impulso.Alcambio.Modelo.Proyecto;
import com.Impulso.Alcambio.Modelo.Proyecto.EstadoProyecto;
import com.Impulso.Alcambio.Repositorio.DesafioRepositorio;
import com.Impulso.Alcambio.Repositorio.ProyectoRepositorio;
import com.Impulso.Alcambio.Repositorio.UsuarioRepositorio;
import com.Impulso.Alcambio.Modelo.Usuario;

import lombok.extern.slf4j.Slf4j;

/**
 * Servicio para la gestión de proyectos en la aplicación.
 * Proporciona métodos para crear, actualizar, eliminar y consultar proyectos,
 * así como para gestionar los desafíos asociados y la participación de usuarios.
 */
@Service
@Slf4j
public class ProyectoServicio {
    private final ProyectoRepositorio proyectoRepositorio;
    private final DesafioRepositorio desafioRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;
    
    @Lazy
    private final UsuarioServicio usuarioServicio;

    @Autowired
    private CacheServicio cacheServicio;

    @Autowired
    public ProyectoServicio(ProyectoRepositorio proyectoRepositorio, 
                          DesafioRepositorio desafioRepositorio,
                          UsuarioRepositorio usuarioRepositorio,
                          @Lazy UsuarioServicio usuarioServicio) {
        this.proyectoRepositorio = proyectoRepositorio;
        this.desafioRepositorio = desafioRepositorio;
        this.usuarioRepositorio = usuarioRepositorio;
        this.usuarioServicio = usuarioServicio;
    }
// OBTENER TODOS LOS PROYECTOS
    /**
     * Obtiene todos los proyectos
     * 
     * @return Lista de todos los proyectos
     */
    public List<Proyecto> obtenerTodos() {
        List<Proyecto> proyectos = proyectoRepositorio.findAll();
        
        // Actualizar estados de los proyectos según su fecha de expiración
        LocalDateTime ahora = LocalDateTime.now();
        List<Proyecto> proyectosActualizados = proyectos.stream()
            .filter(proyecto -> actualizarEstadoProyectoSiExpirado(proyecto, ahora))
            .collect(Collectors.toList());
        
        // Si hubo actualizaciones, guardarlas en la base de datos
        if (!proyectosActualizados.isEmpty()) {
            proyectoRepositorio.saveAll(proyectosActualizados);
        }
        
        return proyectos;
    }
    
    /**
     * Actualiza el estado de un proyecto a EXPIRADO si su fecha de expiración ha pasado
     * o a COMPLETO si se ha alcanzado el límite de participantes
     * @param proyecto El proyecto a verificar
     * @param ahora Fecha actual (para evitar múltiples llamadas a LocalDateTime.now())
     * @return true si el estado del proyecto fue actualizado, false en caso contrario
     */
    private boolean actualizarEstadoProyectoSiExpirado(Proyecto proyecto, LocalDateTime ahora) {
        // Solo verificar proyectos activos
        if (proyecto.getEstado() == EstadoProyecto.ACTIVO) {
            // Si tiene fecha de expiración y ya pasó
            if (proyecto.getFechaExpiracion() != null && proyecto.getFechaExpiracion().isBefore(ahora)) {
                proyecto.setEstado(EstadoProyecto.EXPIRADO);
                return true;
            }
            
            // Si se alcanzó el límite de participantes
            if (proyecto.haAlcanzadoLimiteParticipantes()) {
                proyecto.setEstado(EstadoProyecto.COMPLETO);
                return true;
            }
        }
        return false;
    }
// CREAR PROYECTO
    /**
     * Crea un nuevo proyecto
     * 
     * @param proyecto Proyecto a crear
     * @return Proyecto creado con ID asignado
     * @throws IllegalArgumentException Si el proyecto no tiene nombre o descripción
     */
    @Transactional
    public Proyecto crearProyecto(Proyecto proyecto) {
        // Validar datos básicos
        if (proyecto.getNombre() == null || proyecto.getNombre().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del proyecto es obligatorio");
        }
        
        if (proyecto.getDescripcion() == null || proyecto.getDescripcion().trim().isEmpty()) {
            throw new IllegalArgumentException("La descripción del proyecto es obligatoria");
        }
        
        // Establecer estado ACTIVO por defecto si no tiene uno
        if (proyecto.getEstado() == null) {
            proyecto.setEstado(EstadoProyecto.ACTIVO);
        }
        
        // Establecer contadores iniciales
        if (proyecto.getParticipantesActuales() == null) {
            proyecto.setParticipantesActuales(0);
        }
        
        try {
            log.info("Creando nuevo proyecto: {}", proyecto.getNombre());
            Proyecto proyectoGuardado = proyectoRepositorio.save(proyecto);
            
            // Invalidar caché relacionada con proyectos y estadísticas
            cacheServicio.eliminarPorPatron("dashboard:*");
            cacheServicio.eliminarPorPatron("proyecto:*");
            
            return proyectoGuardado;
        } catch (Exception e) {
            log.error("Error al crear proyecto: {}", e.getMessage());
            throw new RuntimeException("Error al crear el proyecto: " + e.getMessage(), e);
        }
    }
// ACTUALIZAR PROYECTO
    /**
     * Actualiza un proyecto existente
     * 
     * @param proyecto Proyecto con los datos actualizados
     * @return Proyecto actualizado
     * @throws IllegalArgumentException Si el proyecto no tiene ID
     */
    @Transactional
    public Proyecto actualizarProyecto(Proyecto proyecto) {
        if (proyecto.getId() == null || proyecto.getId().trim().isEmpty()) {
            throw new IllegalArgumentException("El ID del proyecto es obligatorio para actualizar");
        }
        
        // Verificar si el proyecto existe
        Optional<Proyecto> proyectoExistente = proyectoRepositorio.findById(proyecto.getId());
        if (proyectoExistente.isEmpty()) {
            throw new IllegalArgumentException("No existe un proyecto con el ID: " + proyecto.getId());
        }
        
        try {
            log.info("Actualizando proyecto: {}", proyecto.getNombre());
            return proyectoRepositorio.save(proyecto);
        } catch (Exception e) {
            log.error("Error al actualizar proyecto: {}", e.getMessage());
            throw new RuntimeException("Error al actualizar el proyecto: " + e.getMessage(), e);
        }
    }
// ELIMINAR PROYECTO
    /**
     * Elimina un proyecto por su ID
     * 
     * @param id ID del proyecto a eliminar
     * @throws IllegalArgumentException Si el proyecto no existe
     */
    @Transactional
    public void eliminarProyecto(String id) {
        if (!proyectoRepositorio.existsById(id)) {
            throw new IllegalArgumentException("No existe un proyecto con el ID: " + id);
        }
        
        try {
            log.info("Eliminando proyecto con ID: {}", id);
            proyectoRepositorio.deleteById(id);
        } catch (Exception e) {
            log.error("Error al eliminar proyecto: {}", e.getMessage());
            throw new RuntimeException("Error al eliminar el proyecto: " + e.getMessage(), e);
        }
    }
// OBTENER PROYECTO POR ID
    /**
     * Obtiene un proyecto por su ID
     * 
     * @param id ID del proyecto
     * @return Optional con el proyecto si existe, empty si no existe
     */
    public Optional<Proyecto> obtenerProyectoPorId(String id) {
        // Generar clave de caché
        String cacheKey = "proyecto:" + id;
        
        // Intentar obtener de caché primero
        Object cachedProyecto = cacheServicio.obtener(cacheKey);
        if (cachedProyecto != null) {
            log.info("Proyecto recuperado de caché: {}", id);
            return Optional.of((Proyecto) cachedProyecto);
        }
        
        // Si no está en caché, buscar en BD
        Optional<Proyecto> proyectoOpt = proyectoRepositorio.findById(id);
        
        // Si el proyecto existe, verificar si ha expirado
        proyectoOpt = proyectoOpt.map(proyecto -> {
            if (actualizarEstadoProyectoSiExpirado(proyecto, LocalDateTime.now())) {
                return proyectoRepositorio.save(proyecto);
            }
            return proyecto;
        });
        
        // Si se encuentra, guardar en caché para futuras consultas (15 minutos)
        proyectoOpt.ifPresent(proyecto -> {
            log.info("Guardando proyecto en caché: {}", id);
            cacheServicio.guardarConExpiracion(cacheKey, proyecto, 15, java.util.concurrent.TimeUnit.MINUTES);
        });
        
        return proyectoOpt;
    }
// BUSCAR PROYECTOS POR NOMBRE
    /**
     * Busca proyectos por nombre (búsqueda parcial)
     * 
     * @param nombre Nombre o parte del nombre a buscar
     * @return Lista de proyectos que coinciden con la búsqueda
     */
    public List<Proyecto> buscarPorNombre(String nombre) {
        return proyectoRepositorio.findByNombreContainingIgnoreCase(nombre);
    }
// OBTENER DESAFIOS DE PROYECTO
    /**
     * Obtiene los desafíos asociados a un proyecto
     * 
     * @param proyectoId ID del proyecto
     * @return Lista de desafíos asociados al proyecto
     */
    public List<Desafio> obtenerDesafiosDeProyecto(String proyectoId) {
        return obtenerProyectoPorId(proyectoId)
            .map(proyecto -> {
                List<String> desafioIds = proyecto.getDesafioIds();
                // Verificar si el proyecto tiene desafíos
                if (desafioIds != null && !desafioIds.isEmpty()) {
                    return desafioRepositorio.findAllById(desafioIds);
                }
                return List.<Desafio>of();
            })
            .orElse(List.of());
    }
// AGREGAR DESAFIO A PROYECTO
    /**
     * Agrega un desafío existente a un proyecto
     * 
     * @param proyectoId ID del proyecto
     * @param desafioId ID del desafío
     * @return Optional con el proyecto actualizado si existe, empty si alguno no existe
     * @throws IllegalArgumentException Si el desafío ya está asignado al proyecto
     */
    @Transactional
    public Optional<Proyecto> agregarDesafioAProyecto(String proyectoId, String desafioId) {
        // Obtener proyecto y desafío
        Optional<Proyecto> proyectoOpt = proyectoRepositorio.findById(proyectoId);
        Optional<Desafio> desafioOpt = desafioRepositorio.findById(desafioId);
        
        // Verificar que ambos existan
        if (proyectoOpt.isEmpty() || desafioOpt.isEmpty()) {
            log.warn("No se pudo asignar desafío: proyecto o desafío no encontrado");
            return Optional.empty();
        }
        
        Proyecto proyecto = proyectoOpt.get();
        
        // Verificar si ya tiene el desafío asignado
        if (proyecto.getDesafioIds() != null && 
            proyecto.getDesafioIds().contains(desafioId)) {
            throw new IllegalArgumentException("El desafío ya está asignado a este proyecto");
        }
        
        // Agregar el desafío al proyecto
        proyecto.agregarDesafioId(desafioId);
        
        try {
            log.info("Agregando desafío {} al proyecto {}", desafioId, proyectoId);
            return Optional.of(proyectoRepositorio.save(proyecto));
        } catch (Exception e) {
            log.error("Error al agregar desafío al proyecto: {}", e.getMessage());
            throw new RuntimeException("Error al agregar desafío al proyecto: " + e.getMessage(), e);
        }
    }
// CONTAR PROYECTOS POR ESTADO
    /**
     * Cuenta el número total de participantes en un proyecto
     * 
     * @param proyectoId ID del proyecto
     * @return Número de participantes
     */
    public int contarParticipantesProyecto(String proyectoId) {
        return (int) usuarioRepositorio.findAll().stream()
                .filter(u -> u.getProyectosParticipadosIds() != null)
                .flatMap(u -> u.getProyectosParticipadosIds().stream())
                .filter(proyectoId::equals)
                .count();
    }
    
    /**
     * Obtiene un mapa con el conteo de proyectos por cada estado     
     * @return Mapa donde las claves son los estados y los valores son la cantidad de proyectos
     */
    public Map<EstadoProyecto, Long> contarTodosLosProyectosPorEstado() {
        return proyectoRepositorio.findAll().stream()
                .collect(Collectors.groupingBy(Proyecto::getEstado, Collectors.counting()));
    }
    
    /**
     * Verifica si un usuario es miembro de un proyecto específico
     * @param proyectoId ID del proyecto
     * @param usuarioId ID del usuario
     * @return true si el usuario es miembro del proyecto, false en caso contrario
     */
    public boolean verificarMiembroProyecto(String proyectoId, String usuarioId) {
        return usuarioServicio.buscarPorId(usuarioId)
            .map(usuario -> usuario.getProyectosParticipadosIds() != null &&
                !usuario.getProyectosParticipadosIds().isEmpty() &&
                usuario.getProyectosParticipadosIds().contains(proyectoId))
            .orElse(false);
    }
    
    /**
     * Actualiza el contador de participantes de un proyecto con los datos reales de la base de datos
     * 
     * @param proyectoId ID del proyecto
     * @return Proyecto actualizado con el contador correcto
     * @throws IllegalArgumentException Si el proyecto no existe
     */
    @Transactional
    public Proyecto actualizarContadorParticipantes(String proyectoId) {
        Optional<Proyecto> proyectoOpt = proyectoRepositorio.findById(proyectoId);
        
        if (proyectoOpt.isEmpty()) {
            throw new IllegalArgumentException("No existe un proyecto con el ID: " + proyectoId);
        }
        
        Proyecto proyecto = proyectoOpt.get();
        
        // Contar participantes actuales
        int participantes = contarParticipantesProyecto(proyectoId);
        
        // Actualizar el contador
        proyecto.setParticipantesActuales(participantes);
        
        // Actualizar estado si se alcanzó el límite
        if (proyecto.haAlcanzadoLimiteParticipantes() && 
            proyecto.getEstado() == EstadoProyecto.ACTIVO) {
            proyecto.setEstado(EstadoProyecto.COMPLETO);
        }
        
        try {
            log.info("Actualizando contador de participantes para proyecto {}: {} participantes", 
                  proyectoId, participantes);
            return proyectoRepositorio.save(proyecto);
        } catch (Exception e) {
            log.error("Error al actualizar contador de participantes: {}", e.getMessage());
            throw new RuntimeException("Error al actualizar contador de participantes: " + e.getMessage(), e);
        }
    }
    
    /**
     * Intenta agregar un participante a un proyecto, verificando el límite
     * 
     * @param proyectoId ID del proyecto
     * @return true si se pudo agregar el participante, false si se alcanzó el límite
     * @throws IllegalArgumentException Si el proyecto no existe
     */
    @Transactional
    public boolean intentarAgregarParticipante(String proyectoId) {
        Optional<Proyecto> proyectoOpt = proyectoRepositorio.findById(proyectoId);
        
        if (proyectoOpt.isEmpty()) {
            throw new IllegalArgumentException("No existe un proyecto con el ID: " + proyectoId);
        }
        
        Proyecto proyecto = proyectoOpt.get();
        
        // Verificar estado del proyecto
        if (proyecto.getEstado() != EstadoProyecto.ACTIVO) {
            return false;
        }
        
        // Verificar si se alcanzó el límite
        if (proyecto.haAlcanzadoLimiteParticipantes()) {
            return false;
        }
        
        // Incrementar contador
        proyecto.incrementarParticipantes();
        
        // Actualizar estado si se alcanzó el límite
        if (proyecto.haAlcanzadoLimiteParticipantes()) {
            proyecto.setEstado(EstadoProyecto.COMPLETO);
        }
        
        try {
            log.info("Agregando participante a proyecto {}", proyectoId);
            proyectoRepositorio.save(proyecto);
            return true;
        } catch (Exception e) {
            log.error("Error al agregar participante: {}", e.getMessage());
            throw new RuntimeException("Error al agregar participante: " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene estadísticas avanzadas de proyectos para el dashboard administrativo
     * @return Mapa con todas las estadísticas
     */
    public java.util.Map<String, Object> obtenerEstadisticasProyectos() {
        java.util.Map<String, Object> estadisticas = new java.util.HashMap<>();
        
        List<Proyecto> todosProyectos = proyectoRepositorio.findAll();
        
        // Contar proyectos por estado
        Map<EstadoProyecto, Long> proyectosPorEstado = contarTodosLosProyectosPorEstado();
        
        // Proyectos con más desafíos
        List<java.util.Map<String, Object>> proyectosConMasDesafios = todosProyectos.stream()
            .filter(p -> p.getDesafioIds() != null && !p.getDesafioIds().isEmpty())
            .sorted((p1, p2) -> Integer.compare(
                p2.getDesafioIds().size(), 
                p1.getDesafioIds().size()
            ))
            .limit(5)
            .map(p -> {
                java.util.Map<String, Object> map = new java.util.HashMap<>();
                map.put("id", p.getId());
                map.put("nombre", p.getNombre());
                map.put("estado", p.getEstado());
                map.put("cantidadDesafios", p.getDesafioIds().size());
                return map;
            })
            .collect(java.util.stream.Collectors.toList());
        
        // Proyectos más recientes
        List<java.util.Map<String, Object>> proyectosRecientes = todosProyectos.stream()
            .filter(p -> p.getFechaCreacion() != null)
            .sorted((p1, p2) -> p2.getFechaCreacion().compareTo(p1.getFechaCreacion()))
            .limit(5)
            .map(p -> {
                java.util.Map<String, Object> map = new java.util.HashMap<>();
                map.put("id", p.getId());
                map.put("nombre", p.getNombre());
                map.put("estado", p.getEstado());
                map.put("fechaCreacion", p.getFechaCreacion().toString());
                return map;
            })
            .collect(java.util.stream.Collectors.toList());
        
        // Estadísticas por tiempo (proyectos por mes en el último año)
        java.util.Map<String, Long> proyectosPorMes = obtenerProyectosPorMesUltimoAnio();
        
        // Compilar todas las estadísticas
        estadisticas.put("totalProyectos", todosProyectos.size());
        estadisticas.put("proyectosPorEstado", proyectosPorEstado);
        estadisticas.put("proyectosActivos", proyectosPorEstado.getOrDefault(EstadoProyecto.ACTIVO, 0L));
        estadisticas.put("proyectosCompletados", proyectosPorEstado.getOrDefault(EstadoProyecto.COMPLETADO, 0L));
        estadisticas.put("proyectosConMasDesafios", proyectosConMasDesafios);
        estadisticas.put("proyectosRecientes", proyectosRecientes);
        estadisticas.put("proyectosPorMes", proyectosPorMes);
        
        return estadisticas;
    }
    
    /**
     * Genera un mapa con la cantidad de proyectos creados por mes en el último año
     */
    private java.util.Map<String, Long> obtenerProyectosPorMesUltimoAnio() {
        java.util.Map<String, Long> proyectosPorMes = new java.util.TreeMap<>();
        
        // Inicializar con los últimos 12 meses
        LocalDateTime ahora = LocalDateTime.now();
        for (int i = 11; i >= 0; i--) {
            LocalDateTime mesAnterior = ahora.minusMonths(i);
            String mesFormateado = mesAnterior.getYear() + "-" + 
                               String.format("%02d", mesAnterior.getMonthValue());
            proyectosPorMes.put(mesFormateado, 0L);
        }
        
        // Filtrar proyectos del último año y agrupar por mes
        Predicate<Proyecto> esDelUltimoAnio = p -> 
            p.getFechaCreacion() != null && 
            p.getFechaCreacion().isAfter(ahora.minusYears(1));
            
        proyectoRepositorio.findAll().stream()
            .filter(esDelUltimoAnio)
            .forEach(p -> {
                String mesFormateado = p.getFechaCreacion().getYear() + "-" + 
                                   String.format("%02d", p.getFechaCreacion().getMonthValue());
                proyectosPorMes.merge(mesFormateado, 1L, Long::sum);
            });
            
        return proyectosPorMes;
    }
    
    /**
     * Obtiene estadísticas detalladas de un proyecto específico
     * @param proyectoId ID del proyecto
     * @return Mapa con las estadísticas del proyecto
     */
    public java.util.Map<String, Object> obtenerEstadisticasProyecto(String proyectoId) {
        java.util.Map<String, Object> estadisticas = new java.util.HashMap<>();
        
        return obtenerProyectoPorId(proyectoId)
            .map(proyecto -> {
                // Información básica
                estadisticas.put("id", proyecto.getId());
                estadisticas.put("nombre", proyecto.getNombre());
                estadisticas.put("descripcion", proyecto.getDescripcion());
                estadisticas.put("estado", proyecto.getEstado());
                estadisticas.put("fechaCreacion", proyecto.getFechaCreacion());
                
                // Estadísticas de desafíos
                int totalDesafios = proyecto.getDesafioIds() != null ? proyecto.getDesafioIds().size() : 0;
                estadisticas.put("totalDesafios", totalDesafios);
                
                // Más estadísticas que se pueden agregar según necesidades...
                
                return estadisticas;
            })
            .orElseGet(() -> {
                estadisticas.put("error", "Proyecto no encontrado");
                return estadisticas;
            });
    }

    /**
     * Cuenta la cantidad de proyectos que tienen un estado específico
     * 
     * @param estado Estado a contar
     * @return Número de proyectos con ese estado
     */
    public long contarProyectosPorEstado(EstadoProyecto estado) {
        return proyectoRepositorio.countByEstado(estado);
    }

    /**
     * Obtiene proyectos con paginación
     * 
     * @param pageable Objeto de paginación con página y tamaño
     * @return Página de proyectos
     */
    public Page<Proyecto> obtenerTodosPaginados(Pageable pageable) {
        Page<Proyecto> proyectosPaginados = proyectoRepositorio.findAll(pageable);
        
        // Actualizar estados de los proyectos según su fecha de expiración
        LocalDateTime ahora = LocalDateTime.now();
        List<Proyecto> proyectosActualizados = proyectosPaginados.getContent().stream()
            .filter(proyecto -> actualizarEstadoProyectoSiExpirado(proyecto, ahora))
            .collect(Collectors.toList());
        
        // Si hubo actualizaciones, guardarlas en la base de datos
        if (!proyectosActualizados.isEmpty()) {
            proyectoRepositorio.saveAll(proyectosActualizados);
        }
        
        return proyectosPaginados;
    }

    /**
     * Busca proyectos por nombre (búsqueda parcial) con paginación
     * 
     * @param nombre Nombre o parte del nombre a buscar
     * @param pageable Objeto de paginación
     * @return Página de proyectos que coinciden con la búsqueda
     */
    public Page<Proyecto> buscarPorNombrePaginado(String nombre, Pageable pageable) {
        List<Proyecto> proyectos = proyectoRepositorio.findByNombreContainingIgnoreCase(nombre);
        
        // Implementar paginación manual
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), proyectos.size());
        
        // Verificar que los índices sean válidos
        if (start > proyectos.size()) {
            return Page.empty(pageable);
        }
        
        List<Proyecto> proyectosPaginados = proyectos.subList(start, end);
        return new PageImpl<>(proyectosPaginados, pageable, proyectos.size());
    }
}
