package com.Impulso.Alcambio.Controller;

import java.util.List;
import java.util.Optional;
import java.security.Principal;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.web.multipart.MultipartFile;

import com.Impulso.Alcambio.Modelo.Proyecto;
import com.Impulso.Alcambio.Modelo.Usuario;
import com.Impulso.Alcambio.Servicio.ProyectoServicio;
import com.Impulso.Alcambio.Servicio.UsuarioServicio;
import com.Impulso.Alcambio.Modelo.Desafio;
import com.Impulso.Alcambio.Servicio.ForoServicio;

/**
 * Este es el controlador para gestionar todo lo relacionado con los proyectos.
 * Maneja las peticiones del frontend para crear, listar, actualizar y borrar
 * proyectos, así como gestionar los participantes y recursos asociados.
 */
@RestController
@RequestMapping("/api/proyectos")
// @PreAuthorize("hasAuthority('ADMIN')") // Comentado para permitir a usuarios autenticados ver proyectos
public class ProyectoController {
    
    @Autowired
    private ProyectoServicio proyectoServicio;
    
    @Autowired
    private UsuarioServicio usuarioServicio;
    
    @Autowired
    private ForoServicio foroServicio;
    
    // =================== MÉTODOS AUXILIARES ===================
    
    /**
     * Obtiene el usuario autenticado a partir del objeto Principal
     * @param principal Información del usuario autenticado
     * @return Optional con el usuario si existe, empty en caso contrario
     */
    private Optional<Usuario> obtenerUsuarioAutenticado(Principal principal) {
        if (principal == null) {
            return Optional.empty();
        }
        return usuarioServicio.buscarPorCorreo(principal.getName());
    }
    
