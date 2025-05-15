package com.Impulso.Alcambio.Servicio;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.Impulso.Alcambio.Modelo.Rol;
import com.Impulso.Alcambio.Modelo.Usuario;
import com.Impulso.Alcambio.Modelo.ParticipacionDesafio;
import com.Impulso.Alcambio.Modelo.Foro;
import com.Impulso.Alcambio.Repositorio.UsuarioRepositorio;
import com.Impulso.Alcambio.Repositorio.ParticipacionDesafioRepositorio;
import com.Impulso.Alcambio.Servicio.ProyectoServicio;
import com.Impulso.Alcambio.Modelo.Proyecto;

/**
 * Servicio para la gestión de usuarios en la aplicación.
 * Proporciona métodos para registro, autenticación, búsqueda y manipulación de usuarios.
 */
@Service
public class UsuarioServicio {
    
    private static final Logger log = LoggerFactory.getLogger(UsuarioServicio.class);

    private final UsuarioRepositorio usuarioRepositorio;
    private final PasswordEncoder passwordEncoder;
    private final ParticipacionDesafioRepositorio participacionRepositorio;
    private final ParticipacionDesafioServicio participacionDesafioServicio;
    private final MongoTemplate mongoTemplate;
    
    @Lazy // Para evitar dependencia circular
    @Autowired
    private ProyectoServicio proyectoServicio;
    
    @Autowired
    public UsuarioServicio(UsuarioRepositorio usuarioRepositorio, 
                           @Lazy PasswordEncoder passwordEncoder, 
                           ParticipacionDesafioRepositorio participacionRepositorio,
                           @Lazy ParticipacionDesafioServicio participacionDesafioServicio,
                           MongoTemplate mongoTemplate) {
        this.usuarioRepositorio = usuarioRepositorio;
        this.passwordEncoder = passwordEncoder;
        this.participacionRepositorio = participacionRepositorio;
        this.participacionDesafioServicio = participacionDesafioServicio;
        this.mongoTemplate = mongoTemplate;
    }
    
    /**
     * Registra un nuevo usuario en el sistema
     * 
     * @param usuario Objeto Usuario con los datos del nuevo usuario
     * @return Usuario registrado con ID asignado
     * @throws RuntimeException Si el correo ya existe en el sistema
     */
    @Transactional
    public Usuario registrarUsuario(Usuario usuario) {
        // Verificar si ya existe un usuario con ese correo
        if (usuarioRepositorio.existsByCorreo(usuario.getCorreo())) {
            throw new RuntimeException("Ya existe un usuario con ese correo electrónico");
        }
        
        // Encriptar la contraseña
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        
        // Asignar rol de voluntario por defecto
        if (usuario.getRol() == null) {
            usuario.setRol(Rol.VOLUNTARIO);
        }
        
        // Establecer fecha de registro si no está establecida
        if (usuario.getFechaRegistro() == null) {
            usuario.setFechaRegistro(LocalDateTime.now());
        }
        
        // Guardar el usuario
        return usuarioRepositorio.save(usuario);
    }
    
    /**
     * Busca un usuario por su correo electrónico
     * 
     * @param correo Correo electrónico del usuario
     * @return Optional con el usuario si existe, empty si no existe
     */
    public Optional<Usuario> buscarPorCorreo(String correo) {
        return usuarioRepositorio.findByCorreo(correo);
    }
    
    /**
     * Busca un usuario por su ID
     * 
     * @param id ID del usuario
     * @return Optional con el usuario si existe, empty si no existe
     */
    public Optional<Usuario> buscarPorId(String id) {
        return usuarioRepositorio.findById(id);
    }
    
    /**
     * Obtiene la lista de todos los usuarios
     * 
     * @return Lista de todos los usuarios
     */
    public List<Usuario> obtenerTodos() {
        return usuarioRepositorio.findAll();
    }
    
    /**
     * Busca usuarios por rol
     * 
     * @param rol Rol de los usuarios a buscar
     * @return Lista de usuarios con el rol especificado
     */
    public List<Usuario> buscarPorRol(Rol rol) {
        return usuarioRepositorio.findByRol(rol);
    }
    
