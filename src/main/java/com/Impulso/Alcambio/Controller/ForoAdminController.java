package com.Impulso.Alcambio.Controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import com.Impulso.Alcambio.Modelo.Foro;
import com.Impulso.Alcambio.Modelo.Rol;
import com.Impulso.Alcambio.Modelo.Usuario;
import com.Impulso.Alcambio.Servicio.ForoServicio;
import com.Impulso.Alcambio.Servicio.UsuarioServicio;

/**
 * Controlador para administración de foros
 * Proporciona endpoints para gestionar foros y comentarios
 */
@RestController
@RequestMapping("/api/admin/foros")
public class ForoAdminController {

    @Autowired
    private ForoServicio foroServicio;
    
    @Autowired
    private UsuarioServicio usuarioServicio;
    
    // Renderizar la vista de administración de foros
    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ModelAndView adminForosView() {
        ModelAndView modelAndView = new ModelAndView("ForoAdmin");
        return modelAndView;
    }
    
    // Obtener estadísticas para el panel de administración
    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Map<String, Object>> obtenerEstadisticas() {
        return ResponseEntity.ok(foroServicio.obtenerEstadisticasAdmin());
    }
    
    // Obtener foros según filtro (all, active, archived, reported)
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Page<Foro>> listarForos(
            @RequestParam(defaultValue = "all") String filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "fechaCreacion") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        Sort sort = Sort.by(sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        return ResponseEntity.ok(foroServicio.obtenerForosAdmin(filter, pageable));
    }
    
    // Buscar foros por título para administración
    @GetMapping("/search")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Page<Foro>> buscarForos(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(foroServicio.buscarPorTituloPaginado(query, pageable));
    }
    
    // Crear un nuevo foro desde el panel de administración
    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Foro> crearForo(@RequestBody Foro foro, Principal principal) {
        Optional<Usuario> usuarioOpt = usuarioServicio.buscarPorCorreo(principal.getName());
        
        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        Usuario admin = usuarioOpt.get();
        
        // Verificar que sea administrador
        if (admin.getRol() != Rol.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        // Configurar la información del autor
        Foro.AutorInfo autorInfo = new Foro.AutorInfo(
            admin.getId(),
            admin.getNombre(),
            admin.getImagenPerfil()
        );
        foro.setAutor(autorInfo);
        
        // Establecer fecha de creación
        foro.setFechaCreacion(LocalDateTime.now());
        
        // Guardar el foro
        Foro nuevoForo = foroServicio.crearForo(foro);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoForo);
    }
    
