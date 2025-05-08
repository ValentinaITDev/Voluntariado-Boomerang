package com.Impulso.Alcambio.Modelo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "participaciones_desafio")
@CompoundIndex(name = "usuario_desafio_idx", def = "{'usuarioId': 1, 'desafioId': 1}", unique = true)
public class ParticipacionDesafio {
    @Id
    private String id;
    
    @Indexed
    private String desafioId;
    
    @Indexed
    private String usuarioId;
    
    private String nombre;  // Nombre del desafío para consultas rápidas
    private int progreso;
    private boolean completado;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaCompletado;
    private List<String> actividadesCompletadas = new ArrayList<>();

    // Constructor vacío requerido por MongoDB
    public ParticipacionDesafio() {
        this.fechaInicio = LocalDateTime.now();
        this.progreso = 0;
        this.completado = false;
    }

    public ParticipacionDesafio(String desafioId, String usuarioId, String nombre) {
        this();
        this.desafioId = desafioId;
        this.usuarioId = usuarioId;
        this.nombre = nombre;
    }

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDesafioId() {
        return desafioId;
    }

    public void setDesafioId(String desafioId) {
        this.desafioId = desafioId;
    }

    public String getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public int getProgreso() {
        return progreso;
    }

    public void setProgreso(int progreso) {
        if (progreso < 0 || progreso > 100) {
            throw new IllegalArgumentException("El progreso debe estar entre 0 y 100.");
        }
        this.progreso = progreso;
        if (progreso == 100 && !completado) {
            setCompletado(true);
        }
    }

    public boolean isCompletado() {
        return completado;
    }

    public void setCompletado(boolean completado) {
        this.completado = completado;
        if (completado && this.fechaCompletado == null) {
            this.fechaCompletado = LocalDateTime.now();
        }
    }

    public LocalDateTime getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDateTime fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDateTime getFechaCompletado() {
        return fechaCompletado;
    }

    public void setFechaCompletado(LocalDateTime fechaCompletado) {
        this.fechaCompletado = fechaCompletado;
    }

    public List<String> getActividadesCompletadas() {
        return Collections.unmodifiableList(actividadesCompletadas);
    }

    public void setActividadesCompletadas(List<String> actividadesCompletadas) {
        this.actividadesCompletadas = new ArrayList<>(actividadesCompletadas); // para evitar referencias mutables externas
    }
    
    // Métodos de utilidad
    public void agregarActividadCompletada(String actividad) {
        if (actividad != null && !actividad.trim().isEmpty() && !this.actividadesCompletadas.contains(actividad)) {
            this.actividadesCompletadas.add(actividad);
        }
    }
    
    public void eliminarActividadCompletada(String actividad) {
        if (actividad != null) {
            this.actividadesCompletadas.remove(actividad);
        }
    }
} 