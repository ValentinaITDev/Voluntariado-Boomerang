package com.Impulso.Alcambio.Modelo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "foro_posts")
public class Foro { 
    //ATRIBUTOS DEL FORO
    @Id
    private String id;
    
    private String contenido;
    private LocalDateTime fechaCreacion;
    private List<String> etiquetas = new ArrayList<>();
    
    // Información del autor embebida
    private AutorInfo autor;
    
    public static class AutorInfo {
        //ATRIBUTOS DEL AUTOR
        private String usuarioId;
        private String nombre;
        private String imagenPerfil;
        private List<String> insignias;

        public AutorInfo(String usuarioId, String nombre, String imagenPerfil) {
            this.usuarioId = usuarioId;
            this.nombre = nombre;
            this.imagenPerfil = imagenPerfil;
            this.insignias = new ArrayList<>();
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

        public List<String> getInsignias() {
            return insignias;
        }

        public void setInsignias(List<String> insignias) {
            this.insignias = insignias;
        }

        public void agregarInsignia(String insignia) {
            if (!this.insignias.contains(insignia)) {
                this.insignias.add(insignia);
            }
        }
    }
    
    // COMENTARIOS ANIDADOS EMBEBIDOS
    private List<Comentario> comentarios = new ArrayList<>();
    
    public static class Comentario {
        //ATRIBUTOS DEL COMENTARIO
        private String id;
        private String contenido;
        private LocalDateTime fechaCreacion;
        private AutorInfo autor;
        private List<Comentario> respuestas = new ArrayList<>();

        public Comentario(String contenido, AutorInfo autor) {
            this.id = UUID.randomUUID().toString();
            this.contenido = contenido;
            this.autor = autor;
            this.fechaCreacion = LocalDateTime.now();
        }

        // GETTERS Y SETTERS DEL COMENTARIO
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

        public List<Comentario> getRespuestas() {
            return respuestas;
        }

        public void setRespuestas(List<Comentario> respuestas) {
            this.respuestas = respuestas;
        }

        public void agregarRespuesta(Comentario respuesta) {
            this.respuestas.add(respuesta);
        }
    }
    
    // CONSTRUCTOR
    public Foro(String contenido, AutorInfo autor) {
        this.id = UUID.randomUUID().toString();
        this.contenido = contenido;
        this.autor = autor;
        this.fechaCreacion = LocalDateTime.now();
    }

    // CONSTRUCTOR VACIO
    public Foro() {
        this.fechaCreacion = LocalDateTime.now();
        this.etiquetas = new ArrayList<>();
        this.comentarios = new ArrayList<>();
    }

    // GETTERS Y SETTERS
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

    // METODOS DE ADICIONALES FORO
    public void agregarComentario(Comentario comentario) {
        this.comentarios.add(comentario);
    }

    public void agregarEtiqueta(String etiqueta) {
        if (!this.etiquetas.contains(etiqueta)) {
            this.etiquetas.add(etiqueta);
        }
    }
}