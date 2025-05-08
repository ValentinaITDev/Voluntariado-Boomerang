package com.Impulso.Alcambio.Repositorio;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.Impulso.Alcambio.Modelo.ParticipacionDesafio;

public interface ParticipacionDesafioRepositorio extends MongoRepository<ParticipacionDesafio, String> {
    List<ParticipacionDesafio> findByUsuarioId(String usuarioId);
    List<ParticipacionDesafio> findByDesafioId(String desafioId);
    Optional<ParticipacionDesafio> findByUsuarioIdAndDesafioId(String usuarioId, String desafioId);
    List<ParticipacionDesafio> findByCompletado(boolean completado);
    List<ParticipacionDesafio> findByUsuarioIdAndCompletado(String usuarioId, boolean completado);

    // Nuevo método para verificar si existe una participación completada para un desafío
    boolean existsByDesafioIdAndCompletado(String desafioId, boolean completado);
} 