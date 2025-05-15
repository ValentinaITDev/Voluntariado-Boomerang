package com.Impulso.Alcambio.Modelo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    // Constantes para el tipo de condición de completitud
    public static final String CONDICION_PARTICIPAR_PROYECTO = "PARTICIPAR_PROYECTO";
    public static final String CONDICION_COMENTAR_FORO = "COMENTAR_FORO";
    public static final String CONDICION_ACCION_GENERICA = "ACCION_GENERICA"; // Por defecto, o para completitud manual/otras lógicas

    private String tipoCondicionCompletitud; // Define cómo se completa el desafío (usa las constantes de arriba)
    private String objetivoId;               // ID del proyecto o foro objetivo, según tipoCondicionCompletitud

    public enum TipoDesafio {
        INDIVIDUAL, GRUPAL, COMPETITIVO
    }

    public Desafio() {
        this.fechaInicio = LocalDateTime.now();
        this.completado = false;
        this.tipoCondicionCompletitud = CONDICION_ACCION_GENERICA; // Valor por defecto
    }

    public Desafio(String proyectoId, String nombre, String descripcion, TipoDesafio tipo) {
        this();
        this.proyectoId = proyectoId;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.tipo = tipo;
    }

    public Desafio(String nombre, String descripcion, TipoDesafio tipo, int puntosRecompensa, 
                   String tipoCondicionCompletitud, String objetivoId, String proyectoAsociadoId) {
        this();
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.tipo = tipo;
        this.puntosRecompensa = puntosRecompensa;
        setTipoCondicionCompletitud(tipoCondicionCompletitud);
        this.objetivoId = objetivoId;
        this.proyectoId = proyectoAsociadoId;
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

    public String getTipoCondicionCompletitud() {
        return tipoCondicionCompletitud;
    }

    public void setTipoCondicionCompletitud(String tipoCondicionCompletitud) {
        if (CONDICION_PARTICIPAR_PROYECTO.equals(tipoCondicionCompletitud) ||
            CONDICION_COMENTAR_FORO.equals(tipoCondicionCompletitud) ||
            CONDICION_ACCION_GENERICA.equals(tipoCondicionCompletitud)) {
            this.tipoCondicionCompletitud = tipoCondicionCompletitud;
        } else {
            this.tipoCondicionCompletitud = CONDICION_ACCION_GENERICA;
        }
    }

    public String getObjetivoId() {
        return objetivoId;
    }

    public void setObjetivoId(String objetivoId) {
        this.objetivoId = objetivoId;
    }

    // Métodos de utilidad
    public void agregarCriterio(CriterioComplecion criterio) {
        this.criterios.add(criterio);
    }

    public boolean estaActivo() {
        return (fechaFin == null || fechaFin.isAfter(LocalDateTime.now()));
    }
} 