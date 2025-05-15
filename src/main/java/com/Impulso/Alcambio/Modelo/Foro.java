package com.Impulso.Alcambio.Modelo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Document(collection = "foro_posts")
@CompoundIndex(name = "titulo_etiquetas_idx", def = "{'titulo' : 1, 'etiquetas' : 1}")
public class Foro { 
    //ATRIBUTOS DEL FORO
    @Id
    private String id;
    
    @Indexed
    @NotBlank
    @Size(min = 5, max = 150)
    private String titulo;
    
    @Indexed
    @Field("etiquetas")
    private List<String> etiquetas = new ArrayList<>();
    
    // Cache de estadísticas del foro
    @Field("cache_estadisticas")
    private Map<String, Object> cacheEstadisticas = new HashMap<>();
    
    @NotBlank
    private String contenido;
    private LocalDateTime fechaCreacion;
    
    // Estado del foro
    private boolean activo = true;
    private boolean archivado = false;
    private boolean requiereModeracion = false;
    
    // Contadores de reportes
    private int contadorReportes = 0;
    @Field("reportes")
    private List<ReporteForo> reportes = new ArrayList<>();
    
    // Restricciones
    private boolean permiteComentarios = true;
    private boolean esPublico = true;
    
    // Clase para reportes
    public static class ReporteForo {
        private String id = UUID.randomUUID().toString();
        private String usuarioId;
        private String motivo;
        private LocalDateTime fechaReporte;
        private boolean resuelto = false;
        
        public ReporteForo() {
            this.fechaReporte = LocalDateTime.now();
        }
        
        public ReporteForo(String usuarioId, String motivo) {
            this();
            this.usuarioId = usuarioId;
            this.motivo = motivo;
        }
        
        // Getters y setters
        public String getId() {
            return id;
        }
        
        public void setId(String id) {
            this.id = id;
        }
        
        public String getUsuarioId() {
            return usuarioId;
        }
        
        public void setUsuarioId(String usuarioId) {
            this.usuarioId = usuarioId;
        }
        
        public String getMotivo() {
            return motivo;
        }
        
        public void setMotivo(String motivo) {
            this.motivo = motivo;
        }
        
        public LocalDateTime getFechaReporte() {
            return fechaReporte;
        }
        
        public void setFechaReporte(LocalDateTime fechaReporte) {
            this.fechaReporte = fechaReporte;
        }
        
        public boolean isResuelto() {
            return resuelto;
        }
        
        public void setResuelto(boolean resuelto) {
            this.resuelto = resuelto;
        }
    }
    
    // Información del autor embebida
    @Field("autor")
    private AutorInfo autor;
    
    public static class AutorInfo {
        //ATRIBUTOS DEL AUTOR
        private String usuarioId;
        private String nombre;
        private String imagenPerfil;

        public AutorInfo(String usuarioId, String nombre, String imagenPerfil) {
            this.usuarioId = usuarioId;
            this.nombre = nombre;
            this.imagenPerfil = imagenPerfil;
        }

        // GETTERS Y SETTERS DEL AUTOR
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

        public String getImagenPerfil() {
            return imagenPerfil;
        }

        public void setImagenPerfil(String imagenPerfil) {
            this.imagenPerfil = imagenPerfil;
        }
    }
    
    // COMENTARIOS ANIDADOS EMBEBIDOS (con un solo nivel de anidación para respuestas)
    @Field("comentarios")
    private List<Comentario> comentarios = new ArrayList<>();
    
    public static class Comentario {
        //ATRIBUTOS DEL COMENTARIO
        // Eliminamos @Id ya que es un documento embebido
        private String id;
        private String contenido;
        private LocalDateTime fechaCreacion;
        @Field("autor")
        private AutorInfo autor;
        @Field("respuestas")
        private List<Respuesta> respuestas = new ArrayList<>();
        private int likes = 0;
        @Field("usuarios_like")
        private Set<String> usuariosQueDieronLike = new HashSet<>();
        
        // Campos para moderación
        private boolean reportado = false;
        private boolean restringido = false;
        private String motivoRestriccion;
        private LocalDateTime fechaRestriccion;
        private String usuarioRestriccionId;
        @Field("reportes")
        private List<ReporteComentario> reportesComentario = new ArrayList<>();

        // Clase para reportes de comentarios
        public static class ReporteComentario {
            private String id = UUID.randomUUID().toString();
            private String usuarioId;
            private String motivo;
            private LocalDateTime fechaReporte;
            private boolean resuelto = false;
            
            public ReporteComentario() {
                this.fechaReporte = LocalDateTime.now();
            }
            
            public ReporteComentario(String usuarioId, String motivo) {
                this();
                this.usuarioId = usuarioId;
                this.motivo = motivo;
            }
            
