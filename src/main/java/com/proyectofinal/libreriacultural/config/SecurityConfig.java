package com.proyectofinal.libreriacultural.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Exponemos el BCryptPasswordEncoder como bean para poder inyectarlo
     * en cualquier servicio o controlador que lo necesite.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configuración principal de la security chain.
     * - Deshabilitamos el CSRF solo para simplificar el proyecto académico
     *   (los formularios Thymeleaf ya incluyen el token si se activa).
     * - Rutas públicas: landing ("/"), registro ("/register"), recursos estáticos.
     * - El resto requiere autenticación gestionada manualmente por sesión HTTP
     *   (no usamos el formLogin de Spring Security para no cambiar la lógica
     *   de sesión existente en SessionViewController).
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Deshabilitamos el login/logout automático de Spring Security
            // para conservar la lógica de sesión ya implementada en los controladores.
            .formLogin(form -> form.disable())
            .logout(logout -> logout.disable())

            // CSRF: lo dejamos desactivado para no romper los formularios existentes
            // (en producción real se debe activar y añadir th:action en cada form).
            .csrf(csrf -> csrf.disable())

            .authorizeHttpRequests(auth -> auth
                // Rutas totalmente públicas
                .requestMatchers("/", "/register", "/login", "/css/**", "/js/**", "/images/**", "/actuator/**").permitAll()
                // Cualquier otra petición necesita sesión (controlada por SessionViewController)
                .anyRequest().permitAll()
            );

        return http.build();
    }
}
