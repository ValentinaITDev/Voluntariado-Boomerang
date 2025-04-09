package com.Impulso.Alcambio.Controller;

import java.util.Collections;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.Impulso.Alcambio.Modelo.Usuario;
import com.Impulso.Alcambio.Repositorio.UsuarioRepositorio;

@Service
public class UserDetail implements UserDetailsService {
    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<Usuario> usuarioOpt = usuarioRepositorio.findByCorreo(username);
        
        if (!usuarioOpt.isPresent()) {
            throw new UsernameNotFoundException("Usuario no encontrado con correo: " + username);
        }
        
        Usuario usuario = usuarioOpt.get();
        
        // Crear autoridades basadas en el rol del usuario
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + usuario.getRol().toString());
        
        // Retornar un UserDetails con la información del usuario
        return new User(
            usuario.getCorreo(),
            usuario.getPassword(),
            Collections.singletonList(authority)
        );
    }
}   