            // Getters y setters
            public String getId() {
                return id;
            }
            
            public void setId(String id) {
                this.id = id;
            }
            
            public String getUsuarioId() {
                return usuarioId;
            }
            
            public void setUsuarioId(String usuarioId) {
                this.usuarioId = usuarioId;
            }
            
            public String getMotivo() {
                return motivo;
            }
            
            public void setMotivo(String motivo) {
                this.motivo = motivo;
            }
            
            public LocalDateTime getFechaReporte() {
                return fechaReporte;
            }
            
            public void setFechaReporte(LocalDateTime fechaReporte) {
                this.fechaReporte = fechaReporte;
            }
            
            public boolean isResuelto() {
                return resuelto;
            }
            
            public void setResuelto(boolean resuelto) {
                this.resuelto = resuelto;
            }
        }

        // Constructor vacío para MongoDB
        public Comentario() {
            this.id = UUID.randomUUID().toString();
            this.fechaCreacion = LocalDateTime.now();
            // Eliminamos inicialización redundante
        }

        public Comentario(String contenido, AutorInfo autor) {
            this();
            this.contenido = contenido;
            this.autor = autor;
        }

        // Clase interna para respuestas (limitando a un nivel)
        public static class Respuesta {
            // Eliminamos @Id ya que es un documento embebido
            private String id;
            private String contenido;
            private LocalDateTime fechaCreacion;
            @Field("autor")
            private AutorInfo autor;
            private int likes = 0;
            @Field("usuarios_like")
            private Set<String> usuariosQueDieronLike = new HashSet<>();

            // Constructor vacío para MongoDB
            public Respuesta() {
                this.id = UUID.randomUUID().toString();
                this.fechaCreacion = LocalDateTime.now();
            }

            public Respuesta(String contenido, AutorInfo autor) {
                this();
                this.contenido = contenido;
                this.autor = autor;
            }

            // Getters y setters
            public String getId() {
                return id;
            }

            public void setId(String id) {
                this.id = id;
            }

            public String getContenido() {
                return contenido;
            }

            public void setContenido(String contenido) {
                this.contenido = contenido;
            }

            public LocalDateTime getFechaCreacion() {
                return fechaCreacion;
            }

            public void setFechaCreacion(LocalDateTime fechaCreacion) {
                this.fechaCreacion = fechaCreacion;
            }

            public AutorInfo getAutor() {
                return autor;
            }

            public void setAutor(AutorInfo autor) {
                this.autor = autor;
            }

            public int getLikes() {
                return likes;
            }

            public void setLikes(int likes) {
                this.likes = likes;
            }

            public Set<String> getUsuariosQueDieronLike() {
                return usuariosQueDieronLike;
            }

            public void setUsuariosQueDieronLike(Set<String> usuariosQueDieronLike) {
                this.usuariosQueDieronLike = usuariosQueDieronLike;
            }

            public void agregarLike(String usuarioId) {
                if (!usuariosQueDieronLike.contains(usuarioId)) {
                    usuariosQueDieronLike.add(usuarioId);
                    this.likes = usuariosQueDieronLike.size();
                }
            }

            public void quitarLike(String usuarioId) {
                if (usuariosQueDieronLike.contains(usuarioId)) {
                    usuariosQueDieronLike.remove(usuarioId);
                    this.likes = usuariosQueDieronLike.size();
                }
            }
        }
        
        // Getters y setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getContenido() {
            return contenido;
        }

        public void setContenido(String contenido) {
            this.contenido = contenido;
        }

        public LocalDateTime getFechaCreacion() {
            return fechaCreacion;
        }

        public void setFechaCreacion(LocalDateTime fechaCreacion) {
            this.fechaCreacion = fechaCreacion;
        }

        public AutorInfo getAutor() {
            return autor;
        }

        public void setAutor(AutorInfo autor) {
            this.autor = autor;
        }

        public List<Respuesta> getRespuestas() {
            return respuestas;
        }

        public void setRespuestas(List<Respuesta> respuestas) {
            this.respuestas = respuestas;
        }

        public void agregarRespuesta(Respuesta respuesta) {
            this.respuestas.add(respuesta);
        }

        public int getLikes() {
            return likes;
        }

        public void setLikes(int likes) {
            this.likes = likes;
        }

        public Set<String> getUsuariosQueDieronLike() {
            return usuariosQueDieronLike;
        }

        public void setUsuariosQueDieronLike(Set<String> usuariosQueDieronLike) {
            this.usuariosQueDieronLike = usuariosQueDieronLike;
        }

        public void agregarLike(String usuarioId) {
            if (!usuariosQueDieronLike.contains(usuarioId)) {
                usuariosQueDieronLike.add(usuarioId);
                this.likes = usuariosQueDieronLike.size();
            }
        }

