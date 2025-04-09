package com.Impulso.Alcambio.Controller;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.Impulso.Alcambio.Modelo.Usuario;
import com.Impulso.Alcambio.Modelo.Rol;
import com.Impulso.Alcambio.Modelo.Proyecto.EstadoProyecto;
import com.Impulso.Alcambio.Modelo.Usuario.ProyectoResumen;
import com.Impulso.Alcambio.Modelo.Usuario.DesafioUsuario;
import com.Impulso.Alcambio.Servicio.UsuarioServicio;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {
    @Autowired
    private UsuarioServicio usuarioServicio;

    // CREAR USUARIO
    @PostMapping
    public ResponseEntity<?> crearUsuario(@RequestBody Usuario usuario) {
        try {
            Usuario usuarioGuardado = usuarioServicio.registrarUsuario(usuario);
            return ResponseEntity.status(HttpStatus.CREATED).body(usuarioGuardado);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    // OBTENER USUARIO POR ID
    @GetMapping("/{id}")
    public ResponseEntity<Usuario> obtenerUsuario(@PathVariable String id) {
        return usuarioServicio.buscarPorId(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    // OBTENER TODOS LOS USUARIOS
    @GetMapping
    public ResponseEntity<List<Usuario>> listarUsuarios() {
        List<Usuario> usuarios = usuarioServicio.obtenerTodos();
        return ResponseEntity.ok(usuarios);
    }

    // ACTUALIZAR USUARIO
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarUsuario(@PathVariable String id, @RequestBody Usuario usuarioNuevo) {
        return usuarioServicio.buscarPorId(id)
            .map(usuarioExistente -> {
                // Actualizar solo campos permitidos
                usuarioExistente.setNombre(usuarioNuevo.getNombre());
                usuarioExistente.setNumero(usuarioNuevo.getNumero());
                usuarioExistente.setEmpresa(usuarioNuevo.getEmpresa());
                usuarioExistente.setImagenPerfil(usuarioNuevo.getImagenPerfil());
                
                // La contraseña debe ser encriptada por el servicio
                if (usuarioNuevo.getPassword() != null && !usuarioNuevo.getPassword().isEmpty()) {
                    usuarioExistente.setPassword(usuarioNuevo.getPassword());
                }
                
                // Guardar a través del servicio (que manejará la encriptación)
                Usuario actualizado = usuarioServicio.actualizarUsuario(usuarioExistente);
                return ResponseEntity.ok(actualizado);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    // ELIMINAR USUARIO
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarUsuario(@PathVariable String id) {
        boolean eliminado = usuarioServicio.eliminarUsuario(id);
        if (eliminado) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    // BUSCAR POR CORREO
    @GetMapping("/buscar")
    public ResponseEntity<Usuario> buscarPorCorreo(@RequestParam String correo) {
        return usuarioServicio.buscarPorCorreo(correo)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    // BUSCAR POR EMPRESA
    @GetMapping("/empresa")
    public ResponseEntity<List<Usuario>> buscarPorEmpresa(@RequestParam String empresa) {
        List<Usuario> usuarios = usuarioServicio.buscarPorEmpresa(empresa);
        return ResponseEntity.ok(usuarios);
    }
    
    // AGREGAR PROYECTO A USUARIO
    @PostMapping("/{id}/proyectos")
    public ResponseEntity<?> agregarProyecto(@PathVariable String id, @RequestBody ProyectoResumen proyecto) {
        Optional<Usuario> usuarioOpt = usuarioServicio.buscarPorId(id);
        
        if (!usuarioOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        
        Usuario usuario = usuarioOpt.get();
        usuario.agregarProyecto(proyecto);
        usuarioServicio.actualizarUsuario(usuario);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(usuario);
    }
    
    // AGREGAR DESAFÍO A USUARIO
    @PostMapping("/{id}/desafios")
    public ResponseEntity<?> agregarDesafio(@PathVariable String id, @RequestBody DesafioUsuario desafio) {
        Optional<Usuario> usuarioOpt = usuarioServicio.buscarPorId(id);
        
        if (!usuarioOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        
        Usuario usuario = usuarioOpt.get();
        usuario.agregarDesafio(desafio);
        usuarioServicio.actualizarUsuario(usuario);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(usuario);
    }
    
    // ACTUALIZAR PROGRESO DE DESAFÍO
    @PutMapping("/{id}/desafios/{desafioId}/progreso")
    public ResponseEntity<?> actualizarProgresoDesafio(
            @PathVariable String id, 
            @PathVariable String desafioId,
            @RequestParam int progreso) {
        
        Optional<Usuario> usuarioOpt = usuarioServicio.buscarPorId(id);
        
        if (!usuarioOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        
        Usuario usuario = usuarioOpt.get();
        usuario.actualizarProgreso(desafioId, progreso);
        usuarioServicio.actualizarUsuario(usuario);
        
        return ResponseEntity.ok().build();
    }
    
    // OBTENER LOS PROYECTOS ACTIVOS Y EXPIRADOS DE UN USUARIO
    @GetMapping("/{id}/proyectos")
    public ResponseEntity<List<ProyectoResumen>> obtenerProyectos(@PathVariable String id) {
        return usuarioServicio.buscarPorId(id)
            .map(usuario -> ResponseEntity.ok(usuario.getProyectosActivos().stream()
                .filter(proyecto -> proyecto.getEstado() == EstadoProyecto.ACTIVO || 
                       proyecto.getEstado() == EstadoProyecto.EXPIRADO) 
                .collect(Collectors.toList())))
            .orElse(ResponseEntity.notFound().build());
    }
}