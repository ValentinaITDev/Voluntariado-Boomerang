package com.Impulso.Alcambio.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.Impulso.Alcambio.Modelo.Usuario;
import com.Impulso.Alcambio.Modelo.Proyecto;
import com.Impulso.Alcambio.Modelo.Rol;
import com.Impulso.Alcambio.Servicio.ProyectoServicio;
import com.Impulso.Alcambio.Servicio.UsuarioServicio;

@RestController
@RequestMapping("/api/admin/usuarios")
public class UsuarioAdministrador {
    @Autowired
    private UsuarioServicio usuarioServicio;
    @Autowired
    private ProyectoServicio proyectoServicio;

    //CREAR USUARIO ADMINISTRADOR
    @PostMapping
    public ResponseEntity<Usuario> crearUsuarioAdministrador(@RequestBody Usuario usuario) {
        usuario.setRol(Rol.ADMIN);
        Usuario usuarioGuardado = usuarioServicio.actualizarUsuario(usuario);
        return ResponseEntity.ok(usuarioGuardado);
    }

    //OBTENER USUARIO ADMINISTRADOR POR ID
    @GetMapping("/{id}")
    public ResponseEntity<Usuario> obtenerUsuarioAdministrador(@PathVariable String id) {
        return usuarioServicio.buscarPorId(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }   

    //ACTUALIZAR USUARIO ADMINISTRADOR
    @PutMapping("/{id}")
    public ResponseEntity<Usuario> actualizarUsuarioAdministrador(@PathVariable String id, @RequestBody Usuario usuario) {
        usuario.setId(id); // Asegurar que el ID sea el correcto
        Usuario usuarioActualizado = usuarioServicio.actualizarUsuario(usuario);
        return ResponseEntity.ok(usuarioActualizado);
    }

    //ELIMINAR USUARIO ADMINISTRADOR
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarUsuarioAdministrador(@PathVariable String id) {
        boolean eliminado = usuarioServicio.eliminarUsuario(id);
        if (eliminado) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    //OBTENER TODOS LOS USUARIOS ADMINISTRADORES
    @GetMapping
    public ResponseEntity<List<Usuario>> obtenerTodosUsuariosAdministradores() {
        List<Usuario> usuarios = usuarioServicio.buscarPorRol(Rol.ADMIN);
        return ResponseEntity.ok(usuarios);
    }

    //OBTENER TODOS LOS VOLUNTARIOS
    @GetMapping("/voluntarios")
    public ResponseEntity<List<Usuario>> obtenerTodosVoluntarios() {
        List<Usuario> voluntarios = usuarioServicio.buscarPorRol(Rol.VOLUNTARIO);
        return ResponseEntity.ok(voluntarios);
    }

    //ELIMINAR USUARIOS VOLUNTARIOS
    @DeleteMapping("/voluntarios")
    public ResponseEntity<Void> eliminarUsuariosVoluntarios() {
        List<Usuario> voluntarios = usuarioServicio.buscarPorRol(Rol.VOLUNTARIO);
        for (Usuario voluntario : voluntarios) {
            usuarioServicio.eliminarUsuario(voluntario.getId());
        }
        return ResponseEntity.noContent().build();
    }
    //CREAR PROYECTO ADMINISTRADOR
    @PostMapping("/proyectos")
    public ResponseEntity<Proyecto> crearProyectoAdministrador(@RequestBody Proyecto proyecto) {
        Proyecto proyectoGuardado = proyectoServicio.crearProyecto(proyecto);
        return ResponseEntity.ok(proyectoGuardado);
    }

    //ACTUALIZAR PROYECTO ADMINISTRADOR
    @PutMapping("/proyectos/{id}")
    public ResponseEntity<Proyecto> actualizarProyectoAdministrador(@PathVariable String id, @RequestBody Proyecto proyecto) {
        proyecto.setId(id);
        Proyecto proyectoActualizado = proyectoServicio.actualizarProyecto(proyecto);
        return ResponseEntity.ok(proyectoActualizado);
    }

    //ELIMINAR PROYECTO ADMINISTRADOR
    @DeleteMapping("/proyectos/{id}")
    public ResponseEntity<Void> eliminarProyectoAdministrador(@PathVariable String id) {
        proyectoServicio.eliminarProyecto(id);
        return ResponseEntity.noContent().build();
    }   

    //OBTENER TODOS LOS PROYECTOS ADMINISTRADORES
    @GetMapping("/proyectos")
    public ResponseEntity<List<Proyecto>> obtenerTodosProyectosAdministradores() {
        List<Proyecto> proyectos = proyectoServicio.obtenerTodos();
        return ResponseEntity.ok(proyectos);
        }
}
