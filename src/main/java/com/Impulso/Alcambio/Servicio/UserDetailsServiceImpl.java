package com.Impulso.Alcambio.Servicio;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.Impulso.Alcambio.Modelo.Usuario;
import com.Impulso.Alcambio.Repositorio.UsuarioRepositorio;

@Service
@Primary
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    @Override
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        Optional<Usuario> usuarioOpt = usuarioRepositorio.findByCorreo(correo);
        
        if (!usuarioOpt.isPresent()) {
            throw new UsernameNotFoundException("No se encontró el usuario con correo: " + correo);
        }
        
        Usuario usuario = usuarioOpt.get();
        
        // Convertir el rol del usuario a una colección de GrantedAuthority
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(usuario.getRol().name()));
        
        // Crear un objeto UserDetails con la información del usuario
        return new User(
            usuario.getCorreo(),           // username (correo en nuestro caso)
            usuario.getPassword(),         // contraseña encriptada
            true,                          // enabled
            true,                          // accountNonExpired
            true,                          // credentialsNonExpired
            true,                          // accountNonLocked
            authorities                    // authorities/roles
        );
    }
} 