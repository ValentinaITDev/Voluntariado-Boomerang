package com.Impulso.Alcambio.Modelo;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Getter;
import lombok.Setter;

// Se elimina la importación de EstadoProyecto si ya no se usa directamente aquí
// import com.Impulso.Alcambio.Modelo.Proyecto.EstadoProyecto;

@Document(collection = "usuarios")
public class Usuario {
    //ATRIBUTOS DEL USUARIO
    @Id
    private String id;
    private String nombre;
    @Indexed(unique = true)
    private String correo;
    private String numero;
    private String password;
    @Indexed
    private String empresa;
    private String imagenPerfil;
    @Indexed
    private Rol rol;
    private LocalDateTime fechaRegistro;

    // Referencias a proyectos usando solo IDs para mejor rendimiento
    @Indexed
    private List<String> proyectosParticipadosIds = new ArrayList<>();
    
    // Cache de estadísticas para evitar cálculos frecuentes
    private Map<String, Object> cacheEstadisticas = new HashMap<>();
    
    // CONSTRUCTOR VACIO REQUERIDO
    public Usuario() {
        this.fechaRegistro = LocalDateTime.now();
    }

    // CONSTRUCTOR CON CAMPOS BASICOS
    public Usuario(String nombre, String correo, String numero, String password, String empresa) {
        this();
        this.nombre = nombre;
        this.correo = correo;
        this.numero = numero;
        this.password = password;
        this.empresa = empresa;
    }

    // Clase embebida para logros
    public static class LogroDesbloqueado {
        private String nombre;
        private LocalDateTime fechaDesbloqueo;
        private int puntosOtorgados;

        public LogroDesbloqueado(String nombre, int puntosOtorgados) {
            this.nombre = nombre;
            this.puntosOtorgados = puntosOtorgados;
            this.fechaDesbloqueo = LocalDateTime.now();
        }

        // Getters y Setters de LogroDesbloqueado
        public String getNombre() {
            return nombre;
        }

        public void setNombre(String nombre) {
            this.nombre = nombre;
        }

        public LocalDateTime getFechaDesbloqueo() {
            return fechaDesbloqueo;
        }

        public void setFechaDesbloqueo(LocalDateTime fechaDesbloqueo) {
            this.fechaDesbloqueo = fechaDesbloqueo;
        }

        public int getPuntosOtorgados() {
            return puntosOtorgados;
        }

        public void setPuntosOtorgados(int puntosOtorgados) {
            this.puntosOtorgados = puntosOtorgados;
        }
    }

    // Getters y Setters de la clase principal Usuario
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

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmpresa() {
        return empresa;
    }

    public void setEmpresa(String empresa) {
        this.empresa = empresa;
    }

    public String getImagenPerfil() {
        return imagenPerfil;
    }

    public void setImagenPerfil(String imagenPerfil) {
        this.imagenPerfil = imagenPerfil;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
            this.fechaRegistro = fechaRegistro;
    }

    public List<String> getProyectosParticipadosIds() {
        return proyectosParticipadosIds;
    }

    public void setProyectosParticipadosIds(List<String> proyectosParticipadosIds) {
        this.proyectosParticipadosIds = proyectosParticipadosIds;
    }

    public void agregarProyectoParticipado(String proyectoId) {
        if (!proyectosParticipadosIds.contains(proyectoId)) {
            proyectosParticipadosIds.add(proyectoId);
            // Invalidar cache cuando se modifica la participación
            cacheEstadisticas.clear();
        }
    }

    public void eliminarProyectoParticipado(String proyectoId) {
        if (proyectosParticipadosIds.remove(proyectoId)) {
            // Invalidar cache cuando se modifica la participación
            cacheEstadisticas.clear();
        }
    }

    public boolean estaParticipandoEnProyecto(String proyectoId) {
        return proyectosParticipadosIds.contains(proyectoId);
    }
    
    public void actualizarCacheEstadisticas(String key, Object value) {
        cacheEstadisticas.put(key, value);
    }
    
    public Object obtenerCacheEstadisticas(String key) {
        return cacheEstadisticas.get(key);
    }
}