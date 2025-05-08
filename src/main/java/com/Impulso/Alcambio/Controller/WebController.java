package com.Impulso.Alcambio.Controller;

import java.security.Principal;
import java.util.Optional;
import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
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
import com.Impulso.Alcambio.Modelo.ParticipacionDesafio;
import com.Impulso.Alcambio.Modelo.Proyecto.EstadoProyecto;
import com.Impulso.Alcambio.Servicio.UsuarioServicio;
import com.Impulso.Alcambio.Servicio.DesafioServicio;
import com.Impulso.Alcambio.Servicio.ParticipacionDesafioServicio;
import com.Impulso.Alcambio.Servicio.ProyectoServicio;
import com.Impulso.Alcambio.Modelo.Foro;
import com.Impulso.Alcambio.Servicio.ForoServicio;
import com.Impulso.Alcambio.Servicio.EstadisticasServicio;
import com.Impulso.Alcambio.Modelo.Desafio;

@Controller
public class WebController {

    private static final Logger log = LoggerFactory.getLogger(WebController.class);

    @Autowired
    private UsuarioServicio usuarioServicio;
    @Autowired
    private ProyectoServicio proyectoServicio;
    @Autowired
    private DesafioServicio desafioServicio;
    @Autowired
    private ParticipacionDesafioServicio participacionDesafioServicio;
    @Autowired
    private ForoServicio foroServicio;
    @Autowired
    private EstadisticasServicio estadisticasServicio;
    
