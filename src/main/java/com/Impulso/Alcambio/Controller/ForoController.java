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

// Clase DTO para el payload de agregar comentario
class ComentarioRequestPayload {
    private String texto;
    // El usuarioId se obtiene del Principal, por lo que no es estrictamente necesario aquí,
    // pero lo mantenemos si el frontend lo envía.
    @SuppressWarnings("unused")
    private String usuarioId;

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public void setUsuarioId(String usuarioId) {
        this.usuarioId = usuarioId;
    }
}

// Clase DTO para el payload de agregar respuesta
class RespuestaRequestPayload {
    private String contenido;

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }
}

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
            @RequestBody ComentarioRequestPayload payload, // Cambiado a DTO
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
        
        // Crear información del autor a partir del usuario autenticado
        Foro.AutorInfo autorInfo = new Foro.AutorInfo(usuario.getId(), usuario.getNombre(), usuario.getImagenPerfil());
        
        // Crear y añadir el comentario usando el texto del payload
        Foro.Comentario comentario = new Foro.Comentario(payload.getTexto(), autorInfo);
        foro.agregarComentario(comentario);
        
        // Guardar el foro actualizado (con el nuevo comentario)
        // El método actualizarForo en el servicio debería manejar la persistencia del objeto foro completo.
        Optional<Foro> foroActualizadoOpt = foroServicio.actualizarForo(foroId, foro);
        
        return foroActualizadoOpt
                .map(f -> ResponseEntity.status(HttpStatus.CREATED).body(f)) // Devolver 201 CREATED con el foro actualizado
                .orElseGet(() -> {
                    log.error("No se pudo actualizar el foro {} después de agregar un comentario.", foroId);
                    // Considerar si el foro no encontrado aquí es un error 500 o 404 si actualizarForo puede devolver empty()
                    // si el foro original desapareció entre el findById y el save.
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                });
    }
    
    /**
     * Añade una respuesta a un comentario existente
     */
    @PostMapping("/{foroId}/comentarios/{comentarioId}/respuestas")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> agregarRespuesta(
            @PathVariable String foroId,
            @PathVariable String comentarioId,
            @RequestBody RespuestaRequestPayload payload, // Cambiado a DTO
            Principal principal) {
        
        Optional<Usuario> usuarioOpt = obtenerUsuarioAutenticado(principal);
        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Usuario usuario = usuarioOpt.get();
        
        Optional<Foro> foroOpt = foroServicio.obtenerPorId(foroId);
        if (foroOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Foro no encontrado"));
        }
        Foro foro = foroOpt.get();
        
        Optional<Foro.Comentario> comentarioPadreOpt = foro.getComentarios().stream()
                .filter(c -> c.getId().equals(comentarioId))
                .findFirst();
        
        if (comentarioPadreOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Comentario padre no encontrado"));
        }
        Foro.Comentario comentarioPadre = comentarioPadreOpt.get();
        
        Foro.AutorInfo autorInfo = new Foro.AutorInfo(usuario.getId(), usuario.getNombre(), usuario.getImagenPerfil());
        Foro.Comentario.Respuesta nuevaRespuesta = new Foro.Comentario.Respuesta(payload.getContenido(), autorInfo);
        comentarioPadre.agregarRespuesta(nuevaRespuesta);
        
        Optional<Foro> foroActualizadoOpt = foroServicio.actualizarForo(foroId, foro);

        if (foroActualizadoOpt.isEmpty()) {
            log.error("Error al actualizar el foro {} después de agregar una respuesta al comentario {}", foroId, comentarioId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "No se pudo actualizar el foro después de agregar la respuesta"));
        }

        // Devolver la respuesta creada o el comentario/foro actualizado.
        // Por simplicidad, devolvemos el foro completo, el frontend ya recarga los comentarios.
        // Una mejora sería devolver solo la respuesta creada o el comentario padre actualizado.
        return ResponseEntity.status(HttpStatus.CREATED).body(foroActualizadoOpt.get());
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
        String usuarioId = usuarioOpt.get().getId();

        Optional<Foro> foroOpt = foroServicio.obtenerPorId(foroId);
        if (foroOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Foro foro = foroOpt.get();

        Optional<Foro.Comentario> comentarioOpt = foro.getComentarios().stream()
                .filter(c -> c.getId().equals(comentarioId))
                .findFirst();

        if (comentarioOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Comentario no encontrado"));
        }
        Foro.Comentario comentario = comentarioOpt.get();

        // Lógica de Toggle: si ya le dio like, se lo quita; si no, se lo da.
        boolean dioLike;
        if (comentario.getUsuariosQueDieronLike().contains(usuarioId)) {
            comentario.quitarLike(usuarioId);
            dioLike = false;
        } else {
            comentario.agregarLike(usuarioId);
            dioLike = true;
        }

        Optional<Foro> foroActualizadoOpt = foroServicio.actualizarForo(foroId, foro);

        if (foroActualizadoOpt.isEmpty()) {
            log.error("Error al actualizar el foro {} después de un 'like' al comentario {}", foroId, comentarioId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "No se pudo actualizar el foro"));
        }

        // Devolver información útil para la UI
        Map<String, Object> response = Map.of(
            "comentarioId", comentario.getId(),
            "likes", comentario.getLikes(),
            "currentUserLiked", dioLike,
            "message", "Like/unlike exitoso"
        );
        return ResponseEntity.ok(response);
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
        String usuarioActualId = usuarioOpt.get().getId();
        
        Optional<Foro> foroOpt = foroServicio.obtenerPorId(foroId);
        if (foroOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Foro no encontrado"));
        }
        Foro foro = foroOpt.get();
        
        Optional<Foro.Comentario> comentarioOpt = foro.getComentarios().stream()
                .filter(c -> c.getId().equals(comentarioId))
                .findFirst();
        
        if (comentarioOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Comentario padre no encontrado"));
        }
        Foro.Comentario comentarioPadre = comentarioOpt.get();
        
        Optional<Foro.Comentario.Respuesta> respuestaOpt = comentarioPadre.getRespuestas().stream()
                .filter(r -> r.getId().equals(respuestaId))
                .findFirst();
        
        if (respuestaOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Respuesta no encontrada"));
        }
        Foro.Comentario.Respuesta respuesta = respuestaOpt.get();
        
        boolean dioLike;
        if (respuesta.getUsuariosQueDieronLike().contains(usuarioActualId)) {
            respuesta.quitarLike(usuarioActualId);
            dioLike = false;
        } else {
            respuesta.agregarLike(usuarioActualId);
            dioLike = true;
        }
        
        Optional<Foro> foroActualizadoOpt = foroServicio.actualizarForo(foroId, foro);

        if (foroActualizadoOpt.isEmpty()) {
            log.error("Error al actualizar el foro {} después de un 'like' a la respuesta {}", foroId, respuestaId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "No se pudo actualizar el foro"));
        }
        
        Map<String, Object> response = Map.of(
            "respuestaId", respuesta.getId(),
            "likes", respuesta.getLikes(),
            "currentUserLiked", dioLike,
            "message", "Like/unlike en respuesta exitoso"
        );
        return ResponseEntity.ok(response);
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
