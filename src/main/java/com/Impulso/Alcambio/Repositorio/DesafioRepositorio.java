package com.Impulso.Alcambio.Repositorio;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.Impulso.Alcambio.Modelo.Desafio;

public interface DesafioRepositorio extends MongoRepository<Desafio, String> {
    List<Desafio> findByProyectoId(String proyectoId);
    List<Desafio> findByTipo(Desafio.TipoDesafio tipo);
} 