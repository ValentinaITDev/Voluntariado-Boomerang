package com.Impulso.Alcambio.Modelo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "proyectos")
public class Proyecto {
    @Id
    private String id;
    
    private String nombre;
    private String descripcion;
    private String imagenUrl;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaExpiracion;
    private EstadoProyecto estado;

    private List<ParticipanteInfo> participantes = new ArrayList<>();
    private List<DesafioProyecto> desafios = new ArrayList<>();

    public enum EstadoProyecto {
        ACTIVO, EXPIRADO, COMPLETADO, CANCELADO
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

    public static class ParticipanteInfo {
        private String usuarioId;
        private String nombre;
        private RolParticipante rol;
        private LocalDateTime fechaUnion;
        private List<Contribucion> contribuciones = new ArrayList<>();

        public enum RolParticipante {
            LIDER, COORDINADOR, MIEMBRO
        }

        public ParticipanteInfo(String usuarioId, String nombre, RolParticipante rol) {
            this.usuarioId = usuarioId;
            this.nombre = nombre;
            this.rol = rol;
            this.fechaUnion = LocalDateTime.now();
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

        public RolParticipante getRol() {
            return rol;
        }

        public void setRol(RolParticipante rol) {
            this.rol = rol;
        }

        public LocalDateTime getFechaUnion() {
            return fechaUnion;
        }

        public void setFechaUnion(LocalDateTime fechaUnion) {
            this.fechaUnion = fechaUnion;
        }

        public List<Contribucion> getContribuciones() {
            return contribuciones;
        }

        public void setContribuciones(List<Contribucion> contribuciones) {
            this.contribuciones = contribuciones;
        }

       
        
    }

    public static class Contribucion {
        private TipoContribucion tipo;
        private String descripcion;
        private LocalDateTime fecha;
        private int impacto;

        public enum TipoContribucion {
            TAREA, IDEA, RECURSO, COORDINACION
        }

        public Contribucion(TipoContribucion tipo, String descripcion, int impacto) {
            this.tipo = tipo;
            this.descripcion = descripcion;
            this.fecha = LocalDateTime.now();
            this.impacto = impacto;
        }

        public TipoContribucion getTipo() {
            return tipo;
        }

        public void setTipo(TipoContribucion tipo) {
            this.tipo = tipo;
        }

        public String getDescripcion() {
            return descripcion;
        }

        public void setDescripcion(String descripcion) {
            this.descripcion = descripcion;
        }

        public LocalDateTime getFecha() {
            return fecha;
        }

        public void setFecha(LocalDateTime fecha) {
            this.fecha = fecha;
        }

        public int getImpacto() {
            return impacto;
        }

        public void setImpacto(int impacto) {
            this.impacto = impacto;
        }

        // Getters y setters básicos...
    }

    public static class DesafioProyecto {
        private String id;
        private String nombre;
        private String descripcion;
        private TipoDesafio tipo;
        private LocalDateTime fechaInicio;
        private LocalDateTime fechaFin;
        private int puntosRecompensa;
        private List<CriterioComplecion> criterios = new ArrayList<>();
        private List<ParticipanteDesafio> participantes = new ArrayList<>();

        public enum TipoDesafio {
            INDIVIDUAL, GRUPAL, COMPETITIVO
        }

        public DesafioProyecto(String nombre, String descripcion, TipoDesafio tipo) {
            this.id = UUID.randomUUID().toString();
            this.nombre = nombre;
            this.descripcion = descripcion;
            this.tipo = tipo;
            this.fechaInicio = LocalDateTime.now();
        }

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
        
        public List<ParticipanteDesafio> getParticipantes() {
            return participantes;
        }
        
        public void setParticipantes(List<ParticipanteDesafio> participantes) {
            this.participantes = participantes;
        }
    }

    public static class CriterioComplecion {
        private String descripcion;
        private int metaRequerida;
        private boolean requiereValidacion;

        public CriterioComplecion(String descripcion, int metaRequerida) {
            this.descripcion = descripcion;
            this.metaRequerida = metaRequerida;
            this.requiereValidacion = false;
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
    }

    public static class ParticipanteDesafio {
        private String usuarioId;
        private String nombre;
        private int progreso;
        private boolean completado;
        private LocalDateTime fechaCompletado;

        public ParticipanteDesafio(String usuarioId, String nombre) {
            this.usuarioId = usuarioId;
            this.nombre = nombre;
            this.progreso = 0;
            this.completado = false;
        }

        public void actualizarProgreso(int progreso) {
            this.progreso = progreso;
            if (progreso >= 100 && !completado) {
                this.completado = true;
                this.fechaCompletado = LocalDateTime.now();
            }
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
            this.progreso = progreso;
        }
        
        public boolean isCompletado() {
            return completado;
        }
        
        public void setCompletado(boolean completado) {
            this.completado = completado;
        }
        
        public LocalDateTime getFechaCompletado() {
            return fechaCompletado;
        }
        
        public void setFechaCompletado(LocalDateTime fechaCompletado) {
            this.fechaCompletado = fechaCompletado;
        }
    }

    // Métodos de utilidad
    public void agregarParticipante(ParticipanteInfo participante) {
        this.participantes.add(participante);
    }

    public void agregarDesafio(DesafioProyecto desafio) {
        this.desafios.add(desafio);
    }

    public boolean isActivo() {
        return estado == EstadoProyecto.ACTIVO && 
               (fechaExpiracion == null || fechaExpiracion.isAfter(LocalDateTime.now()));
    }

    // Métodos para obtener estadísticas
    public int getTotalParticipantes() {
        return participantes.size();
    }

    public int getDesafiosCompletados() {
        int completados = 0;
        for (DesafioProyecto desafio : desafios) {
            for (ParticipanteDesafio participante : desafio.getParticipantes()) {
                if (participante.isCompletado()) {
                    completados++;
                }
            }
        }
        return completados;
    }

    public int getImpactoTotal() {
        return participantes.stream()
                .flatMap(p -> p.getContribuciones().stream())
                .mapToInt(Contribucion::getImpacto)
                .sum();
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

    public List<ParticipanteInfo> getParticipantes() {
        return participantes;
    }

    public void setParticipantes(List<ParticipanteInfo> participantes) {
        this.participantes = participantes;
    }

    public List<DesafioProyecto> getDesafios() {
        return desafios;
    }

    public void setDesafios(List<DesafioProyecto> desafios) {
        this.desafios = desafios;
    }
}