    // Página de inicio
    @GetMapping("/")
    public String inicio(Model model) {
        try {
            Map<String, Object> estadisticas = estadisticasServicio.obtenerResumenDashboard();
            
            // Extraer solo los datos relevantes para la página de inicio pública
            model.addAttribute("totalVoluntarios", estadisticas.getOrDefault("totalVoluntarios", 0L));
            model.addAttribute("proyectosActivos", estadisticas.getOrDefault("proyectosActivos", 0L));
            model.addAttribute("desafiosCompletados", estadisticas.getOrDefault("desafiosCompletados", 0L));
            model.addAttribute("empresasParticipantes", estadisticas.getOrDefault("empresasParticipantes", 0L));
            
        } catch (Exception e) {
            // En caso de error, establecer valores por defecto
            model.addAttribute("totalVoluntarios", 0L);
            model.addAttribute("proyectosActivos", 0L);
            model.addAttribute("desafiosCompletados", 0L);
            model.addAttribute("empresasParticipantes", 0L);
        }
        
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
    
    //DASHBOARD UNIFICADO PARA TODOS LOS USUARIOS(ADMINISTRADORES y VOLUNTARIOS)
    @GetMapping("/dashboard")
    @PreAuthorize("isAuthenticated()")
    public String dashboard(Principal principal, Model model) {
        try {
            log.info("Iniciando carga de dashboard");
            
            if (principal == null) {
                log.warn("Principal es nulo, redirigiendo a login");
                return "redirect:/login";
            }
            
            log.info("Buscando usuario con correo: {}", principal.getName());
            Optional<Usuario> usuarioOpt = usuarioServicio.buscarPorCorreo(principal.getName());
            
            if (usuarioOpt.isPresent()) {
                Usuario usuario = usuarioOpt.get();
                
                model.addAttribute("usuario", usuario);
                model.addAttribute("nombre", usuario.getNombre());
                log.info("Usuario encontrado: {}, Rol: {}", usuario.getNombre(), usuario.getRol());
                
                if (usuario.getRol() == Rol.ADMIN) {
                    try {
                        log.info("Cargando dashboard para admin");
                        // Estadísticas para administradores
                        java.util.Map<String, Object> estadisticas = new HashMap<>();
                        try {
                            estadisticas = usuarioServicio.obtenerEstadisticasUsuarios();
                            log.info("Estadísticas cargadas: {}", estadisticas.keySet());
                        } catch (Exception e) {
                            log.error("Error al obtener estadísticas de usuarios: ", e);
                            estadisticas = new HashMap<>();
                            estadisticas.put("totalUsuarios", 0L);
                            estadisticas.put("totalVoluntarios", 0L);
                            estadisticas.put("totalAdministradores", 0L);
                        }
                        model.addAttribute("estadisticas", estadisticas);
                        
                        // Número de proyectos activos
                        long proyectosActivos = 0;
                        try {
                            proyectosActivos = proyectoServicio.contarProyectosPorEstado(EstadoProyecto.ACTIVO);
                            log.info("Proyectos activos: {}", proyectosActivos);
                        } catch (Exception e) {
                            log.error("Error al contar proyectos activos: ", e);
                        }
                        model.addAttribute("proyectosActivos", proyectosActivos);
                        
                        // Número de foros activos
                        long forosActivos = 0;
                        try {
                            forosActivos = foroServicio.obtenerTodos().size();
                            log.info("Foros activos: {}", forosActivos);
                        } catch (Exception e) {
                            log.error("Error al obtener foros activos: ", e);
                        }
                        model.addAttribute("forosActivos", forosActivos);
                        
                        log.info("Renderizando plantilla DashboardAdmin");
                        return "DashboardAdmin";
                    } catch (Exception e) {
                        log.error("Error al cargar el dashboard admin: ", e);
                        // Añadir valores predeterminados en caso de error
                        model.addAttribute("estadisticas", new HashMap<String, Object>());
                        model.addAttribute("proyectosActivos", 0L);
                        model.addAttribute("forosActivos", 0L);
                        model.addAttribute("error", "Error al cargar estadísticas: " + e.getMessage());
                        return "DashboardAdmin";
                    }
                } else if (usuario.getRol() == Rol.VOLUNTARIO) {
                    try {
                        // Estadísticas para voluntarios
                        
                        // Obtener estadísticas comunes para el dashboard
                        long proyectosActivosCount = 0;
                        // Obtener todos los IDs de proyectos en los que participa el usuario
                        List<String> proyectoIdsParticipados = usuario.getProyectosParticipadosIds();
                        // Obtener todos los proyectos existentes una vez
                        List<Proyecto> todosLosProyectos = proyectoServicio.obtenerTodos(); 
                        // Filtrar los proyectos participados completos
                        List<Proyecto> proyectosParticipadosCompletos = todosLosProyectos.stream()
                                .filter(p -> proyectoIdsParticipados.contains(p.getId()))
                                .toList();

                        if (proyectosParticipadosCompletos != null) {
                            proyectosActivosCount = proyectosParticipadosCompletos.stream()
                                .filter(p -> p.getEstado() != null && p.getEstado() == EstadoProyecto.ACTIVO)
                                .count();
                        }
                        
                        // Calcular desafíos completados usando el método en UsuarioServicio
                        long desafiosCompletados = usuarioServicio.calcularDesafiosCompletados(usuario.getId());
                        
                        int puntosTotal = participacionDesafioServicio.calcularPuntosTotales(usuario.getId());
                        List<String> insignias = participacionDesafioServicio.obtenerInsigniasUsuario(usuario.getId());
                        int insigniasTotal = insignias != null ? insignias.size() : 0;
                        
                        model.addAttribute("proyectosActivos", proyectosActivosCount);
                        model.addAttribute("desafiosCompletados", desafiosCompletados);
                        model.addAttribute("puntosTotal", puntosTotal);
                        model.addAttribute("insigniasTotal", insigniasTotal);
                        
                        // Filtrar proyectos para mostrar solo los activos
                        if (proyectosParticipadosCompletos != null) {
                            model.addAttribute("proyectosActuales", proyectosParticipadosCompletos.stream()
                                .filter(p -> p.getEstado() != null && p.getEstado() == EstadoProyecto.ACTIVO)
                                .toList());
                        } else {
                            model.addAttribute("proyectosActuales", List.of());
                        }
                        
                        // Obtener participaciones y simplificar para usar solo el nombre
                        List<ParticipacionDesafio> participaciones = participacionDesafioServicio.obtenerPorUsuario(usuario.getId());
                        
                        // Simplificar el modelo para la vista
                        model.addAttribute("participacionesDesafio", participaciones != null ? 
                            participaciones.stream().map(p -> {
                                // Crear un mapa con la información mínima necesaria para la vista
                                var infoDesafio = new java.util.HashMap<String, Object>();
                                infoDesafio.put("id", p.getId());
                                infoDesafio.put("nombre", p.getNombre());
                                infoDesafio.put("completado", p.isCompletado());
                                infoDesafio.put("progreso", p.getProgreso());
                                return infoDesafio;
                            }).toList() : 
                            List.of());
                        
                        return "InicioUsuario";
                    } catch (Exception e) {
                        log.error("Error al cargar el dashboard de voluntario: ", e);
                        // Proporcionar valores por defecto en caso de error
                        model.addAttribute("proyectosActivos", 0L);
                        model.addAttribute("desafiosCompletados", 0L);
                        model.addAttribute("puntosTotal", 0);
                        model.addAttribute("insigniasTotal", 0);
                        model.addAttribute("proyectosActuales", List.of());
                        model.addAttribute("participacionesDesafio", List.of());
                        model.addAttribute("error", "Error al cargar los datos del perfil");
                        return "InicioUsuario";
                    }
                } else {
                    // Rol no reconocido o no asignado
                    log.warn("Rol no reconocido: {}", usuario.getRol());
                    return "redirect:/login";
                }
            } else {
                log.warn("Usuario no encontrado con correo: {}", principal.getName());
                return "redirect:/login";
            }
        } catch (Exception e) {
            log.error("Error general en dashboard: ", e);
            model.addAttribute("error", "Error general: " + e.getMessage());
            model.addAttribute("estadisticas", new HashMap<String, Object>());
            model.addAttribute("proyectosActivos", 0L);
            model.addAttribute("forosActivos", 0L);
            return "DashboardAdmin"; // Intentamos mostrar el dashboard con valores por defecto
        }
    }

    //ADMINISTRAR PROYECTOS(ADMINISTRADORES)
    @GetMapping("/admin/proyectos")
    @PreAuthorize("hasAuthority('ADMIN')")
    public String administrarProyectos(Model model) {
        model.addAttribute("proyectos", proyectoServicio.obtenerTodos());
        return "ProyectosAdmin";
    }
    
    //CREAR PROYECTO(ADMINISTRADORES)
    @PostMapping("/admin/proyectos/crear")
    @PreAuthorize("hasAuthority('ADMIN')")
    public String crearProyecto(@ModelAttribute Proyecto proyecto, 
                               @RequestParam("imagenProyecto") MultipartFile archivo) {
        // Guardar la imagen en el sistema de archivos
        if (!archivo.isEmpty()) {
            try {
                // Crear directorio si no existe
                String rutaDirectorio = "src/main/resources/static/Proyectos";
                File directorio = new File(rutaDirectorio);
                if (!directorio.exists()) {
                    directorio.mkdirs();
                }
                
                // Generar un nombre único para el archivo
                String nombreArchivo = UUID.randomUUID().toString() + "_" + archivo.getOriginalFilename();
                Path rutaCompleta = Path.of(rutaDirectorio, nombreArchivo);
                
                // Guardar el archivo
                archivo.transferTo(rutaCompleta);
                
                // Guardar la URL relativa en el objeto proyecto
                proyecto.setImagenUrl("/Proyectos/" + nombreArchivo);
                
            } catch (IOException e) {
                e.printStackTrace();
                // Manejar el error
            }
        }
        
        // Guardar el proyecto en la base de datos
        proyectoServicio.crearProyecto(proyecto);
        
        return "redirect:/admin/proyectos";
    }
    
    //ACTUALIZAR PROYECTO(ADMINISTRADORES)
    @GetMapping("/admin/proyectos/{id}/editar")
    @PreAuthorize("hasAuthority('ADMIN')")
    public String editarProyecto(@PathVariable String id, Model model) {
        model.addAttribute("proyecto", proyectoServicio.obtenerProyectoPorId(id).orElse(null));
        return "EditarProyecto";
    }
    
    //ACTUALIZAR PROYECTO(ADMINISTRADORES)
    @PostMapping("/admin/proyectos/{id}/actualizar")
    @PreAuthorize("hasAuthority('ADMIN')")
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
                    String rutaDirectorio = "src/main/resources/static/Proyectos";
                    
                    // Eliminar la imagen anterior si existe
                    if (proyectoActual.getImagenUrl() != null && !proyectoActual.getImagenUrl().isEmpty()) {
                        String nombreImagenAnterior = proyectoActual.getImagenUrl().substring(proyectoActual.getImagenUrl().lastIndexOf("/") + 1);
                        Path rutaImagenAnterior = Path.of(rutaDirectorio, nombreImagenAnterior);
                        Files.deleteIfExists(rutaImagenAnterior);
                    }
                    
                    // Guardar la nueva imagen
                    String nombreArchivo = UUID.randomUUID().toString() + "_" + archivo.getOriginalFilename();
                    Path rutaCompleta = Path.of(rutaDirectorio, nombreArchivo);
                    
                    archivo.transferTo(rutaCompleta);
                    
                    proyectoActual.setImagenUrl("/Proyectos/" + nombreArchivo);
                    
                } catch (IOException e) {
                    e.printStackTrace();
                    // Manejar el error
                }
            }
            
            // Guardar el proyecto actualizado
            proyectoServicio.actualizarProyecto(proyectoActual);
        }
        
        return "redirect:/admin/proyectos";
    }
    
    //ELIMINAR PROYECTO(ADMINISTRADORES)
    @GetMapping("/admin/proyectos/{id}/eliminar")
    @PreAuthorize("hasAuthority('ADMIN')")
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
        
        return "redirect:/admin/proyectos";
    }
    
    //BUSCAR PROYECTOS(ADMINISTRADORES y VOLUNTARIOS)
    @GetMapping("/admin/proyectos/buscar")
    @PreAuthorize("hasAuthority('ADMIN')")
    public String buscarProyectos(@RequestParam String nombre, Model model) {
        model.addAttribute("proyectos", proyectoServicio.buscarPorNombre(nombre));
        return "ProyectosAdmin";
    }

    //PERFIL DEL USUARIO(ADMINISTRADORES y VOLUNTARIOS)
    @GetMapping("/usuario/perfil")
    @PreAuthorize("isAuthenticated()")
    public String perfil(Principal principal, Model model) {
        try {
            if (principal == null) {
                log.warn("Intento de acceso a perfil sin autenticación");
                return "redirect:/login";
            }
            
            Optional<Usuario> usuarioOpt = usuarioServicio.buscarPorCorreo(principal.getName());
            
            if (usuarioOpt.isPresent()) {
                Usuario usuario = usuarioOpt.get();
                // Cargar datos adicionales del usuario
                cargarDatosAdicionalesPerfil(usuario, model);
                model.addAttribute("usuario", usuario);
                return "PerfilUsuario";
            } else {
                log.warn("Usuario no encontrado para correo: {}", principal.getName());
                return "redirect:/login";
            }
        } catch (Exception e) {
            log.error("Error al cargar perfil de usuario: ", e);
            model.addAttribute("error", "Error al cargar el perfil");
            return "error";
        }
    }
    
    private void cargarDatosAdicionalesPerfil(Usuario usuario, Model model) {
        try {
            // Cargar estadísticas del usuario
            int puntosTotales = participacionDesafioServicio.calcularPuntosTotales(usuario.getId());
            List<String> insignias = participacionDesafioServicio.obtenerInsigniasUsuario(usuario.getId());
            
            // Cargar proyectos activos
            List<Proyecto> proyectosActivos = usuario.getProyectosParticipadosIds().stream()
                .map(id -> proyectoServicio.obtenerProyectoPorId(id))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(p -> p.getEstado() == EstadoProyecto.ACTIVO)
                .collect(Collectors.toList());
            
            // Cargar desafíos en progreso
            List<ParticipacionDesafio> desafiosEnProgreso = participacionDesafioServicio.obtenerPorUsuario(usuario.getId())
                .stream()
                .filter(pd -> !pd.isCompletado())
                .collect(Collectors.toList());
            
            model.addAttribute("puntosTotales", puntosTotales);
            model.addAttribute("insignias", insignias);
            model.addAttribute("proyectosActivos", proyectosActivos);
            model.addAttribute("desafiosEnProgreso", desafiosEnProgreso);
        } catch (Exception e) {
            log.error("Error al cargar datos adicionales del perfil: ", e);
            // No lanzamos la excepción para no interrumpir la carga del perfil
        }
    }
    
    //PERFIL DEL USUARIO MEJORADO(ADMINISTRADORES y VOLUNTARIOS)
    @GetMapping("/usuario/perfil/mejorado")
    @PreAuthorize("isAuthenticated()")
    public String perfilMejorado(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }
        
        Optional<Usuario> usuarioOpt = usuarioServicio.buscarPorCorreo(principal.getName());
        
        if (usuarioOpt.isPresent()) {
            model.addAttribute("usuario", usuarioOpt.get());
            return "PerfilUsuario";
        } else {
            return "redirect:/login";
        }
    }
    
    //PROYECTOS DEL USUARIO(VOLUNTARIOS)
    @GetMapping("/usuario/proyectos")
    @PreAuthorize("isAuthenticated()")
    public String misProyectos(Principal principal, Model model) {
        try {
            if (principal == null) {
                return "redirect:/login";
            }
            
            Optional<Usuario> usuarioOpt = usuarioServicio.buscarPorCorreo(principal.getName());
            
            if (usuarioOpt.isPresent()) {
                Usuario usuario = usuarioOpt.get();
                model.addAttribute("usuario", usuario);

                // Optimización: Obtener todos los proyectos una sola vez
                List<Proyecto> todosLosProyectos = proyectoServicio.obtenerTodos();
                
                // Obtener IDs de proyectos participados
                Set<String> proyectoIdsParticipados = usuario.getProyectosParticipadosIds().stream()
                        .collect(Collectors.toSet());

                // Filtrar proyectos usando el set para mejor rendimiento
                List<Proyecto> proyectosParticipados = todosLosProyectos.stream()
                        .filter(p -> proyectoIdsParticipados.contains(p.getId()))
                        .collect(Collectors.toList());

                // Separar proyectos por estado usando streams
                Map<EstadoProyecto, List<Proyecto>> proyectosPorEstado = proyectosParticipados.stream()
                        .collect(Collectors.groupingBy(Proyecto::getEstado));

                // Proyectos disponibles (no participados)
                List<Proyecto> proyectosDisponibles = todosLosProyectos.stream()
                        .filter(p -> !proyectoIdsParticipados.contains(p.getId()))
                        .collect(Collectors.toList());

                model.addAttribute("proyectosActivos", 
                    proyectosPorEstado.getOrDefault(EstadoProyecto.ACTIVO, Collections.emptyList()));
                model.addAttribute("proyectosExpirados", 
                    proyectosPorEstado.getOrDefault(EstadoProyecto.EXPIRADO, Collections.emptyList()));
                model.addAttribute("proyectosDisponibles", proyectosDisponibles);
                
                return "ProyectosUsuarios";
            } else {
                return "redirect:/login";
            }
        } catch (Exception e) {
            log.error("Error al cargar proyectos del usuario: ", e);
            model.addAttribute("error", "Error al cargar los proyectos");
            return "error";
        }
    }
    
    // Mapeo para redirigir a la vista de proyectos de usuario
    @GetMapping("/mis-proyectos")
    @PreAuthorize("isAuthenticated()")
    public String misProyectosRedirect(Principal principal) {
        return "redirect:/usuario/proyectos";
    }
    
    //DESAFÍOS DEL USUARIO(VOLUNTARIOS)
    @GetMapping("/usuario/desafios")
    @PreAuthorize("isAuthenticated()")
    public String misDesafios(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }
        
        Optional<Usuario> usuarioOpt = usuarioServicio.buscarPorCorreo(principal.getName());
        
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            model.addAttribute("usuario", usuario);
            
            var participaciones = participacionDesafioServicio.obtenerPorUsuario(usuario.getId());
            
            model.addAttribute("desafiosActivos", participaciones.stream()
                    .filter(d -> !d.isCompletado())
                    .toList());
            model.addAttribute("desafiosCompletados", participaciones.stream()
                    .filter(d -> d.isCompletado())
                    .toList());
            
            return "MisDesafios";
        } else {
            return "redirect:/login";
        }
    }
    
    // Mapeo para redirigir a la vista de desafíos de usuario
    @GetMapping("/mis-desafios")
    @PreAuthorize("isAuthenticated()")
    public String misDesafiosRedirect(Principal principal) {
        return "redirect:/usuario/desafios";
    }

    // FOROS DE PROYECTOS (PARA TODOS LOS USUARIOS)
    @GetMapping("/foros")
    @PreAuthorize("isAuthenticated()")
    public String forosProyectos(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }
        
        Optional<Usuario> usuarioOpt = usuarioServicio.buscarPorCorreo(principal.getName());
        
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            model.addAttribute("usuario", usuario);
            
            // Si es admin, redirigir a la vista de administración de foros
            if (usuario.getRol() == Rol.ADMIN) {
                return "redirect:/admin/foros";
            }
            
            // Para usuarios normales, obtener todos los foros y mostrar la vista regular
            List<Foro> todosForos = foroServicio.obtenerTodos();
            model.addAttribute("foros", todosForos);
            model.addAttribute("esAdmin", false);
            
            return "ForoUsuario";
        } else {
            return "redirect:/login";
        }
    }
    
    // FORO ESPECÍFICO DE UN PROYECTO
    @GetMapping("/proyectos/{proyectoId}/foro")
    @PreAuthorize("isAuthenticated()")
    public String foroDeProyecto(@PathVariable String proyectoId, Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }
        
        Optional<Usuario> usuarioOpt = usuarioServicio.buscarPorCorreo(principal.getName());
        Optional<Proyecto> proyectoOpt = proyectoServicio.obtenerProyectoPorId(proyectoId);
        
        if (!usuarioOpt.isPresent() || !proyectoOpt.isPresent()) {
            return "redirect:/dashboard";
        }
        
        Usuario usuario = usuarioOpt.get();
        Proyecto proyecto = proyectoOpt.get();
        
        model.addAttribute("usuario", usuario);
        model.addAttribute("proyecto", proyecto);
        
        // Obtener foros relacionados con este proyecto
        List<Foro> forosProyecto = foroServicio.obtenerForosPorProyecto(proyectoId);
        model.addAttribute("foros", forosProyecto);
        
        return "ForoUsuario";
    }

    // Página administrativa principal
    @GetMapping("/admin")
    @PreAuthorize("hasAuthority('ADMIN')")
    public String adminRoot() {
        return "redirect:/admin/proyectos";
    }

    // Dashboard administrativo directo
    @GetMapping("/dashboardAdmin")
    @PreAuthorize("hasAuthority('ADMIN')")
    public String dashboardAdmin(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }
        
        Optional<Usuario> usuarioOpt = usuarioServicio.buscarPorCorreo(principal.getName());
        
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            
            if (usuario.getRol() != Rol.ADMIN) {
                return "redirect:/dashboard";
            }
            
            model.addAttribute("usuario", usuario);
            model.addAttribute("nombre", usuario.getNombre());
            
            // Estadísticas para administradores
            java.util.Map<String, Object> estadisticas = usuarioServicio.obtenerEstadisticasUsuarios();
            model.addAttribute("estadisticas", estadisticas);
            
            // Número de proyectos activos
            long proyectosActivos = proyectoServicio.contarProyectosPorEstado(EstadoProyecto.ACTIVO);
            model.addAttribute("proyectosActivos", proyectosActivos);
            
            // Número de foros activos
            long forosActivos = foroServicio.obtenerTodos().size();
            model.addAttribute("forosActivos", forosActivos);
            
            return "DashboardAdmin";
        } else {
            return "redirect:/login";
        }
    }

    // Nuevo endpoint específico para el perfil de administrador
    @GetMapping("/admin/perfil")
    @PreAuthorize("hasAuthority('ADMIN')")
    public String perfilAdmin(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }
        
        Optional<Usuario> usuarioOpt = usuarioServicio.buscarPorCorreo(principal.getName());
        
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            
            if (usuario.getRol() != Rol.ADMIN) {
                return "redirect:/usuario/perfil";
            }
            
            model.addAttribute("usuario", usuario);
            
            return "PerfilAdmin";  // Usa la plantilla PerfilAdmin.html específica
        } else {
            return "redirect:/login";
        }
    }

    // Dashboard administrativo avanzado (Versión sin carga de datos)
    @GetMapping("/admin/dashboard")
    @PreAuthorize("hasAuthority(\'ADMIN\')")
    public String dashboardAdminAvanzado(Model model, Principal principal) {
        try {
            if (principal == null) {
                return "redirect:/login";
            }

            Optional<Usuario> usuarioOpt = usuarioServicio.buscarPorCorreo(principal.getName());
            if (!usuarioOpt.isPresent()) {
                return "redirect:/login?error=userNotFound";
            }
            
            Usuario usuario = usuarioOpt.get();
            if (usuario.getRol() != Rol.ADMIN) {
                return "redirect:/dashboard";
            }

            // CAMBIO RADICAL: Sólo pasamos el nombre de usuario y una estructura mínima
            // Todo lo demás se cargará vía AJAX desde el JavaScript
            model.addAttribute("nombreUsuario", usuario.getNombre());
            
            // Usar plantilla dashboard.html
            return "dashboard";
        } catch (Exception e) {
            log.error("Error al cargar el dashboard: ", e);
            
            // Simplemente mostrar dashboard vacío
            model.addAttribute("nombreUsuario", "Administrador");
            model.addAttribute("error", "No se pudieron cargar las estadísticas");
            
            return "dashboard";
        }
    }

    // Ruta alternativa desde el perfil (Restaurada)
    @GetMapping("/usuario/admin/dashboard")
    public String dashboardAdminDesdePerfilAvanzado(Model model, Principal principal) {
        // Simplemente redirige al dashboard principal
        return "redirect:/admin/dashboard";
    }

    // Dashboard administrativo - Pestaña de usuarios (Restaurado)
    @GetMapping("/admin/dashboard/usuarios")
    @PreAuthorize("hasAuthority(\'ADMIN\')")
    public String dashboardAdminUsuarios(
            Principal principal,
            Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (principal == null) {
            return "redirect:/login";
        }

        Optional<Usuario> usuarioOpt = usuarioServicio.buscarPorCorreo(principal.getName());
        if (!usuarioOpt.isPresent() || usuarioOpt.get().getRol() != Rol.ADMIN) {
            return "redirect:/login";
        }

        Usuario usuario = usuarioOpt.get();
        model.addAttribute("usuario", usuario); // Para la navbar u otros elementos

        // Obtener estadísticas detalladas y paginadas de usuarios
        Map<String, Object> estadisticasUsuarios = usuarioServicio.obtenerEstadisticasDetalladas(page, size);
        model.addAttribute("estadisticasUsuarios", estadisticasUsuarios); // Contiene la lista paginada y metadatos
        model.addAttribute("paginaActual", page); // Pasar página actual para la paginación
        model.addAttribute("tamanoPagina", size); // Pasar tamaño para la paginación

        // Se necesita una plantilla 'admin/UsuariosEstadisticas.html'
        return "admin/UsuariosEstadisticas";
    }

    // Dashboard administrativo - Pestaña de proyectos (Restaurado)
    @GetMapping("/admin/dashboard/proyectos")
    @PreAuthorize("hasAuthority(\'ADMIN\')")
    public String dashboardAdminProyectos(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }

        Optional<Usuario> usuarioOpt = usuarioServicio.buscarPorCorreo(principal.getName());
        if (!usuarioOpt.isPresent() || usuarioOpt.get().getRol() != Rol.ADMIN) {
            return "redirect:/login";
        }

        Usuario usuario = usuarioOpt.get();
        model.addAttribute("usuario", usuario); // Para la navbar

        // Obtener estadísticas de proyectos
        Map<String, Object> estadisticasProyectos = proyectoServicio.obtenerEstadisticasProyectos();
        model.addAttribute("estadisticasProyectos", estadisticasProyectos);

        // Se necesita una plantilla 'admin/ProyectosEstadisticas.html'
        return "admin/ProyectosEstadisticas";
    }

    // Dashboard administrativo - Pestaña de empresas (Restaurado)
    @GetMapping("/admin/dashboard/empresas")
    @PreAuthorize("hasAuthority(\'ADMIN\')")
    public String dashboardAdminEmpresas(
            Principal principal,
            Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) { // Ajustar tamaño si es necesario

        if (principal == null) {
            return "redirect:/login";
        }

        Optional<Usuario> usuarioOpt = usuarioServicio.buscarPorCorreo(principal.getName());
        if (!usuarioOpt.isPresent() || usuarioOpt.get().getRol() != Rol.ADMIN) {
            return "redirect:/login";
        }

        Usuario usuario = usuarioOpt.get();
        model.addAttribute("usuario", usuario); // Para la navbar

        // Obtener estadísticas detalladas (que incluye ranking de empresas)
        // Reutilizamos el método de estadísticas detalladas de usuarios/empresas
        Map<String, Object> estadisticasEmpresas = usuarioServicio.obtenerEstadisticasDetalladas(page, size);
        model.addAttribute("estadisticasEmpresas", estadisticasEmpresas);
        model.addAttribute("paginaActual", page);
        model.addAttribute("tamanoPagina", size);

        // Se necesita una plantilla 'admin/EmpresasEstadisticas.html'
        return "admin/EmpresasEstadisticas";
    }

    // Dashboard administrativo - Detalle de empresa (Restaurado)
    @GetMapping("/admin/dashboard/empresas/{empresa}")
    @PreAuthorize("hasAuthority(\'ADMIN\')")
    public String dashboardAdminDetalleEmpresa(
            @PathVariable String empresa,
            Principal principal,
            Model model) {

        if (principal == null) {
            return "redirect:/login";
        }

        Optional<Usuario> usuarioOpt = usuarioServicio.buscarPorCorreo(principal.getName());
        if (!usuarioOpt.isPresent() || usuarioOpt.get().getRol() != Rol.ADMIN) {
            return "redirect:/login";
        }

        Usuario usuario = usuarioOpt.get();
        model.addAttribute("usuario", usuario); // Para la navbar

        // Obtener estadísticas específicas de la empresa
        Map<String, Object> estadisticasEmpresa = usuarioServicio.obtenerEstadisticasPorEmpresa(empresa);
        model.addAttribute("estadisticasEmpresa", estadisticasEmpresa);
        model.addAttribute("nombreEmpresa", empresa); // Pasar el nombre para el título

        // Se necesita una plantilla 'admin/EmpresaDetalle.html'
        return "admin/EmpresaDetalle";
    }

    // ENDPOINT PARA PANEL DE NOTIFICACIONES DE ADMIN
    @GetMapping("/admin/notificaciones")
    @PreAuthorize("hasAuthority('ADMIN')")
    public String panelNotificaciones(Principal principal, Model model) {
        model.addAttribute("usuarios", usuarioServicio.obtenerTodos());
        model.addAttribute("proyectos", proyectoServicio.obtenerTodos());
        
        // Si hay un usuario autenticado, agregar sus datos al modelo
        if (principal != null) {
            Optional<Usuario> usuarioOpt = usuarioServicio.buscarPorCorreo(principal.getName());
            usuarioOpt.ifPresent(usuario -> {
                model.addAttribute("usuario", usuario);
            });
        }
        
        return "admin/NotificacionesPanel";
    }

    // Método de depuración temporal para diagnosticar problemas
    @GetMapping("/dashboard-debug")
    @PreAuthorize("isAuthenticated()")
    public String dashboardDebug(Principal principal, Model model) {
        try {
            log.info("Iniciando método de depuración dashboard-debug");
            
            if (principal == null) {
                log.warn("Principal es nulo");
                return "redirect:/login";
            }
            
            Optional<Usuario> usuarioOpt = usuarioServicio.buscarPorCorreo(principal.getName());
            if (!usuarioOpt.isPresent()) {
                log.warn("Usuario no encontrado");
                return "redirect:/login";
            }
            
            Usuario usuario = usuarioOpt.get();
            log.info("Usuario encontrado: {}, Rol: {}", usuario.getNombre(), usuario.getRol());
            
            // Añadir información básica al modelo
            model.addAttribute("nombre", usuario.getNombre());
            model.addAttribute("usuario", usuario);
            
            // Añadir estadísticas simples hardcodeadas para evitar errores
            HashMap<String, Object> estadisticas = new HashMap<>();
            estadisticas.put("totalUsuarios", 100L);
            estadisticas.put("totalVoluntarios", 90L);
            estadisticas.put("totalAdministradores", 10L);
            estadisticas.put("desafiosCompletados", 50L);
            
            model.addAttribute("estadisticas", estadisticas);
            model.addAttribute("proyectosActivos", 20L);
            model.addAttribute("forosActivos", 30L);
            
            log.info("Modelo preparado, intentando renderizar plantilla");
            return "DashboardAdmin";
        } catch (Exception e) {
            log.error("Error en método de depuración: ", e);
            // Mostrar una página de error simple
            model.addAttribute("error", "Error en depuración: " + e.getMessage());
            return "error"; // Asumiendo que existe una plantilla error.html simple
        }
    }

    // Endpoint para la vista de administración de foros
    @GetMapping("/admin/foros")
    @PreAuthorize("hasAuthority('ADMIN')")
    public String adminForos(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }
        
        Optional<Usuario> usuarioOpt = usuarioServicio.buscarPorCorreo(principal.getName());
        
        if (usuarioOpt.isPresent()) {
            Usuario admin = usuarioOpt.get();
            
            // Verificar que sea administrador
            if (admin.getRol() != Rol.ADMIN) {
                return "redirect:/foros";
            }
            
            model.addAttribute("usuario", admin);
            model.addAttribute("esAdmin", true);
            
            // Cargar estadísticas de foros para el panel de administración
            try {
                Map<String, Object> estadisticas = foroServicio.obtenerEstadisticasAdmin();
                model.addAttribute("estadisticas", estadisticas);
            } catch (Exception e) {
                model.addAttribute("error", "Error al cargar estadísticas: " + e.getMessage());
            }
            
            return "ForoAdmin"; // Esta es la plantilla HTML para administración de foros
        } else {
            return "redirect:/login";
        }
    }

    @GetMapping("/admin/proyectos/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public String detallesProyectoAdmin(@PathVariable String id, Model model, Principal principal) {
        // Redirigir a la página de proyectos admin, el detalle se mostrará en un modal
        return "redirect:/admin/proyectos?showDetail=" + id;
    }
}
   