    /**
     * Verifica si un proyecto existe y retorna una respuesta apropiada
     * @param id Identificador del proyecto a verificar
     * @return ResponseEntity con error si no existe, null si existe
     */
    private <T> ResponseEntity<T> verificarProyectoExiste(String id) {
        if (!proyectoServicio.obtenerProyectoPorId(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        return null; // Proyecto existe
    }
    
    /**
     * Extrae la extensión de un nombre de archivo
     * @param filename Nombre del archivo
     * @return Extensión del archivo o ".jpg" por defecto
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return ".jpg"; // Extensión por defecto
        }
        return filename.substring(filename.lastIndexOf("."));
    }
    
    // =================== CONSULTA DE PROYECTOS ===================
    
    // Endpoint principal que lista TODOS los proyectos existentes
    // Lo usamos en el dashboard y la página de exploración de proyectos
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Proyecto>> obtenerProyectos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Proyecto> proyectosPage = proyectoServicio.obtenerTodosPaginados(pageable);
        return ResponseEntity.ok(proyectosPage.getContent());
    }
    
    // Devuelve los detalles completos de un proyecto específico
    // Se usa cuando el usuario hace clic en un proyecto para ver su info
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Proyecto> obtenerProyectoPorId(@PathVariable String id) {
        return proyectoServicio.obtenerProyectoPorId(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    // Permite buscar proyectos por nombre, útil para el buscador de la aplicación
    @GetMapping("/buscar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<Proyecto>> buscarProyectosPorNombre(
            @RequestParam String nombre,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(proyectoServicio.buscarPorNombrePaginado(nombre, pageable));
    }
    
    /**
     * Obtiene proyectos disponibles para el usuario actual (donde no participa)
     */
    @GetMapping("/disponibles")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Proyecto>> obtenerProyectosDisponibles(Principal principal) {
        Optional<Usuario> usuarioOpt = obtenerUsuarioAutenticado(principal);
        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        Usuario usuario = usuarioOpt.get();
        
        List<Proyecto> todosProyectos = proyectoServicio.obtenerTodos();
        
        List<String> proyectosDelUsuario = usuario.getProyectosParticipadosIds().stream()
                .toList();
        
        List<Proyecto> proyectosDisponibles = todosProyectos.stream()
                .filter(p -> !proyectosDelUsuario.contains(p.getId()))
                .toList();
        
        return ResponseEntity.ok(proyectosDisponibles);
    }
    
    // =================== OPERACIONES CRUD (ADMIN) ===================
    
    /**
     * Crea un nuevo proyecto (solo administradores)
     */
    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Proyecto> crearProyecto(
            @RequestBody Proyecto proyecto,
            @RequestParam(required = false, defaultValue = "true") boolean crearForo,
            Principal principal) {
        
        Proyecto proyectoGuardado = proyectoServicio.crearProyecto(proyecto);
        
        // Si se indica que se debe crear el foro automáticamente
        if (crearForo) {
            try {
                // Obtener el usuario administrador que está creando el proyecto
                Optional<Usuario> usuarioOpt = obtenerUsuarioAutenticado(principal);
                if (usuarioOpt.isPresent()) {
                    Usuario admin = usuarioOpt.get();
                    
                    // Crear el foro asociado al proyecto
                    foroServicio.crearForoParaProyecto(proyectoGuardado, admin);
                }
            } catch (Exception e) {
                // Si falla la creación del foro, loguear pero no afectar la creación del proyecto
                System.err.println("Error al crear el foro automático: " + e.getMessage());
            }
        }
        
        return ResponseEntity.status(HttpStatus.CREATED).body(proyectoGuardado);
    }
    
    /**
     * Actualiza un proyecto existente (solo administradores)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Proyecto> actualizarProyecto(@PathVariable String id, @RequestBody Proyecto proyecto) {
        ResponseEntity<Proyecto> verificacion = verificarProyectoExiste(id);
        if (verificacion != null) {
            return verificacion;
        }
        
        proyecto.setId(id);
        Proyecto proyectoActualizado = proyectoServicio.actualizarProyecto(proyecto);
        return ResponseEntity.ok(proyectoActualizado);
    }
    
    /**
     * Elimina un proyecto (solo administradores)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> eliminarProyecto(@PathVariable String id) {
        ResponseEntity<Void> verificacion = verificarProyectoExiste(id);
        if (verificacion != null) {
            return verificacion;
        }
        
        proyectoServicio.eliminarProyecto(id);
        return ResponseEntity.noContent().build();
    }
    
    // =================== GESTIÓN DE PARTICIPANTES ===================
    
    /**
     * Permite a un usuario unirse a un proyecto
     */
    @PostMapping("/{id}/unirse")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> unirseAProyecto(
            @PathVariable String id, 
            @RequestParam String rol,
            Principal principal) {
        
        Optional<Usuario> usuarioOpt = obtenerUsuarioAutenticado(principal);
        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no encontrado");
        }
        
        Optional<Proyecto> proyectoOpt = proyectoServicio.obtenerProyectoPorId(id);
        if (proyectoOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Proyecto proyecto = proyectoOpt.get();
        Usuario usuario = usuarioOpt.get();
        
        // Verificar si el proyecto está activo
        if (proyecto.getEstado() != Proyecto.EstadoProyecto.ACTIVO) {
            String mensaje = "No es posible unirse a este proyecto.";
            if (proyecto.getEstado() == Proyecto.EstadoProyecto.EXPIRADO) {
                mensaje = "El proyecto ha expirado.";
            } else if (proyecto.getEstado() == Proyecto.EstadoProyecto.COMPLETO) {
                mensaje = "Se cumplió el número de participantes de este proyecto.";
            } else if (proyecto.getEstado() == Proyecto.EstadoProyecto.CANCELADO) {
                mensaje = "El proyecto ha sido cancelado.";
            }
            return ResponseEntity.status(HttpStatus.CONFLICT).body(mensaje);
        }
        
        // Verificar si ha alcanzado el límite de participantes
        if (proyecto.haAlcanzadoLimiteParticipantes()) {
            // Actualizar estado del proyecto si se alcanzó el límite
            proyecto.setEstado(Proyecto.EstadoProyecto.COMPLETO);
            proyectoServicio.actualizarProyecto(proyecto);
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Se cumplió el número de participantes de este proyecto.");
        }
        
        boolean yaParticipando = usuario.getProyectosParticipadosIds().stream()
                .anyMatch(p -> p.equals(id));
        
        if (yaParticipando) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Ya estás participando en este proyecto");
        }
        
        // Intentar agregar participante al proyecto
        boolean participanteAgregado = proyectoServicio.intentarAgregarParticipante(id);
        if (!participanteAgregado) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("No se pudo unir al proyecto, se ha alcanzado el límite de participantes.");
        }
        
        usuario.agregarProyectoParticipado(proyecto.getId());
        usuarioServicio.actualizarUsuario(usuario);
        
        return ResponseEntity.status(HttpStatus.OK).body("Te has unido al proyecto exitosamente");
    }
    
