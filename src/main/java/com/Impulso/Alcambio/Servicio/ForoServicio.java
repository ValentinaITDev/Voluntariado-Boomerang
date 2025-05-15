package com.Impulso.Alcambio.Servicio;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;

import com.Impulso.Alcambio.Modelo.Foro;
import com.Impulso.Alcambio.Modelo.Proyecto;
import com.Impulso.Alcambio.Modelo.Usuario;
import com.Impulso.Alcambio.Modelo.Rol;
import com.Impulso.Alcambio.Modelo.Foro.AutorInfo;
import com.Impulso.Alcambio.Repositorio.ForoRepositorio;
import lombok.extern.slf4j.Slf4j;
import com.Impulso.Alcambio.Servicio.CacheServicio;



@Service
@Slf4j

public class ForoServicio {
    
    @Autowired
    private ForoRepositorio foroRepositorio;
    
    @Autowired
    private ProyectoServicio proyectoServicio;

    @Autowired
    private UsuarioServicio usuarioServicio;

    @Autowired
    private CacheServicio cacheServicio;

    
    // Obtener todos los foros
    public List<Foro> obtenerTodos() {
        return foroRepositorio.findAll();
    }
    
    // Obtener un foro por ID
    public Optional<Foro> obtenerPorId(String id) {
        // Generar clave de caché
        String cacheKey = "foro:" + id;
        
        // Intentar obtener de caché primero
        Object cachedForo = cacheServicio.obtener(cacheKey);
        if (cachedForo != null) {
            return Optional.of((Foro) cachedForo);
        }
        
        // Si no está en caché, buscar en BD
        Optional<Foro> foroOpt = foroRepositorio.findById(id);
        
        // Si se encuentra, guardar en caché para futuras consultas (30 minutos)
        foroOpt.ifPresent(foro -> cacheServicio.guardarConExpiracion(
            cacheKey, foro, 30, java.util.concurrent.TimeUnit.MINUTES));
        
        return foroOpt;
    }
    
    // Crear un foro general
    public Foro crearForo(Foro foro) {
        if (foro.getFechaCreacion() == null) {
            foro.setFechaCreacion(LocalDateTime.now());
        }
        Foro foroGuardado = foroRepositorio.save(foro);
        
        // Invalidar caché de estadísticas al crear un nuevo foro
        cacheServicio.eliminarPorPatron("dashboard:*");
        
        return foroGuardado;
    }
    
