package com.Impulso.Alcambio.Repositorio;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.Impulso.Alcambio.Modelo.Proyecto;
import com.Impulso.Alcambio.Modelo.Proyecto.EstadoProyecto;

public interface ProyectoRepositorio extends MongoRepository<Proyecto, String> {
    //Buscar proyectos por nombre
    List<Proyecto> findByNombreContaining(String nombre);
    
    //Buscar proyectos por nombre (insensible a mayúsculas/minúsculas)
    List<Proyecto> findByNombreContainingIgnoreCase(String nombre);
    
    //Buscar proyectos por estado
    List<Proyecto> findByEstado(EstadoProyecto estado);
    
    //Contar proyectos por estado
    long countByEstado(EstadoProyecto estado);
    //Buscar proyectos por usuario
}