    /**
     * Autentica un usuario verificando sus credenciales
     * 
     * @param correo Correo electrónico del usuario
     * @param password Contraseña sin encriptar
     * @return true si las credenciales son válidas, false en caso contrario
     */
    public boolean autenticarUsuario(String correo, String password) {
        return buscarPorCorreo(correo)
                .map(usuario -> passwordEncoder.matches(password, usuario.getPassword()))
                .orElse(false);
    }
    
    /**
     * Actualiza los datos de un usuario existente y la información denormalizada en foros.
     * 
     * @param usuario Usuario con los datos actualizados
     * @return Usuario actualizado
     */
    @Transactional
    public Usuario actualizarUsuario(Usuario usuario) {
        boolean passwordChanged = false;
        // Si hay una nueva contraseña a guardar (no encriptada), la encriptamos
        if (usuario.getPassword() != null && !usuario.getPassword().isEmpty() && !usuario.getPassword().startsWith("$2a$")) {
            usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
            passwordChanged = true;
        } else if (!passwordChanged && usuario.getPassword() != null && usuario.getPassword().startsWith("$2a$")) {
             // Si no se cambió la contraseña pero aún existe la encriptada, la mantenemos.
             // Si se desea eliminar, se debe poner explícitamente a null o vacío antes de llamar.
        } else {
            // Si no se envió contraseña nueva, ni existía una encriptada (o se envió vacía/null),
            // nos aseguramos de que no se guarde una contraseña vacía. Buscamos la existente.
            Optional<Usuario> existenteOpt = usuarioRepositorio.findById(usuario.getId());
            if (existenteOpt.isPresent() && existenteOpt.get().getPassword() != null) {
                usuario.setPassword(existenteOpt.get().getPassword()); // Mantener contraseña existente
            } else {
                usuario.setPassword(null); // No hay contraseña existente o es null
            }
        }
        
        Usuario usuarioActualizado = usuarioRepositorio.save(usuario);
        
        // Después de guardar el usuario, actualizar la AutorInfo en los foros
        actualizarAutorInfoEnForos(usuarioActualizado.getId(), 
                                  usuarioActualizado.getNombre(), 
                                  usuarioActualizado.getImagenPerfil());
        
        return usuarioActualizado;
    }
    
