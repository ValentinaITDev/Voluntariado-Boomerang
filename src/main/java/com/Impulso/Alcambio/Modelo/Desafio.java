package com.Impulso.Alcambio.Modelo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "desafios")
public class Desafio {
    @Id
    private String id;
    
    private String proyectoId;
    private String nombre;
    private String descripcion;
    private TipoDesafio tipo;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private int puntosRecompensa;
    private List<CriterioComplecion> criterios = new ArrayList<>();
    private boolean completado;

    public enum TipoDesafio {
        INDIVIDUAL, GRUPAL, COMPETITIVO
    }

    public Desafio() {
        this.fechaInicio = LocalDateTime.now();
        this.completado = false;
    }

    public Desafio(String proyectoId, String nombre, String descripcion, TipoDesafio tipo) {
        this();
        this.proyectoId = proyectoId;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.tipo = tipo;
    }

    public static class CriterioComplecion {
        private String descripcion;
        private int metaRequerida;
        private boolean requiereValidacion;
        private String tipo;

        public CriterioComplecion(String descripcion, int metaRequerida) {
            this.descripcion = descripcion;
            this.metaRequerida = metaRequerida;
            this.requiereValidacion = false;
            this.tipo = "GENERICO";
        }
        
        public CriterioComplecion(String descripcion, int metaRequerida, String tipo) {
            this.descripcion = descripcion;
            this.metaRequerida = metaRequerida;
            this.requiereValidacion = false;
            this.tipo = tipo;
        }
        
        public String getDescripcion() {
            return descripcion;
        }
        
        public void setDescripcion(String descripcion) {
            this.descripcion = descripcion;
        }
        
        public int getMetaRequerida() {
            return metaRequerida;
        }
        
        public void setMetaRequerida(int metaRequerida) {
            this.metaRequerida = metaRequerida;
        }
        
        public boolean isRequiereValidacion() {
            return requiereValidacion;
        }
        
        public void setRequiereValidacion(boolean requiereValidacion) {
            this.requiereValidacion = requiereValidacion;
        }
        
        public String getTipo() {
            return tipo;
        }
        
        public void setTipo(String tipo) {
            this.tipo = tipo;
        }
    }

    // Getters y setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProyectoId() {
        return proyectoId;
    }

    public void setProyectoId(String proyectoId) {
        this.proyectoId = proyectoId;
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

    public TipoDesafio getTipo() {
        return tipo;
    }

    public void setTipo(TipoDesafio tipo) {
        this.tipo = tipo;
    }

    public LocalDateTime getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDateTime fechaInicio) {
        this.fechaInicio = fechaInicio;
    }
    
    public LocalDateTime getFechaFin() {
        return fechaFin;
    }
    
    public void setFechaFin(LocalDateTime fechaFin) {
        this.fechaFin = fechaFin;
    }
    
    public int getPuntosRecompensa() {
        return puntosRecompensa;
    }
    
    public void setPuntosRecompensa(int puntosRecompensa) {
        this.puntosRecompensa = puntosRecompensa;
    }
    
    public List<CriterioComplecion> getCriterios() {
        return criterios;
    }
    
    public void setCriterios(List<CriterioComplecion> criterios) {
        this.criterios = criterios;
    }
    
    public boolean isCompletado() {
        return completado;
    }
    
    public void setCompletado(boolean completado) {
        this.completado = completado;
    }

    // Métodos de utilidad
    public void agregarCriterio(CriterioComplecion criterio) {
        this.criterios.add(criterio);
    }

    public boolean estaActivo() {
        return (fechaFin == null || fechaFin.isAfter(LocalDateTime.now()));
    }
} 