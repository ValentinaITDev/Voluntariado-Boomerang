package com.Impulso.Alcambio.Controller;

import java.security.Principal;
import java.util.Optional;
import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.Impulso.Alcambio.Modelo.Usuario;
import com.Impulso.Alcambio.Modelo.Rol;
import com.Impulso.Alcambio.Modelo.Proyecto;
import com.Impulso.Alcambio.Modelo.Proyecto.EstadoProyecto;
import com.Impulso.Alcambio.Servicio.UsuarioServicio;
import com.Impulso.Alcambio.Servicio.ProyectoServicio;

@Controller
public class WebController {

    @Autowired
    private UsuarioServicio usuarioServicio;
    @Autowired
    private ProyectoServicio proyectoServicio;
    
    // Página de inicio
    @GetMapping("/")
    public String inicio() {
        return "Inicio";
    }
    
    // Página de inicio de sesión
    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error,
                        @RequestParam(value = "logout", required = false) String logout,
                        @RequestParam(value = "registroExitoso", required = false) String registroExitoso,
                        Model model) {
        if (error != null) {
            model.addAttribute("errorMsg", "Credenciales inválidas");
        }
        
        if (logout != null) {
            model.addAttribute("logoutMsg", "Has cerrado sesión correctamente");
        }
        
        if (registroExitoso != null) {
            model.addAttribute("registroExitosoMsg", "¡Registro exitoso! Ahora puedes iniciar sesión");
        }
        
        return "Login";
    }
    
    // Página de registro
    @GetMapping("/registro")
    public String registroForm(Model model) {
        model.addAttribute("usuario", new Usuario());
        return "Registro";
    }
    
    // Procesar formulario de registro
    @PostMapping("/registro")
    public String procesarRegistro(@ModelAttribute Usuario usuario, Model model) {
        try {
            usuarioServicio.registrarUsuario(usuario);
            return "redirect:/login?registroExitoso=true";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "Registro";
        }
    }
    
    // Dashboard unificado para todos los usuarios (administradores y voluntarios)
    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }
        
        // Obtener información del usuario autenticado
        Optional<Usuario> usuarioOpt = usuarioServicio.buscarPorCorreo(principal.getName());
        
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            
            // Añadir datos básicos del usuario al modelo
            model.addAttribute("usuario", usuario);
            model.addAttribute("nombre", usuario.getNombre());
            
            // Obtener estadísticas comunes para el dashboard
            long proyectosActivos = usuario.getProyectosActivos().stream()
                    .filter(p -> p.getEstado() == EstadoProyecto.ACTIVO)
                    .count();
            
            long desafiosCompletados = usuario.getDesafios().stream()
                    .filter(d -> d.isCompletado())
                    .count();
            
            int puntosTotal = usuario.getMecanicaJuego().getPuntos();
            int insigniasTotal = usuario.getMecanicaJuego().getInsignias().size();
            
            // Añadir estadísticas al modelo
            model.addAttribute("proyectosActivos", proyectosActivos);
            model.addAttribute("desafiosCompletados", desafiosCompletados);
            model.addAttribute("puntosTotal", puntosTotal);
            model.addAttribute("insigniasTotal", insigniasTotal);
            
            // Filtrar proyectos para mostrar solo los activos
            model.addAttribute("proyectosActuales", usuario.getProyectosActivos().stream()
                    .filter(p -> p.getEstado() == EstadoProyecto.ACTIVO)
                    .toList());
            
            // Determinar qué vista mostrar según el rol del usuario
            if (usuario.getRol() == Rol.ADMIN) {
                // Datos específicos para administradores si es necesario
                
                return "DashboardAdmin";
            } else {
                // Para usuarios voluntarios
                return "InicioUsuario";
            }
        } else {
            return "redirect:/login";
        }
    }

    //ADMINISTRAR PROYECTOS
    @GetMapping("/ProyectosAdmin")
    public String administrarProyectos(Model model) {
        model.addAttribute("proyectos", proyectoServicio.obtenerTodos());
        return "ProyectosAdmin";
    }
    
    //CREAR PROYECTO
    @PostMapping("/proyectos/crear")
    public String crearProyecto(@ModelAttribute Proyecto proyecto, 
                               @RequestParam("imagenProyecto") MultipartFile archivo) {
        // Guardar la imagen en el sistema de archivos
        if (!archivo.isEmpty()) {
            try {
                // Crear directorio si no existe
                String rutaDirectorio = "src/main/resources/Proyectos";
                File directorio = new File(rutaDirectorio);
                if (!directorio.exists()) {
                    directorio.mkdirs();
                }
                
                // Generar un nombre único para el archivo
                String nombreArchivo = UUID.randomUUID().toString() + "_" + archivo.getOriginalFilename();
                Path rutaCompleta = Path.of(rutaDirectorio, nombreArchivo);
                
                // Guardar el archivo
                Files.copy(archivo.getInputStream(), rutaCompleta, StandardCopyOption.REPLACE_EXISTING);
                
                // Guardar la URL relativa en el objeto proyecto
                proyecto.setImagenUrl("/Proyectos/" + nombreArchivo);
                
            } catch (IOException e) {
                e.printStackTrace();
                // Manejar el error
            }
        }
        
        // Guardar el proyecto en la base de datos
        proyectoServicio.crearProyecto(proyecto);
        
        return "redirect:/ProyectosAdmin";
    }
    
    //ACTUALIZAR PROYECTO
    @GetMapping("/proyectos/{id}/editar")
    public String editarProyecto(@PathVariable String id, Model model) {
        model.addAttribute("proyecto", proyectoServicio.obtenerProyectoPorId(id).orElse(null));
        return "EditarProyecto";
    }
    
    @PostMapping("/proyectos/{id}/actualizar")
    public String actualizarProyecto(@PathVariable String id, 
                                    @ModelAttribute Proyecto proyecto,
                                    @RequestParam(value = "imagenProyecto", required = false) MultipartFile archivo) {
        
        // Obtener el proyecto existente
        Optional<Proyecto> proyectoExistente = proyectoServicio.obtenerProyectoPorId(id);
        
        if (proyectoExistente.isPresent()) {
            Proyecto proyectoActual = proyectoExistente.get();
            
            // Actualizar los campos
            proyectoActual.setNombre(proyecto.getNombre());
            proyectoActual.setDescripcion(proyecto.getDescripcion());
            proyectoActual.setEstado(proyecto.getEstado());
            
            // Actualizar la imagen si se subió una nueva
            if (archivo != null && !archivo.isEmpty()) {
                try {
                    String rutaDirectorio = "src/main/resources/Proyectos";
                    
                    // Eliminar la imagen anterior si existe
                    if (proyectoActual.getImagenUrl() != null && !proyectoActual.getImagenUrl().isEmpty()) {
                        String nombreImagenAnterior = proyectoActual.getImagenUrl().substring(proyectoActual.getImagenUrl().lastIndexOf("/") + 1);
                        Path rutaImagenAnterior = Path.of(rutaDirectorio, nombreImagenAnterior);
                        Files.deleteIfExists(rutaImagenAnterior);
                    }
                    
                    // Guardar la nueva imagen
                    String nombreArchivo = UUID.randomUUID().toString() + "_" + archivo.getOriginalFilename();
                    Path rutaCompleta = Path.of(rutaDirectorio, nombreArchivo);
                    
                    Files.copy(archivo.getInputStream(), rutaCompleta, StandardCopyOption.REPLACE_EXISTING);
                    
                    proyectoActual.setImagenUrl("/Proyectos/" + nombreArchivo);
                    
                } catch (IOException e) {
                    e.printStackTrace();
                    // Manejar el error
                }
            }
            
            // Guardar el proyecto actualizado
            proyectoServicio.actualizarProyecto(proyectoActual);
        }
        
        return "redirect:/ProyectosAdmin";
    }
    
    //ELIMINAR PROYECTO
    @GetMapping("/proyectos/{id}/eliminar")
    public String eliminarProyecto(@PathVariable String id) {
        // Obtener el proyecto
        Optional<Proyecto> proyecto = proyectoServicio.obtenerProyectoPorId(id);
        
        if (proyecto.isPresent()) {
            // Eliminar la imagen asociada si existe
            String imagenUrl = proyecto.get().getImagenUrl();
            if (imagenUrl != null && !imagenUrl.isEmpty()) {
                try {
                    String rutaDirectorio = "src/main/resources/Proyectos";
                    String nombreImagen = imagenUrl.substring(imagenUrl.lastIndexOf("/") + 1);
                    Path rutaImagen = Path.of(rutaDirectorio, nombreImagen);
                    Files.deleteIfExists(rutaImagen);
                } catch (IOException e) {
                    e.printStackTrace();
                    // Manejar el error
                }
            }
            
            // Eliminar el proyecto de la base de datos
            proyectoServicio.eliminarProyecto(id);
        }
        
        return "redirect:/ProyectosAdmin";
    }
    
    //BUSCAR PROYECTOS
    @GetMapping("/proyectos/buscar")
    public String buscarProyectos(@RequestParam String nombre, Model model) {
        model.addAttribute("proyectos", proyectoServicio.buscarPorNombre(nombre));
        return "ProyectosAdmin";
    }

    // Perfil del usuario
    @GetMapping("/perfil")
    public String perfil(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }
        
        Optional<Usuario> usuarioOpt = usuarioServicio.buscarPorCorreo(principal.getName());
        
        if (usuarioOpt.isPresent()) {
            model.addAttribute("usuario", usuarioOpt.get());
            return "Perfil";
        } else {
            return "redirect:/login";
        }
    }
    
    // Página de proyectos del usuario
    @GetMapping("/mis-proyectos")
    public String misProyectos(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }
        
        Optional<Usuario> usuarioOpt = usuarioServicio.buscarPorCorreo(principal.getName());
        
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            model.addAttribute("usuario", usuario);
            model.addAttribute("proyectosActivos", usuario.getProyectosActivos().stream()
                    .filter(p -> p.getEstado() == EstadoProyecto.ACTIVO)
                    .toList());
            model.addAttribute("proyectosExpirados", usuario.getProyectosActivos().stream()
                    .filter(p -> p.getEstado() == EstadoProyecto.EXPIRADO)
                    .toList());
            
            return "MisProyectos";
        } else {
            return "redirect:/login";
        }
    }
    
    // Página de desafíos del usuario
    @GetMapping("/mis-desafios")
    public String misDesafios(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }
        
        Optional<Usuario> usuarioOpt = usuarioServicio.buscarPorCorreo(principal.getName());
        
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            model.addAttribute("usuario", usuario);
            model.addAttribute("desafiosActivos", usuario.getDesafios().stream()
                    .filter(d -> !d.isCompletado())
                    .toList());
            model.addAttribute("desafiosCompletados", usuario.getDesafios().stream()
                    .filter(d -> d.isCompletado())
                    .toList());
            
            return "MisDesafios";
        } else {
            return "redirect:/login";
        }
    }
}
   