    // Archivar/desarchivar un foro
    @PutMapping("/{id}/archive")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Foro> archivarForo(
            @PathVariable String id,
            @RequestParam boolean archive) {
        
        return foroServicio.cambiarEstadoArchivado(id, archive)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    // Activar/desactivar un foro
    @PutMapping("/{id}/active")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Foro> activarForo(
            @PathVariable String id,
            @RequestParam boolean active) {
        
        return foroServicio.cambiarEstadoActivo(id, active)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    // Activar/desactivar comentarios en un foro
    @PutMapping("/{id}/comments")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Foro> permitirComentarios(
            @PathVariable String id,
            @RequestParam boolean allow) {
        
        return foroServicio.cambiarPermitirComentarios(id, allow)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    // Obtener todos los comentarios reportados
    @GetMapping("/reported-comments")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Page<Map<String, Object>>> listarComentariosReportados(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(foroServicio.obtenerComentariosReportados(pageable));
    }
    
    // Restringir un comentario
    @PutMapping("/{foroId}/comments/{comentarioId}/restrict")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> restringirComentario(
            @PathVariable String foroId,
            @PathVariable String comentarioId,
            @RequestBody(required = false) String motivo,
            Principal principal) {
        
        try {
            Optional<Usuario> usuarioOpt = usuarioServicio.buscarPorCorreo(principal.getName());
            
            if (usuarioOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Usuario no autenticado", "mensaje", "Debe iniciar sesión para realizar esta acción"));
            }
            
            Usuario admin = usuarioOpt.get();
            
            // Verificar que sea administrador
            if (admin.getRol() != Rol.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Acceso denegado", "mensaje", "Solo los administradores pueden realizar esta acción"));
            }
            
            // Si no se proporciona motivo, usar uno por defecto
            String motivoRestriccion = (motivo != null && !motivo.trim().isEmpty()) 
                    ? motivo : "Contenido inapropiado según políticas de la comunidad";
            
            Optional<Foro> resultado = foroServicio.restringirComentario(foroId, comentarioId, admin.getId(), motivoRestriccion);
            
            if (resultado.isPresent()) {
                // Devolver información detallada sobre la acción realizada
                Map<String, Object> respuesta = new HashMap<>();
                respuesta.put("mensaje", "Comentario restringido correctamente");
                respuesta.put("motivoRestriccion", motivoRestriccion);
                respuesta.put("fechaAccion", LocalDateTime.now());
                respuesta.put("adminNombre", admin.getNombre());
                
                return ResponseEntity.ok(respuesta);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Recurso no encontrado", "mensaje", "No se encontró el foro o comentario especificado"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al procesar la solicitud", "mensaje", e.getMessage()));
        }
    }
    
    // Quitar restricción de un comentario
    @PutMapping("/{foroId}/comments/{comentarioId}/unrestrict")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> quitarRestriccionComentario(
            @PathVariable String foroId,
            @PathVariable String comentarioId,
            Principal principal) {
        
        try {
            Optional<Usuario> usuarioOpt = usuarioServicio.buscarPorCorreo(principal.getName());
            
            if (usuarioOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Usuario no autenticado", "mensaje", "Debe iniciar sesión para realizar esta acción"));
            }
            
            Usuario admin = usuarioOpt.get();
            
            // Verificar que sea administrador
            if (admin.getRol() != Rol.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Acceso denegado", "mensaje", "Solo los administradores pueden realizar esta acción"));
            }
            
            Optional<Foro> resultado = foroServicio.eliminarRestriccionComentario(foroId, comentarioId);
            
            if (resultado.isPresent()) {
                // Devolver información detallada sobre la acción realizada
                Map<String, Object> respuesta = new HashMap<>();
                respuesta.put("mensaje", "Restricción eliminada correctamente");
                respuesta.put("fechaAccion", LocalDateTime.now());
                respuesta.put("adminNombre", admin.getNombre());
                
                return ResponseEntity.ok(respuesta);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Recurso no encontrado", "mensaje", "No se encontró el foro o comentario especificado"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al procesar la solicitud", "mensaje", e.getMessage()));
        }
    }
    
    // Aprobar un comentario reportado
    @PutMapping("/{foroId}/comments/{comentarioId}/approve")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> aprobarComentario(
            @PathVariable String foroId,
            @PathVariable String comentarioId,
            Principal principal) {
        
        try {
            Optional<Usuario> usuarioOpt = usuarioServicio.buscarPorCorreo(principal.getName());
            
            if (usuarioOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Usuario no autenticado", "mensaje", "Debe iniciar sesión para realizar esta acción"));
            }
            
            Usuario admin = usuarioOpt.get();
            
            // Verificar que sea administrador
            if (admin.getRol() != Rol.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Acceso denegado", "mensaje", "Solo los administradores pueden realizar esta acción"));
            }
            
            Optional<Foro> resultado = foroServicio.aprobarComentarioReportado(foroId, comentarioId);
            
            if (resultado.isPresent()) {
                // Devolver información detallada sobre la acción realizada
                Map<String, Object> respuesta = new HashMap<>();
                respuesta.put("mensaje", "Comentario aprobado correctamente");
                respuesta.put("fechaAccion", LocalDateTime.now());
                respuesta.put("adminNombre", admin.getNombre());
                respuesta.put("comentariosRestantes", contarComentariosReportadosRestantes(foroId));
                
                return ResponseEntity.ok(respuesta);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Recurso no encontrado", "mensaje", "No se encontró el foro o comentario especificado"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al procesar la solicitud", "mensaje", e.getMessage()));
        }
    }
    
    // Método auxiliar para contar comentarios reportados restantes en un foro
    private int contarComentariosReportadosRestantes(String foroId) {
        try {
            Optional<Foro> foroOpt = foroServicio.obtenerPorId(foroId);
            if (foroOpt.isEmpty()) {
                return 0;
            }
            
            Foro foro = foroOpt.get();
            return (int) foro.getComentarios().stream()
                    .filter(comentario -> comentario.isReportado())
                    .count();
        } catch (Exception e) {
            return 0;
        }
    }
    
    // Publicar comentario como administrador
    @PostMapping("/{foroId}/comments")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Foro> comentarComoAdmin(
            @PathVariable String foroId,
            @RequestBody String contenido,
            Principal principal) {
        
        Optional<Usuario> usuarioOpt = usuarioServicio.buscarPorCorreo(principal.getName());
        
        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        Usuario admin = usuarioOpt.get();
        
        // Verificar que sea administrador
        if (admin.getRol() != Rol.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        // Crear información del autor
        Foro.AutorInfo autorInfo = new Foro.AutorInfo(
            admin.getId(),
            admin.getNombre() + " (Admin)",  // Indicar explícitamente que es un comentario de administrador
            admin.getImagenPerfil()
        );
        
        // Crear el comentario
        Foro.Comentario comentario = new Foro.Comentario(contenido, autorInfo);
        
        // Buscar foro y añadir comentario
        Optional<Foro> foroOpt = foroServicio.obtenerPorId(foroId);
        
        if (foroOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Foro foro = foroOpt.get();
        foro.agregarComentario(comentario);
        
        // Guardar y devolver
        Foro foroActualizado = foroServicio.crearForo(foro);
        return ResponseEntity.status(HttpStatus.CREATED).body(foroActualizado);
    }
} 