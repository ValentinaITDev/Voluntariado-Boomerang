package com.Impulso.Alcambio.Modelo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "proyectos")
public class Proyecto {
    @Id
    private String id;
    
    @Indexed
    private String nombre;
    
    @Indexed
    private String descripcion;
    
    @Indexed
    private String imagenUrl;
    
    @Indexed
    private LocalDateTime fechaCreacion;
    
    @Indexed
    private LocalDateTime fechaExpiracion;
    
    @Indexed
    private EstadoProyecto estado;
    
    @Indexed
    private String foroId; // ID del foro asociado al proyecto
    
    @Indexed
    private Integer limiteParticipantes; // Límite máximo de participantes
    
    @Indexed
    private Integer participantesActuales = 0; // Contador de participantes actuales

    @Indexed
    private List<String> desafioIds = new ArrayList<>();

    private Map<String, Object> cacheEstadisticas = new HashMap<>();

    public enum EstadoProyecto {
        ACTIVO, EXPIRADO, COMPLETADO, CANCELADO, COMPLETO // Añadido COMPLETO para cuando se alcanza el límite
    }

    public Proyecto() {
        this.fechaCreacion = LocalDateTime.now();
        this.estado = EstadoProyecto.ACTIVO;
    }

    public Proyecto(String nombre, String descripcion) {
        this();
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    // Métodos de utilidad
    public void agregarDesafioId(String desafioId) {
        if (!desafioIds.contains(desafioId)) {
            desafioIds.add(desafioId);
            cacheEstadisticas.clear();
        }
    }

    public void actualizarCacheEstadisticas(String key, Object value) {
        cacheEstadisticas.put(key, value);
    }

    public Object obtenerCacheEstadisticas(String key) {
        return cacheEstadisticas.get(key);
    }

    public boolean isActivo() {
        return estado == EstadoProyecto.ACTIVO && 
               !haExpirado() &&
               !haAlcanzadoLimiteParticipantes();
    }
    
    /**
     * Verifica si el proyecto ha expirado basado en su fecha de expiración
     * La verificación se realiza comparando la fecha de expiración con la fecha actual
     * Un proyecto ha expirado si su fecha de expiración es anterior a la fecha actual
     * 
     * @return true si el proyecto tiene fecha de expiración y ésta es anterior a la fecha actual
     */
    public boolean haExpirado() {
        LocalDateTime ahora = LocalDateTime.now();
        // Verificar si la fecha de expiración existe y es anterior a la fecha actual
        return fechaExpiracion != null && 
               fechaExpiracion.isBefore(ahora);
    }
    
    /**
     * Verifica si se ha alcanzado el límite de participantes del proyecto
     * @return true si se ha alcanzado el límite, false si aún hay espacios disponibles o no hay límite
     */
    public boolean haAlcanzadoLimiteParticipantes() {
        return limiteParticipantes != null && 
               participantesActuales != null && 
               participantesActuales >= limiteParticipantes;
    }
    
    /**
     * Incrementa el contador de participantes y verifica si se debe cerrar el proyecto
     * @return true si el participante pudo ser agregado, false si se alcanzó el límite
     */
    public boolean incrementarParticipantes() {
        if (haAlcanzadoLimiteParticipantes()) {
            return false;
        }
        
        if (participantesActuales == null) {
            participantesActuales = 0;
        }
        
        participantesActuales++;
        
        // Si después de incrementar se alcanzó el límite, cambiar estado a COMPLETO
        if (haAlcanzadoLimiteParticipantes()) {
            this.estado = EstadoProyecto.COMPLETO;
        }
        
        return true;
    }
    
    /**
     * Decrementa el contador de participantes y actualiza el estado si es necesario
     * @return true si el contador fue decrementado, false si ya estaba en cero
     */
    public boolean decrementarParticipantes() {
        if (participantesActuales == null || participantesActuales <= 0) {
            participantesActuales = 0;
            return false;
        }
        
        participantesActuales--;
        
        // Si el proyecto estaba completo pero ahora hay espacio, cambiarlo a activo
        if (this.estado == EstadoProyecto.COMPLETO && !haAlcanzadoLimiteParticipantes()) {
            this.estado = EstadoProyecto.ACTIVO;
        }
        
        return true;
    }

    // Getters y setters básicos
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getImagenUrl() {
        return imagenUrl;
    }

    public void setImagenUrl(String imagenUrl) {
        this.imagenUrl = imagenUrl;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getFechaExpiracion() {
        return fechaExpiracion;
    }

    public void setFechaExpiracion(LocalDateTime fechaExpiracion) {
        this.fechaExpiracion = fechaExpiracion;
    }

    public EstadoProyecto getEstado() {
        return estado;
    }

    public void setEstado(EstadoProyecto estado) {
        this.estado = estado;
    }

    public List<String> getDesafioIds() {
        return desafioIds;
    }

    public void setDesafioIds(List<String> desafioIds) {
        this.desafioIds = desafioIds;
    }

    public String getForoId() {
        return foroId;
    }

    public void setForoId(String foroId) {
        this.foroId = foroId;
    }
    
    public Integer getLimiteParticipantes() {
        return limiteParticipantes;
    }

    public void setLimiteParticipantes(Integer limiteParticipantes) {
        this.limiteParticipantes = limiteParticipantes;
    }

    public Integer getParticipantesActuales() {
        return participantesActuales;
    }

    public void setParticipantesActuales(Integer participantesActuales) {
        this.participantesActuales = participantesActuales;
    }
}