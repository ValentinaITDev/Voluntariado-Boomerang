package com.Impulso.Alcambio.Controller;

import java.security.Principal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.Impulso.Alcambio.Modelo.Foro;
import com.Impulso.Alcambio.Modelo.Usuario;
import com.Impulso.Alcambio.Servicio.ForoServicio;
import com.Impulso.Alcambio.Servicio.UsuarioServicio;

/**
 * Controlador para manejar reportes de comentarios en foros
 */
@RestController
@RequestMapping("/api/reportes")
public class ReporteController {
    
    @Autowired
    private UsuarioServicio usuarioServicio;
    
    @Autowired
    private ForoServicio foroServicio;
    
    /**
     * Endpoint para reportar un comentario como inapropiado
     */
    @PostMapping("/foros/{foroId}/comentarios/{comentarioId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> reportarComentario(
            @PathVariable String foroId,
            @PathVariable String comentarioId,
            @RequestBody String motivo,
            Principal principal) {
        
        // Verificar usuario autenticado
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        Optional<Usuario> usuarioOpt = usuarioServicio.buscarPorCorreo(principal.getName());
        
        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuario no encontrado", 
                                  "mensaje", "No se pudo identificar al usuario"));
        }
        
        Usuario usuario = usuarioOpt.get();
        
        try {
            // Verificar que el foro existe
            Optional<Foro> foroOpt = foroServicio.obtenerPorId(foroId);
            
            if (foroOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Foro no encontrado", 
                                     "mensaje", "El foro solicitado no existe"));
            }
            
            // Intentar reportar el comentario
            Optional<Foro> foroActualizado = foroServicio.reportarComentario(
                    foroId, 
                    comentarioId, 
                    usuario.getId(), 
                    motivo);
            
            if (foroActualizado.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Comentario no encontrado", 
                                     "mensaje", "El comentario que intenta reportar no existe"));
            }
            
            // Construir respuesta de éxito
            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("mensaje", "El comentario ha sido reportado y será revisado por un administrador");
            respuesta.put("fechaReporte", new Date());
            respuesta.put("estado", "REPORTADO");
            respuesta.put("foroId", foroId);
            respuesta.put("comentarioId", comentarioId);
            
            return ResponseEntity.ok(respuesta);
            
        } catch (Exception e) {
            System.err.println("Error al reportar comentario: " + e.getMessage());
            e.printStackTrace();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al procesar el reporte", 
                                 "mensaje", e.getMessage()));
        }
    }
} 