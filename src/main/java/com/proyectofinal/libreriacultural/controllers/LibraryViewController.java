package com.proyectofinal.libreriacultural.controllers;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.proyectofinal.libreriacultural.Repositories.ContentRepository;
import com.proyectofinal.libreriacultural.Repositories.UserContentRepository;
import com.proyectofinal.libreriacultural.Repositories.UserRepository;
import com.proyectofinal.libreriacultural.domain.Content;
import com.proyectofinal.libreriacultural.domain.User;
import com.proyectofinal.libreriacultural.domain.UserContent;

@Controller
@RequestMapping("/ui/library")
public class LibraryViewController {

    private static final List<String> VALID_STATUSES = List.of("pendiente", "en_progreso", "visto", "abandonado");

    private final UserContentRepository userContentRepository;
    private final UserRepository userRepository;
    private final ContentRepository contentRepository;

    public LibraryViewController(UserContentRepository userContentRepository, UserRepository userRepository,
            ContentRepository contentRepository) {
        this.userContentRepository = userContentRepository;
        this.userRepository = userRepository;
        this.contentRepository = contentRepository;
    }

    @GetMapping
    public String showLibrary(@RequestParam(required = false) Long userId, Model model) {
        List<User> users = userRepository.findAll();
        List<Content> contents = contentRepository.findAll();

        Long selectedUserId = resolveSelectedUserId(userId, users);
        List<UserContent> entries = selectedUserId == null ? List.of() : userContentRepository.findByUserId(selectedUserId);
        Map<String, Long> stats = selectedUserId == null ? emptyStats() : loadStats(selectedUserId);
        long totalCount = selectedUserId == null ? 0L : userContentRepository.countByUserId(selectedUserId);

        model.addAttribute("users", users);
        model.addAttribute("contents", contents);
        model.addAttribute("selectedUserId", selectedUserId);
        model.addAttribute("entries", entries);
        model.addAttribute("statusOptions", VALID_STATUSES);
        model.addAttribute("stats", stats);
        model.addAttribute("totalCount", totalCount);

        return "library";
    }

    @PostMapping("/add")
    public String addToLibrary(@RequestParam Long userId, @RequestParam Long contentId, @RequestParam String status,
            RedirectAttributes redirectAttributes) {
        User user = userRepository.findById(userId).orElse(null);
        Content content = contentRepository.findById(contentId).orElse(null);

        if (user == null || content == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "No se pudo guardar: usuario o contenido inexistente");
            return redirectToLibrary(userId);
        }

        String normalizedStatus = normalizeStatus(status);
        if (!VALID_STATUSES.contains(normalizedStatus)) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Estado no valido. Usa: pendiente, en_progreso, visto o abandonado");
            return redirectToLibrary(userId);
        }

        if (userContentRepository.existsByUserIdAndContentId(userId, contentId)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ese contenido ya esta en la biblioteca del usuario");
            return redirectToLibrary(userId);
        }

        UserContent userContent = new UserContent();
        userContent.setUser(user);
        userContent.setContent(content);
        userContent.setStatus(normalizedStatus);
        userContent.setAddedDate(LocalDate.now());

        try {
            userContentRepository.save(userContent);
            redirectAttributes.addFlashAttribute("successMessage", "Contenido anadido correctamente");
        } catch (DataIntegrityViolationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ese contenido ya esta en la biblioteca del usuario");
        }

        return redirectToLibrary(userId);
    }

    @PostMapping("/{entryId}/status")
    public String updateStatus(@PathVariable Long entryId, @RequestParam Long userId, @RequestParam String status,
            RedirectAttributes redirectAttributes) {
        UserContent entry = userContentRepository.findById(entryId).orElse(null);
        if (entry == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Registro no encontrado");
            return redirectToLibrary(userId);
        }

        String normalizedStatus = normalizeStatus(status);
        if (!VALID_STATUSES.contains(normalizedStatus)) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Estado no valido. Usa: pendiente, en_progreso, visto o abandonado");
            return redirectToLibrary(userId);
        }

        entry.setStatus(normalizedStatus);
        userContentRepository.save(entry);
        redirectAttributes.addFlashAttribute("successMessage", "Estado actualizado");

        return redirectToLibrary(userId);
    }

    @PostMapping("/{entryId}/delete")
    public String deleteEntry(@PathVariable Long entryId, @RequestParam Long userId,
            RedirectAttributes redirectAttributes) {
        if (!userContentRepository.existsById(entryId)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Registro no encontrado");
            return redirectToLibrary(userId);
        }

        userContentRepository.deleteById(entryId);
        redirectAttributes.addFlashAttribute("successMessage", "Registro eliminado");

        return redirectToLibrary(userId);
    }

    private Long resolveSelectedUserId(Long userId, List<User> users) {
        if (userId != null && userRepository.existsById(userId)) {
            return userId;
        }
        if (users.isEmpty()) {
            return null;
        }
        return users.get(0).getId();
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "pendiente";
        }
        return status.trim().toLowerCase();
    }

    private Map<String, Long> loadStats(Long userId) {
        Map<String, Long> stats = new LinkedHashMap<>();
        for (String status : VALID_STATUSES) {
            stats.put(status, userContentRepository.countByUserIdAndStatus(userId, status));
        }
        return stats;
    }

    private Map<String, Long> emptyStats() {
        Map<String, Long> stats = new LinkedHashMap<>();
        for (String status : VALID_STATUSES) {
            stats.put(status, 0L);
        }
        return stats;
    }

    private String redirectToLibrary(Long userId) {
        return "redirect:/ui/library?userId=" + userId;
    }
}
