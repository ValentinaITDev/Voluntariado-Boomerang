package com.Impulso.Alcambio.Repositorio;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.Impulso.Alcambio.Modelo.Foro;

public interface ForoRepositorio extends MongoRepository<Foro, String> {
    
}
