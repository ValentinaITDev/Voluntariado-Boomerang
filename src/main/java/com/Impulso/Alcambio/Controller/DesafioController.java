package com.Impulso.Alcambio.Controller;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
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

import com.Impulso.Alcambio.Modelo.Desafio;
import com.Impulso.Alcambio.Modelo.ParticipacionDesafio;
import com.Impulso.Alcambio.Modelo.Usuario;
import com.Impulso.Alcambio.Servicio.DesafioServicio;
import com.Impulso.Alcambio.Servicio.ParticipacionDesafioServicio;
import com.Impulso.Alcambio.Servicio.UsuarioServicio;
import com.Impulso.Alcambio.Servicio.CacheServicio;

import lombok.extern.slf4j.Slf4j;

/**
 * Controlador para manejar toda la lógica de desafíos en la aplicación
 * Se encarga de gestionar la creación, asignación y seguimiento de desafíos,
 * así como la participación de los usuarios en ellos
 */
@RestController
@RequestMapping("/api/desafios")
@Slf4j
public class DesafioController {

    @Autowired
    private DesafioServicio desafioServicio;
    
    @Autowired
    private ParticipacionDesafioServicio participacionDesafioServicio;
    
    @Autowired
    private UsuarioServicio usuarioServicio;
    
    @Autowired
    private CacheServicio cacheServicio;
    
    
    /**
     * Verifica y obtiene el usuario autenticado mediante su correo
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
     * Verifica si un desafío existe
     * @param id Identificador del desafío a verificar
     * @return ResponseEntity con error si no existe, null si existe
     */
    private <T> ResponseEntity<T> verificarDesafioExiste(String id) {
        if (!desafioServicio.obtenerPorId(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        return null;
    }
    
    /**
     * Construye una respuesta para participación exitosa en desafío
     */
    private ResponseEntity<?> construirRespuestaParticipacion(ParticipacionDesafio participacion, Usuario usuario, Desafio desafio) {
        Map<String, Object> respuesta = new HashMap<>();
        
        // Calcular puntos totales usando el servicio de participación
        int puntosActuales = participacionDesafioServicio.calcularPuntosTotales(usuario.getId());
        
        respuesta.put("mensaje", "¡Desafío completado exitosamente!");
        respuesta.put("participacion", participacion);
        respuesta.put("puntosOtorgados", desafio.getPuntosRecompensa());
        respuesta.put("puntosActuales", puntosActuales);
        
        return ResponseEntity.ok(respuesta);
    }
    
    // =================== CONSULTA DE DESAFÍOS ===================
    
    /**
     * Endpoint principal - Lista todos los desafíos disponibles en el sistema
     * Lo usamos en la pantalla principal de desafíos y en el dashboard
     */
    @GetMapping("/paginados")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<Desafio>> obtenerDesafios(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(desafioServicio.obtenerTodosPaginados(pageable));
    }
    
    /**
     * Versión pública de los desafíos para mostrar en landing page
     * No requiere autenticación, es para atraer nuevos usuarios
     */
    @GetMapping("/publicos")
    public ResponseEntity<Page<Desafio>> obtenerDesafiosPublicos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Desafio> desafios = desafioServicio.obtenerTodosPaginados(pageable);
            return ResponseEntity
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(desafios);
        } catch (Exception e) {
            log.error("Error al obtener desafíos públicos", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Devuelve los detalles de un desafío específico al hacer clic en él
     * Incluye toda la información necesaria para mostrar la página del desafío
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Desafio> obtenerDesafioPorId(@PathVariable String id) {
        return desafioServicio.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Busca desafíos por palabra clave con paginación
     */
    @GetMapping("/buscar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<Desafio>> buscarDesafiosPorPalabraClave(
            @RequestParam String palabraClave,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(desafioServicio.buscarPorPalabraClavePaginado(palabraClave, pageable));
    }
    
    /**
     * Obtiene desafíos por proyecto con paginación
     */
    @GetMapping("/proyecto/{proyectoId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<Desafio>> obtenerDesafiosPorProyecto(
            @PathVariable String proyectoId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(desafioServicio.obtenerPorProyectoPaginado(proyectoId, pageable));
    }
    
    /**
     * Obtiene las participaciones de un desafío específico con paginación
     */
    @GetMapping("/{id}/participaciones")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Page<ParticipacionDesafio>> obtenerParticipacionesPorDesafio(
            @PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        // Verificar que el desafío existe
        ResponseEntity<Page<ParticipacionDesafio>> verificacion = verificarDesafioExiste(id);
        if (verificacion != null) {
            return verificacion;
        }
        
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(participacionDesafioServicio.obtenerPorDesafioPaginado(id, pageable));
    }
    
    // =================== OPERACIONES CRUD (SOLO ADMIN) ===================
    
    /**
     * Crea un nuevo desafío (solo administradores)
     */
    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Desafio> crearDesafio(@RequestBody Desafio desafio) {
        Desafio nuevoDesafio = desafioServicio.guardar(desafio);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoDesafio);
    }
    
    /**
     * Actualiza un desafío existente (solo administradores)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Desafio> actualizarDesafio(@PathVariable String id, @RequestBody Desafio desafio) {
        ResponseEntity<Desafio> verificacion = verificarDesafioExiste(id);
        if (verificacion != null) {
            return verificacion;
        }
        
        desafio.setId(id);
        Desafio desafioActualizado = desafioServicio.guardar(desafio);
        return ResponseEntity.ok(desafioActualizado);
    }
    
    /**
     * Elimina un desafío (solo administradores)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> eliminarDesafio(@PathVariable String id) {
        ResponseEntity<Void> verificacion = verificarDesafioExiste(id);
        if (verificacion != null) {
            return verificacion;
        }
        
        desafioServicio.eliminar(id);
        return ResponseEntity.noContent().build();
    }
    
    // =================== OPERACIONES DE PARTICIPACIÓN ===================
    
    /**
     * Registra la participación de un usuario en un desafío
     */
    @PostMapping("/{id}/participar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> participarEnDesafio(@PathVariable String id, Principal principal) {
        // Obtener usuario actual
        Optional<Usuario> usuarioOpt = obtenerUsuarioAutenticado(principal);
        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no encontrado");
        }
        
        // Obtener desafío
        Optional<Desafio> desafioOpt = desafioServicio.obtenerPorId(id);
        if (desafioOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Desafio desafio = desafioOpt.get();
        Usuario usuario = usuarioOpt.get();
        
        // Verificar si ya participa
        Optional<ParticipacionDesafio> participacionExistente = 
            participacionDesafioServicio.obtenerPorUsuarioYDesafio(usuario.getId(), desafio.getId());
        
        if (participacionExistente.isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body("Ya estás participando en este desafío");
        }
        
        // Crear nueva participación
        ParticipacionDesafio nuevaParticipacion = new ParticipacionDesafio(
            desafio.getId(),
            usuario.getId(),
            desafio.getNombre()
        );
        
        ParticipacionDesafio participacionGuardada = participacionDesafioServicio.guardar(nuevaParticipacion);
        return ResponseEntity.status(HttpStatus.CREATED).body(participacionGuardada);
    }
    
    /**
     * Valida y completa un desafío en el que un usuario está participando
     */
    @PostMapping("/{id}/completar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> completarDesafio(@PathVariable String id, Principal principal) {
        try {
            // Verificar si hay principal (usuario autenticado)
            if (principal == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "No hay usuario autenticado");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON).body(error);
            }
            
            // Obtener usuario actual
            Optional<Usuario> usuarioOpt = obtenerUsuarioAutenticado(principal);
            if (usuarioOpt.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Usuario no encontrado en la base de datos");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON).body(error);
            }
            
            Usuario usuario = usuarioOpt.get();
            
            // Verificar si el desafío existe
            Optional<Desafio> desafioOpt = desafioServicio.obtenerPorId(id);
            if (desafioOpt.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "El desafío no existe");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON).body(error);
            }
            
            // Verificar y completar el desafío
            try {
                ParticipacionDesafio participacionCompletada = 
                    participacionDesafioServicio.validarYCompletarDesafio(usuario.getId(), id);
                
                // Obtener el desafío para acceder a los puntos
                Desafio desafio = desafioOpt.get();
                
                // Construir respuesta
                Map<String, Object> respuesta = new HashMap<>();
                respuesta.put("mensaje", "¡Desafío completado exitosamente!");
                respuesta.put("participacion", participacionCompletada);
                respuesta.put("puntosOtorgados", desafio.getPuntosRecompensa());
                
                // Calcular puntos totales usando el servicio de participación
                int puntosActuales = participacionDesafioServicio.calcularPuntosTotales(usuario.getId());
                respuesta.put("puntosActuales", puntosActuales);
                
                return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(respuesta);
            } catch (RuntimeException e) {
                // Capturar excepciones de validación
                Map<String, String> error = new HashMap<>();
                error.put("error", e.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body(error);
            }
        } catch (Exception e) {
            // Cualquier otra excepción no controlada
            log.error("Error al completar desafío", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error interno del servidor: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).contentType(MediaType.APPLICATION_JSON).body(error);
        }
    }
    