    /**
     * Método privado para actualizar la información del autor (nombre, imagen)
     * en todos los posts, comentarios y respuestas del foro donde aparece.
     */
    private void actualizarAutorInfoEnForos(String usuarioId, String nuevoNombre, String nuevaImagenPerfil) {
        log.info("Actualizando AutorInfo para usuario {} en foros...", usuarioId);
        
        // Actualizar autor principal (documentos donde el usuario es autor principal)
        Query queryAutorPrincipal = new Query(Criteria.where("autor.usuarioId").is(usuarioId));
        Update updateAutorPrincipal = new Update()
            .set("autor.nombre", nuevoNombre)
            .set("autor.imagenPerfil", nuevaImagenPerfil);
        
        try {
            com.mongodb.client.result.UpdateResult resultAutorPrincipal = mongoTemplate.updateMulti(
                queryAutorPrincipal, 
                updateAutorPrincipal,
                Foro.class
            );
            log.info("Actualización de autores principales: Modificados {}, Coincidentes {}", 
                     resultAutorPrincipal.getModifiedCount(), resultAutorPrincipal.getMatchedCount());
        } catch (Exception e) {
            log.error("Error al actualizar autores principales: {}", e.getMessage(), e);
        }
        
        // Actualizar autor en comentarios
        Query queryComentarios = new Query(Criteria.where("comentarios.autor.usuarioId").is(usuarioId));
        Update updateComentarios = new Update()
            .set("comentarios.$.autor.nombre", nuevoNombre)
            .set("comentarios.$.autor.imagenPerfil", nuevaImagenPerfil);
        
        try {
            com.mongodb.client.result.UpdateResult resultComentarios = mongoTemplate.updateMulti(
                queryComentarios, 
                updateComentarios,
                Foro.class
            );
            log.info("Actualización de autores en comentarios: Modificados {}, Coincidentes {}", 
                     resultComentarios.getModifiedCount(), resultComentarios.getMatchedCount());
        } catch (Exception e) {
            log.error("Error al actualizar autores en comentarios: {}", e.getMessage(), e);
        }
        
        // Para las respuestas, necesitamos usar operaciones más avanzadas de MongoDB
        // ya que son arrays anidados. Esto requiere usar directamente el driver de MongoDB
        try {
            // Obtener la colección directamente
            com.mongodb.client.MongoCollection<org.bson.Document> collection = 
                mongoTemplate.getCollection(mongoTemplate.getCollectionName(Foro.class));
            
            // Crear el filtro para documentos que contienen respuestas del usuario
            org.bson.conversions.Bson filter = Filters.elemMatch("comentarios.respuestas", 
                Filters.eq("autor.usuarioId", usuarioId));
            
            // Crear la actualización usando operadores de array con $[]
            org.bson.Document update = new org.bson.Document("$set", 
                new org.bson.Document("comentarios.$[].respuestas.$[resp].autor.nombre", nuevoNombre)
                    .append("comentarios.$[].respuestas.$[resp].autor.imagenPerfil", nuevaImagenPerfil));
            
            // Configurar el arrayFilter para identificar las respuestas del usuario
            com.mongodb.client.model.UpdateOptions options = new UpdateOptions()
                .arrayFilters(List.of(new org.bson.Document("resp.autor.usuarioId", usuarioId)));
            
            // Ejecutar la actualización
            com.mongodb.client.result.UpdateResult resultRespuestas = 
                collection.updateMany(filter, update, options);
                
            log.info("Actualización de autores en respuestas: Modificados {}, Coincidentes {}", 
                     resultRespuestas.getModifiedCount(), resultRespuestas.getMatchedCount());
                     
        } catch (Exception e) {
            log.error("Error al actualizar autores en respuestas: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Elimina un usuario por su ID
     * 
     * @param id ID del usuario a eliminar
     * @return true si el usuario fue eliminado, false si no existe
     */
    @Transactional
    public boolean eliminarUsuario(String id) {
        if (usuarioRepositorio.existsById(id)) {
            usuarioRepositorio.deleteById(id);
            return true;
        }
        return false;
    }
    
    /**
     * Busca usuarios por empresa
     * 
     * @param empresa Nombre de la empresa
     * @return Lista de usuarios que pertenecen a la empresa
     */
    public List<Usuario> buscarPorEmpresa(String empresa) {
        return usuarioRepositorio.findByEmpresa(empresa);
    }
    
    /**
     * Calcula el número de desafíos completados por un usuario
     * 
     * @param usuarioId ID del usuario
     * @return Número de desafíos completados
     */
    public long calcularDesafiosCompletados(String usuarioId) {
        // Encuentra todas las participaciones del usuario
        List<ParticipacionDesafio> participaciones = participacionRepositorio.findByUsuarioId(usuarioId);
        
        // Cuenta cuántas de esas participaciones están marcadas como completadas
        return participaciones.stream()
                            .filter(ParticipacionDesafio::isCompletado)
                            .count();
    }
    
    /**
     * Elimina la participación de un usuario en un proyecto
     * 
     * @param usuarioId ID del usuario
     * @param proyectoId ID del proyecto
     * @return true si se eliminó la participación, false si no existe
     */
    @Transactional
    public boolean eliminarParticipacion(String usuarioId, String proyectoId) {
        return buscarPorId(usuarioId)
            .map(usuario -> {
                boolean eliminado = Optional.ofNullable(usuario.getProyectosParticipadosIds())
                    .map(proyectos -> proyectos.remove(proyectoId))
                    .orElse(false);
                
                if (eliminado) {
                    usuarioRepositorio.save(usuario);
                }
                
                return eliminado;
            })
            .orElse(false);
    }
    
    /**
     * Obtiene estadísticas generales de usuarios
     * 
     * @return Mapa con diversas estadísticas de usuarios
     */
    public Map<String, Object> obtenerEstadisticasUsuarios() {
        Map<String, Object> estadisticas = new HashMap<>();
        
        // Estadísticas básicas
        estadisticas.put("totalUsuarios", usuarioRepositorio.count());
        estadisticas.put("totalVoluntarios", usuarioRepositorio.countByRol(Rol.VOLUNTARIO));
        estadisticas.put("totalAdministradores", usuarioRepositorio.countByRol(Rol.ADMIN));
        
        // Calcular usuarios nuevos del mes actual
        LocalDateTime inicioMes = LocalDateTime.now()
            .withDayOfMonth(1)
            .withHour(0)
            .withMinute(0)
            .withSecond(0);
            
        long usuariosNuevosMes = usuarioRepositorio.findAll().stream()
            .filter(u -> Optional.ofNullable(u.getFechaRegistro())
                .map(fecha -> fecha.isAfter(inicioMes))
                .orElse(false))
            .count();
        
        estadisticas.put("usuariosNuevosMes", usuariosNuevosMes);
        
        // Estadísticas por empresa
        Map<String, Long> usuariosPorEmpresa = usuarioRepositorio.findAll().stream()
            .filter(u -> Optional.ofNullable(u.getEmpresa())
                .filter(empresa -> !empresa.isBlank())
                .isPresent())
            .collect(Collectors.groupingBy(
                Usuario::getEmpresa,
                Collectors.counting()
            ));
        
        estadisticas.put("empresasActivas", usuariosPorEmpresa.size());
        estadisticas.put("usuariosPorEmpresa", usuariosPorEmpresa);
        
        return estadisticas;
    }
    
    /**
     * Verifica y cambia la contraseña de un usuario
     * 
     * @param id ID del usuario
     * @param oldPassword Contraseña actual
     * @param newPassword Nueva contraseña
     * @return true si el cambio fue exitoso, false si la contraseña actual es incorrecta
     * @throws RuntimeException si el usuario no existe
     */
    @Transactional
    public boolean cambiarPassword(String id, String oldPassword, String newPassword) {
        return buscarPorId(id).map(usuario -> {
            if (passwordEncoder.matches(oldPassword, usuario.getPassword())) {
                usuario.setPassword(passwordEncoder.encode(newPassword));
                usuarioRepositorio.save(usuario);
                return true;
            } else {
                return false;
            }
        }).orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));
    }
    
    /**
     * Obtiene el endpoint para cambiar la contraseña de un usuario
     * 
     * @param userId ID del usuario
     * @return String con la ruta del endpoint
     */
    public String obtenerEndpointCambioPassword(String userId) {
        return "/api/usuarios/" + userId + "/cambiar-password";
    }
    
    /**
     * Obtiene estadísticas detalladas de usuarios para el panel de administración
     * 
     * @param page Número de página (empezando desde 0)
     * @param size Tamaño de la página
     * @return Mapa con todas las estadísticas
     */
    public Map<String, Object> obtenerEstadisticasDetalladas(int page, int size) {
        Map<String, Object> estadisticas = new HashMap<>();
        
        // Estadísticas básicas
        long totalUsuarios = usuarioRepositorio.count();
        long totalVoluntarios = usuarioRepositorio.countByRol(Rol.VOLUNTARIO);
        long totalAdministradores = usuarioRepositorio.countByRol(Rol.ADMIN);
        
        estadisticas.put("totalUsuarios", totalUsuarios);
        estadisticas.put("totalVoluntarios", totalVoluntarios);
        estadisticas.put("totalAdministradores", totalAdministradores);
        
        // Obtener usuarios activos (con participación en proyectos o desafíos)
        List<Usuario> todosUsuarios = usuarioRepositorio.findAll();
        
        // Usuarios con más proyectos
        List<Map<String, Object>> usuariosProyectos = todosUsuarios.stream()
            .filter(u -> u.getProyectosParticipadosIds() != null && !u.getProyectosParticipadosIds().isEmpty())
            .sorted((u1, u2) -> Integer.compare(
                u2.getProyectosParticipadosIds().size(),
                u1.getProyectosParticipadosIds().size()))
            .limit(10)
            .map(u -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", u.getId());
                map.put("nombre", u.getNombre());
                map.put("empresa", u.getEmpresa());
                map.put("cantidadProyectos", u.getProyectosParticipadosIds().size());
                return map;
            })
            .collect(Collectors.toList());
        
        estadisticas.put("usuariosConMasProyectos", usuariosProyectos);
        
        // Usuarios con más puntos
        List<Map<String, Object>> usuariosPuntos = todosUsuarios.stream()
            .map(u -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", u.getId());
                map.put("nombre", u.getNombre());
                map.put("empresa", u.getEmpresa());
                map.put("puntos", participacionDesafioServicio.calcularPuntosTotales(u.getId()));
                return map;
            })
            .sorted((m1, m2) -> Integer.compare(
                (Integer) m2.get("puntos"),
                (Integer) m1.get("puntos")))
            .limit(10)
            .collect(Collectors.toList());
        