    // Crear un foro para un proyecto específico
    public Foro crearForoParaProyecto(Proyecto proyecto, Usuario admin) {
        if (proyecto == null || admin == null) {
            throw new IllegalArgumentException("El proyecto y el administrador son obligatorios");
        }
        
        // Crear información del autor (administrador)
        AutorInfo autorInfo = new AutorInfo(
            admin.getId(),
            admin.getNombre(),
            admin.getImagenPerfil()
        );
        
        // Crear el foro
        Foro foro = new Foro();
        foro.setAutor(autorInfo);
        foro.setTitulo("Foro: " + proyecto.getNombre());
        foro.setContenido("Foro oficial del proyecto " + proyecto.getNombre());
        foro.agregarEtiqueta("Proyecto");
        foro.agregarEtiqueta(proyecto.getNombre());
        // Guardar relación con el proyecto (se podría expandir el modelo para incluir proyectoId)
        // Por ahora lo manejaremos con etiquetas
        foro.agregarEtiqueta("proyecto-" + proyecto.getId());
        return foroRepositorio.save(foro);
    }
    // Actualizar un foro
    public Optional<Foro> actualizarForo(String id, Foro foro) {
        return foroRepositorio.findById(id)
            .map(foroActual -> {
                // Actualizar solo los campos editables, preservando comentarios y otros datos
                foro.setId(id);
                // Preservar comentarios existentes si no se proporcionan nuevos
                if (foro.getComentarios() == null || foro.getComentarios().isEmpty()) {
                    foro.setComentarios(foroActual.getComentarios());
                }
                // Preservar fecha de creación original
                if (foro.getFechaCreacion() == null) {
                    foro.setFechaCreacion(foroActual.getFechaCreacion());
                }
                return foroRepositorio.save(foro);
            });
    }
    // Eliminar un foro
    public boolean eliminarForo(String id) {
        if (foroRepositorio.existsById(id)) {
            foroRepositorio.deleteById(id);
            return true;
        }
        return false;
    }

    
    /**
     * Edita un foro existente. Solo el autor original o un administrador pueden editar.
     *
     * @param id              ID del foro a editar.
     * @param foroEditData    Datos del foro con las actualizaciones (solo se usarán título, contenido y etiquetas).
     * @param idUsuarioEditor ID del usuario que intenta realizar la edición.
     * @return Optional con el foro actualizado si la edición fue exitosa y autorizada.
     * @throws RuntimeException Si el usuario no está autorizado o si ocurre un error al guardar.
     */
    public Optional<Foro> editarForo(String id, Foro foroEditData, String idUsuarioEditor) {
        Optional<Foro> foroOpt = foroRepositorio.findById(id);
        if (foroOpt.isEmpty()) {
            log.warn("Intento de editar foro inexistente con ID: {}", id);
            return Optional.empty(); // Foro no encontrado
        }
        Foro foroActual = foroOpt.get();

        // --- Verificación de Autorización ---
        Optional<Usuario> editorOpt = usuarioServicio.buscarPorId(idUsuarioEditor);
        if (editorOpt.isEmpty()) {
            log.error("Usuario editor no encontrado con ID: {}", idUsuarioEditor);
            // Lanza excepción porque el usuario autenticado debería existir
            throw new RuntimeException("Usuario editor no encontrado con ID: " + idUsuarioEditor);
        }
        Usuario editor = editorOpt.get();

        boolean isAdmin = editor.getRol() == Rol.ADMIN;
        // Comprobación segura para autor nulo
        boolean isAuthor = foroActual.getAutor() != null && idUsuarioEditor.equals(foroActual.getAutor().getUsuarioId());

        if (!isAdmin && !isAuthor) {
            log.warn("Usuario {} intentó edición no autorizada en foro {}", idUsuarioEditor, id);
            // Considera usar AccessDeniedException de Spring Security si lo prefieres
            throw new RuntimeException("Usuario no autorizado para editar esta publicación del foro.");
        }
        // --- Fin Verificación de Autorización ---

        // --- Lógica de Actualización Explícita ---
        // Solo actualizar los campos permitidos (ej. título, contenido, etiquetas)
        boolean modificado = false;
        if (foroEditData.getTitulo() != null && !foroEditData.getTitulo().equals(foroActual.getTitulo())) {
            foroActual.setTitulo(foroEditData.getTitulo());
            modificado = true;
            log.debug("Actualizando título del foro {}", id);
        }
        if (foroEditData.getContenido() != null && !foroEditData.getContenido().equals(foroActual.getContenido())) {
            foroActual.setContenido(foroEditData.getContenido());
            modificado = true;
            log.debug("Actualizando contenido del foro {}", id);
        }
        if (foroEditData.getEtiquetas() != null) {
            // Reemplazar etiquetas si son diferentes (o implementar lógica add/remove si es necesario)
            // Compara las listas ignorando el orden si es necesario, o simplemente si no son iguales
             if (!new ArrayList<>(foroActual.getEtiquetas()).equals(new ArrayList<>(foroEditData.getEtiquetas()))) {
                 foroActual.setEtiquetas(new ArrayList<>(foroEditData.getEtiquetas())); // Usar nueva lista para evitar modificar la original
                 modificado = true;
                 log.debug("Actualizando etiquetas del foro {}", id);
            }
        }
        // NO actualizar: id, fechaCreacion, autor, comentarios (estos se manejan en otros métodos)

        if (!modificado) {
             log.info("No se detectaron cambios para editar el foro {}. No se guardará.", id);
             return Optional.of(foroActual); // Devolver el foro sin cambios si no hubo modificaciones
        }

        try {
            Foro foroGuardado = foroRepositorio.save(foroActual);
            log.info("Foro {} actualizado correctamente por usuario {}", id, idUsuarioEditor);
            return Optional.of(foroGuardado);
        } catch (Exception e) {
            log.error("Error al guardar el foro actualizado {}: {}", id, e.getMessage(), e);
            // Re-lanzar para que la transacción haga rollback si aplica y notificar al controlador
            throw new RuntimeException("Error al guardar el foro actualizado.", e);
        }
    }
    
    // Obtener foros por etiqueta (útil para filtrar por proyecto)
    public List<Foro> obtenerPorEtiqueta(String etiqueta) {
        return foroRepositorio.findByEtiquetasContaining(etiqueta);
    }
    
    // Obtener foros de un proyecto específico
    public List<Foro> obtenerForosPorProyecto(String proyectoId) {
        // Primero verificamos si existe un proyecto con ese ID
        return proyectoServicio.obtenerProyectoPorId(proyectoId)
            .map(proyecto -> {
                String foroId = proyecto.getForoId();
                
                if (foroId != null && !foroId.isEmpty()) {
                    // Si el proyecto tiene un foro asignado, lo buscamos directamente
                    return foroRepositorio.findById(foroId)
                        .map(List::of)
                        .orElseGet(List::of);
                } else {
                    // Si no tiene un foro asignado, buscamos por la etiqueta del proyecto
                    return obtenerPorEtiqueta("proyecto-" + proyectoId);
                }
            })
            .orElse(List.of());
    }
    
