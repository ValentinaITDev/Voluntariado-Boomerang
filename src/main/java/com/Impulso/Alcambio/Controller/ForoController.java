package com.Impulso.Alcambio.Controller;

import java.security.Principal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.Impulso.Alcambio.Modelo.Foro;
import com.Impulso.Alcambio.Modelo.Rol;
import com.Impulso.Alcambio.Modelo.Usuario;
import com.Impulso.Alcambio.Servicio.ForoServicio;
import com.Impulso.Alcambio.Servicio.UsuarioServicio;

import lombok.extern.slf4j.Slf4j;

/**
 * Este es el controlador principal para toda la gestión de foros.
 * Aquí manejamos toda la comunicación entre el frontend y la base de datos
 * para los foros de discusión de la plataforma.
 */
@RestController
@RequestMapping("/api/foros")
@Slf4j
public class ForoController {
    
    @Autowired
    private ForoServicio foroServicio;
    
    @Autowired
    private UsuarioServicio usuarioServicio;
    
    // =================== MÉTODOS AUXILIARES ===================
    
    /**
     * Obtiene el usuario autenticado a partir del objeto Principal
     * @param principal Información del usuario autenticado
     * @return Usuario si está autenticado, empty si no
     */
    private Optional<Usuario> obtenerUsuarioAutenticado(Principal principal) {
        if (principal == null) {
            return Optional.empty();
        }
        return usuarioServicio.buscarPorCorreo(principal.getName());
    }
    
    /**
     * Verifica si un usuario es el autor de un foro o es administrador
     * @param usuario Usuario a verificar
     * @param foro Foro a comprobar autoría
     * @return true si es autor o administrador, false en caso contrario
     */
    private boolean esAutorOAdmin(Usuario usuario, Foro foro) {
        return foro.getAutor().getUsuarioId().equals(usuario.getId()) || 
               "ADMIN".equals(usuario.getRol().toString());
    }
    
    // =================== OPERACIONES BÁSICAS CRUD ===================
    