        estadisticas.put("usuariosConMasPuntos", usuariosPuntos);
        
        return estadisticas;
    }
    
    /**
     * Obtiene estadísticas específicas para una empresa
     * 
     * @param empresa Nombre de la empresa
     * @return Mapa con estadísticas de la empresa
     */
    public Map<String, Object> obtenerEstadisticasPorEmpresa(String empresa) {
        Map<String, Object> estadisticas = new HashMap<>();
        
        // Obtener usuarios de la empresa
        List<Usuario> usuariosEmpresa = buscarPorEmpresa(empresa);
        
        estadisticas.put("totalUsuarios", usuariosEmpresa.size());
        
        // Calcular proyectos participados
        long totalProyectos = usuariosEmpresa.stream()
            .filter(u -> u.getProyectosParticipadosIds() != null)
            .flatMap(u -> u.getProyectosParticipadosIds().stream())
            .distinct()
            .count();
            
        estadisticas.put("totalProyectos", totalProyectos);
        
        // Calcular puntos totales
        int puntosTotales = usuariosEmpresa.stream()
            .mapToInt(u -> participacionDesafioServicio.calcularPuntosTotales(u.getId()))
            .sum();
            
        estadisticas.put("puntosTotales", puntosTotales);
        
        // Calcular desafíos completados
        long desafiosCompletados = usuariosEmpresa.stream()
            .mapToLong(u -> calcularDesafiosCompletados(u.getId()))
            .sum();
            
        estadisticas.put("desafiosCompletados", desafiosCompletados);
        
        return estadisticas;
    }
    
    /**
     * Obtiene la lista de usuarios con paginación
     * 
     * @param pageable Objeto de paginación con página y tamaño
     * @return Página de usuarios
     */
    public Page<Usuario> obtenerTodosPaginados(Pageable pageable) {
        return usuarioRepositorio.findAll(pageable);
    }
    
    /**
     * Calcula el número de proyectos completados en los que ha participado un usuario.
     * Itera sobre los IDs de proyecto del usuario y verifica el estado de cada uno.
     * 
     * @param usuarioId ID del usuario
     * @return Número de proyectos completados en los que participa
     */
    public long calcularProyectosCompletadosParticipados(String usuarioId) {
        Optional<Usuario> usuarioOpt = buscarPorId(usuarioId);
        if (usuarioOpt.isEmpty() || usuarioOpt.get().getProyectosParticipadosIds() == null || usuarioOpt.get().getProyectosParticipadosIds().isEmpty()) {
            return 0L;
        }

        Usuario usuario = usuarioOpt.get();
        List<String> proyectoIds = usuario.getProyectosParticipadosIds();

        long contadorCompletados = 0;
        for (String proyectoId : proyectoIds) {
            Optional<Proyecto> proyectoOpt = proyectoServicio.obtenerProyectoPorId(proyectoId);
            if (proyectoOpt.isPresent() && proyectoOpt.get().getEstado() == Proyecto.EstadoProyecto.COMPLETADO) {
                contadorCompletados++;
            }
        }
        return contadorCompletados;
    }
    
    /**
     * Obtiene usuarios filtrados por estado de baneo y/o búsqueda
     * 
     * @param filtro Filtro a aplicar: todos, activos, baneados
     * @param query Texto de búsqueda para nombre o correo (opcional)
     * @param pageable Configuración de paginación
     * @return Página de usuarios filtrados
     */
    public Page<Usuario> obtenerUsuariosFiltrados(String filtro, String query, Pageable pageable) {
        // Crear una consulta MongoDB para filtrado personalizado
        Query mongoQuery = new Query();
        
        // Aplicar filtros de baneo
        if ("baneados".equals(filtro)) {
            mongoQuery.addCriteria(Criteria.where("baneado").is(true));
        } else if ("activos".equals(filtro)) {
            mongoQuery.addCriteria(Criteria.where("baneado").is(false));
        }
        
        // Aplicar búsqueda por texto si se especificó
        if (query != null && !query.trim().isEmpty()) {
            // Crear criterios para buscar en nombre o correo
            Criteria textSearch = new Criteria().orOperator(
                Criteria.where("nombre").regex(query, "i"),
                Criteria.where("correo").regex(query, "i")
            );
            mongoQuery.addCriteria(textSearch);
        }
        
        // Contar el total de registros para la paginación
        long total = mongoTemplate.count(mongoQuery, Usuario.class);
        
        // Aplicar paginación y ordenación
        mongoQuery.with(pageable);
        
        // Ejecutar consulta
        List<Usuario> usuarios = mongoTemplate.find(mongoQuery, Usuario.class);
        
        // Crear y devolver el objeto Page
        return new org.springframework.data.domain.PageImpl<>(
            usuarios,
            pageable,
            total
        );
    }
    
    /**
     * Obtiene estadísticas por mes de participación para mostrar
     * en gráficas del dashboard
     * 
     * @return Mapa con datos de participación mensual
     */
    public Map<String, Object> obtenerEstadisticasParticipacionPorMes() {
        // Contador por meses para participaciones en el último año
        Map<String, Integer> participacionesPorMes = new HashMap<>();
        
        // Inicializar todos los meses con cero
        Month[] meses = Month.values();
        for (Month mes : meses) {
            participacionesPorMes.put(mes.toString(), 0);
        }
        
        // Contar proyectos participados agrupados por mes
        int contadorCompletados = 0;
        
        // Cuenta los distintos proyectos por mes usando los registros de participación
        try {
            // Obtiene totales de participaciones o proyectos por mes
            // (Aquí habría lógica para contar o bien desde proyectos o bien desde participaciones)
            
            // Por ahora simulamos con datos estáticos para la demostración
            participacionesPorMes.put("JANUARY", 8);
            participacionesPorMes.put("FEBRUARY", 12);
            participacionesPorMes.put("MARCH", 15);
            participacionesPorMes.put("APRIL", 10);
            participacionesPorMes.put("MAY", 18);
            participacionesPorMes.put("JUNE", 24);
            participacionesPorMes.put("JULY", 29);
            participacionesPorMes.put("AUGUST", 26);
            participacionesPorMes.put("SEPTEMBER", 21);
            participacionesPorMes.put("OCTOBER", 14);
            participacionesPorMes.put("NOVEMBER", 10);
            participacionesPorMes.put("DECEMBER", 7);
        } catch (Exception e) {
            log.error("Error al calcular estadísticas de participación por mes", e);
        }
        
        Map<String, Object> resultado = new HashMap<>();
        resultado.put("participacionesPorMes", participacionesPorMes);
        resultado.put("totalCompletados", contadorCompletados);
        
        return resultado;
    }
}
