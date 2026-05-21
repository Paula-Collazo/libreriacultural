package com.proyectofinal.libreriacultural.controllers;

import com.proyectofinal.libreriacultural.Repositories.*;
import com.proyectofinal.libreriacultural.domain.*;
import com.proyectofinal.libreriacultural.services.UserAccountService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class RestApiController {

    private final UserRepository userRepository;
    private final UserContentRepository userContentRepository;
    private final ContentRepository contentRepository;
    private final CustomListRepository customListRepository;
    private final UserAccountService userAccountService;
    private final FriendshipRepository friendshipRepository;

    public RestApiController(UserRepository userRepository, 
                           UserContentRepository userContentRepository,
                           ContentRepository contentRepository,
                           CustomListRepository customListRepository,
                           UserAccountService userAccountService,
                           FriendshipRepository friendshipRepository) {
        this.userRepository = userRepository;
        this.userContentRepository = userContentRepository;
        this.contentRepository = contentRepository;
        this.customListRepository = customListRepository;
        this.userAccountService = userAccountService;
        this.friendshipRepository = friendshipRepository;
    }

    private User getSessionUser(HttpSession session) {
        Long userId = (Long) session.getAttribute("sessionUserId");
        if (userId == null) return null;
        return userRepository.findById(userId).orElse(null);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials, HttpSession session) {
        User user = userAccountService.authenticate(credentials.get("username"), credentials.get("password"));
        if (user != null) {
            session.setAttribute("sessionUserId", user.getId());
            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getId());
            response.put("username", user.getUsername());
            response.put("email", user.getEmail());
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(401).body("Credenciales incorrectas");
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body, HttpSession session) {
        String username = body.get("username");
        String email = body.get("email");
        String password = body.get("password");
        String passwordConfirm = body.get("passwordConfirm");

        if (username == null || username.isBlank()) return ResponseEntity.badRequest().body("Usuario vacío");
        if (email == null || !email.contains("@")) return ResponseEntity.badRequest().body("Email inválido");
        if (password == null || password.length() < 6) return ResponseEntity.badRequest().body("Contraseña corta");
        if (!password.equals(passwordConfirm)) return ResponseEntity.badRequest().body("Las contraseñas no coinciden");

        User newUser = userAccountService.register(username, email, password);
        if (newUser == null) return ResponseEntity.badRequest().body("El usuario ya existe");

        session.setAttribute("sessionUserId", newUser.getId());
        return ResponseEntity.ok(newUser);
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(HttpSession session) {
        User user = getSessionUser(session);
        if (user == null) {
            // Para desarrollo, si no hay sesión, devolvemos el primero si existe
            List<User> users = userRepository.findAll();
            if(!users.isEmpty()) user = users.get(0);
            else return ResponseEntity.status(401).body("No autorizado");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("user", user);
        data.put("content", userContentRepository.findByUserId(user.getId()));
        data.put("lists", customListRepository.findByUser(user));
        
        // Stats by type
        Map<String, Object> stats = new HashMap<>();
        stats.put("peliculas", userContentRepository.countByUserIdAndContentType(user.getId(), "PELICULA"));
        stats.put("series", userContentRepository.countByUserIdAndContentType(user.getId(), "SERIE"));
        stats.put("libros", userContentRepository.countByUserIdAndContentType(user.getId(), "LIBRO"));
        stats.put("discos", userContentRepository.countByUserIdAndContentType(user.getId(), "DISCO"));
        
        // Premium Stats (matching backend screenshot)
        Long totalPages = userContentRepository.sumTotalPagesByUserId(user.getId());
        long recentAdded = userContentRepository.countRecentItems(user.getId(), java.time.LocalDate.now().minusDays(30));

        stats.put("paginasLeidas", totalPages != null ? totalPages : 0);
        stats.put("episodiosVistos", 0); // Funcionalidad pendiente de implementación en Entity
        stats.put("novedades30d", recentAdded);

        data.put("stats", stats);
        data.put("totalCount", userContentRepository.countByUserId(user.getId()));
        
        return ResponseEntity.ok(data);
    }

    @GetMapping("/content/{type}")
    public ResponseEntity<?> getContentByType(@PathVariable String type, HttpSession session) {
        User user = getSessionUser(session);
        if (user == null) {
            List<User> users = userRepository.findAll();
            if(!users.isEmpty()) user = users.get(0);
            else return ResponseEntity.status(401).body("No autorizado");
        }
        return ResponseEntity.ok(userContentRepository.findByUserIdAndContentType(user.getId(), type));
    }

    @PostMapping("/content/{id}/delete")
    public ResponseEntity<?> deleteContent(@PathVariable Long id, HttpSession session) {
        User user = getSessionUser(session);
        if (user == null) return ResponseEntity.status(401).body("No autorizado");
        
        UserContent uc = userContentRepository.findById(id).orElse(null);
        if (uc != null && uc.getUser().getId().equals(user.getId())) {
            userContentRepository.delete(uc);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(404).body("No encontrado");
    }

    @PostMapping("/content/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body, HttpSession session) {
        User user = getSessionUser(session);
        if (user == null) return ResponseEntity.status(401).body("No autorizado");

        UserContent uc = userContentRepository.findById(id).orElse(null);
        if (uc != null && uc.getUser().getId().equals(user.getId())) {
            uc.setStatus(body.get("status"));
            userContentRepository.save(uc);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(404).body("No encontrado");
    }

    @PostMapping("/content/add")
    public ResponseEntity<?> addContent(@RequestBody Map<String, String> body, HttpSession session) {
        User user = getSessionUser(session);
        if (user == null) {
            List<User> users = userRepository.findAll();
            if(!users.isEmpty()) user = users.get(0);
            else return ResponseEntity.status(401).body("No autorizado");
        }

        // Buscamos si ya existe el contenido en nuestra DB global
        String externalId = body.get("externalId");
        String contentType = body.get("type");
        String title = body.get("title");
        String imageUrl = body.get("imageUrl");

        Content content = contentRepository.findByExternalId(externalId).orElse(null);
        if (content == null) {
            content = new Content();
            content.setExternalId(externalId);
            content.setTitle(title);
            content.setCoverUrl(imageUrl);
            content.setType(contentType);
            content = contentRepository.save(content);
        }

        // Evitar duplicados para el usuario
        if (userContentRepository.existsByUserIdAndContentId(user.getId(), content.getId())) {
            return ResponseEntity.badRequest().body("Ya está en tu biblioteca");
        }

        UserContent userContent = new UserContent();
        userContent.setUser(user);
        userContent.setContent(content);
        userContent.setStatus("PLANNING");
        userContentRepository.save(userContent);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/friends")
    public ResponseEntity<?> getFriends(HttpSession session) {
        User user = getSessionUser(session);
        if (user == null) return ResponseEntity.status(401).body("No autorizado");

        List<Friendship> friends = friendshipRepository.findByRequesterOrReceiverAndStatus(user, user, Friendship.FriendshipStatus.ACCEPTED);
        return ResponseEntity.ok(friends);
    }
}
