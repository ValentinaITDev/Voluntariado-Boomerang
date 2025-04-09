package com.Impulso.Alcambio.Servicio;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

import com.Impulso.Alcambio.Modelo.Proyecto;
import com.Impulso.Alcambio.Repositorio.ProyectoRepositorio;

@Service
public class ProyectoServicio {
    private final ProyectoRepositorio proyectoRepositorio;
//CONSTRUCTOR
    public ProyectoServicio(ProyectoRepositorio proyectoRepositorio) {
        this.proyectoRepositorio = proyectoRepositorio;
    }

    //OBTENER TODOS LOS PROYECTOS
    public List<Proyecto> obtenerTodos() {
        return proyectoRepositorio.findAll();
    }

    //CREAR PROYECTO
    public Proyecto crearProyecto(Proyecto proyecto) {
        return proyectoRepositorio.save(proyecto);
    }

    //ACTUALIZAR PROYECTO
    public Proyecto actualizarProyecto(Proyecto proyecto) {
        return proyectoRepositorio.save(proyecto);
    }
    
    //ELIMINAR PROYECTO
    public void eliminarProyecto(String id) {
            proyectoRepositorio.deleteById(id);
    }

    //OBTENER PROYECTO POR ID
    public Optional<Proyecto> obtenerProyectoPorId(String id) {
        return proyectoRepositorio.findById(id);
    }
    
    //Buscar proyectos por nombre
    public List<Proyecto> buscarPorNombre(String nombre) {
        return proyectoRepositorio.findByNombreContaining(nombre);
    }
}