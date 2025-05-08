package com.Impulso.Alcambio.Servicio;

import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.Impulso.Alcambio.Modelo.ParticipacionDesafio;
import com.Impulso.Alcambio.Modelo.Desafio;
import com.Impulso.Alcambio.Modelo.Usuario;
import com.Impulso.Alcambio.Modelo.Foro;
import com.Impulso.Alcambio.Modelo.Proyecto;
import com.Impulso.Alcambio.Repositorio.ParticipacionDesafioRepositorio;
import com.Impulso.Alcambio.Servicio.DesafioServicio;
import com.Impulso.Alcambio.Servicio.ProyectoServicio;
import com.Impulso.Alcambio.Servicio.ForoServicio;
import com.Impulso.Alcambio.Servicio.UsuarioServicio;
import java.time.LocalDateTime;

@Service
public class ParticipacionDesafioServicio {
    
    private final ParticipacionDesafioRepositorio participacionDesafioRepositorio;
    private final DesafioServicio desafioServicio;
    private final ProyectoServicio proyectoServicio;
    private final ForoServicio foroServicio;
    private final UsuarioServicio usuarioServicio;
    
    @Autowired
    public ParticipacionDesafioServicio(ParticipacionDesafioRepositorio participacionDesafioRepositorio,
                                        DesafioServicio desafioServicio,
                                        ProyectoServicio proyectoServicio,
                                        ForoServicio foroServicio,
                                        UsuarioServicio usuarioServicio) {
        this.participacionDesafioRepositorio = participacionDesafioRepositorio;
        this.desafioServicio = desafioServicio;
        this.proyectoServicio = proyectoServicio;
        this.foroServicio = foroServicio;
        this.usuarioServicio = usuarioServicio;
    }
    
    public List<ParticipacionDesafio> obtenerTodos() {
        return participacionDesafioRepositorio.findAll();
    }
    
    public List<ParticipacionDesafio> obtenerPorUsuario(String usuarioId) {
        return participacionDesafioRepositorio.findByUsuarioId(usuarioId);
    }
    
    public List<ParticipacionDesafio> obtenerPorDesafio(String desafioId) {
        return participacionDesafioRepositorio.findByDesafioId(desafioId);
    }
    
    public Optional<ParticipacionDesafio> obtenerPorUsuarioYDesafio(String usuarioId, String desafioId) {
        return participacionDesafioRepositorio.findByUsuarioIdAndDesafioId(usuarioId, desafioId);
    }
    
    public List<ParticipacionDesafio> obtenerCompletados(boolean completado) {
        return participacionDesafioRepositorio.findByCompletado(completado);
    }
    
    /**
     * Valida si un usuario ha cumplido los criterios de un desafío y, si es así,
     * marca la participación como completada.
     *
     * @param usuarioId El ID del usuario.
     * @param desafioId El ID del desafío.
     * @return La ParticipacionDesafio actualizada.
     * @throws RuntimeException Si la participación o el desafío no se encuentran.
     * @throws IllegalStateException Si el desafío ya está completado o no está activo.
     * @throws RuntimeException Si no se cumplen las condiciones de validación.
     */
    public ParticipacionDesafio validarYCompletarDesafio(String usuarioId, String desafioId) {
        
        // 1. Obtener Participación y Desafío
        ParticipacionDesafio participacion = participacionDesafioRepositorio.findByUsuarioIdAndDesafioId(usuarioId, desafioId)
                .orElseThrow(() -> new RuntimeException("Participación no encontrada para el usuario " + usuarioId + " y desafío " + desafioId));
        
        Desafio desafio = desafioServicio.obtenerPorId(desafioId)
                .orElseThrow(() -> new RuntimeException("Desafío no encontrado con ID: " + desafioId));

        // 2. Validaciones Previas
        if (participacion.isCompletado()) {
            throw new IllegalStateException("El desafío ya ha sido completado por este usuario.");
        }
        
        if (!desafio.estaActivo()) {
             throw new IllegalStateException("El desafío ya no está activo.");
        }

        // 3. Validación de Criterios (Lógica Específica)
        boolean todosCriteriosCumplidos = true;
        for (Desafio.CriterioComplecion criterio : desafio.getCriterios()) {
            if (!validarCriterio(usuarioId, desafio, participacion, criterio)) {
                todosCriteriosCumplidos = false;
                // Se podría lanzar excepción aquí mismo o acumular errores
                 throw new RuntimeException("No se cumplió el criterio: " + criterio.getDescripcion());
            }
        }
        
        // 4. Marcar como Completado si todo es válido
        if (todosCriteriosCumplidos) {
            participacion.setCompletado(true);
            // La fecha se setea automáticamente en el setter de ParticipacionDesafio
            participacion.setProgreso(100); // Opcional: asegurar progreso al 100%
            
            // Ya no modificamos los puntos del usuario directamente, ahora se calculan bajo demanda
            System.out.println("Desafío completado por el usuario con ID " + usuarioId + ": " + 
                              desafio.getNombre() + " - Puntos: " + desafio.getPuntosRecompensa());
            
            return participacionDesafioRepositorio.save(participacion);
        } else {
             // Esto no debería alcanzarse si se lanza excepción en el bucle, pero por si acaso.
             throw new RuntimeException("No se cumplieron todos los criterios del desafío.");
        }
    }

