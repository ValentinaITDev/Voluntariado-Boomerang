package com.Impulso.Alcambio.Repositorio;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.springframework.data.domain.Example;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery;

import com.Impulso.Alcambio.Modelo.Usuario;
import com.Impulso.Alcambio.Modelo.Rol;

public interface UsuarioRepositorio extends MongoRepository<Usuario, String> {
    List<Usuario> findByRol(Rol rol);
    Optional<Usuario> findByCorreo(String correo);
    boolean existsByCorreo(String correo);
    List<Usuario> findByEmpresa(String empresa);
}
