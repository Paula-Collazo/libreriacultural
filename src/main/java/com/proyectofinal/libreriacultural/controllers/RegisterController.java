package com.proyectofinal.libreriacultural.controllers;

import com.proyectofinal.libreriacultural.domain.User;
import com.proyectofinal.libreriacultural.services.UserAccountService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class RegisterController {

    private static final String SESSION_USER_ID = "sessionUserId";

    private final UserAccountService userAccountService;

    public RegisterController(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    @GetMapping("/register")
    public String showRegisterPage(HttpSession session) {
        // Si ya tiene sesión activa, redirigir al perfil
        if (session.getAttribute(SESSION_USER_ID) != null) {
            return "redirect:/profile";
        }
        return "register";
    }

    @PostMapping("/register")
    public String processRegister(
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String passwordConfirm,
            RedirectAttributes redirectAttributes,
            HttpSession session) {

        // Validaciones básicas de frontend
        if (username == null || username.isBlank()) {
            redirectAttributes.addFlashAttribute("errorMessage", "El nombre de usuario no puede estar vacío");
            return "redirect:/register";
        }
        if (email == null || email.isBlank() || !email.contains("@")) {
            redirectAttributes.addFlashAttribute("errorMessage", "Introduce un email válido");
            return "redirect:/register";
        }
        if (password == null || password.length() < 6) {
            redirectAttributes.addFlashAttribute("errorMessage", "La contraseña debe tener al menos 6 caracteres");
            return "redirect:/register";
        }
        if (!password.equals(passwordConfirm)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Las contraseñas no coinciden");
            return "redirect:/register";
        }

        User newUser = userAccountService.register(username, email, password);
        if (newUser == null) {
            // Intentamos dar un mensaje más preciso
            redirectAttributes.addFlashAttribute("errorMessage",
                    "El nombre de usuario o el email ya están registrados");
            return "redirect:/register";
        }

        // Inicio de sesión automático tras el registro
        session.setAttribute(SESSION_USER_ID, newUser.getId());
        redirectAttributes.addFlashAttribute("successMessage", "¡Bienvenid@ a Librería Cultural, " + newUser.getUsername() + "!");
        return "redirect:/profile";
    }
}