    // Este endpoint devuelve TODOS los foros disponibles en el sistema
    // Lo usamos principalmente en la página principal de foros
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<Foro>> obtenerForos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(foroServicio.obtenerTodosPaginados(pageable));
    }
    
    // Este endpoint obtiene los detalles de un foro específico
    // Es lo que carga cuando un usuario hace clic en un foro para verlo
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Foro> obtenerForoPorId(@PathVariable String id) {
        return foroServicio.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Crea un nuevo foro
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Foro> crearForo(@RequestBody Foro foro, Principal principal) {
        Optional<Usuario> usuarioOpt = obtenerUsuarioAutenticado(principal);
        
        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        Usuario usuario = usuarioOpt.get();
        
        // Si no tiene autor, establecer al usuario actual como autor
        if (foro.getAutor() == null) {
            Foro.AutorInfo autorInfo = new Foro.AutorInfo(usuario.getId(), usuario.getNombre(), usuario.getImagenPerfil());
            foro.setAutor(autorInfo);
        }
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(foroServicio.crearForo(foro));
    }
    
    /**
     * Actualiza un foro existente
     */
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Foro> actualizarForo(@PathVariable String id, @RequestBody Foro foro, Principal principal) {
        Optional<Foro> foroExistente = foroServicio.obtenerPorId(id);
        
        if (foroExistente.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        // Verificar que el usuario sea el autor o un administrador
        Optional<Usuario> usuarioOpt = obtenerUsuarioAutenticado(principal);
        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        Usuario usuario = usuarioOpt.get();
        Foro foroActual = foroExistente.get();
        
        // Solo el autor o un administrador puede actualizar
        if (!esAutorOAdmin(usuario, foroActual)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return foroServicio.actualizarForo(id, foro)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Elimina un foro (solo administradores)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> eliminarForo(@PathVariable String id) {
        boolean eliminado = foroServicio.eliminarForo(id);
        
        return eliminado 
                ? ResponseEntity.noContent().build() 
                : ResponseEntity.notFound().build();
    }
    
    /**
     * editarForo
     * @param id
     * @param foro
     * @param principal
     * @return
     */
    @PutMapping("/{id}/editar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Foro> editarForo(
            @PathVariable String id,
            @RequestBody Foro foro,
            Principal principal) {
        
        Optional<Usuario> usuarioOpt = obtenerUsuarioAutenticado(principal);
        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        Usuario usuarioEditor = usuarioOpt.get();
        
        // Llamar al servicio con el ID del editor
        try {
            Optional<Foro> foroActualizadoOpt = foroServicio.editarForo(id, foro, usuarioEditor.getId());
            
            return foroActualizadoOpt
                .map(ResponseEntity::ok) // Si se actualizó, devolver OK con el foro
                .orElse(ResponseEntity.notFound().build()); // Si el foro no existía, devolver Not Found
        } catch (RuntimeException e) {
            // Capturar errores de autorización o guardado desde el servicio
            if (e.getMessage().contains("no autorizado")) {
                 return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            } else {
                // Otros errores (ej., fallo al guardar)
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }
    }
    /**
     * Obtiene los foros relacionados con un proyecto específico
     */
    @GetMapping("/proyecto/{proyectoId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<Foro>> obtenerForosPorProyecto(
            @PathVariable String proyectoId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(foroServicio.obtenerForosPorProyectoPaginados(proyectoId, pageable));
    }
    
    // =================== OPERACIONES DE COMENTARIOS Y RESPUESTAS ===================
    
    /**
     * Añade un nuevo comentario a un foro
     */
    @PostMapping("/{foroId}/comentarios")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Foro> agregarComentario(
            @PathVariable String foroId,
            @RequestBody String contenido,
            Principal principal) {
        
        Optional<Foro> foroOpt = foroServicio.obtenerPorId(foroId);
        if (foroOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Optional<Usuario> usuarioOpt = obtenerUsuarioAutenticado(principal);
        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        Usuario usuario = usuarioOpt.get();
        Foro foro = foroOpt.get();
        
        // Crear información del autor
        Foro.AutorInfo autorInfo = new Foro.AutorInfo(usuario.getId(), usuario.getNombre(), usuario.getImagenPerfil());
        
        // Crear y añadir el comentario
        Foro.Comentario comentario = new Foro.Comentario(contenido, autorInfo);
        foro.agregarComentario(comentario);
        
        // Guardar el foro actualizado
        return ResponseEntity.ok(foroServicio.crearForo(foro));
    }
    
    /**
     * Añade una respuesta a un comentario existente
     */
    @PostMapping("/{foroId}/comentarios/{comentarioId}/respuestas")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> agregarRespuesta(
            @PathVariable String foroId,
            @PathVariable String comentarioId,
            @RequestBody String contenidoRespuesta,
            Principal principal) {
        
        Optional<Usuario> usuarioOpt = obtenerUsuarioAutenticado(principal);
        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        Usuario usuario = usuarioOpt.get();
        
        try {
            // Buscar el foro
            Optional<Foro> foroOpt = foroServicio.obtenerPorId(foroId);
            if (foroOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Foro foro = foroOpt.get();
            
            // Buscar el comentario
            Optional<Foro.Comentario> comentarioOpt = foro.getComentarios().stream()
                    .filter(c -> comentarioId.equals(c.getId()))
                    .findFirst();
            
            if (comentarioOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Comentario no encontrado con ID: " + comentarioId);
            }
            
            // Crear información del autor para la respuesta
            Foro.AutorInfo autorInfo = new Foro.AutorInfo(usuario.getId(), usuario.getNombre(), usuario.getImagenPerfil());
            
            // Crear el objeto Respuesta
            Foro.Comentario.Respuesta respuesta = new Foro.Comentario.Respuesta(contenidoRespuesta, autorInfo);
            
            // Añadir la respuesta al comentario
            Foro.Comentario comentario = comentarioOpt.get();
            comentario.agregarRespuesta(respuesta);
            
            // Guardar el foro actualizado
            Foro foroActualizado = foroServicio.crearForo(foro);
            return ResponseEntity.ok(foroActualizado);
            
        } catch (RuntimeException e) {
            // Capturar errores como foro o comentario no encontrado
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            // Otros errores inesperados
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al agregar la respuesta");
        }
    }
    
    /**
     * Edita un comentario de un foro
     */
    @PutMapping("/{foroId}/comentarios/{comentarioId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> editarComentario(
            @PathVariable String foroId,
            @PathVariable String comentarioId,
            @RequestBody String nuevoContenido,
            Principal principal) {
        
        Optional<Usuario> usuarioOpt = obtenerUsuarioAutenticado(principal);
        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        Usuario usuario = usuarioOpt.get();
        
        try {
            // Buscar el foro
            Optional<Foro> foroOpt = foroServicio.obtenerPorId(foroId);
            if (foroOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Foro foro = foroOpt.get();
            
            // Buscar el comentario
            Optional<Foro.Comentario> comentarioOpt = foro.getComentarios().stream()
                    .filter(c -> comentarioId.equals(c.getId()))
                    .findFirst();
            
            if (comentarioOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Comentario no encontrado con ID: " + comentarioId);
            }
            
            Foro.Comentario comentario = comentarioOpt.get();
            
            // Verificar que el usuario sea el autor del comentario o un administrador
            boolean esAutorOAdmin = comentario.getAutor().getUsuarioId().equals(usuario.getId()) || 
                                   "ADMIN".equals(usuario.getRol().toString());
            
            if (!esAutorOAdmin) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("No autorizado para editar este comentario");
            }
            
            // Eliminar las comillas extras si el contenido está entre comillas dobles
            String contenidoLimpio = nuevoContenido;
            if (nuevoContenido.startsWith("\"") && nuevoContenido.endsWith("\"")) {
                contenidoLimpio = nuevoContenido.substring(1, nuevoContenido.length() - 1);
            }
            
            // Actualizar el contenido del comentario
            comentario.setContenido(contenidoLimpio);
            
            // Guardar el foro actualizado
            Foro foroActualizado = foroServicio.crearForo(foro);
            
            return ResponseEntity.ok(foroActualizado);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al editar el comentario: " + e.getMessage());
        }
    }
    
    /**
     * Elimina un comentario de un foro
     */
    @DeleteMapping("/{foroId}/comentarios/{comentarioId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> eliminarComentario(
            @PathVariable String foroId,
            @PathVariable String comentarioId,
            Principal principal) {
        
        Optional<Usuario> usuarioOpt = obtenerUsuarioAutenticado(principal);
        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        Usuario usuario = usuarioOpt.get();
        
        try {
            // Buscar el foro
            Optional<Foro> foroOpt = foroServicio.obtenerPorId(foroId);
            if (foroOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Foro foro = foroOpt.get();
            
            // Buscar el comentario
            Optional<Foro.Comentario> comentarioOpt = foro.getComentarios().stream()
                    .filter(c -> comentarioId.equals(c.getId()))
                    .findFirst();
            
            if (comentarioOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Comentario no encontrado con ID: " + comentarioId);
            }
            
            Foro.Comentario comentario = comentarioOpt.get();
            
            // Verificar que el usuario sea el autor del comentario o un administrador
            boolean esAutorOAdmin = comentario.getAutor().getUsuarioId().equals(usuario.getId()) || 
                                   "ADMIN".equals(usuario.getRol().toString());
            
            if (!esAutorOAdmin) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("No autorizado para eliminar este comentario");
            }
            
            // Eliminar el comentario
            foro.getComentarios().remove(comentario);
            
            // Guardar el foro actualizado
            Foro foroActualizado = foroServicio.crearForo(foro);
            
            return ResponseEntity.ok(foroActualizado);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al eliminar el comentario: " + e.getMessage());
        }
    }
    
    // =================== OPERACIONES DE BÚSQUEDA ===================
    
    /**
     * Busca foros por título con paginación
     */
    @GetMapping("/buscar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<Foro>> buscarForosPorTitulo(
            @RequestParam String titulo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(foroServicio.buscarPorTituloPaginado(titulo, pageable));
    }

    /**
     * Agrega o quita un like a un comentario
     */
    @PostMapping("/{foroId}/comentarios/{comentarioId}/like")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> toggleLikeComentario(
            @PathVariable String foroId,
            @PathVariable String comentarioId,
            Principal principal) {
        
        Optional<Usuario> usuarioOpt = obtenerUsuarioAutenticado(principal);
        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            // Buscar el foro
            Optional<Foro> foroOpt = foroServicio.obtenerPorId(foroId);
            if (foroOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Foro foro = foroOpt.get();
            
            // Buscar el comentario
            Optional<Foro.Comentario> comentarioOpt = foro.getComentarios().stream()
                    .filter(c -> comentarioId.equals(c.getId()))
                    .findFirst();
            
            if (comentarioOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Comentario no encontrado con ID: " + comentarioId);
            }
            
            Foro.Comentario comentario = comentarioOpt.get();
            String usuarioId = usuarioOpt.get().getId();
            
            // Toggle like
            if (comentario.getUsuariosQueDieronLike().contains(usuarioId)) {
                comentario.quitarLike(usuarioId);
            } else {
                comentario.agregarLike(usuarioId);
            }
            
            // Guardar el foro actualizado
            Foro foroActualizado = foroServicio.crearForo(foro);
            
            return ResponseEntity.ok(foroActualizado);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al actualizar like: " + e.getMessage());
        }
    }

    /**
     * Agrega o quita un like a una respuesta
     */
    @PostMapping("/{foroId}/comentarios/{comentarioId}/respuestas/{respuestaId}/like")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> toggleLikeRespuesta(
            @PathVariable String foroId,
            @PathVariable String comentarioId,
            @PathVariable String respuestaId,
            Principal principal) {
        
        Optional<Usuario> usuarioOpt = obtenerUsuarioAutenticado(principal);
        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            // Buscar el foro
            Optional<Foro> foroOpt = foroServicio.obtenerPorId(foroId);
            if (foroOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Foro foro = foroOpt.get();
            
            // Buscar el comentario
            Optional<Foro.Comentario> comentarioOpt = foro.getComentarios().stream()
                    .filter(c -> comentarioId.equals(c.getId()))
                    .findFirst();
            
            if (comentarioOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Comentario no encontrado con ID: " + comentarioId);
            }
            
            Foro.Comentario comentario = comentarioOpt.get();
            
            // Buscar la respuesta
            Optional<Foro.Comentario.Respuesta> respuestaOpt = comentario.getRespuestas().stream()
                    .filter(r -> respuestaId.equals(r.getId()))
                    .findFirst();
            
            if (respuestaOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Respuesta no encontrada con ID: " + respuestaId);
            }
            
            Foro.Comentario.Respuesta respuesta = respuestaOpt.get();
            String usuarioId = usuarioOpt.get().getId();
            
            // Toggle like
            if (respuesta.getUsuariosQueDieronLike().contains(usuarioId)) {
                respuesta.quitarLike(usuarioId);
            } else {
                respuesta.agregarLike(usuarioId);
            }
            
            // Guardar el foro actualizado
            Foro foroActualizado = foroServicio.crearForo(foro);
            
            return ResponseEntity.ok(foroActualizado);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al actualizar like: " + e.getMessage());
        }
    }

    /**
     * Reportar un comentario como inapropiado
     */
    @PostMapping("/{foroId}/comentarios/{comentarioId}/reportar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> reportarComentario(
            @PathVariable String foroId,
            @PathVariable String comentarioId,
            @RequestBody String motivo,
            Principal principal) {
        
        Optional<Usuario> usuarioOpt = obtenerUsuarioAutenticado(principal);
        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        Usuario usuario = usuarioOpt.get();
        
        // Verificar que el foro exista
        Optional<Foro> foroOpt = foroServicio.obtenerPorId(foroId);
        if (foroOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Foro no encontrado", "mensaje", "El foro solicitado no existe"));
        }
        
        try {
            // Reportar el comentario en el servicio
            Optional<Foro> foroActualizado = foroServicio.reportarComentario(foroId, comentarioId, usuario.getId(), motivo);
            
            if (foroActualizado.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Comentario no encontrado", "mensaje", "El comentario que intentas reportar no existe"));
            }
            
            return ResponseEntity.ok(Map.of(
                "mensaje", "El comentario ha sido reportado correctamente y será revisado por un administrador",
                "usuario", usuario.getNombre(),
                "fechaReporte", new Date()
            ));
            
        } catch (Exception e) {
            System.err.println("Error al reportar comentario: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al reportar comentario", "mensaje", e.getMessage()));
        }
    }
}