    /**
     * Obtiene las participaciones del usuario actual en desafíos
     */
    @GetMapping("/mis-participaciones")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> obtenerMisParticipaciones(
            Principal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Optional<Usuario> usuarioOpt = obtenerUsuarioAutenticado(principal);
        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        String usuarioId = usuarioOpt.get().getId();
        
        // Obtener todas las participaciones del usuario
        List<ParticipacionDesafio> participaciones = 
            participacionDesafioServicio.obtenerPorUsuario(usuarioId);
        
        // Implementar paginación manual
        int start = page * size;
        int end = Math.min(start + size, participaciones.size());
        
        // Verificar que los índices sean válidos
        if (start > participaciones.size()) {
            Page<ParticipacionDesafio> emptyPage = new org.springframework.data.domain.PageImpl<>(
                List.of(), PageRequest.of(page, size), 0);
            return ResponseEntity.ok(emptyPage);
        }
        
        List<ParticipacionDesafio> participacionesPaginadas = participaciones.subList(start, end);
        Page<ParticipacionDesafio> pageResult = new org.springframework.data.domain.PageImpl<>(
            participacionesPaginadas, PageRequest.of(page, size), participaciones.size());
        
        return ResponseEntity.ok(pageResult);
    }
    
    /**
     * Obtiene el resumen de gamificación del usuario actual
     */
    @GetMapping("/gamificacion")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> obtenerResumenGameificacion(Principal principal) {
        Optional<Usuario> usuarioOpt = obtenerUsuarioAutenticado(principal);
        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        String usuarioId = usuarioOpt.get().getId();
        Map<String, Object> resumen = participacionDesafioServicio.obtenerResumenGameificacion(usuarioId);
        
        return ResponseEntity.ok(resumen);
    }
    