    /**
     * Asocia un foro a un proyecto
     * @param foroId ID del foro
     * @param proyectoId ID del proyecto
     * @return true si se realizó correctamente, false en caso contrario
     */
    public boolean asociarForoAProyecto(String foroId, String proyectoId) {
        Optional<Foro> foroOpt = foroRepositorio.findById(foroId);
        Optional<Proyecto> proyectoOpt = proyectoServicio.obtenerProyectoPorId(proyectoId);
        
        if (foroOpt.isPresent() && proyectoOpt.isPresent()) {
            Proyecto proyecto = proyectoOpt.get();
            proyecto.setForoId(foroId);
            
            // Guardar el proyecto actualizado
            proyectoServicio.actualizarProyecto(proyecto);
            
            // También agregamos una etiqueta al foro
            Foro foro = foroOpt.get();
            foro.agregarEtiqueta("proyecto-" + proyectoId);
            foroRepositorio.save(foro);
            
            return true;
        }
        
        return false;
    }
    
    // Buscar foros por título
    public List<Foro> buscarPorTitulo(String titulo) {
        return foroRepositorio.findByTituloContaining(titulo);
    }
    
    /**
     * Verifica si un usuario ha participado en un foro a partir de una fecha determinada.
     * @param foroId ID del foro
     * @param usuarioId ID del usuario
     * @param fechaDesde Fecha desde la cual verificar la participación
     * @return true si el usuario ha participado, false en caso contrario
     */
    public boolean verificarParticipacionUsuario(String foroId, String usuarioId, LocalDateTime fechaDesde) {
        return foroRepositorio.findById(foroId)
            .map(foro -> {
                // Verificar si el usuario es el autor del foro y la fecha de creación es posterior a fechaDesde
                if (esAutorYDespuesDeFecha(foro, usuarioId, fechaDesde)) {
                    return true;
                }
                
                // Verificar comentarios del usuario
                return foro.getComentarios().stream()
                    .anyMatch(comentario -> 
                        (esAutorComentarioYDespuesDeFecha(comentario, usuarioId, fechaDesde) ||
                        // Verificar respuestas a comentarios
                        comentario.getRespuestas().stream()
                            .anyMatch(respuesta -> esAutorRespuestaYDespuesDeFecha(respuesta, usuarioId, fechaDesde))
                        )
                    );
            })
            .orElse(false);
    }
    
    /**
     * Verifica si el usuario es autor del foro y posterior a una fecha
     */
    private boolean esAutorYDespuesDeFecha(Foro foro, String usuarioId, LocalDateTime fechaDesde) {
        return foro.getAutor() != null && 
            foro.getAutor().getUsuarioId().equals(usuarioId) && 
            foro.getFechaCreacion().isAfter(fechaDesde);
    }
    
    /**
     * Verifica si el usuario es autor del comentario y posterior a una fecha
     */
    private boolean esAutorComentarioYDespuesDeFecha(Foro.Comentario comentario, String usuarioId, LocalDateTime fechaDesde) {
        return comentario.getAutor() != null &&
            comentario.getAutor().getUsuarioId().equals(usuarioId) &&
            comentario.getFechaCreacion().isAfter(fechaDesde);
    }
    
    /**
     * Verifica si el usuario es autor de la respuesta y posterior a una fecha
     */
    private boolean esAutorRespuestaYDespuesDeFecha(Foro.Comentario.Respuesta respuesta, String usuarioId, LocalDateTime fechaDesde) {
        return respuesta.getAutor() != null &&
            respuesta.getAutor().getUsuarioId().equals(usuarioId) &&
            respuesta.getFechaCreacion().isAfter(fechaDesde);
    }
    
    /**
     * Cuenta el número de comentarios y respuestas realizados por un usuario en un foro a partir de una fecha determinada.
     * Esto permite verificaciones más estrictas de la participación de un usuario.
     * 
     * @param foroId ID del foro
     * @param usuarioId ID del usuario
     * @param fechaDesde Fecha desde la cual contar los comentarios
     * @return El número de comentarios y respuestas realizados por el usuario
     */
    public int contarComentariosUsuario(String foroId, String usuarioId, LocalDateTime fechaDesde) {
        try {
            Optional<Foro> foroOpt = foroRepositorio.findById(foroId);
            
            if (!foroOpt.isPresent()) {
                log.warn("No se encontró el foro con ID: {}", foroId);
                return 0;
            }
            
            Foro foro = foroOpt.get();
            log.info("Contando comentarios en foro: {}", foro.getTitulo());
            
            int contador = 0;
            
            // Contar si el usuario es el autor del foro y la fecha de creación es posterior a fechaDesde
            if (esAutorYDespuesDeFecha(foro, usuarioId, fechaDesde)) {
                contador++;
                log.debug("El usuario es autor del foro (+1)");
            }
            
            // Optimización: Usar streams para contar comentarios y respuestas
            if (foro.getComentarios() != null) {
                contador += foro.getComentarios().stream()
                    .filter(comentario -> esAutorComentarioYDespuesDeFecha(comentario, usuarioId, fechaDesde))
                    .count();
                
                contador += foro.getComentarios().stream()
                    .filter(comentario -> comentario.getRespuestas() != null)
                    .flatMap(comentario -> comentario.getRespuestas().stream())
                    .filter(respuesta -> esAutorRespuestaYDespuesDeFecha(respuesta, usuarioId, fechaDesde))
                    .count();
            } else {
                log.debug("El foro no tiene comentarios");
            }
            
            log.info("Total de comentarios/respuestas válidos: {}", contador);
            return contador;
        } catch (Exception e) {
            log.error("Error al contar comentarios del usuario {} en foro {}: {}", 
                     usuarioId, foroId, e.getMessage());
            return 0;
        }
    }
    