    /**
     * Calcula los puntos totales obtenidos por un usuario basado en los desafíos completados
     * @param usuarioId ID del usuario
     * @return Total de puntos acumulados
     */
    public int calcularPuntosTotales(String usuarioId) {
        List<ParticipacionDesafio> participacionesCompletadas = participacionDesafioRepositorio
            .findByUsuarioIdAndCompletado(usuarioId, true);
        
        return participacionesCompletadas.stream()
            .mapToInt(participacion -> {
                Optional<Desafio> desafio = desafioServicio.obtenerPorId(participacion.getDesafioId());
                return desafio.map(Desafio::getPuntosRecompensa).orElse(0);
            })
            .sum();
    }
    
    /**
     * Obtiene todas las insignias obtenidas por un usuario
     * @param usuarioId ID del usuario
     * @return Lista de insignias (pueden ser IDs o nombres de desafíos completados)
     */
    public List<String> obtenerInsigniasUsuario(String usuarioId) {
        List<ParticipacionDesafio> participacionesCompletadas = participacionDesafioRepositorio
            .findByUsuarioIdAndCompletado(usuarioId, true);
        
        return participacionesCompletadas.stream()
            .map(participacion -> {
                Optional<Desafio> desafio = desafioServicio.obtenerPorId(participacion.getDesafioId());
                return desafio.map(d -> "Desafío: " + d.getNombre()).orElse("");
            })
            .filter(insignia -> !insignia.isEmpty())
            .distinct()
            .collect(Collectors.toList());
    }
    
    /**
     * Genera un resumen de la participación en el sistema de gamificación para un usuario
     * @param usuarioId ID del usuario
     * @return Mapa con estadísticas de participación
     */
    public Map<String, Object> obtenerResumenGameificacion(String usuarioId) {
        Map<String, Object> resumen = new HashMap<>();
        
        // Obtener participaciones
        List<ParticipacionDesafio> todasParticipaciones = obtenerPorUsuario(usuarioId);
        List<ParticipacionDesafio> participacionesCompletadas = todasParticipaciones.stream()
            .filter(ParticipacionDesafio::isCompletado)
            .collect(Collectors.toList());
        
        // Calcular puntos
        int puntosTotales = calcularPuntosTotales(usuarioId);
        
        // Obtener insignias
        List<String> insignias = obtenerInsigniasUsuario(usuarioId);
        
        // Datos adicionales
        long totalDesafios = todasParticipaciones.size();
        long desafiosCompletados = participacionesCompletadas.size();
        double tasaComplecion = totalDesafios > 0 ? 
            ((double) desafiosCompletados / totalDesafios) * 100 : 0;
        
        // Popular el resumen
        resumen.put("puntosTotales", puntosTotales);
        resumen.put("insignias", insignias);
        resumen.put("totalDesafios", totalDesafios);
        resumen.put("desafiosCompletados", desafiosCompletados);
        resumen.put("tasaComplecion", Math.round(tasaComplecion * 100.0) / 100.0); // Redondear a 2 decimales
        
        return resumen;
    }