    /**
     * Obtiene todos los desafíos o filtra por proyecto o tipo
     */
    @GetMapping("/filtrados")
    public ResponseEntity<List<Desafio>> obtenerDesafios(
            @RequestParam(required = false) String proyectoId,
            @RequestParam(required = false) String tipo) {
        
        List<Desafio> desafios;
        
        if (proyectoId != null && !proyectoId.isEmpty()) {
            desafios = desafioServicio.obtenerPorProyecto(proyectoId);
        } else if (tipo != null && !tipo.isEmpty()) {
            try {
                Desafio.TipoDesafio tipoEnum = Desafio.TipoDesafio.valueOf(tipo.toUpperCase());
                desafios = desafioServicio.obtenerPorTipo(tipoEnum);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        } else {
            desafios = desafioServicio.obtenerTodos();
        }
        
        return ResponseEntity.ok(desafios);
    }
    
    /**
     * Obtiene los desafíos activos (fecha fin posterior a la actual)
     */
    @GetMapping("/activos")
    public ResponseEntity<?> obtenerActivos() {
        try {
            List<Desafio> desafiosActivos = desafioServicio.obtenerDesafiosActivos();
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(desafiosActivos);
        } catch (Exception e) {
            log.error("Error al obtener desafíos activos", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error al obtener desafíos activos: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(error);
        }
    }
    
    /**
     * Busca desafíos por palabra clave
     */
    @GetMapping("/texto")
    public ResponseEntity<List<Desafio>> buscar(@RequestParam String q) {
        if (q == null || q.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        return ResponseEntity.ok(desafioServicio.buscarPorPalabraClave(q));
    }
    
    /**
     * Obtiene los desafíos ordenados por puntos de recompensa
     */
    @GetMapping("/por-puntos")
    public ResponseEntity<List<Desafio>> obtenerPorPuntos() {
        return ResponseEntity.ok(desafioServicio.obtenerOrdenadosPorPuntos());
    }
    
    /**
     * Limpia todas las cachés relacionadas con desafíos en Redis
     */
    @PostMapping("/cache/limpiar")
    public ResponseEntity<Map<String, String>> limpiarCache() {
        log.info("Limpiando caché de desafíos");
        desafioServicio.limpiarCache();
        return ResponseEntity.ok(Map.of("mensaje", "Caché de desafíos limpiada correctamente"));
    }
    
    /**
     * Endpoint para verificar el estado de caché de un desafío específico
     */
    @GetMapping("/{id}/cache/status")
    public ResponseEntity<Map<String, Object>> verificarEstadoCache(@PathVariable String id) {
        log.info("Verificando estado de caché para desafío {}", id);
        
        // Verificar si el desafío existe primero
        String cacheKey = "desafio:" + id;
        boolean enCache = cacheServicio.existe(cacheKey);
        
        // Verificar si el desafío existe en base de datos
        Optional<Desafio> desafioOpt = desafioServicio.obtenerPorId(id);
        if (desafioOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        // Medir tiempo de respuesta para segunda llamada (que debería venir de caché)
        long startTime = System.nanoTime();
        desafioServicio.obtenerPorId(id);
        long endTime = System.nanoTime();
        
        // Calcular tiempo de respuesta en milisegundos
        double responseTime = (endTime - startTime) / 1_000_000.0;
        
        return ResponseEntity.ok(Map.of(
            "id", id,
            "enCache", enCache,
            "tiempoRespuesta", responseTime + " ms",
            "mensaje", enCache ? "El desafío está en caché" : "El desafío no está en caché y se ha cargado ahora"
        ));
    }
    
    /**
     * Obtiene estadísticas sobre el uso de caché en Redis
     */
    @GetMapping("/cache/estadisticas")
    public ResponseEntity<Map<String, Object>> obtenerEstadisticasCache() {
        log.info("Obteniendo estadísticas de caché para desafíos");
        
        // Contar las claves por categoría
        long totalDesafios = cacheServicio.contarClaves("desafio:*");
        long porProyecto = cacheServicio.contarClaves("desafios:proyecto:*");
        long porTipo = cacheServicio.contarClaves("desafios:tipo:*");
        long busquedas = cacheServicio.contarClaves("desafios:busqueda:*");
        
        return ResponseEntity.ok(Map.of(
            "desafiosIndividuales", totalDesafios,
            "porProyecto", porProyecto,
            "porTipo", porTipo,
            "busquedas", busquedas,
            "total", totalDesafios + porProyecto + porTipo + busquedas
        ));
    }
    
    /**
     * Precarga un desafío en caché
     */
    @PostMapping("/{id}/cache/precargar")
    public ResponseEntity<Map<String, String>> precargarEnCache(@PathVariable String id) {
        log.info("Precargando desafío {} en caché", id);
        
        Optional<Desafio> desafioOpt = desafioServicio.obtenerPorId(id);
        if (desafioOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        String cacheKey = "desafio:" + id;
        cacheServicio.guardarConExpiracion(cacheKey, desafioOpt.get(), 30, java.util.concurrent.TimeUnit.MINUTES);
        
        return ResponseEntity.ok(Map.of("mensaje", "Desafío precargado en caché correctamente"));
    }
} 