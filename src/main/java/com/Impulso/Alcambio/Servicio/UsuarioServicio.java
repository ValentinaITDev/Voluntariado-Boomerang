package com.Impulso.Alcambio.Servicio;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.Impulso.Alcambio.Modelo.Rol;
import com.Impulso.Alcambio.Modelo.Usuario;
import com.Impulso.Alcambio.Repositorio.UsuarioRepositorio;

@Service
public class UsuarioServicio {
    
    @Autowired
    private UsuarioRepositorio usuarioRepositorio;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    // Método para registrar un nuevo usuario
    public Usuario registrarUsuario(Usuario usuario) {
        // Verificar si ya existe un usuario con ese correo
        if (usuarioRepositorio.existsByCorreo(usuario.getCorreo())) {
            throw new RuntimeException("Ya existe un usuario con ese correo electrónico");
        }
        
        // Encriptar la contraseña
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        
        // Asignar rol de voluntario por defecto
        usuario.setRol(Rol.VOLUNTARIO);
        
        
        // Guardar el usuario
        return usuarioRepositorio.save(usuario);
    }
    
    // Buscar usuario por correo
    public Optional<Usuario> buscarPorCorreo(String correo) {
        return usuarioRepositorio.findByCorreo(correo);
    }
    
    // Buscar usuario por ID
    public Optional<Usuario> buscarPorId(String id) {
        return usuarioRepositorio.findById(id);
    }
    
    // Obtener todos los usuarios
    public List<Usuario> obtenerTodos() {
        return usuarioRepositorio.findAll();
    }
    
    // Buscar usuarios por rol
    public List<Usuario> buscarPorRol(Rol rol) {
        return usuarioRepositorio.findByRol(rol);
    }
    
    // Autenticar usuario (login)
    public boolean autenticarUsuario(String correo, String password) {
        Optional<Usuario> usuarioOpt = usuarioRepositorio.findByCorreo(correo);
        
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            return passwordEncoder.matches(password, usuario.getPassword());
        }
        
        return false;
    }
    
    // Actualizar usuario
    public Usuario actualizarUsuario(Usuario usuario) {
        // Si hay una nueva contraseña a guardar (no encriptada), la encriptamos
        if (usuario.getPassword() != null && !usuario.getPassword().isEmpty() && !usuario.getPassword().startsWith("$2a$")) {
            usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        }
        
        return usuarioRepositorio.save(usuario);
    }
    
    // Eliminar usuario
    public boolean eliminarUsuario(String id) {
        if (usuarioRepositorio.existsById(id)) {
            usuarioRepositorio.deleteById(id);
            return true;
        }
        return false;
    }
    
    // Buscar usuarios por empresa
    public List<Usuario> buscarPorEmpresa(String empresa) {
        return usuarioRepositorio.findByEmpresa(empresa);
    }
}