    /**
     * Permite a un usuario abandonar un proyecto
     */
    @DeleteMapping("/{id}/abandonar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> abandonarProyecto(@PathVariable String id, Principal principal) {
        // Verificar primero si el proyecto existe
        Optional<Proyecto> proyectoOpt = proyectoServicio.obtenerProyectoPorId(id);
        if (proyectoOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Optional<Usuario> usuarioOpt = obtenerUsuarioAutenticado(principal);
        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no encontrado");
        }
        
        Usuario usuario = usuarioOpt.get();
        
        boolean eliminado = usuarioServicio.eliminarParticipacion(usuario.getId(), id);
        
        if (eliminado) {
            // Actualizar el contador de participantes en el proyecto
            Proyecto proyecto = proyectoOpt.get();
            // Usar el método decrementarParticipantes
            proyecto.decrementarParticipantes();
            proyectoServicio.actualizarProyecto(proyecto);
            
            return ResponseEntity.ok("Has abandonado el proyecto exitosamente");
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("No estás participando en este proyecto");
        }
    }
    
    /**
     * Obtiene los participantes de un proyecto
     */
    @GetMapping("/{id}/participantes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> obtenerParticipantesProyecto(@PathVariable String id) {
        ResponseEntity<Void> verificacion = verificarProyectoExiste(id);
        if (verificacion != null) {
            return ResponseEntity.notFound().build();
        }
        
        List<Usuario> todosUsuarios = usuarioServicio.obtenerTodos();
        
        List<Map<String, Object>> participantes = todosUsuarios.stream()
            .filter(usuario -> usuario.getProyectosParticipadosIds() != null)
            .filter(usuario -> usuario.getProyectosParticipadosIds().stream()
                    .anyMatch(p -> p.equals(id)))
            .map(usuario -> {
                Map<String, Object> datosParticipante = new HashMap<>();
                datosParticipante.put("id", usuario.getId());
                datosParticipante.put("nombre", usuario.getNombre());
                datosParticipante.put("imagenPerfil", usuario.getImagenPerfil());
                
                // Obtener el rol específico para este proyecto
                String rol = usuario.getProyectosParticipadosIds().stream()
                        .filter(p -> p.equals(id))
                        .map(p -> "Participante") // Por ahora todos son participantes
                        .findFirst()
                        .orElse("Participante");
                
                datosParticipante.put("rol", rol);
                return datosParticipante;
            })
            .collect(Collectors.toList());
        
        // Actualizar el contador de participantes en el proyecto
        proyectoServicio.actualizarContadorParticipantes(id);
        
        return ResponseEntity.ok(participantes);
    }
    
