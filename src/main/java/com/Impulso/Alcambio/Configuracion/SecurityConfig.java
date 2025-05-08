package com.Impulso.Alcambio.Configuracion;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig implements WebMvcConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    @Autowired
    @Qualifier("userDetailsServiceImpl")
    private UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                // Permitir acceso a recursos estáticos y páginas públicas
                .requestMatchers("/", "/css/**", "/js/**", "/images/**", "/Proyectos/**", "/registro", 
                                 "/login", "/error", "/favicon.ico", "/Foro.css", 
                                 "/Styles.css", "/Proyectos.css", "/ProyectoUsuario.css").permitAll()
                // Permitir acceso a /dashboard a cualquier usuario autenticado
                .requestMatchers("/dashboard").authenticated()
                // Proteger las rutas administrativas
                .requestMatchers("/admin/**", "/dashboardAdmin").hasAuthority("ADMIN")
                // Proteger las rutas de usuario
                .requestMatchers("/usuario/**").authenticated()
                // Proteger las API con prefijo /api/
                .requestMatchers("/api/admin/**").hasAuthority("ADMIN")
                .requestMatchers("/api/proyectos/**").authenticated()
                .requestMatchers("/api/usuarios/**").authenticated()
                .requestMatchers("/api/foros/**").authenticated()
                // Foros requiere autenticación
                .requestMatchers("/foros").authenticated()
                // Cualquier otra ruta requiere autenticación
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/perform_login")
                .usernameParameter("correo")
                .passwordParameter("password")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessHandler(customLogoutSuccessHandler())
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID", "remember-me")
                
                .clearAuthentication(true)
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                .permitAll()
            )
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                // Desactivar CSRF para API (mejor sería usar tokens JWT)
                .ignoringRequestMatchers("/api/**")
            )
            .exceptionHandling(exc -> exc
                .accessDeniedPage("/error/403")
            );
            
        return http.build();
    }
    
    @Bean
    public LogoutSuccessHandler customLogoutSuccessHandler() {
        return new LogoutSuccessHandler() {
            @Override
            public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, 
                                        Authentication authentication) throws IOException, ServletException {
                logger.info("Usuario cerró sesión exitosamente");
                
                // Limpiar todas las cookies de sesión para prevenir problemas de sesión persistente
                Cookie[] cookies = request.getCookies();
                if (cookies != null) {
                    for (Cookie cookie : cookies) {
                        Cookie newCookie = new Cookie(cookie.getName(), null);
                        newCookie.setMaxAge(0);
                        newCookie.setPath("/");
                        response.addCookie(newCookie);
                    }
                }
                
                // Redireccionar a la página de login con mensaje de cierre de sesión exitoso
                response.sendRedirect(request.getContextPath() + "/login?logout=true");
            }
        };
    }
    
    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService)
            .passwordEncoder(passwordEncoder());
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer
            .mediaType("css", MediaType.valueOf("text/css"))
            .mediaType("js", MediaType.valueOf("application/javascript"))
            .mediaType("png", MediaType.IMAGE_PNG)
            .mediaType("jpg", MediaType.IMAGE_JPEG)
            .mediaType("jpeg", MediaType.IMAGE_JPEG);
    }
} 