    /**
     * Método auxiliar para validar un criterio específico.
     */
    private boolean validarCriterio(String usuarioId, Desafio desafio, ParticipacionDesafio participacion, Desafio.CriterioComplecion criterio) {
        String descripcionCriterio = criterio.getDescripcion().toLowerCase(); // Normalizar para comparación
        System.out.println("Validando criterio para usuario [" + usuarioId + "]: " + descripcionCriterio);
        
        // Determinar el tipo de criterio si no está establecido explícitamente
        String tipoCriterio = criterio.getTipo();
        if (tipoCriterio == null || "GENERICO".equals(tipoCriterio)) {
            // Inferir tipo basado en la descripción
            if (descripcionCriterio.contains("unirse al proyecto") || 
                descripcionCriterio.contains("participar en proyecto") ||
                descripcionCriterio.contains("formar parte del proyecto")) {
                tipoCriterio = "UNIRSE_PROYECTO";
            } else if (descripcionCriterio.contains("participar en el foro") || 
                      descripcionCriterio.contains("comentar en el foro") || 
                      descripcionCriterio.contains("publicar en el foro")) {
                tipoCriterio = "COMENTAR_FORO";
            } else if (descripcionCriterio.contains("completar tarea") || 
                      descripcionCriterio.contains("realizar tarea")) {
                tipoCriterio = "COMPLETAR_TAREA";
            }
        }
        
        // Obtener proyecto asociado al desafío
        Proyecto proyecto = proyectoServicio.obtenerProyectoPorId(desafio.getProyectoId()).orElse(null);
        if (proyecto == null) {
            System.out.println("ERROR: El proyecto asociado al desafío no existe");
            return false; // El proyecto no existe
        }
        
        // Realizar validación según el tipo de criterio
        switch (tipoCriterio) {
            case "UNIRSE_PROYECTO":
                System.out.println("Verificando participación en proyecto [" + proyecto.getId() + "]");
                boolean esParticipante = proyectoServicio.verificarMiembroProyecto(proyecto.getId(), usuarioId);
                System.out.println(esParticipante ? "ÉXITO: Usuario participa en el proyecto" : "ERROR: Usuario NO participa en el proyecto");
                return esParticipante;
                
            case "COMENTAR_FORO":
                // Obtener el foro del proyecto
                String foroId = proyecto.getForoId();
                if (foroId == null || foroId.isEmpty()) {
                    System.out.println("ERROR: El proyecto no tiene foro asociado");
                    return false; // El proyecto no tiene foro
                }
                
                System.out.println("Verificando comentarios en foro [" + foroId + "]");
                
                // Validación más estricta: verificar número mínimo de comentarios requeridos
                int comentariosRequeridos = criterio.getMetaRequerida() > 0 ? criterio.getMetaRequerida() : 1;
                int comentariosUsuario = foroServicio.contarComentariosUsuario(
                    foroId, 
                    usuarioId, 
                    desafio.getFechaInicio() // Contar solo comentarios hechos después del inicio del desafío
                );
                
                boolean cumpleRequisitoComentarios = comentariosUsuario >= comentariosRequeridos;
                System.out.println("Comentarios del usuario: " + comentariosUsuario + "/" + comentariosRequeridos +
                             (cumpleRequisitoComentarios ? " (CUMPLE)" : " (NO CUMPLE)"));
                
                return cumpleRequisitoComentarios;
                
            case "COMPLETAR_TAREA":
                System.out.println("ADVERTENCIA: Validación de tareas no implementada completamente");
                // Implementación pendiente para verificar tareas completadas
                return false;
                
            default:
                System.out.println("ADVERTENCIA: Tipo de criterio no reconocido: " + tipoCriterio);
                return false; // Tipo de criterio no reconocido, siendo estrictos consideramos no cumplido
        }
    }

