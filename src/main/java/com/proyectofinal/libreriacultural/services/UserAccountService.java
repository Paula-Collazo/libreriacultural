package com.proyectofinal.libreriacultural.services;

import com.proyectofinal.libreriacultural.Repositories.UserRepository;
import com.proyectofinal.libreriacultural.domain.User;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Servicio para operaciones de cuenta de usuario:
 *   - Registro con contraseña cifrada en BCrypt.
 *   - Verificación de credenciales al hacer login.
 */
@Service
public class UserAccountService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserAccountService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registra un usuario nuevo.
     * @return el usuario guardado, o null si el username ya existe.
     */
    public User register(String username, String email, String rawPassword) {
        if (username == null || username.isBlank()) return null;
        if (email == null || email.isBlank()) return null;
        if (rawPassword == null || rawPassword.length() < 6) return null;

        // No permitir duplicados de username
        if (userRepository.findByUsername(username.trim()).isPresent()) {
            return null;
        }

        // No permitir duplicados de email
        if (userRepository.findByEmail(email.trim()).isPresent()) {
            return null;
        }

        User user = new User();
        user.setUsername(username.trim());
        user.setEmail(email.trim());
        // Ciframos con BCrypt antes de persistir
        user.setPassword(passwordEncoder.encode(rawPassword));
        return userRepository.save(user);
    }

    /**
     * Verifica credenciales: busca por username y comprueba BCrypt.
     * @return el usuario si las credenciales son correctas, null en caso contrario.
     */
    public User authenticate(String username, String rawPassword) {
        if (username == null || rawPassword == null) return null;
        return userRepository.findByUsername(username.trim())
                .filter(u -> u.getPassword() != null && passwordEncoder.matches(rawPassword, u.getPassword()))
                .orElse(null);
    }
}
