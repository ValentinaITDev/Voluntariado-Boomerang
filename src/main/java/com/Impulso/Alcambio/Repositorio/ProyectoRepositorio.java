package com.Impulso.Alcambio.Repositorio;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.Impulso.Alcambio.Modelo.Proyecto;

public interface ProyectoRepositorio extends MongoRepository<Proyecto, String> {
    //Buscar proyectos por nombre
    List<Proyecto> findByNombreContaining(String nombre);
}
