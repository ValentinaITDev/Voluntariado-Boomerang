package com.Impulso.Alcambio.Servicio;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;

import com.Impulso.Alcambio.Modelo.Desafio;
import com.Impulso.Alcambio.Repositorio.DesafioRepositorio;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DesafioServicio {
    
    private final DesafioRepositorio desafioRepositorio;
    
    @Autowired
    private CacheServicio cacheServicio;
    
    @Autowired
    public DesafioServicio(DesafioRepositorio desafioRepositorio) {
        this.desafioRepositorio = desafioRepositorio;
    }
    
    /**
     * Obtiene todos los desafíos
     */
    @Cacheable(value = "desafios", key = "'all'")
    public List<Desafio> obtenerTodos() {
        log.debug("Cargando todos los desafíos desde base de datos");
        return desafioRepositorio.findAll();
    }
    
    /**
     * Obtiene desafíos filtrados por proyecto
     */
    public List<Desafio> obtenerPorProyecto(String proyectoId) {
        String cacheKey = "desafios:proyecto:" + proyectoId;
        
        // Intentar obtener de caché
        Object cachedResult = cacheServicio.obtener(cacheKey);
        if (cachedResult != null) {
            log.debug("Recuperando desafíos por proyecto {} de caché", proyectoId);
            return (List<Desafio>) cachedResult;
        }
        
        // Si no está en caché, buscar en base de datos
        log.debug("Cargando desafíos para proyecto {} desde base de datos", proyectoId);
        List<Desafio> desafios = desafioRepositorio.findByProyectoId(proyectoId);
        
        // Guardar en caché por 10 minutos
        cacheServicio.guardarConExpiracion(cacheKey, desafios, 10, TimeUnit.MINUTES);
        
        return desafios;
    }
    
    /**
     * Obtiene desafíos filtrados por tipo
     */
    public List<Desafio> obtenerPorTipo(Desafio.TipoDesafio tipo) {
        String cacheKey = "desafios:tipo:" + tipo.name();
        
        // Intentar obtener de caché
        Object cachedResult = cacheServicio.obtener(cacheKey);
        if (cachedResult != null) {
            log.debug("Recuperando desafíos por tipo {} de caché", tipo);
            return (List<Desafio>) cachedResult;
        }
        
        // Si no está en caché, buscar en base de datos
        log.debug("Cargando desafíos de tipo {} desde base de datos", tipo);
        List<Desafio> desafios = desafioRepositorio.findByTipo(tipo);
        
        // Guardar en caché por 15 minutos
        cacheServicio.guardarConExpiracion(cacheKey, desafios, 15, TimeUnit.MINUTES);
        
        return desafios;
    }
    
    /**
     * Obtiene un desafío por su ID
     */
    public Optional<Desafio> obtenerPorId(String id) {
        String cacheKey = "desafio:" + id;
        
        // Intentar obtener de caché
        Object cachedDesafio = cacheServicio.obtener(cacheKey);
        if (cachedDesafio != null) {
            log.debug("Desafío {} recuperado de caché", id);
            return Optional.of((Desafio) cachedDesafio);
        }
        
        // Si no está en caché, buscar en base de datos
        log.debug("Cargando desafío {} desde base de datos", id);
        Optional<Desafio> desafioOpt = desafioRepositorio.findById(id);
        
        // Si existe, guardar en caché
        desafioOpt.ifPresent(desafio -> {
            cacheServicio.guardarConExpiracion(cacheKey, desafio, 30, TimeUnit.MINUTES);
        });
        
        return desafioOpt;
    }
    
    /**
     * Guarda un desafío (crear o actualizar)
     */
    public Desafio guardar(Desafio desafio) {
        // Si tiene ID, es una actualización
        if (desafio.getId() != null && !desafio.getId().isEmpty()) {
            return obtenerPorId(desafio.getId())
                .map(desafioExistente -> {
                    // Preservar datos que no deben cambiar si no se proporcionan
                    if (desafio.getProyectoId() == null || desafio.getProyectoId().isEmpty()) {
                        desafio.setProyectoId(desafioExistente.getProyectoId());
                    }
                    if (desafio.getFechaInicio() == null) {
                        desafio.setFechaInicio(desafioExistente.getFechaInicio());
                    }
                    if (desafio.getCriterios() == null || desafio.getCriterios().isEmpty()) {
                        desafio.setCriterios(desafioExistente.getCriterios());
                    }
                    
                    // Guardar desafío actualizado
                    Desafio desafioActualizado = desafioRepositorio.save(desafio);
                    
                    // Actualizar caché
                    String cacheKey = "desafio:" + desafio.getId();
                    cacheServicio.guardarConExpiracion(cacheKey, desafioActualizado, 30, TimeUnit.MINUTES);
                    
                    // Invalidar cachés relacionadas
                    cacheServicio.eliminarPorPatron("desafios:proyecto:" + desafio.getProyectoId());
                    cacheServicio.eliminarPorPatron("desafios:*");
                    
                    return desafioActualizado;
                })
                .orElse(desafioRepositorio.save(desafio)); // Si no existe, guardar como nuevo
        } else {
            // Nuevo desafío
            Desafio nuevoDesafio = desafioRepositorio.save(desafio);
            
            // Invalidar cachés relacionadas
            if (nuevoDesafio.getProyectoId() != null) {
                cacheServicio.eliminarPorPatron("desafios:proyecto:" + nuevoDesafio.getProyectoId());
            }
            cacheServicio.eliminarPorPatron("desafios:*");
            
            return nuevoDesafio;
        }
    }
    
    /**
     * Elimina un desafío por su ID
     */
    public void eliminar(String id) {
        // Obtener el desafío antes de eliminarlo para invalidar cachés relacionadas
        Optional<Desafio> desafioOpt = obtenerPorId(id);
        
        desafioRepositorio.deleteById(id);
        
        // Eliminar de la caché
        String cacheKey = "desafio:" + id;
        cacheServicio.eliminar(cacheKey);
        
        // Invalidar cachés relacionadas
        desafioOpt.ifPresent(desafio -> {
            if (desafio.getProyectoId() != null) {
                cacheServicio.eliminarPorPatron("desafios:proyecto:" + desafio.getProyectoId());
            }
        });
        
        cacheServicio.eliminarPorPatron("desafios:*");
    }
    
    /**
     * Obtiene los desafíos activos (con fecha de fin posterior a la actual)
     */
    public List<Desafio> obtenerDesafiosActivos() {
        String cacheKey = "desafios:activos";
        
        // Intentar obtener de caché
        Object cachedResult = cacheServicio.obtener(cacheKey);
        if (cachedResult != null) {
            log.debug("Recuperando desafíos activos de caché");
            return (List<Desafio>) cachedResult;
        }
        
        LocalDateTime ahora = LocalDateTime.now();
        List<Desafio> desafiosActivos = desafioRepositorio.findAll().stream()
                .filter(d -> d.getFechaFin() == null || d.getFechaFin().isAfter(ahora))
                .collect(Collectors.toList());
        
        // Guardar en caché por un tiempo más corto (5 minutos) porque la "actividad" puede cambiar
        cacheServicio.guardarConExpiracion(cacheKey, desafiosActivos, 5, TimeUnit.MINUTES);
        
        return desafiosActivos;
    }
    
    /**
     * Obtiene desafíos por palabra clave en nombre o descripción
     */
    public List<Desafio> buscarPorPalabraClave(String palabraClave) {
        String cacheKey = "desafios:busqueda:" + palabraClave.toLowerCase();
        
        // Intentar obtener de caché
        Object cachedResult = cacheServicio.obtener(cacheKey);
        if (cachedResult != null) {
            log.debug("Recuperando resultados de búsqueda '{}' de caché", palabraClave);
            return (List<Desafio>) cachedResult;
        }
        
        String clave = palabraClave.toLowerCase();
        List<Desafio> resultados = desafioRepositorio.findAll().stream()
                .filter(d -> (d.getNombre() != null && d.getNombre().toLowerCase().contains(clave)) || 
                           (d.getDescripcion() != null && d.getDescripcion().toLowerCase().contains(clave)))
                .collect(Collectors.toList());
        
        // Guardar resultados de búsqueda en caché por 10 minutos
        cacheServicio.guardarConExpiracion(cacheKey, resultados, 10, TimeUnit.MINUTES);
        
        return resultados;
    }
    
    /**
     * Obtiene desafíos ordenados por puntos de recompensa (de mayor a menor)
     */
    public List<Desafio> obtenerOrdenadosPorPuntos() {
        String cacheKey = "desafios:ordenados:puntos";
        
        // Intentar obtener de caché
        Object cachedResult = cacheServicio.obtener(cacheKey);
        if (cachedResult != null) {
            log.debug("Recuperando desafíos ordenados por puntos de caché");
            return (List<Desafio>) cachedResult;
        }
        
        List<Desafio> desafiosOrdenados = desafioRepositorio.findAll().stream()
                .sorted((d1, d2) -> Integer.compare(d2.getPuntosRecompensa(), d1.getPuntosRecompensa()))
                .collect(Collectors.toList());
        
        // Guardar en caché por 15 minutos
        cacheServicio.guardarConExpiracion(cacheKey, desafiosOrdenados, 15, TimeUnit.MINUTES);
        
        return desafiosOrdenados;
    }
    
    /**
     * Verifica si un desafío está activo
     */
    public boolean estaActivo(String desafioId) {
        return obtenerPorId(desafioId)
                .map(Desafio::estaActivo)
                .orElse(false);
    }
    
    /**
     * Obtiene todos los desafíos con paginación
     * 
     * @param pageable Objeto de paginación con página y tamaño
     * @return Página de desafíos
     */
    public Page<Desafio> obtenerTodosPaginados(Pageable pageable) {
        // No cacheamos resultados paginados ya que hay demasiadas combinaciones posibles
        return desafioRepositorio.findAll(pageable);
    }
    
    /**
     * Busca desafíos por palabra clave con paginación
     * 
     * @param palabraClave Palabra clave a buscar en nombre o descripción
     * @param pageable Objeto de paginación
     * @return Página de desafíos que coinciden con la búsqueda
     */
    public Page<Desafio> buscarPorPalabraClavePaginado(String palabraClave, Pageable pageable) {
        // Obtener lista filtrada (aprovechando caché implementado en el método base)
        List<Desafio> desafios = buscarPorPalabraClave(palabraClave);
        
        // Implementar paginación manual
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), desafios.size());
        
        // Verificar que los índices sean válidos
        if (start > desafios.size()) {
            return Page.empty(pageable);
        }
        
        List<Desafio> desafiosPaginados = desafios.subList(start, end);
        return new PageImpl<>(desafiosPaginados, pageable, desafios.size());
    }
    
    /**
     * Obtiene desafíos por proyecto con paginación
     * 
     * @param proyectoId ID del proyecto
     * @param pageable Objeto de paginación
     * @return Página de desafíos del proyecto
     */
    public Page<Desafio> obtenerPorProyectoPaginado(String proyectoId, Pageable pageable) {
        // Aprovechar el método cacheado para obtener todos los desafíos del proyecto
        List<Desafio> desafios = obtenerPorProyecto(proyectoId);
        
        // Implementar paginación manual
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), desafios.size());
        
        // Verificar que los índices sean válidos
        if (start > desafios.size()) {
            return Page.empty(pageable);
        }
        
        List<Desafio> desafiosPaginados = desafios.subList(start, end);
        return new PageImpl<>(desafiosPaginados, pageable, desafios.size());
    }
    
    /**
     * Limpia todas las cachés relacionadas con desafíos
     */
    public void limpiarCache() {
        log.info("Limpiando todas las cachés de desafíos");
        cacheServicio.eliminarPorPatron("desafio:*");
        cacheServicio.eliminarPorPatron("desafios:*");
    }
}