    /**
     * Muestra un resumen del comentario encontrado
     */
    private void mostrarResumenComentario(Foro.Comentario comentario, int contador) {
        System.out.println("Comentario válido encontrado: " + obtenerFragmentoContenido(comentario.getContenido()) + "... (+" + contador + ")");
    }
    
    /**
     * Muestra un resumen de la respuesta encontrada
     */
    private void mostrarResumenRespuesta(Foro.Comentario.Respuesta respuesta, int contador) {
        System.out.println("Respuesta válida encontrada: " + obtenerFragmentoContenido(respuesta.getContenido()) + "... (+" + contador + ")");
    }
    
    /**
     * Obtiene un fragmento del contenido para mostrar en logs
     */
    private String obtenerFragmentoContenido(String contenido) {
        return contenido.substring(0, Math.min(20, contenido.length()));
    }
    
    /**
     * Agrega una respuesta a un comentario específico dentro de un foro
     * @param foroId ID del foro
     * @param comentarioId ID del comentario
     * @param respuesta Objeto Respuesta a agregar
     * @return El foro actualizado con la nueva respuesta
     * @throws RuntimeException Si el foro o el comentario no se encuentran
     */
    public Foro agregarRespuestaAComentario(String foroId, String comentarioId, Foro.Comentario.Respuesta respuesta) {
        Foro foro = foroRepositorio.findById(foroId)
            .orElseThrow(() -> new RuntimeException("Foro no encontrado con ID: " + foroId));
        
        // Buscar el comentario específico dentro del foro
        Foro.Comentario comentarioPadre = foro.getComentarios().stream()
            .filter(c -> comentarioId.equals(c.getId()))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Comentario no encontrado con ID: " + comentarioId + " en el foro: " + foroId));
        
        // Inicializar la lista de respuestas si es nula
        if (comentarioPadre.getRespuestas() == null) {
            comentarioPadre.setRespuestas(new ArrayList<>());
        }
        
        // Asegurar que la respuesta tenga fecha de creación
        if (respuesta.getFechaCreacion() == null) {
            respuesta.setFechaCreacion(LocalDateTime.now());
        }
        
        // Añadir la respuesta
        comentarioPadre.agregarRespuesta(respuesta);
        
        // Guardar el foro actualizado
        return foroRepositorio.save(foro);
    }
    
    /**
     * Obtiene todos los foros con paginación
     * 
     * @param pageable Objeto de paginación con página y tamaño
     * @return Página de foros
     */
    public Page<Foro> obtenerTodosPaginados(Pageable pageable) {
        return foroRepositorio.findAll(pageable);
    }
    