        public void quitarLike(String usuarioId) {
            if (usuariosQueDieronLike.contains(usuarioId)) {
                usuariosQueDieronLike.remove(usuarioId);
                this.likes = usuariosQueDieronLike.size();
            }
        }

        public boolean isReportado() {
            return reportado;
        }

        public void setReportado(boolean reportado) {
            this.reportado = reportado;
        }

        public boolean isRestringido() {
            return restringido;
        }

        public void setRestringido(boolean restringido) {
            this.restringido = restringido;
        }

        public String getMotivoRestriccion() {
            return motivoRestriccion;
        }

        public void setMotivoRestriccion(String motivoRestriccion) {
            this.motivoRestriccion = motivoRestriccion;
        }

        public LocalDateTime getFechaRestriccion() {
            return fechaRestriccion;
        }

        public void setFechaRestriccion(LocalDateTime fechaRestriccion) {
            this.fechaRestriccion = fechaRestriccion;
        }

        public String getUsuarioRestriccionId() {
            return usuarioRestriccionId;
        }

        public void setUsuarioRestriccionId(String usuarioRestriccionId) {
            this.usuarioRestriccionId = usuarioRestriccionId;
        }

        public List<ReporteComentario> getReportesComentario() {
            return reportesComentario;
        }

        public void setReportesComentario(List<ReporteComentario> reportesComentario) {
            this.reportesComentario = reportesComentario;
        }

        public void agregarReporteComentario(ReporteComentario reporte) {
            this.reportesComentario.add(reporte);
            this.reportado = true;
        }
    }
    
    // CONSTRUCTOR
    public Foro(String titulo, String contenido, AutorInfo autor) {
        this();
        this.titulo = titulo;
        this.contenido = contenido;
        this.autor = autor;
    }

    // CONSTRUCTOR VACIO
    public Foro() {
        this.id = UUID.randomUUID().toString();
        this.fechaCreacion = LocalDateTime.now();
        // Eliminamos inicializaciones redundantes
    }

    // GETTERS Y SETTERS
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
    
    public String getTitulo() {
        return titulo;
    }
    
    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public AutorInfo getAutor() {
        return autor;
    }

    public void setAutor(AutorInfo autor) {
        this.autor = autor;
    }

    public List<Comentario> getComentarios() {
        return comentarios;
    }

    public void setComentarios(List<Comentario> comentarios) {
        this.comentarios = comentarios;
    }

    public List<String> getEtiquetas() {
        return etiquetas;
    }

    public void setEtiquetas(List<String> etiquetas) {
        this.etiquetas = etiquetas;
    }

    // METODOS ADICIONALES FORO
    public void agregarComentario(Comentario comentario) {
        this.comentarios.add(comentario);
    }

    public void agregarEtiqueta(String etiqueta) {
        if (!this.etiquetas.contains(etiqueta)) {
            this.etiquetas.add(etiqueta);
        }
    }
    
    public void actualizarCacheEstadisticas(String key, Object value) {
        cacheEstadisticas.put(key, value);
    }
    
    public Object obtenerCacheEstadisticas(String key) {
        return cacheEstadisticas.get(key);
    }

    // Nuevos getters y setters para la clase Foro
    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public boolean isArchivado() {
        return archivado;
    }

    public void setArchivado(boolean archivado) {
        this.archivado = archivado;
    }

    public boolean isRequiereModeracion() {
        return requiereModeracion;
    }

    public void setRequiereModeracion(boolean requiereModeracion) {
        this.requiereModeracion = requiereModeracion;
    }

    public int getContadorReportes() {
        return contadorReportes;
    }

    public void setContadorReportes(int contadorReportes) {
        this.contadorReportes = contadorReportes;
    }

    public List<ReporteForo> getReportes() {
        return reportes;
    }

    public void setReportes(List<ReporteForo> reportes) {
        this.reportes = reportes;
    }

    public void agregarReporte(ReporteForo reporte) {
        this.reportes.add(reporte);
        this.contadorReportes++;
    }

    public boolean isPermiteComentarios() {
        return permiteComentarios;
    }

    public void setPermiteComentarios(boolean permiteComentarios) {
        this.permiteComentarios = permiteComentarios;
    }

    public boolean isEsPublico() {
        return esPublico;
    }

    public void setEsPublico(boolean esPublico) {
        this.esPublico = esPublico;
    }
    
    @JsonIgnore
    public Map<String, Object> getCacheEstadisticas() {
        return cacheEstadisticas;
    }
    
    public void setCacheEstadisticas(Map<String, Object> cacheEstadisticas) {
        this.cacheEstadisticas = cacheEstadisticas;
    }
}