    /**
     * Obtiene información sobre el límite y contador de participantes en un proyecto
     */
    @GetMapping("/{id}/participantes/contador")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> obtenerContadorParticipantes(@PathVariable String id) {
        return proyectoServicio.obtenerProyectoPorId(id)
            .map(proyecto -> {
                Map<String, Object> datos = new HashMap<>();
                
                // Si no hay contador, calcularlo
                if (proyecto.getParticipantesActuales() == null) {
                    int participantes = proyectoServicio.contarParticipantesProyecto(id);
                    proyecto.setParticipantesActuales(participantes);
                    proyectoServicio.actualizarProyecto(proyecto);
                }
                
                datos.put("participantesActuales", proyecto.getParticipantesActuales());
                datos.put("limiteParticipantes", proyecto.getLimiteParticipantes());
                datos.put("estado", proyecto.getEstado());
                datos.put("estaCompleto", proyecto.haAlcanzadoLimiteParticipantes());
                
                return ResponseEntity.ok(datos);
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    // =================== OPERACIONES DE DESAFÍOS ===================
    
    /**
     * Obtiene los desafíos asociados a un proyecto
     */
    @GetMapping("/{id}/desafios")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Desafio>> obtenerDesafiosDeProyecto(@PathVariable String id) {
        ResponseEntity<List<Desafio>> verificacion = verificarProyectoExiste(id);
        if (verificacion != null) {
            return verificacion;
        }
        
        return ResponseEntity.ok(proyectoServicio.obtenerDesafiosDeProyecto(id));
    }

    /**
     * Agrega un desafío existente a un proyecto (solo administradores)
     */
    @PostMapping("/{proyectoId}/desafios/{desafioId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Proyecto> agregarDesafioAProyecto(@PathVariable String proyectoId, @PathVariable String desafioId) {
        return proyectoServicio.agregarDesafioAProyecto(proyectoId, desafioId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    // =================== SUBIDA DE ARCHIVOS ===================
    
    /**
     * Sube una imagen para un proyecto (solo administradores)
     */
    @PostMapping("/upload/proyecto")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> subirImagenProyecto(@RequestParam("imagen") MultipartFile file) {
        try {
            // Verificar que el archivo no esté vacío
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("El archivo está vacío");
            }
            
            // Verificar que sea una imagen
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body("Solo se permiten archivos de imagen");
            }
            
            // Crear directorio si no existe
            String uploadDir = "src/main/resources/static/Proyectos";
            File directory = new File(uploadDir);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // Generar nombre único para el archivo
            String extension = getFileExtension(file.getOriginalFilename());
            String newFileName = UUID.randomUUID().toString() + extension;
            
            // Guardar archivo
            Path path = Paths.get(uploadDir + "/" + newFileName);
            file.transferTo(path);
            
            // Devolver información
            Map<String, String> response = new HashMap<>();
            response.put("nombreArchivo", newFileName);
            response.put("url", "/Proyectos/" + newFileName);
            
            return ResponseEntity.ok(response);
            
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error al guardar el archivo: " + e.getMessage());
        }
    }

    // Endpoint para obtener participantes de un proyecto (para modal admin)
    @GetMapping("/admin/{id}/participantes")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> obtenerParticipantesProyectoAdmin(@PathVariable String id) {
        try {
            // Obtener todos los usuarios
            List<Usuario> todosLosUsuarios = usuarioServicio.obtenerTodos();
            
            // Filtrar por los que participan en este proyecto
            List<Map<String, Object>> participantes = todosLosUsuarios.stream()
                .filter(u -> u.getProyectosParticipadosIds() != null && 
                       u.getProyectosParticipadosIds().contains(id))
                .map(u -> {
                    Map<String, Object> info = new HashMap<>();
                    info.put("id", u.getId());
                    info.put("nombre", u.getNombre());
                    info.put("correo", u.getCorreo());
                    info.put("rol", u.getRol().toString());
                    info.put("imagenPerfil", u.getImagenPerfil());
                    return info;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(participantes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error al obtener participantes: " + e.getMessage()));
        }
    }
    
    // Endpoint para obtener desafíos de un proyecto (para modal admin)
    @GetMapping("/admin/{id}/desafios")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> obtenerDesafiosProyectoAdmin(@PathVariable String id) {
        try {
            List<Desafio> desafios = proyectoServicio.obtenerDesafiosDeProyecto(id);
            return ResponseEntity.ok(desafios);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error al obtener desafíos: " + e.getMessage()));
        }
    }
    
    // Endpoint para obtener estadísticas del proyecto (para modal admin)
    @GetMapping("/admin/{id}/estadisticas")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> obtenerEstadisticasProyectoAdmin(@PathVariable String id) {
        try {
            Map<String, Object> estadisticas = proyectoServicio.obtenerEstadisticasProyecto(id);
            return ResponseEntity.ok(estadisticas);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error al obtener estadísticas: " + e.getMessage()));
        }
    }
} 