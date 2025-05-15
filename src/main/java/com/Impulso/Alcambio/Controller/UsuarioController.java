package com.Impulso.Alcambio.Controller;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.ArrayList;

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
import org.springframework.web.multipart.MultipartFile;

import com.Impulso.Alcambio.Modelo.Usuario;
import com.Impulso.Alcambio.Modelo.ParticipacionDesafio;
import com.Impulso.Alcambio.Modelo.Proyecto.EstadoProyecto;
import com.Impulso.Alcambio.Servicio.UsuarioServicio;
import com.Impulso.Alcambio.Servicio.ParticipacionDesafioServicio;
import com.Impulso.Alcambio.Modelo.Proyecto;
import com.Impulso.Alcambio.Servicio.ProyectoServicio;
import com.Impulso.Alcambio.Modelo.Desafio;
import com.Impulso.Alcambio.Servicio.DesafioServicio;

/**
 * Controlador principal para la gestión de usuarios
 * Maneja todo lo relacionado con cuentas, perfiles y 
 * autorización de usuarios en la plataforma
 */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {
    @Autowired
    private UsuarioServicio usuarioServicio;
    
    @Autowired
    private ParticipacionDesafioServicio participacionDesafioServicio;

    @Autowired
    private ProyectoServicio proyectoServicio;

    @Autowired
    private DesafioServicio desafioServicio;

    // =================== MÉTODOS AUXILIARES ===================
    
    /**
     * Verifica la autenticación y autorización para acceder a datos de usuario
     * @param id ID del usuario a verificar acceso
     * @param principal Principal actual
     * @param requiresAdmin Si se requiere rol de administrador
     * @return ResponseEntity con error si hay fallo de autenticación/autorización, null si todo está correcto
     */
    private ResponseEntity<?> verificarAcceso(String id, Principal principal, boolean requiresAdmin) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No autenticado");
        }
        
        Optional<Usuario> authUserOpt = usuarioServicio.buscarPorCorreo(principal.getName());
        if (authUserOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        Usuario authUser = authUserOpt.get();
        boolean isAdmin = authUser.getRol().toString().equals("ADMIN");
        
        if (requiresAdmin && !isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Acceso denegado");
        }
        
        if (!isAdmin && !authUser.getId().equals(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Acceso denegado");
        }
        
        return null; // Todo correcto
    }

    // =================== OPERACIONES CRUD BÁSICAS ===================
    
    /**
     * Endpoint para crear un nuevo usuario en el sistema (registro)
     * Lo utilizamos en el formulario de registro de la aplicación
     */
    @PostMapping
    public ResponseEntity<?> crearUsuario(@RequestBody Usuario usuario) {
        try {
            Usuario usuarioGuardado = usuarioServicio.registrarUsuario(usuario);
            return ResponseEntity.status(HttpStatus.CREATED).body(usuarioGuardado);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    /**
     * Obtiene la información de un usuario específico
     * Se usa para cargar perfiles de usuario y pantallas de configuración
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> obtenerUsuario(@PathVariable String id, Principal principal) {
        ResponseEntity<?> accesoResponse = verificarAcceso(id, principal, false);
        if (accesoResponse != null) {
            return accesoResponse;
        }

        return usuarioServicio.buscarPorId(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Endpoint para la administración - muestra todos los usuarios del sistema
     * Solo disponible para administradores en el panel de control
     */
    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Page<Usuario>> obtenerUsuarios(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(usuarioServicio.obtenerTodosPaginados(pageable));
    }

    /**
     * Actualiza un usuario existente
     */
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> actualizarUsuario(@PathVariable String id, @RequestBody Usuario usuarioNuevo, Principal principal) {
        ResponseEntity<?> accesoResponse = verificarAcceso(id, principal, false);
        if (accesoResponse != null) {
            return accesoResponse;
        }
        
        return usuarioServicio.buscarPorId(id)
            .map(usuarioExistente -> {
                if (!usuarioExistente.getId().equals(id)) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Inconsistencia de ID");
                }
                
                usuarioExistente.setNombre(usuarioNuevo.getNombre());
                usuarioExistente.setNumero(usuarioNuevo.getNumero());
                usuarioExistente.setEmpresa(usuarioNuevo.getEmpresa());
                usuarioExistente.setImagenPerfil(usuarioNuevo.getImagenPerfil());
                
                if (usuarioNuevo.getPassword() != null && !usuarioNuevo.getPassword().isEmpty()) {
                    usuarioExistente.setPassword(usuarioNuevo.getPassword());
                } else {
                    usuarioExistente.setPassword(null);
                }
                
                Usuario actualizado = usuarioServicio.actualizarUsuario(usuarioExistente);
                return ResponseEntity.ok(actualizado);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Elimina un usuario (solo admin)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> eliminarUsuario(@PathVariable String id) {
        boolean eliminado = usuarioServicio.eliminarUsuario(id);
        
        return eliminado 
            ? ResponseEntity.noContent().build() 
            : ResponseEntity.notFound().build();
    }
    
    // =================== OPERACIONES DE BÚSQUEDA ===================
    
    /**
     * Busca usuarios por correo electrónico (solo admin)
     */
    @GetMapping("/buscar/correo")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Usuario> buscarPorCorreo(@RequestParam String correo) {
        return usuarioServicio.buscarPorCorreo(correo)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Busca usuarios por empresa (solo admin)
     */
    @GetMapping("/buscar/empresa")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<Usuario>> buscarPorEmpresa(@RequestParam String empresa) {
        return ResponseEntity.ok(usuarioServicio.buscarPorEmpresa(empresa));
    }
    
    // =================== PERFIL DE USUARIO ===================
    
    /**
     * Obtiene el perfil de un usuario específico
     */
    @GetMapping("/{id}/perfil")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> obtenerPerfilUsuario(@PathVariable String id, Principal principal) {
        ResponseEntity<?> accesoResponse = verificarAcceso(id, principal, false);
        if (accesoResponse != null) {
            return accesoResponse;
        }
               
        return usuarioServicio.buscarPorId(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Obtiene el perfil completo del usuario actual
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> obtenerUsuarioActual(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No autenticado");
        }
        
        Optional<Usuario> usuarioOpt = usuarioServicio.buscarPorCorreo(principal.getName());
        
        // Crear datos básicos por defecto incluso si el usuario no existe en la base de datos
        if (usuarioOpt.isEmpty()) {
            Map<String, Object> datosFallback = new HashMap<>();
            datosFallback.put("id", "usuario-no-encontrado");
            datosFallback.put("nombre", principal.getName());
            datosFallback.put("correo", principal.getName());
            datosFallback.put("proyectosParticipadosIds", new ArrayList<>());
            datosFallback.put("mensaje", "Usuario autenticado pero no encontrado en la base de datos");
            
            return ResponseEntity.ok(datosFallback);
        }
        
        Usuario usuario = usuarioOpt.get();
        
        // Crear un mapa con toda la información del perfil
        Map<String, Object> perfilCompleto = new HashMap<>();
        
        // Datos básicos del usuario
        perfilCompleto.put("id", usuario.getId());
        perfilCompleto.put("nombre", usuario.getNombre());
        perfilCompleto.put("correo", usuario.getCorreo());
        perfilCompleto.put("numero", usuario.getNumero());
        perfilCompleto.put("empresa", usuario.getEmpresa());
        perfilCompleto.put("rol", usuario.getRol());
        perfilCompleto.put("imagenPerfil", usuario.getImagenPerfil());
        
        // Inicializar listas por defecto para evitar problemas con valores null
        List<Proyecto> proyectosActivos = List.of();
        List<Proyecto> proyectosCompletados = List.of();
        int totalProyectos = 0;
        
        // Comprobar que proyectosParticipadosIds no sea null antes de operar con él
        if (usuario.getProyectosParticipadosIds() != null && !usuario.getProyectosParticipadosIds().isEmpty()) {
            // Obtener IDs de proyectos participados
            List<String> proyectoIds = usuario.getProyectosParticipadosIds();
            // Obtener los objetos Proyecto completos (necesitamos ProyectoServicio)
            List<Proyecto> proyectosCompletos = obtenerProyectosCompletosParaUsuario(usuario);

            // Información de proyectos
            proyectosActivos = proyectosCompletos.stream()
                    .filter(p -> p.getEstado() != null && p.getEstado() == EstadoProyecto.ACTIVO)
                    .collect(Collectors.toList());

            proyectosCompletados = proyectosCompletos.stream()
                    .filter(p -> p.getEstado() != null && p.getEstado() == EstadoProyecto.COMPLETO)
                    .collect(Collectors.toList());

            totalProyectos = proyectosCompletos.size(); // Contar proyectos completos, no participaciones
        }
        
        // Siempre asignar los valores, incluso si son listas vacías
        perfilCompleto.put("proyectosParticipadosIds", usuario.getProyectosParticipadosIds() != null ? 
                                                   usuario.getProyectosParticipadosIds() : 
                                                   new ArrayList<>());
        perfilCompleto.put("proyectosActivos", proyectosActivos);
        perfilCompleto.put("proyectosCompletados", proyectosCompletados);
        perfilCompleto.put("totalProyectos", totalProyectos);
        
        // Información de desafíos - inicializar con valores por defecto
        List<ParticipacionDesafio> desafiosActivos = List.of();
        List<ParticipacionDesafio> desafiosCompletados = List.of();
        int totalDesafiosCompletados = 0;
        
        // Información de desafíos
        List<ParticipacionDesafio> participaciones = 
                participacionDesafioServicio.obtenerPorUsuario(usuario.getId());
        
        if (participaciones != null && !participaciones.isEmpty()) {
            desafiosCompletados = participaciones.stream()
                    .filter(ParticipacionDesafio::isCompletado)
                    .collect(Collectors.toList());
            
            desafiosActivos = participaciones.stream()
                    .filter(p -> !p.isCompletado())
                    .collect(Collectors.toList());
                    
            totalDesafiosCompletados = desafiosCompletados.size();
        }
        
        // Siempre asignar los valores
        perfilCompleto.put("desafiosActivos", desafiosActivos);
        perfilCompleto.put("desafiosCompletados", desafiosCompletados);
        perfilCompleto.put("totalDesafiosCompletados", totalDesafiosCompletados);
        
        // Valores por defecto para mecánica de juego
        int puntos = 0;
        List<?> insignias = List.of();
        List<?> logros = List.of();
        
        // Obtener información de gamificación usando ParticipacionDesafioServicio
        puntos = participacionDesafioServicio.calcularPuntosTotales(usuario.getId());
        insignias = participacionDesafioServicio.obtenerInsigniasUsuario(usuario.getId());
        
        // Como ya no tenemos la clase LogroDesbloqueado, usamos un mapa simple
        logros = participacionDesafioServicio.obtenerPorUsuario(usuario.getId()).stream()
            .filter(ParticipacionDesafio::isCompletado)
            .map(p -> {
                Map<String, Object> logro = new HashMap<>();
                logro.put("nombre", p.getNombre());
                logro.put("fechaDesbloqueo", p.getFechaCompletado());
                logro.put("puntosOtorgados", desafioServicio.obtenerPorId(p.getDesafioId())
                          .map(Desafio::getPuntosRecompensa)
                          .orElse(0));
                return logro;
            })
            .collect(Collectors.toList());
        
        // Siempre asignar los valores
        perfilCompleto.put("puntos", puntos);
        perfilCompleto.put("insignias", insignias);
        perfilCompleto.put("logros", logros);
        
        // URLs para actualización de perfil
        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("actualizarPerfil", "/api/usuarios/" + usuario.getId());
        endpoints.put("cambiarPassword", usuarioServicio.obtenerEndpointCambioPassword(usuario.getId()));
        perfilCompleto.put("endpoints", endpoints);
        
        return ResponseEntity.ok(perfilCompleto);
    }
    
    /**
     * Cambia la contraseña de un usuario
     */
    @PutMapping("/{id}/cambiar-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> cambiarPassword(
            @PathVariable String id, 
            @RequestBody Map<String, String> passwordData,
            Principal principal) {
            
        ResponseEntity<?> accesoResponse = verificarAcceso(id, principal, false);
        if (accesoResponse != null) {
            return accesoResponse;
        }
        
        // Validar datos
        String oldPassword = passwordData.get("oldPassword");
        String newPassword = passwordData.get("newPassword");
        
        if (oldPassword == null || newPassword == null) {
            return ResponseEntity.badRequest().body("Se requieren contraseña antigua y nueva");
        }
        
        try {
            boolean exito = usuarioServicio.cambiarPassword(id, oldPassword, newPassword);
            
            return exito 
                ? ResponseEntity.ok().body(Map.of("mensaje", "Contraseña actualizada con éxito")) 
                : ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Contraseña incorrecta");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al cambiar contraseña: " + e.getMessage());
        }
    }
    
    /**
     * Sube una imagen de perfil para un usuario
     */
    @PostMapping("/{id}/imagen-perfil")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> subirImagenPerfil(
            @PathVariable String id,
            @RequestParam("imagen") MultipartFile archivo,
            Principal principal) {
            
        ResponseEntity<?> accesoResponse = verificarAcceso(id, principal, false);
        if (accesoResponse != null) {
            return accesoResponse;
        }
        
        try {
            // Obtener usuario a actualizar
            Optional<Usuario> usuarioOpt = usuarioServicio.buscarPorId(id);
            if (usuarioOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Usuario usuario = usuarioOpt.get();
            
            // Verificar que no esté vacío y sea una imagen
            if (archivo.isEmpty()) {
                return ResponseEntity.badRequest().body("La imagen no puede estar vacía");
            }
            
            String contentType = archivo.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body("El archivo debe ser una imagen");
            }
            
            // Crear directorio si no existe
            java.io.File directorio = new java.io.File("src/main/resources/static/Perfiles");
            if (!directorio.exists()) {
                directorio.mkdirs();
            }
            
            // Generar nombre único para el archivo
            String nombreArchivo = java.util.UUID.randomUUID().toString() + "_" + 
                                 archivo.getOriginalFilename().replaceAll("\\s+", "_");
            
            java.nio.file.Path rutaCompleta = java.nio.file.Path.of(directorio.getPath(), nombreArchivo);
            
            // Copiar el archivo a la ruta de destino usando transferTo
            archivo.transferTo(rutaCompleta);
            
            // Actualizar el campo imagenPerfil del usuario con la URL relativa
            String urlImagen = "/Perfiles/" + nombreArchivo;
            usuario.setImagenPerfil(urlImagen);
            
            // Guardar el usuario actualizado
            usuarioServicio.actualizarUsuario(usuario);
            
            // Devolver URL de la imagen
            return ResponseEntity.ok().body(Map.of(
                "mensaje", "Imagen de perfil actualizada con éxito",
                "imagenUrl", urlImagen
            ));
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al subir la imagen: " + e.getMessage());
        }
    }
    
    // =================== GESTIÓN DE PROYECTOS ===================
    
    /**
     * Agrega un proyecto a un usuario
     */
    @PostMapping("/{id}/proyectos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> agregarProyecto(@PathVariable String id, @RequestBody Map<String, String> payload, Principal principal) {
        ResponseEntity<?> accesoResponse = verificarAcceso(id, principal, false);
        if (accesoResponse != null) {
            return accesoResponse;
        }
        
        String proyectoId = payload.get("proyectoId");
        if (proyectoId == null || proyectoId.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Se requiere el campo 'proyectoId' en el cuerpo de la solicitud."));
        }

        Optional<Usuario> usuarioOpt = usuarioServicio.buscarPorId(id);
        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Usuario usuario = usuarioOpt.get();
        // Usar el proyectoId extraído del payload
        usuario.agregarProyectoParticipado(proyectoId); 
        Usuario usuarioActualizado = usuarioServicio.actualizarUsuario(usuario);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioActualizado);
    }

    /**
     * Obtiene los proyectos en los que participa un usuario
     */
    @GetMapping("/{id}/proyectos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> obtenerProyectosParticipados(@PathVariable String id, Principal principal) {
        ResponseEntity<?> accesoResponse = verificarAcceso(id, principal, false);
        if (accesoResponse != null) {
            return accesoResponse;
        }
        
        Optional<Usuario> usuarioOpt = usuarioServicio.buscarPorId(id);
        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Usuario usuario = usuarioOpt.get();
        List<String> proyectosIds = usuario.getProyectosParticipadosIds() != null ? 
                                   usuario.getProyectosParticipadosIds() : 
                                   new ArrayList<>();
                                   
        // Obtener información completa de los proyectos
        List<Map<String, Object>> proyectosInfo = new ArrayList<>();
        
        for (String proyectoId : proyectosIds) {
            Optional<Proyecto> proyectoOpt = proyectoServicio.obtenerProyectoPorId(proyectoId);
            if (proyectoOpt.isPresent()) {
                Proyecto proyecto = proyectoOpt.get();
                Map<String, Object> proyectoInfo = new HashMap<>();
                proyectoInfo.put("id", proyecto.getId());
                proyectoInfo.put("nombre", proyecto.getNombre());
                proyectoInfo.put("estado", proyecto.getEstado());
                proyectoInfo.put("fechaCreacion", proyecto.getFechaCreacion());
                proyectoInfo.put("fechaExpiracion", proyecto.getFechaExpiracion());
                proyectosInfo.add(proyectoInfo);
            }
        }
        
        // Devolver la lista de IDs y la información de los proyectos
        Map<String, Object> response = new HashMap<>();
        response.put("proyectosParticipadosIds", proyectosIds);
        response.put("proyectosInfo", proyectosInfo);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Elimina la participación de un usuario en un proyecto
     */
    @DeleteMapping("/{id}/proyectos/{proyectoId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> eliminarParticipacionProyecto(@PathVariable String id, @PathVariable String proyectoId, Principal principal) {
        ResponseEntity<?> accesoResponse = verificarAcceso(id, principal, false);
        if (accesoResponse != null) {
            return accesoResponse;
        }
        
        boolean eliminado = usuarioServicio.eliminarParticipacion(id, proyectoId);
        
        return eliminado 
            ? ResponseEntity.noContent().build() 
            : ResponseEntity.notFound().build();
    }

    // Método auxiliar para obtener los Proyectos completos en los que participa un usuario
    private List<Proyecto> obtenerProyectosCompletosParaUsuario(Usuario usuario) {
        if (usuario.getProyectosParticipadosIds() == null || usuario.getProyectosParticipadosIds().isEmpty()) {
            return List.of();
        }
        List<String> proyectoIds = usuario.getProyectosParticipadosIds();
        // Usar la lógica de obtener todos y filtrar como en WebController
        List<Proyecto> todosLosProyectos = proyectoServicio.obtenerTodos();
        return todosLosProyectos.stream()
               .filter(p -> proyectoIds.contains(p.getId()))
               .toList();
    }
}