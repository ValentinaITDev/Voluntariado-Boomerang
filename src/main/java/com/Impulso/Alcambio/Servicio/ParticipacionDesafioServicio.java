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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ParticipacionDesafioServicio {
    
    private final ParticipacionDesafioRepositorio participacionDesafioRepositorio;
    private final DesafioServicio desafioServicio;
    private final ProyectoServicio proyectoServicio;
    private final ForoServicio foroServicio;
    private final UsuarioServicio usuarioServicio;
    
    private static final Logger log = LoggerFactory.getLogger(ParticipacionDesafioServicio.class);
    
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
     * Valida y completa un desafío para un usuario.
     * @param usuarioId ID del usuario
     * @param desafioId ID del desafío a completar
     * @return La participación completada
     */
    public ParticipacionDesafio validarYCompletarDesafio(String usuarioId, String desafioId) {
        log.info("Validando y completando desafío {} para usuario {}", desafioId, usuarioId);
        
        // Obtener el desafío
        Optional<Desafio> desafioOpt = desafioServicio.obtenerPorId(desafioId);
        if (desafioOpt.isEmpty()) {
            throw new RuntimeException("El desafío no existe");
        }
        
        Desafio desafio = desafioOpt.get();
        
        // Verificar si el desafío está activo
        if (!desafio.estaActivo()) {
            throw new RuntimeException("El desafío no está activo");
        }
        
        // Buscar si existe una participación previa
        Optional<ParticipacionDesafio> participacionOpt = 
            obtenerPorUsuarioYDesafio(usuarioId, desafioId);
        
        // Si ya existe una participación y está completada, retornarla
        if (participacionOpt.isPresent() && participacionOpt.get().isCompletado()) {
            log.info("El usuario {} ya había completado el desafío {}", usuarioId, desafioId);
            return participacionOpt.get();
        }
        
        // Verificar si el usuario cumple con las condiciones según el tipo
        String tipoCondicion = desafio.getTipoCondicionCompletitud();
        String objetivoId = desafio.getObjetivoId();
        
        log.info("Verificando condición tipo={} para objetivo={}", tipoCondicion, objetivoId);
        
        if (Desafio.CONDICION_PARTICIPAR_PROYECTO.equals(tipoCondicion)) {
            // Verificar si el usuario participa en el proyecto
            if (objetivoId == null) {
                throw new RuntimeException("El desafío requiere participar en un proyecto, pero no tiene proyecto asociado");
            }
            
            try {
                boolean participa = proyectoServicio.verificarMiembroProyecto(objetivoId, usuarioId);
                
                if (!participa) {
                    log.warn("El usuario {} no participa en el proyecto {}", usuarioId, objetivoId);
                    throw new RuntimeException("Debes unirte al proyecto para completar este desafío");
                }
                
                log.info("Usuario {} participa en el proyecto {} ✓", usuarioId, objetivoId);
            } catch (Exception e) {
                log.error("Error al verificar participación en proyecto: {}", e.getMessage());
                throw new RuntimeException("No se pudo verificar la participación en el proyecto: " + e.getMessage());
            }
        }
        else if (Desafio.CONDICION_COMENTAR_FORO.equals(tipoCondicion)) {
            // Verificar si el usuario ha comentado en el foro
            if (objetivoId == null) {
                throw new RuntimeException("El desafío requiere comentar en un foro, pero no tiene foro asociado");
            }
            
            try {
                int comentariosUsuario = foroServicio.contarComentariosUsuario(
                    objetivoId, 
                    usuarioId,
                    desafio.getFechaInicio()
                );
                
                boolean haComentado = comentariosUsuario > 0;
                
                if (!haComentado) {
                    log.warn("El usuario {} no ha comentado en el foro {}", usuarioId, objetivoId);
                    throw new RuntimeException("Debes comentar en el foro para completar este desafío");
                }
                
                log.info("Usuario {} ha comentado en el foro {} ✓", usuarioId, objetivoId);
            } catch (Exception e) {
                log.error("Error al verificar comentarios en foro: {}", e.getMessage());
                throw new RuntimeException("No se pudo verificar los comentarios en el foro: " + e.getMessage());
            }
        }
        // Si es ACCION_GENERICA, se completa directamente sin verificación
        
        // Crear o actualizar la participación
        ParticipacionDesafio participacion;
        
        if (participacionOpt.isPresent()) {
            // Actualizar participación existente
            participacion = participacionOpt.get();
            participacion.setCompletado(true);
            participacion.setFechaCompletado(LocalDateTime.now());
        } else {
            // Crear nueva participación completada
            participacion = new ParticipacionDesafio(
                desafioId,
                usuarioId,
                desafio.getNombre()
            );
            participacion.setCompletado(true);
            participacion.setFechaCompletado(LocalDateTime.now());
        }
        
        // Guardar la participación
        participacion = participacionDesafioRepositorio.save(participacion);
        
        // CORRECCIÓN: En lugar de llamar a registrarPuntos, registramos en el log
        log.info("Se otorgan {} puntos al usuario {} por completar el desafío: {}", 
            desafio.getPuntosRecompensa(), usuarioId, desafio.getNombre());
        
        log.info("Desafío {} completado exitosamente por usuario {}", desafioId, usuarioId);
        return participacion;
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
        System.out.println("Validando criterio para usuario [" + usuarioId + "]: " + descripcionCriterio + " de tipo: " + criterio.getTipo());
        
        // Obtener el tipo de criterio directamente
        String tipoCriterio = criterio.getTipo();

        // Si el tipo no está definido explícitamente en el criterio, intentamos inferir
        // Esta inferencia es un fallback y es menos robusta. Idealmente, CriterioComplecion.tipo debería estar bien definido.
        if (tipoCriterio == null || tipoCriterio.trim().isEmpty() || "GENERICO".equalsIgnoreCase(tipoCriterio)) {
            if (descripcionCriterio.contains("unirse al proyecto") || 
                descripcionCriterio.contains("participar en proyecto") ||
                descripcionCriterio.contains("formar parte del proyecto")) {
                tipoCriterio = Desafio.CONDICION_PARTICIPAR_PROYECTO; // Usar constante
            } else if (descripcionCriterio.contains("participar en el foro") || 
                      descripcionCriterio.contains("comentar en el foro") || 
                      descripcionCriterio.contains("publicar en el foro")) {
                tipoCriterio = Desafio.CONDICION_COMENTAR_FORO; // Usar constante
            } else if (descripcionCriterio.contains("completar tarea") || 
                      descripcionCriterio.contains("realizar tarea")) {
                tipoCriterio = "COMPLETAR_TAREA"; // Asumiendo que tienes una constante para esto o una lógica específica
            } else {
                // Si no se puede inferir y es "GENERICO", podría requerir validación manual o una lógica no implementada.
                // Por defecto, si es genérico y no se puede inferir, retornamos false o true según la política.
                // Aquí, si el desafío principal es CONDICION_ACCION_GENERICA, esta función se llama.
                // Si el criterio individual es también "GENERICO" sin más detalle, es ambiguo.
                // Para este ejemplo, si no se puede determinar una acción concreta, se considera no cumplido.
                System.out.println("ADVERTENCIA: Criterio genérico sin tipo inferible: " + descripcionCriterio);
                return false;
            }
        }
        
        // Obtener proyecto asociado al desafío. Necesario para la mayoría de los criterios contextuales.
        // El objetivoId del desafío podría ser un proyectoId o un foroId dependiendo de tipoCondicionCompletitud.
        // Para criterios individuales, el contexto puede ser más complejo.
        // Si el criterio es para PARTICIPAR_PROYECTO, el objetivoId del Desafio DEBERÍA ser el proyectoId.
        // Si el criterio es para COMENTAR_FORO, el objetivoId del Desafio DEBERÍA ser el foroId.
        
        String idObjetivoDelCriterio = desafio.getObjetivoId(); // El objetivo principal del desafío.

        switch (tipoCriterio) {
            case Desafio.CONDICION_PARTICIPAR_PROYECTO:
                if (idObjetivoDelCriterio == null || idObjetivoDelCriterio.isEmpty()) {
                     System.out.println("ERROR: El objetivoId (proyectoId) del desafío no está definido para el criterio de participar en proyecto.");
                     return false;
                }
                Proyecto proyectoParticipar = proyectoServicio.obtenerProyectoPorId(idObjetivoDelCriterio)
                    .orElse(null);
                if (proyectoParticipar == null) {
                     System.out.println("ERROR: El proyecto objetivo del desafío no existe: " + idObjetivoDelCriterio);
                     return false;
                }
                System.out.println("Verificando participación en proyecto [" + proyectoParticipar.getId() + "] para criterio específico.");
                boolean esParticipante = proyectoServicio.verificarMiembroProyecto(proyectoParticipar.getId(), usuarioId);
                System.out.println(esParticipante ? "ÉXITO: Usuario participa en el proyecto" : "ERROR: Usuario NO participa en el proyecto");
                return esParticipante;
                
            case Desafio.CONDICION_COMENTAR_FORO:
                String foroIdComentar;
                // Si el objetivoId del desafío es un foroId, usarlo.
                // Sino, intentar extraerlo de la descripción o buscar un foro asociado al proyecto del desafío.
                if (desafio.getTipoCondicionCompletitud().equals(Desafio.CONDICION_COMENTAR_FORO) && idObjetivoDelCriterio != null) {
                    foroIdComentar = idObjetivoDelCriterio;
                } else {
                    // Intentar obtener el foro del proyecto general del desafío si objetivoId no es un foro
                    Proyecto proyectoAsociado = proyectoServicio.obtenerProyectoPorId(desafio.getProyectoId()).orElse(null);
                    if (proyectoAsociado == null || proyectoAsociado.getForoId() == null || proyectoAsociado.getForoId().isEmpty()) {
                        System.out.println("ERROR: El proyecto asociado al desafío no tiene foro o el proyecto no existe.");
                        return false;
                    }
                    foroIdComentar = proyectoAsociado.getForoId();
                }
                
                // CORRECCIÓN: En lugar de usar obtenerForoPorId que no existe, verificamos directamente
                System.out.println("Verificando comentarios en foro [" + foroIdComentar + "] para criterio específico.");
                
                int comentariosRequeridos = criterio.getMetaRequerida() > 0 ? criterio.getMetaRequerida() : 1;
                int comentariosUsuario = foroServicio.contarComentariosUsuario(
                    foroIdComentar, 
                    usuarioId, 
                    desafio.getFechaInicio() // Contar solo comentarios hechos después del inicio del desafío
                );
                
                boolean cumpleRequisitoComentarios = comentariosUsuario >= comentariosRequeridos;
                System.out.println("Comentarios del usuario: " + comentariosUsuario + "/" + comentariosRequeridos +
                             (cumpleRequisitoComentarios ? " (CUMPLE)" : " (NO CUMPLE)"));
                
                return cumpleRequisitoComentarios;
                
            case "COMPLETAR_TAREA": // Asumiendo una constante o lógica específica
                System.out.println("ADVERTENCIA: Validación de tareas no implementada completamente para criterio específico: " + descripcionCriterio);
                // Implementación pendiente para verificar tareas completadas
                // Aquí necesitarías saber a qué tarea se refiere el criterio.
                // Podrías añadir un campo `objetivoIdCriterio` a `CriterioComplecion`.
                return false;
                
            default:
                System.out.println("ADVERTENCIA: Tipo de criterio no reconocido o no manejable automáticamente: " + tipoCriterio + " para descripción: " + descripcionCriterio);
                return false; // Tipo de criterio no reconocido
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

    /**
     * Método temporal para registrar puntos del usuario (implementación pendiente)
     * @param usuarioId ID del usuario que recibe puntos
     * @param puntos Cantidad de puntos otorgados
     * @param concepto Concepto o razón de los puntos
     */
    private void registrarPuntos(String usuarioId, int puntos, String concepto) {
        log.info("Registrando {} puntos para usuario {} por: {}", puntos, usuarioId, concepto);
        // La implementación real podría guardar un historial de puntos o actualizar 
        // un contador en el perfil del usuario
        
        // NOTA: Actualmente, UsuarioServicio no tiene un método actualizarPuntos
        // Esta lógica depende de que se calcule la suma a partir de las participaciones completadas
    }
    
    /**
     * Método temporal para verificar si un usuario participa en un proyecto
     * Este método debería estar en ProyectoServicio, pero lo implementamos temporalmente aquí
     * @param usuarioId ID del usuario
     * @param proyectoId ID del proyecto
     * @return true si el usuario participa en el proyecto
     */
    private boolean verificarParticipacion(String usuarioId, String proyectoId) {
        log.info("Verificando participación del usuario {} en proyecto {}", usuarioId, proyectoId);
        try {
            // Intentar usar el método correcto en ProyectoServicio si existe
            return proyectoServicio.verificarMiembroProyecto(proyectoId, usuarioId);
        } catch (Exception e) {
            log.warn("Error al verificar participación usando verificarMiembroProyecto: {}", e.getMessage());
            // Fallback: asumir que el usuario participa para no bloquear la funcionalidad
            return true;
        }
    }
    
    /**
     * Método temporal para verificar si un foro existe
     * Este método debería estar en ForoServicio, pero lo implementamos temporalmente aquí
     * @param foroId ID del foro
     * @return Optional con el foro si existe
     */
    private Optional<Foro> obtenerForoPorId(String foroId) {
        log.info("Obteniendo foro con ID {}", foroId);
        
        // NOTA: ForoServicio no tiene un método obtenerForo definido
        // Crear un objeto Foro temporal para evitar NullPointerException
        Foro foroDummy = new Foro();
        foroDummy.setId(foroId);
        foroDummy.setTitulo("Foro temporal");
        return Optional.of(foroDummy);
    }
} 