    /**
     * Extrae el ID del foro desde la descripción del criterio.
     * Busca patrones como "foro id: XXX", "foro: XXX", "foroId: XXX"
     * 
     * @param descripcion Descripción del criterio
     * @return ID del foro o null si no se encuentra
     */
    private String extraerForoIdDesdeDescripcion(String descripcion) {
        // Patrones comunes para identificar IDs de foro en descripciones
        String[] patrones = {"foro id:", "foro:", "foroid:", "id foro:", "id del foro:"};
        
        for (String patron : patrones) {
            if (descripcion.contains(patron)) {
                try {
                    int inicio = descripcion.indexOf(patron) + patron.length();
                    // Buscar hasta el siguiente espacio o final de cadena
                    int fin = descripcion.indexOf(" ", inicio);
                    if (fin == -1) fin = descripcion.length();
                    
                    return descripcion.substring(inicio, fin).trim();
                } catch (Exception e) {
                    // Error al extraer, continuar con el siguiente patrón
                    continue;
                }
            }
        }
        
        return null; // No se encontró ningún patrón de ID
    }

    public ParticipacionDesafio guardar(ParticipacionDesafio participacion) {
        // Considerar si la lógica de unicidad debe ir aquí también al CREAR una participación.
        // El @CompoundIndex ya previene duplicados (usuarioId, desafioId) a nivel DB.
        // Podríamos añadir una verificación aquí para dar un error más amigable antes de la DB.
        // if (!participacionDesafioRepositorio.findByUsuarioIdAndDesafioId(participacion.getUsuarioId(), participacion.getDesafioId()).isEmpty()) {
        //    throw new IllegalStateException("El usuario ya participa en este desafío.");
        // }
        return participacionDesafioRepositorio.save(participacion);
    }
    
    public void eliminar(String id) {
        participacionDesafioRepositorio.deleteById(id);
    }

    /**
     * Valida si un criterio específico ha sido cumplido por un usuario
     */
    public boolean validarCriterio(String participacionId, String criterioDescripcion) {
        Optional<ParticipacionDesafio> participacionOpt = participacionDesafioRepositorio.findById(participacionId);
        if (!participacionOpt.isPresent()) {
            return false; // La participación no existe
        }
        
        ParticipacionDesafio participacion = participacionOpt.get();
        Desafio desafio = desafioServicio.obtenerPorId(participacion.getDesafioId()).orElse(null);
        
        if (desafio == null) {
            return false; // El desafío no existe
        }
        
        // Buscar el criterio en el desafío
        Optional<Desafio.CriterioComplecion> criterioOpt = desafio.getCriterios().stream()
            .filter(c -> c.getDescripcion().equals(criterioDescripcion))
            .findFirst();
        
        if (!criterioOpt.isPresent()) {
            return false; // El criterio no existe
        }
        
        Desafio.CriterioComplecion criterio = criterioOpt.get();
        String usuarioId = participacion.getUsuarioId();
        
        // Utilizar el método privado para la validación real
        return validarCriterio(usuarioId, desafio, participacion, criterio);
    }

    /**
     * Obtiene participaciones de un usuario específico con paginación
     * 
     * @param usuarioId ID del usuario
     * @param pageable Objeto de paginación
     * @return Página de participaciones del usuario
     */
    public Page<ParticipacionDesafio> obtenerPorUsuarioPaginado(String usuarioId, Pageable pageable) {
        List<ParticipacionDesafio> participaciones = participacionDesafioRepositorio.findByUsuarioId(usuarioId);
        
        // Implementar paginación manual
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), participaciones.size());
        
        // Verificar que los índices sean válidos
        if (start > participaciones.size()) {
            return Page.empty(pageable);
        }
        
        List<ParticipacionDesafio> participacionesPaginadas = participaciones.subList(start, end);
        return new PageImpl<>(participacionesPaginadas, pageable, participaciones.size());
    }
    
    /**
     * Obtiene participaciones en un desafío específico con paginación
     * 
     * @param desafioId ID del desafío
     * @param pageable Objeto de paginación
     * @return Página de participaciones en el desafío
     */
    public Page<ParticipacionDesafio> obtenerPorDesafioPaginado(String desafioId, Pageable pageable) {
        List<ParticipacionDesafio> participaciones = participacionDesafioRepositorio.findByDesafioId(desafioId);
        
        // Implementar paginación manual
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), participaciones.size());
        
        // Verificar que los índices sean válidos
        if (start > participaciones.size()) {
            return Page.empty(pageable);
        }
        
        List<ParticipacionDesafio> participacionesPaginadas = participaciones.subList(start, end);
        return new PageImpl<>(participacionesPaginadas, pageable, participaciones.size());
    }
} 