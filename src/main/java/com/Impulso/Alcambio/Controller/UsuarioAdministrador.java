package com.Impulso.Alcambio.Controller;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
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

import com.Impulso.Alcambio.Modelo.Usuario;
import com.Impulso.Alcambio.Modelo.Rol;
import com.Impulso.Alcambio.Servicio.UsuarioServicio;
import com.Impulso.Alcambio.Servicio.ProyectoServicio;
import com.Impulso.Alcambio.Servicio.ParticipacionDesafioServicio;

// Este controlador se encarga de toda la gestión de usuarios administradores
// Es una API separada que solo pueden usar los administradores de la plataforma
// para manejar cuentas y permisos

@RestController
@RequestMapping("/api/admin/usuarios")
@PreAuthorize("hasAuthority('ADMIN')")
public class UsuarioAdministrador {
    @Autowired
    private UsuarioServicio usuarioServicio;

    @Autowired
    private ProyectoServicio proyectoServicio;
    
    @Autowired
    private ParticipacionDesafioServicio participacionDesafioServicio;

    // Crea un nuevo usuario con permisos de administrador
    // Útil cuando necesitamos agregar nuevos administradores al sistema
    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Usuario> crearUsuarioAdministrador(@RequestBody Usuario usuario) {
        usuario.setRol(Rol.ADMIN);
        Usuario usuarioGuardado = usuarioServicio.actualizarUsuario(usuario);
        return ResponseEntity.ok(usuarioGuardado);
    }

    // Obtiene los datos de un administrador específico por su ID
    // Se usa en la página de detalles de administrador del panel de control
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Usuario> obtenerUsuarioAdministrador(@PathVariable String id) {
        return usuarioServicio.buscarPorId(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }   

    // Actualiza la información de un administrador existente
    // Permite cambiar todos los datos excepto bajar permisos
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Usuario> actualizarUsuarioAdministrador(@PathVariable String id, @RequestBody Usuario usuario) {
        usuario.setId(id); // Asegurar que el ID sea el correcto
        Usuario usuarioActualizado = usuarioServicio.actualizarUsuario(usuario);
        return ResponseEntity.ok(usuarioActualizado);
    }

    //ELIMINAR USUARIO ADMINISTRADOR
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
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
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<Usuario>> obtenerTodosUsuariosAdministradores() {
        List<Usuario> usuarios = usuarioServicio.buscarPorRol(Rol.ADMIN);
        return ResponseEntity.ok(usuarios);
    }

    //OBTENER TODOS LOS VOLUNTARIOS
    @GetMapping("/voluntarios")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<Usuario>> obtenerTodosVoluntarios() {
        List<Usuario> voluntarios = usuarioServicio.buscarPorRol(Rol.VOLUNTARIO);
        return ResponseEntity.ok(voluntarios);
    }

    //ELIMINAR USUARIOS VOLUNTARIOS
    @DeleteMapping("/voluntarios")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> eliminarUsuariosVoluntarios() {
        List<Usuario> voluntarios = usuarioServicio.buscarPorRol(Rol.VOLUNTARIO);
        for (Usuario voluntario : voluntarios) {
            usuarioServicio.eliminarUsuario(voluntario.getId());
        }
        return ResponseEntity.noContent().build();
    }

    // ESTADÍSTICAS DE USUARIOS (PARA DASHBOARD ADMIN)
    @GetMapping("/estadisticas")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<java.util.Map<String, Object>> obtenerEstadisticasUsuarios() {
        Map<String, Object> estadisticasUsuarios = usuarioServicio.obtenerEstadisticasUsuarios();
        Map<String, Object> estadisticasProyectos = proyectoServicio.obtenerEstadisticasProyectos();
        
        // Combinar estadísticas
        Map<String, Object> todasEstadisticas = new HashMap<>();
        todasEstadisticas.putAll(estadisticasUsuarios);
        todasEstadisticas.putAll(estadisticasProyectos);
        
        // Calcular estadísticas de empresas
        List<Usuario> usuarios = usuarioServicio.obtenerTodos();
        Set<String> empresasUnicas = usuarios.stream()
            .filter(u -> u.getEmpresa() != null && !u.getEmpresa().isEmpty())
            .map(Usuario::getEmpresa)
            .collect(Collectors.toSet());
        
        // Calcular desafíos completados reales
        long desafiosCompletados = usuarios.stream()
            .mapToLong(u -> usuarioServicio.calcularDesafiosCompletados(u.getId()))
            .sum();
        
        todasEstadisticas.put("totalEmpresas", empresasUnicas.size());
        todasEstadisticas.put("empresasParticipantes", empresasUnicas.size());
        todasEstadisticas.put("desafiosCompletados", desafiosCompletados);
        
        // Calcular tendencias mensuales
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime mesAnterior = ahora.minusMonths(1);
        
        // Calcular tendencia de usuarios
        long usuariosActuales = (long) todasEstadisticas.get("totalUsuarios");
        long usuariosMesAnterior = usuarios.stream()
            .filter(u -> u.getFechaRegistro() != null && u.getFechaRegistro().isBefore(mesAnterior))
            .count();
        double tendenciaUsuarios = usuariosMesAnterior > 0 ? 
            ((double)(usuariosActuales - usuariosMesAnterior) / usuariosMesAnterior) * 100 : 0;
        
        // Calcular tendencia de proyectos
        long proyectosActivos = (long) todasEstadisticas.get("proyectosActivos");
        Map<String, Long> proyectosPorMes = (Map<String, Long>) todasEstadisticas.get("proyectosPorMes");
        String mesAnteriorKey = mesAnterior.getYear() + "-" + String.format("%02d", mesAnterior.getMonthValue());
        long proyectosMesAnterior = proyectosPorMes != null ? proyectosPorMes.getOrDefault(mesAnteriorKey, 0L) : 0L;
        double tendenciaProyectos = proyectosMesAnterior > 0 ? 
            ((double)(proyectosActivos - proyectosMesAnterior) / proyectosMesAnterior) * 100 : 0;
        
        todasEstadisticas.put("tendenciaUsuarios", tendenciaUsuarios);
        todasEstadisticas.put("tendenciaProyectos", tendenciaProyectos);
        
        return ResponseEntity.ok(todasEstadisticas);
    }

    // ESTADÍSTICAS AVANZADAS (PAGINADAS) PARA DASHBOARD ADMIN
    @GetMapping("/estadisticas/detalladas")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<java.util.Map<String, Object>> obtenerEstadisticasDetalladas(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(usuarioServicio.obtenerEstadisticasDetalladas(page, size));
    }
    
    // ESTADÍSTICAS POR EMPRESA
    @GetMapping("/estadisticas/empresa/{empresa}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<java.util.Map<String, Object>> obtenerEstadisticasPorEmpresa(
            @PathVariable String empresa) {
        return ResponseEntity.ok(usuarioServicio.obtenerEstadisticasPorEmpresa(empresa));
    }
    
    // OBTENER RESUMEN DE GAMIFICACIÓN PARA UN USUARIO
    @GetMapping("/{id}/gamificacion")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Map<String, Object>> obtenerResumenGameficacionUsuario(
            @PathVariable String id) {
        if (!usuarioServicio.buscarPorId(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> resumen = participacionDesafioServicio.obtenerResumenGameificacion(id);
        return ResponseEntity.ok(resumen);
    }
}