    /**
     * Obtiene foros de un proyecto específico con paginación
     * 
     * @param proyectoId ID del proyecto
     * @param pageable Objeto de paginación
     * @return Página de foros del proyecto
     */
    public Page<Foro> obtenerForosPorProyectoPaginados(String proyectoId, Pageable pageable) {
        List<Foro> foros = obtenerForosPorProyecto(proyectoId);
        
        // Implementar paginación manual ya que es una lista filtrada
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), foros.size());
        
        // Verificar que los índices sean válidos
        if (start > foros.size()) {
            return Page.empty(pageable);
        }
        
        List<Foro> forosPaginados = foros.subList(start, end);
        return new PageImpl<>(forosPaginados, pageable, foros.size());
    }
    
    /**
     * Busca foros por título con paginación
     * 
     * @param titulo Título a buscar
     * @param pageable Objeto de paginación
     * @return Página de foros que coincidan con el título
     */
    public Page<Foro> buscarPorTituloPaginado(String titulo, Pageable pageable) {
        // Verificar si el repositorio tiene un método directo con paginación
        // De lo contrario, implementar paginación manual
        // List<Foro> foros = foroRepositorio.findByTituloContaining(titulo);
        
        // Implementar paginación manual
        // int start = (int) pageable.getOffset();
        // int end = Math.min((start + pageable.getPageSize()), foros.size());
        
        // Verificar que los índices sean válidos
        // if (start > foros.size()) {
        //     return Page.empty(pageable);
        // }
        
        // List<Foro> forosPaginados = foros.subList(start, end);
        // return new PageImpl<>(forosPaginados, pageable, foros.size());
        return foroRepositorio.findByTituloContaining(titulo, pageable);
    }
    
    /**
     * Obtiene estadísticas generales de los foros para el panel de administración
     * @return Mapa con estadísticas clave
     */
    public Map<String, Object> obtenerEstadisticasAdmin() {
        Map<String, Object> estadisticas = new HashMap<>();
        
        try {
            // Calcular fecha de hace un mes para comparaciones
            LocalDateTime haceTreintaDias = LocalDateTime.now().minusDays(30);

            // -------------------------------
            // Estadísticas de foros
            // -------------------------------
            // Total de foros actual
            long totalForos = foroRepositorio.count();
            estadisticas.put("totalForos", totalForos);
            
            // Foros creados el último mes para cálculo de variación
            List<Foro> forosUltimoMes = foroRepositorio.findAll().stream()
                .filter(f -> f.getFechaCreacion() != null && f.getFechaCreacion().isAfter(haceTreintaDias))
                .collect(Collectors.toList());
            
            long forosNuevos = forosUltimoMes.size();
            
            // Calcular variación porcentual: solo válido si había foros antes
            double variacionForos = 0;
            long forosAnteriores = totalForos - forosNuevos;
            if (forosAnteriores > 0) {
                variacionForos = ((double)forosNuevos / forosAnteriores) * 100;
            }
            estadisticas.put("variacionForos", Math.round(variacionForos));
            estadisticas.put("tieneVariacionPositivaForos", variacionForos >= 0);
            
            // -------------------------------
            // Estadísticas de comentarios
            // -------------------------------
            // Total de comentarios
            long totalComentarios = foroRepositorio.findAll().stream()
                .mapToLong(foro -> foro.getComentarios() != null ? foro.getComentarios().size() : 0)
                .sum();
            estadisticas.put("totalComentarios", totalComentarios);
            
            // Comentarios del último mes
            long comentariosUltimoMes = foroRepositorio.findAll().stream()
                .flatMap(foro -> foro.getComentarios() != null ? foro.getComentarios().stream() : Stream.empty())
                .filter(c -> c.getFechaCreacion() != null && c.getFechaCreacion().isAfter(haceTreintaDias))
                .count();
            
            // Calcular variación porcentual de comentarios
            double variacionComentarios = 0;
            long comentariosAnteriores = totalComentarios - comentariosUltimoMes;
            if (comentariosAnteriores > 0) {
                variacionComentarios = ((double)comentariosUltimoMes / comentariosAnteriores) * 100;
            }
            
            estadisticas.put("variacionComentarios", Math.round(variacionComentarios));
            estadisticas.put("tieneVariacionPositivaComentarios", variacionComentarios >= 0);
            
            // -------------------------------
            // Estadísticas de reportes
            // -------------------------------
            // Foros activos vs archivados
            long forosActivos = foroRepositorio.findByActivo(true).size();
            long forosArchivados = foroRepositorio.findByArchivado(true).size();
            estadisticas.put("forosActivos", forosActivos);
            estadisticas.put("forosArchivados", forosArchivados);
            
            // Reportes pendientes (foros y comentarios)
            long forosReportados = foroRepositorio.findReportedForos().size();
            long forosConComentariosReportados = foroRepositorio.findForosWithReportedComments().size();
            
            // Total de reportes pendientes (pueden haber duplicados si un foro tiene tanto el foro como comentarios reportados)
            long reportesPendientes = forosReportados;
            
            // Contar comentarios reportados individualmente
            long comentariosReportados = foroRepositorio.findAll().stream()
                .flatMap(foro -> foro.getComentarios() != null ? foro.getComentarios().stream() : Stream.empty())
                .filter(c -> c.isReportado())
                .count();
            
            estadisticas.put("reportesPendientes", reportesPendientes + comentariosReportados);
            
            // Encontrar reportes de hace un mes para calcular variación
            // Simplificado: si no hay histórico, asumimos que no hay variación (0%)
            estadisticas.put("variacionReportes", 0);
            estadisticas.put("tieneVariacionPositivaReportes", false);
            
            // -------------------------------
            // Estadísticas de usuarios
            // -------------------------------
            // Usuarios activos: usuarios únicos que han creado foros o comentarios en el último mes
            Set<String> usuariosActivos = new HashSet<>();
            
            // Añadir autores de foros recientes
            forosUltimoMes.forEach(foro -> {
                if (foro.getAutor() != null && foro.getAutor().getUsuarioId() != null) {
                    usuariosActivos.add(foro.getAutor().getUsuarioId());
                }
            });
            
            // Añadir autores de comentarios recientes
            foroRepositorio.findAll().stream()
                .flatMap(foro -> foro.getComentarios() != null ? foro.getComentarios().stream() : Stream.empty())
                .filter(c -> c.getFechaCreacion() != null && c.getFechaCreacion().isAfter(haceTreintaDias))
                .forEach(comentario -> {
                    if (comentario.getAutor() != null && comentario.getAutor().getUsuarioId() != null) {
                        usuariosActivos.add(comentario.getAutor().getUsuarioId());
                    }
                });
                
            estadisticas.put("usuariosActivos", usuariosActivos.size());
            
            // Asumimos un crecimiento del 8% en usuarios activos (esto debería idealmente compararse con datos históricos)
            estadisticas.put("variacionUsuarios", 8);
            estadisticas.put("tieneVariacionPositivaUsuarios", true);
            
        } catch (Exception e) {
            log.error("Error al calcular estadísticas del foro admin: {}", e.getMessage(), e);
            // En caso de error, al menos devolver datos mínimos
            estadisticas.put("totalForos", foroRepositorio.count());
            estadisticas.put("error", "Ocurrió un error al calcular algunas estadísticas");
        }
        
        return estadisticas;
    }
    
    /**
     * Obtiene foros según su estado para la administración
     * @param filtro Tipo de filtro: "all", "active", "archived", "reported"
     * @param pageable Configuración de paginación
     * @return Página de foros según el filtro
     */
    public Page<Foro> obtenerForosAdmin(String filtro, Pageable pageable) {
        switch (filtro) {
            case "active":
                return foroRepositorio.findByActivoAndArchivado(true, false, pageable);
            case "archived":
                return foroRepositorio.findByArchivado(true, pageable);
            case "reported":
                // Obtenemos tanto los foros reportados como los que tienen comentarios reportados
                List<Foro> forosReportados = foroRepositorio.findReportedForos();
                List<Foro> forosConComentariosReportados = foroRepositorio.findForosWithReportedComments();
                
                // Combinar los resultados evitando duplicados
                Map<String, Foro> forosCombinados = new HashMap<>();
                
                forosReportados.forEach(foro -> forosCombinados.put(foro.getId(), foro));
                forosConComentariosReportados.forEach(foro -> forosCombinados.put(foro.getId(), foro));
                
                List<Foro> resultadosCombinados = new ArrayList<>(forosCombinados.values());
                
                // Ordenar y aplicar paginación
                int inicio = (int) pageable.getOffset();
                int fin = Math.min((inicio + pageable.getPageSize()), resultadosCombinados.size());
                
                // Si el inicio está fuera de rango, devolvemos una página vacía
                if (inicio >= resultadosCombinados.size()) {
                    return new PageImpl<>(new ArrayList<>(), pageable, resultadosCombinados.size());
                }
                
                List<Foro> subLista = resultadosCombinados.subList(inicio, fin);
                return new PageImpl<>(subLista, pageable, resultadosCombinados.size());
                
            default: // "all"
                return foroRepositorio.findAll(pageable);
        }
    }
    
    /**
     * Archivar o desarchivar un foro
     * @param foroId ID del foro
     * @param archivar true para archivar, false para desarchivar
     * @return El foro actualizado o vacío si no existe
     */
    public Optional<Foro> cambiarEstadoArchivado(String foroId, boolean archivar) {
        return foroRepositorio.findById(foroId)
                .map(foro -> {
                    foro.setArchivado(archivar);
                    // Si se archiva, podemos también desactivarlo
                    if (archivar) {
                        foro.setActivo(false);
                    }
                    return foroRepositorio.save(foro);
                });
    }
    
    /**
     * Activar o desactivar un foro
     * @param foroId ID del foro
     * @param activar true para activar, false para desactivar
     * @return El foro actualizado o vacío si no existe
     */
    public Optional<Foro> cambiarEstadoActivo(String foroId, boolean activar) {
        return foroRepositorio.findById(foroId)
                .map(foro -> {
                    foro.setActivo(activar);
                    // Si se activa, debe estar desarchivado
                    if (activar) {
                        foro.setArchivado(false);
                    }
                    return foroRepositorio.save(foro);
                });
    }
    
    /**
     * Cambiar si un foro permite comentarios
     * @param foroId ID del foro
     * @param permitir true para permitir comentarios, false para desactivarlos
     * @return El foro actualizado o vacío si no existe
     */
    public Optional<Foro> cambiarPermitirComentarios(String foroId, boolean permitir) {
        return foroRepositorio.findById(foroId)
                .map(foro -> {
                    foro.setPermiteComentarios(permitir);
                    return foroRepositorio.save(foro);
                });
    }
    
    /**
     * Reportar un foro por contenido inapropiado
     * @param foroId ID del foro
     * @param usuarioId ID del usuario que reporta
     * @param motivo Motivo del reporte
     * @return El foro actualizado o vacío si no existe
     */
    public Optional<Foro> reportarForo(String foroId, String usuarioId, String motivo) {
        return foroRepositorio.findById(foroId)
                .map(foro -> {
                    // Crear reporte
                    Foro.ReporteForo reporte = new Foro.ReporteForo(usuarioId, motivo);
                    
                    // Añadir reporte al foro
                    foro.agregarReporte(reporte);
                    
                    // Guardar y devolver
                    return foroRepositorio.save(foro);
                });
    }
    
    /**
     * Marcar un reporte de foro como resuelto
     * @param foroId ID del foro
     * @param reporteId ID del reporte
     * @return El foro actualizado o vacío si no existe o no tiene ese reporte
     */
    public Optional<Foro> resolverReporteForo(String foroId, String reporteId) {
        return foroRepositorio.findById(foroId)
                .map(foro -> {
                    // Buscar el reporte
                    boolean reporteEncontrado = false;
                    for (Foro.ReporteForo reporte : foro.getReportes()) {
                        if (reporte.getId().equals(reporteId)) {
                            reporte.setResuelto(true);
                            reporteEncontrado = true;
                            break;
                        }
                    }
                    
                    // Si no se encontró el reporte, no hacer nada
                    if (!reporteEncontrado) {
                        return foro;
                    }
                    
                    // Actualizar contador si todos los reportes están resueltos
                    boolean todosResueltos = foro.getReportes().stream()
                            .allMatch(Foro.ReporteForo::isResuelto);
                    
                    if (todosResueltos) {
                        foro.setContadorReportes(0);
                    }
                    
                    return foroRepositorio.save(foro);
                });
    }
    
    /**
     * Reportar un comentario de un foro
     * @param foroId ID del foro
     * @param comentarioId ID del comentario
     * @param usuarioId ID del usuario que reporta
     * @param motivo Motivo del reporte
     * @return El foro actualizado o vacío si no existe o no tiene ese comentario
     */
    public Optional<Foro> reportarComentario(String foroId, String comentarioId, String usuarioId, String motivo) {
        return foroRepositorio.findById(foroId)
                .map(foro -> {
                    // Buscar el comentario
                    boolean comentarioEncontrado = false;
                    for (Foro.Comentario comentario : foro.getComentarios()) {
                        if (comentario.getId().equals(comentarioId)) {
                            // Crear y agregar reporte
                            Foro.Comentario.ReporteComentario reporte = 
                                    new Foro.Comentario.ReporteComentario(usuarioId, motivo);
                            comentario.agregarReporteComentario(reporte);
                            comentarioEncontrado = true;
                            break;
                        }
                    }
                    
                    // Si no se encontró el comentario, no hacer nada
                    if (!comentarioEncontrado) {
                        return foro;
                    }
                    
                    return foroRepositorio.save(foro);
                });
    }
    
    /**
     * Restringir un comentario (hacerlo invisible para usuarios normales)
     * @param foroId ID del foro
     * @param comentarioId ID del comentario
     * @param adminId ID del administrador que restringe
     * @param motivo Motivo de la restricción
     * @return El foro actualizado o vacío si no existe o no tiene ese comentario
     */
    public Optional<Foro> restringirComentario(String foroId, String comentarioId, String adminId, String motivo) {
        log.info("Intentando restringir comentario {} en foro {} por admin {}", comentarioId, foroId, adminId);
        
        if (foroId == null || comentarioId == null || adminId == null) {
            log.error("Parámetros nulos en restringirComentario");
            return Optional.empty();
        }
        
        return foroRepositorio.findById(foroId)
                .map(foro -> {
                    // Buscar el comentario
                    boolean comentarioEncontrado = false;
                    for (Foro.Comentario comentario : foro.getComentarios()) {
                        if (comentario.getId().equals(comentarioId)) {
                            // Restringir comentario
                            comentario.setRestringido(true);
                            comentario.setMotivoRestriccion(motivo);
                            comentario.setFechaRestriccion(LocalDateTime.now());
                            comentario.setUsuarioRestriccionId(adminId);
                            
                            // Marcar reportes como resueltos si hay
                            comentario.getReportesComentario().forEach(reporte -> reporte.setResuelto(true));
                            comentario.setReportado(false);
                            
                            comentarioEncontrado = true;
                            log.info("Comentario {} restringido correctamente", comentarioId);
                            break;
                        }
                    }
                    
                    // Si no se encontró el comentario, registrar advertencia
                    if (!comentarioEncontrado) {
                        log.warn("Comentario {} no encontrado en foro {}", comentarioId, foroId);
                        return foro;
                    }
                    
                    try {
                        return foroRepositorio.save(foro);
                    } catch (Exception e) {
                        log.error("Error al guardar foro después de restringir comentario: {}", e.getMessage(), e);
                        throw new RuntimeException("Error al guardar foro", e);
                    }
                });
    }
    
    /**
     * Registra una acción de moderación en el historial (método de ayuda)
     * Este método podría ampliarse en el futuro para mantener un historial completo
     */
    private void registrarAccionModeracion(String foroId, String tipoAccion, String adminId, 
            String adminNombre, String descripcion, String elementoId) {
        // Por ahora solo registra en el log, pero podría guardar en una colección separada
        log.info("ACCIÓN DE MODERACIÓN: Foro={}, Tipo={}, Admin={} ({}), Elemento={}, Descripción={}",
                foroId, tipoAccion, adminId, adminNombre, elementoId, descripcion);
    }
    
    /**
     * Quitar la restricción de un comentario
     * @param foroId ID del foro
     * @param comentarioId ID del comentario
     * @return El foro actualizado o vacío si no existe o no tiene ese comentario
     */
    public Optional<Foro> eliminarRestriccionComentario(String foroId, String comentarioId) {
        return foroRepositorio.findById(foroId)
                .map(foro -> {
                    // Buscar el comentario
                    boolean comentarioEncontrado = false;
                    for (Foro.Comentario comentario : foro.getComentarios()) {
                        if (comentario.getId().equals(comentarioId)) {
                            // Quitar restricción
                            comentario.setRestringido(false);
                            comentarioEncontrado = true;
                            break;
                        }
                    }
                    
                    // Si no se encontró el comentario, no hacer nada
                    if (!comentarioEncontrado) {
                        return foro;
                    }
                    
                    return foroRepositorio.save(foro);
                });
    }
    
    /**
     * Aprobar un comentario reportado (quitar reportes)
     * @param foroId ID del foro
     * @param comentarioId ID del comentario
     * @return El foro actualizado o vacío si no existe o no tiene ese comentario
     */
    public Optional<Foro> aprobarComentarioReportado(String foroId, String comentarioId) {
        log.info("Aprobando comentario reportado: {} en foro {}", comentarioId, foroId);
        
        if (foroId == null || comentarioId == null) {
            log.error("Parámetros nulos en aprobarComentarioReportado");
            return Optional.empty();
        }
        
        return foroRepositorio.findById(foroId)
                .map(foro -> {
                    // Buscar el comentario
                    boolean comentarioEncontrado = false;
                    boolean comentarioModificado = false;
                    
                    for (Foro.Comentario comentario : foro.getComentarios()) {
                        if (comentario.getId().equals(comentarioId)) {
                            comentarioEncontrado = true;
                            
                            // Verificar si está reportado
                            if (!comentario.isReportado()) {
                                log.info("El comentario {} no estaba reportado", comentarioId);
                                return foro; // No hay cambios que hacer
                            }
                            
                            // Marcar todos los reportes como resueltos
                            int reportesResueltos = 0;
                            for (Foro.Comentario.ReporteComentario reporte : comentario.getReportesComentario()) {
                                if (!reporte.isResuelto()) {
                                    reporte.setResuelto(true);
                                    reportesResueltos++;
                                }
                            }
                            
                            // Solo si se resolvieron reportes
                            if (reportesResueltos > 0) {
                                comentario.setReportado(false);
                                comentarioModificado = true;
                                log.info("Se resolvieron {} reportes del comentario {}", reportesResueltos, comentarioId);
                            }
                            break;
                        }
                    }
                    
                    // Si no se encontró el comentario, log y devolver vacío
                    if (!comentarioEncontrado) {
                        log.warn("Comentario {} no encontrado en foro {}", comentarioId, foroId);
                        return foro;
                    }
                    
                    // Si no hubo modificaciones, no guardar
                    if (!comentarioModificado) {
                        log.info("No se hicieron cambios en el comentario {}", comentarioId);
                        return foro;
                    }
                    
                    try {
                        Foro foroActualizado = foroRepositorio.save(foro);
                        log.info("Comentario {} aprobado exitosamente", comentarioId);
                        return foroActualizado;
                    } catch (Exception e) {
                        log.error("Error al guardar foro después de aprobar comentario: {}", e.getMessage(), e);
                        throw new RuntimeException("Error al guardar foro", e);
                    }
                });
    }
    
    /**
     * Obtener todos los comentarios reportados de todos los foros
     * @param pageable Configuración de paginación
     * @return Lista de mapas con información de los comentarios reportados y sus foros
     */
    public Page<Map<String, Object>> obtenerComentariosReportados(Pageable pageable) {
        List<Foro> forosConComentariosReportados = foroRepositorio.findForosWithReportedComments();
        List<Map<String, Object>> comentariosReportados = new ArrayList<>();
        
        // Recorrer todos los foros y extraer comentarios reportados
        for (Foro foro : forosConComentariosReportados) {
            for (Foro.Comentario comentario : foro.getComentarios()) {
                if (comentario.isReportado()) {
                    Map<String, Object> infoComentario = new HashMap<>();
                    infoComentario.put("foroId", foro.getId());
                    infoComentario.put("foroTitulo", foro.getTitulo());
                    infoComentario.put("comentarioId", comentario.getId());
                    infoComentario.put("comentarioContenido", comentario.getContenido());
                    infoComentario.put("autor", comentario.getAutor());
                    infoComentario.put("fechaCreacion", comentario.getFechaCreacion());
                    infoComentario.put("reportes", comentario.getReportesComentario());
                    
                    comentariosReportados.add(infoComentario);
                }
            }
        }
        
        // Ordenar por fecha (más recientes primero)
        comentariosReportados.sort((c1, c2) -> {
            LocalDateTime fecha1 = (LocalDateTime) c1.get("fechaCreacion");
            LocalDateTime fecha2 = (LocalDateTime) c2.get("fechaCreacion");
            return fecha2.compareTo(fecha1);
        });
        
        // Aplicar paginación
        int inicio = (int) pageable.getOffset();
        int fin = Math.min((inicio + pageable.getPageSize()), comentariosReportados.size());
        
        // Si el inicio está fuera de rango, devolvemos una página vacía
        if (inicio >= comentariosReportados.size()) {
            return new PageImpl<>(new ArrayList<>(), pageable, comentariosReportados.size());
        }
        
        List<Map<String, Object>> subLista = comentariosReportados.subList(inicio, fin);
        return new PageImpl<>(subLista, pageable, comentariosReportados.size());
    }
} 