package com.Impulso.Alcambio.Modelo;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.Impulso.Alcambio.Modelo.Proyecto.EstadoProyecto;

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
    private String empresa;
    private String imagenPerfil;
    private Rol rol;
    private LocalDateTime fechaRegistro;
    
    // SISTEMA DE JUEGO
    private MecanicaJuego mecanicaJuego;

    // PROYECTOS ACTIVOS EMBEBIDO
    private List<ProyectoResumen> proyectosActivos = new ArrayList<>();
    
    // DESAFIOS ACTIVOS EMBEBIDO
    private List<DesafioUsuario> desafios = new ArrayList<>();

    // CONSTRUCTOR VACIO REQUERIDO PARA QU ENO HAYA EL ERROR DEL NULLPOINTER AL TRATAR DE ASIGNAR EL OBJETO GAMIFICACION
    public Usuario() {
        this.mecanicaJuego = new MecanicaJuego();
    }

    // CONSTRUCTOR CON CAMPOS BASICOS"USUARIO"
    public Usuario(String nombre, String correo, String numero, String password, String empresa) {
        this();
        this.nombre = nombre;
        this.correo = correo;
        this.numero = numero;
        this.password = password;
        this.empresa = empresa;
        this.fechaRegistro = LocalDateTime.now();
    }

    // CLASE EMBEBIDA PARA LA MECANICA DE JUEGO
    public static class MecanicaJuego {
        //ATRIBUTOS DE LA MECANICA DE JUEGO
        private int puntos;
        private List<String> insignias = new ArrayList<>();
        private List<LogroDesbloqueado> logros = new ArrayList<>();

        // GETTERS Y SETTERS DE LA MECANICA DE JUEGO
        public int getPuntos() {
            return puntos;
        }

        public void setPuntos(int puntos) {
            this.puntos = puntos;
        }

        public List<String> getInsignias() {
            return insignias;
        }

        public void setInsignias(List<String> insignias) {
            this.insignias = insignias;
        }

        public List<LogroDesbloqueado> getLogros() {
            return logros;
        }

        public void setLogros(List<LogroDesbloqueado> logros) {
            this.logros = logros;
        }

        // Métodos ADICIONALES
        public void agregarLogro(LogroDesbloqueado logro) {
            this.logros.add(logro);
            this.puntos += logro.getPuntosOtorgados();
        }

        public void agregarInsignia(String insignia) {
            if (!this.insignias.contains(insignia)) {
                this.insignias.add(insignia);
            }
        }
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

    // Clase embebida para resumen de proyectos
    public static class ProyectoResumen {
        private String proyectoId;
        private String nombre;
        private LocalDateTime fechaUnion;
        private String rol;
        private EstadoProyecto estado;
        private List<String> contribuciones = new ArrayList<>();

        // Constructor
        public ProyectoResumen(String proyectoId, String nombre, String rol) {
            this.proyectoId = proyectoId;
            this.nombre = nombre;
            this.rol = rol;
            this.estado = EstadoProyecto.ACTIVO;
            this.fechaUnion = LocalDateTime.now();
        }

        // Getters y Setters de ProyectoResumen
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

        public LocalDateTime getFechaUnion() {
            return fechaUnion;
        }

        public void setFechaUnion(LocalDateTime fechaUnion) {
            this.fechaUnion = fechaUnion;
        }

        public String getRol() {
            return rol;
        }

        public void setRol(String rol) {
            this.rol = rol;
        }

        public List<String> getContribuciones() {
            return contribuciones;
        }

        public void setContribuciones(List<String> contribuciones) {
            this.contribuciones = contribuciones;
        }

        public EstadoProyecto getEstado() { 
            return estado;
        }

        public void setEstado(EstadoProyecto estado) {
            this.estado = estado;
        }
    }

    // Clase embebida para desafíos del usuario
    public static class DesafioUsuario {
        private String desafioId;
        private String nombre;
        private LocalDateTime fechaInicio;
        private LocalDateTime fechaCompletado;
        private boolean completado;
        private int progreso;
        private List<String> actividadesCompletadas = new ArrayList<>();

        // Constructor
        public DesafioUsuario(String desafioId, String nombre) {
            this.desafioId = desafioId;
            this.nombre = nombre;
            this.fechaInicio = LocalDateTime.now();
            this.completado = false;
            this.progreso = 0;
        }

        // Getters y Setters de DesafioUsuario
        public String getDesafioId() {
            return desafioId;
        }

        public void setDesafioId(String desafioId) {
            this.desafioId = desafioId;
        }

        public String getNombre() {
            return nombre;
        }

        public void setNombre(String nombre) {
            this.nombre = nombre;
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

        public boolean isCompletado() {
            return completado;
        }

        public void setCompletado(boolean completado) {
            this.completado = completado;
            if (completado && fechaCompletado == null) {
                this.fechaCompletado = LocalDateTime.now();
            }
        }

        public int getProgreso() {
            return progreso;
        }

        public void setProgreso(int progreso) {
            this.progreso = progreso;
        }

        public List<String> getActividadesCompletadas() {
            return actividadesCompletadas;
        }

        public void setActividadesCompletadas(List<String> actividadesCompletadas) {
            this.actividadesCompletadas = actividadesCompletadas;
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

    public MecanicaJuego getMecanicaJuego() {
        return mecanicaJuego;
    }

    public void setMecanicaJuego(MecanicaJuego mecanicaJuego) {
        this.mecanicaJuego = mecanicaJuego;
    }

    public List<ProyectoResumen> getProyectosActivos() {
        return proyectosActivos;
    }

    public void setProyectosActivos(List<ProyectoResumen> proyectosActivos) {
        this.proyectosActivos = proyectosActivos;
    }

    public List<DesafioUsuario> getDesafios() {
        return desafios;
    }

    public void setDesafios(List<DesafioUsuario> desafios) {
        this.desafios = desafios;
    }

    // Métodos de utilidad
    public void agregarProyecto(ProyectoResumen proyecto) {
        this.proyectosActivos.add(proyecto);
    }

    public void agregarDesafio(DesafioUsuario desafio) {
        this.desafios.add(desafio);
    }

    public void actualizarProgreso(String desafioId, int progreso) {
        this.desafios.stream()
            .filter(d -> d.getDesafioId().equals(desafioId))
            .findFirst()
            .ifPresent(d -> {
                d.setProgreso(progreso);
                if (progreso >= 100) {
                    d.setCompletado(true);
                }
            });
    }
}