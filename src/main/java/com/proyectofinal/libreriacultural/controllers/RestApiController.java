package com.proyectofinal.libreriacultural.controllers;

import java.time.LocalDate;
import com.proyectofinal.libreriacultural.Repositories.*;
import com.proyectofinal.libreriacultural.domain.*;
import com.proyectofinal.libreriacultural.services.UserAccountService;
import com.proyectofinal.libreriacultural.services.ExternalContentSearchService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
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
    private final ExternalContentSearchService externalContentSearchService;

    public RestApiController(UserRepository userRepository, 
                           UserContentRepository userContentRepository,
                           ContentRepository contentRepository,
                           CustomListRepository customListRepository,
                           UserAccountService userAccountService,
                           FriendshipRepository friendshipRepository,
                           ExternalContentSearchService externalContentSearchService) {
        this.userRepository = userRepository;
        this.userContentRepository = userContentRepository;
        this.contentRepository = contentRepository;
        this.customListRepository = customListRepository;
        this.userAccountService = userAccountService;
        this.friendshipRepository = friendshipRepository;
        this.externalContentSearchService = externalContentSearchService;
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
        stats.put("peliculas", userContentRepository.countByUserIdAndContentTypeIgnoreCase(user.getId(), "pelicula"));
        stats.put("series", userContentRepository.countByUserIdAndContentTypeIgnoreCase(user.getId(), "serie"));
        stats.put("libros", userContentRepository.countByUserIdAndContentTypeIgnoreCase(user.getId(), "libro"));
        stats.put("discos", userContentRepository.countByUserIdAndContentTypeIgnoreCase(user.getId(), "disco"));
        
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
        // Búsqueda insensible a mayúsculas/minúsculas para mayor robustez
        return ResponseEntity.ok(userContentRepository.findByUserIdAndContentTypeIgnoreCase(user.getId(), type));
    }

    @PostMapping("/content/{id}/delete")
    public ResponseEntity<?> deleteContent(@PathVariable Long id, HttpSession session) {
        User user = getSessionUser(session);
        if (user == null) {
            List<User> users = userRepository.findAll();
            if(!users.isEmpty()) user = users.get(0);
            else return ResponseEntity.status(401).body("No autorizado");
        }
        
        System.out.println("[DEBUG] Intentando borrar id: " + id + " para usuario: " + user.getUsername());
        
        UserContent uc = userContentRepository.findById(id).orElse(null);
        if (uc != null) {
            // Permitir borrar si es el dueño o si estamos en modo fallback/dev
            if (uc.getUser().getId().equals(user.getId()) || userRepository.count() == 1) {
                userContentRepository.delete(uc);
                return ResponseEntity.ok().build();
            } else {
                System.out.println("[DEBUG] Mismatch de usuario: uc_user=" + uc.getUser().getId() + " session_user=" + user.getId());
            }
        } else {
            System.out.println("[DEBUG] No se encontró UserContent con id: " + id);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No encontrado");
    }

    @PostMapping("/content/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body, HttpSession session) {
        User user = getSessionUser(session);
        if (user == null) {
            List<User> users = userRepository.findAll();
            if(!users.isEmpty()) user = users.get(0);
            else return ResponseEntity.status(401).body("No autorizado");
        }

        UserContent uc = userContentRepository.findById(id).orElse(null);
        if (uc != null) {
            uc.setStatus(body.get("status"));
            userContentRepository.save(uc);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(404).body("No encontrado");
    }

    @PostMapping("/content/{id}/progress")
    public ResponseEntity<?> updateProgress(@PathVariable Long id, @RequestBody Map<String, Integer> body, HttpSession session) {
        User user = getSessionUser(session);
        if (user == null) {
            List<User> users = userRepository.findAll();
            if(!users.isEmpty()) user = users.get(0);
            else return ResponseEntity.status(401).body("No autorizado");
        }

        UserContent uc = userContentRepository.findById(id).orElse(null);
        if (uc != null) {
            if (body.containsKey("currentPage")) uc.setBookCurrentPage(body.get("currentPage"));
            if (body.containsKey("totalPages")) uc.setBookTotalPages(body.get("totalPages"));
            // Podríamos añadir más según necesidad
            userContentRepository.save(uc);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(404).body("No encontrado");
    }

    @PostMapping("/content/{id}/favorite")
    public ResponseEntity<?> toggleFavorite(@PathVariable Long id, HttpSession session) {
        User user = getSessionUser(session);
        if (user == null) {
            List<User> users = userRepository.findAll();
            if(!users.isEmpty()) user = users.get(0);
            else return ResponseEntity.status(401).body("No autorizado");
        }

        UserContent uc = userContentRepository.findById(id).orElse(null);
        if (uc != null && (uc.getUser().getId().equals(user.getId()) || userRepository.count() == 1)) {
            uc.setFavorite(!uc.getFavorite());
            userContentRepository.save(uc);
            return ResponseEntity.ok(Map.of("favorite", uc.getFavorite()));
        }
        return ResponseEntity.status(404).body("No encontrado");
    }


    @PostMapping("/content/{id}/rating")
    public ResponseEntity<?> updateRating(@PathVariable Long id, @RequestBody Map<String, Object> body, HttpSession session) {
        User user = getSessionUser(session);
        if (user == null) {
            List<User> users = userRepository.findAll();
            if(!users.isEmpty()) user = users.get(0);
            else return ResponseEntity.status(401).body("No autorizado");
        }

        UserContent uc = userContentRepository.findById(id).orElse(null);
        if (uc != null) {
            Object rating = body.get("rating");
            if (rating instanceof Number) {
                uc.setRating(((Number) rating).doubleValue());
                userContentRepository.save(uc);
                return ResponseEntity.ok().build();
            }
        }
        return ResponseEntity.status(404).body("No encontrado");
    }

    @PostMapping("/content/{id}/top-rank")
    public ResponseEntity<?> updateTopRank(@PathVariable Long id, @RequestBody Map<String, Integer> body, HttpSession session) {
        User user = getSessionUser(session);
        if (user == null) {
            List<User> users = userRepository.findAll();
            if(!users.isEmpty()) user = users.get(0);
            else return ResponseEntity.status(401).body("No autorizado");
        }

        UserContent uc = userContentRepository.findById(id).orElse(null);
        if (uc != null) {
            Integer rank = body.get("topRank");
            // Clear existing rank if needed
            if (rank != null) {
                List<UserContent> others = userContentRepository.findByUserId(user.getId());
                for (UserContent other : others) {
                    if (rank.equals(other.getTopRank()) && !other.getId().equals(uc.getId())) {
                        other.setTopRank(null);
                        userContentRepository.save(other);
                    }
                }
            }
            uc.setTopRank(rank);
            userContentRepository.save(uc);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(404).body("No encontrado");
    }

    @PostMapping("/content/{id}/completion-date")
    public ResponseEntity<?> updateCompletionDate(@PathVariable Long id, @RequestBody Map<String, String> body, HttpSession session) {
        User user = getSessionUser(session);
        if (user == null) {
            List<User> users = userRepository.findAll();
            if(!users.isEmpty()) user = users.get(0);
            else return ResponseEntity.status(401).body("No autorizado");
        }

        UserContent uc = userContentRepository.findById(id).orElse(null);
        if (uc != null) {
            String date = body.get("completionDate");
            if (date == null || date.isBlank()) {
                uc.setCompletionDate(null);
            } else {
                uc.setCompletionDate(java.time.LocalDate.parse(date));
            }
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

        // Normalizar tipos (Consistente con UserContentController)
        if (contentType != null) {
            String lowerType = contentType.trim().toLowerCase();
            if (lowerType.contains("pelicula") || lowerType.contains("movie")) contentType = "pelicula";
            else if (lowerType.contains("serie") || lowerType.contains("tv")) contentType = "serie";
            else if (lowerType.contains("libro") || lowerType.contains("book")) contentType = "libro";
            else if (lowerType.contains("disco") || lowerType.contains("album") || lowerType.contains("musica") || lowerType.contains("music")) contentType = "disco";
            else contentType = lowerType;
        }

        System.out.println("[DEBUG] Intentando añadir contenido: " + title + " (" + contentType + ") id: " + externalId);

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
        userContent.setAddedDate(LocalDate.now());

        String customStatus = body.get("status");
        String completionDateStr = body.get("completionDate");
        String ratingStr = body.get("rating");
        String favoriteStr = body.get("favorite");

        if (ratingStr != null && !ratingStr.isBlank()) {
            try {
                userContent.setRating(Double.parseDouble(ratingStr));
            } catch (Exception e) {}
        }
        
        if (favoriteStr != null) {
            userContent.setFavorite(Boolean.parseBoolean(favoriteStr));
        }

        if (customStatus != null && !customStatus.isBlank()) {
            String st = customStatus.trim().toLowerCase();
            if ("visto".equals(st) || "leido".equals(st) || "completado".equals(st)) {
                if ("pelicula".equals(contentType)) {
                    userContent.setStatus("visto");
                    userContent.setMovieWatched(true);
                } else if ("libro".equals(contentType)) {
                    userContent.setStatus("leido");
                } else if ("serie".equals(contentType)) {
                    userContent.setStatus("visto");
                } else {
                    userContent.setStatus("completado");
                }
            } else {
                if ("pelicula".equals(contentType)) {
                    userContent.setStatus("no_visto");
                    userContent.setMovieWatched(false);
                } else if ("libro".equals(contentType)) {
                    userContent.setStatus("no_iniciado");
                } else if ("serie".equals(contentType)) {
                    userContent.setStatus("seguimiento_episodios");
                } else {
                    userContent.setStatus("seguimiento_canciones");
                }
            }
        } else {
            userContent.setStatus("PLANNING");
        }

        if (completionDateStr != null && !completionDateStr.isBlank()) {
            try {
                userContent.setCompletionDate(LocalDate.parse(completionDateStr));
            } catch (Exception e) {
                System.err.println("[WARN] Invalid completionDate format: " + completionDateStr);
            }
        }

        // Fetch deep details to populate tracks/episodes/pages
        try {
            if ("disco".equalsIgnoreCase(contentType) || "album".equalsIgnoreCase(contentType) || "musica".equalsIgnoreCase(contentType)) {
                Map<String, Object> details = externalContentSearchService.getDetails("Spotify", externalId, "musica");
                if (details != null) {
                    if (details.containsKey("totalTracks")) {
                        userContent.setAlbumTotalTracks((Integer) details.get("totalTracks"));
                    }
                    if (details.containsKey("trackListString")) {
                        userContent.setAlbumTrackList((String) details.get("trackListString"));
                    }
                    if (content.getCoverUrl() == null || content.getCoverUrl().isBlank()) {
                        String cover = (String) details.get("coverUrl");
                        if (cover != null && !cover.isBlank()) {
                            content.setCoverUrl(cover);
                            contentRepository.save(content);
                        }
                    }
                }
            } else if ("serie".equalsIgnoreCase(contentType) || "tv".equalsIgnoreCase(contentType)) {
                Map<String, Object> details = externalContentSearchService.getDetails("TMDb", externalId, "serie");
                if (details != null) {
                    if (details.containsKey("totalSeasons")) {
                        userContent.setSeriesTotalSeasons((Integer) details.get("totalSeasons"));
                    }
                    if (details.containsKey("totalEpisodes")) {
                        userContent.setSeriesTotalEpisodes((Integer) details.get("totalEpisodes"));
                    }
                    if (details.containsKey("seasonData")) {
                        userContent.setSeriesSeasonData((String) details.get("seasonData"));
                    }
                    if (content.getCoverUrl() == null || content.getCoverUrl().isBlank()) {
                        String cover = (String) details.get("coverUrl");
                        if (cover != null && !cover.isBlank()) {
                            content.setCoverUrl(cover);
                            contentRepository.save(content);
                        }
                    }
                }
            } else if ("libro".equalsIgnoreCase(contentType) || "book".equalsIgnoreCase(contentType)) {
                String source = (externalId != null && (externalId.toLowerCase().contains("works") || externalId.toUpperCase().contains("OL"))) 
                                ? "OpenLibrary" : "GoogleBooks";
                Map<String, Object> details = externalContentSearchService.getDetails(source, externalId, "libro");
                if (details != null) {
                    if (details.containsKey("totalPages")) {
                        Integer total = (Integer) details.get("totalPages");
                        userContent.setBookTotalPages(total);
                        if ("leido".equals(userContent.getStatus())) {
                            userContent.setBookCurrentPage(total);
                        } else {
                            userContent.setBookCurrentPage(0);
                        }
                    } else {
                        userContent.setBookCurrentPage(0);
                        if (details.containsKey("description") && details.get("description") != null) {
                            if (content.getDescription() == null || content.getDescription().isBlank()) {
                                content.setDescription((String) details.get("description"));
                                contentRepository.save(content);
                            }
                        }
                    }
                    if (content.getCoverUrl() == null || content.getCoverUrl().isBlank()) {
                        String cover = (String) details.get("coverUrl");
                        if (cover != null && !cover.isBlank()) {
                            content.setCoverUrl(cover);
                            contentRepository.save(content);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[WARN] Could not fetch deep details for content addition: " + e.getMessage());
        }

        userContentRepository.save(userContent);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/profile/update")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> body, HttpSession session) {
        User user = getSessionUser(session);
        if (user == null) {
            List<User> users = userRepository.findAll();
            if(!users.isEmpty()) user = users.get(0);
            else return ResponseEntity.status(401).body("No autorizado");
        }

        String username = body.get("username");
        String email = body.get("email");
        String bio = body.get("bio");
        String favoriteGenre = body.get("favoriteGenre");
        String profilePicture = body.get("profilePicture");

        if (username != null && !username.isBlank()) {
            java.util.Optional<User> existing = userRepository.findByUsername(username.trim());
            if (existing.isPresent() && !existing.get().getId().equals(user.getId())) {
                return ResponseEntity.badRequest().body("El nombre de usuario ya está en uso");
            }
            user.setUsername(username.trim());
        }

        if (email != null && !email.isBlank()) {
            user.setEmail(email.trim());
        }

        if (bio != null) {
            user.setBio(bio.trim());
        }

        if (favoriteGenre != null) {
            user.setFavoriteGenre(favoriteGenre.trim());
        }

        if (profilePicture != null) {
            user.setProfilePicture(profilePicture);
        }

        User saved = userRepository.save(user);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/friends/request")
    public ResponseEntity<?> sendFriendRequest(@RequestBody Map<String, Object> body, HttpSession session) {
        User requester = getSessionUser(session);
        if (requester == null) return ResponseEntity.status(401).body("No autorizado");

        User receiver = null;
        if (body.get("receiverId") != null) {
            Long receiverId = Long.valueOf(body.get("receiverId").toString());
            receiver = userRepository.findById(receiverId).orElse(null);
        } else if (body.get("username") != null) {
            String username = (String) body.get("username");
            receiver = userRepository.findByUsername(username).orElse(null);
        }

        if (receiver == null) return ResponseEntity.status(404).body("Usuario no encontrado");

        if (receiver.getId().equals(requester.getId())) {
            return ResponseEntity.badRequest().body("No puedes agregarte a ti mismo");
        }

        if (friendshipRepository.findBetweenUsers(requester, receiver).isPresent()) {
            return ResponseEntity.badRequest().body("Ya existe una solicitud o amistad");
        }

        Friendship friendship = new Friendship();
        friendship.setRequester(requester);
        friendship.setReceiver(receiver);
        friendship.setStatus(Friendship.FriendshipStatus.PENDING);
        friendshipRepository.save(friendship);

        return ResponseEntity.ok(Map.of("message", "Solicitud enviada"));
    }

    @GetMapping("/community/members")
    public ResponseEntity<?> getCommunityMembers(HttpSession session) {
        User user = getSessionUser(session);
        if (user == null) {
            List<User> users = userRepository.findAll();
            if(!users.isEmpty()) user = users.get(0);
            else return ResponseEntity.status(401).body("No autorizado");
        }

        List<User> allUsers = userRepository.findAll();
        List<Map<String, Object>> members = new java.util.ArrayList<>();

        for (User u : allUsers) {
            Map<String, Object> memberInfo = new HashMap<>();
            memberInfo.put("id", u.getId());
            memberInfo.put("username", u.getUsername());
            memberInfo.put("bio", u.getBio() != null ? u.getBio() : "Sin biografía aún.");
            memberInfo.put("favoriteGenre", u.getFavoriteGenre() != null ? u.getFavoriteGenre() : "Todo");
            memberInfo.put("profilePicture", u.getProfilePicture());
            memberInfo.put("createdAt", u.getCreatedAt());
            
            // Determine social relationship status
            String relationStatus = "NONE";
            if (user != null && !user.getId().equals(u.getId())) {
                java.util.Optional<Friendship> fOpt = friendshipRepository.findBetweenUsers(user, u);
                if (fOpt.isPresent()) {
                    Friendship f = fOpt.get();
                    if (f.getStatus() == Friendship.FriendshipStatus.ACCEPTED) {
                        relationStatus = "ACCEPTED";
                    } else if (f.getStatus() == Friendship.FriendshipStatus.PENDING) {
                        if (f.getRequester().getId().equals(user.getId())) {
                            relationStatus = "PENDING_SENT";
                        } else {
                            relationStatus = "PENDING_RECEIVED";
                        }
                    }
                }
            } else if (user != null && user.getId().equals(u.getId())) {
                relationStatus = "SELF";
            }
            memberInfo.put("relationStatus", relationStatus);
            
            Map<String, Long> memberStats = new HashMap<>();
            memberStats.put("peliculas", userContentRepository.countByUserIdAndContentTypeIgnoreCase(u.getId(), "PELICULA"));
            memberStats.put("series", userContentRepository.countByUserIdAndContentTypeIgnoreCase(u.getId(), "SERIE"));
            memberStats.put("libros", userContentRepository.countByUserIdAndContentTypeIgnoreCase(u.getId(), "LIBRO"));
            memberStats.put("discos", userContentRepository.countByUserIdAndContentTypeIgnoreCase(u.getId(), "DISCO"));
            
            long total = userContentRepository.countByUserId(u.getId());
            memberInfo.put("stats", memberStats);
            memberInfo.put("totalCount", total);

            List<UserContent> publicItems = userContentRepository.findByUserId(u.getId());
            List<Map<String, Object>> showcasedItems = new java.util.ArrayList<>();
            int count = 0;
            for (UserContent uc : publicItems) {
                if (uc.getFavorite() != null && uc.getFavorite()) {
                    Map<String, Object> itemMap = new HashMap<>();
                    itemMap.put("title", uc.getContent().getTitle());
                    itemMap.put("type", uc.getContent().getType());
                    itemMap.put("coverUrl", uc.getContent().getCoverUrl());
                    showcasedItems.add(itemMap);
                    count++;
                    if (count >= 4) break;
                }
            }
            if (count < 4) {
                for (UserContent uc : publicItems) {
                    if (uc.getFavorite() == null || !uc.getFavorite()) {
                        Map<String, Object> itemMap = new HashMap<>();
                        itemMap.put("title", uc.getContent().getTitle());
                        itemMap.put("type", uc.getContent().getType());
                        itemMap.put("coverUrl", uc.getContent().getCoverUrl());
                        showcasedItems.add(itemMap);
                        count++;
                        if (count >= 4) break;
                    }
                }
            }
            memberInfo.put("showcase", showcasedItems);
            members.add(memberInfo);
        }

        return ResponseEntity.ok(members);
    }

    @GetMapping("/friends")
    public ResponseEntity<?> getFriends(HttpSession session) {
        User user = getSessionUser(session);
        if (user == null) return ResponseEntity.status(401).body("No autorizado");

        List<Friendship> friends = friendshipRepository.findByRequesterOrReceiverAndStatus(user, user, Friendship.FriendshipStatus.ACCEPTED);
        return ResponseEntity.ok(friends);
    }

    @PostMapping("/friends/accept")
    public ResponseEntity<?> acceptFriendRequest(@RequestBody Map<String, String> body, HttpSession session) {
        User receiver = getSessionUser(session);
        if (receiver == null) {
            List<User> users = userRepository.findAll();
            if(!users.isEmpty()) receiver = users.get(0);
            else return ResponseEntity.status(401).body("No autorizado");
        }

        String username = body.get("username");
        if (username == null || username.isBlank()) return ResponseEntity.badRequest().body("Usuario vacío");

        java.util.Optional<User> requesterOpt = userRepository.findByUsername(username.trim());
        if (requesterOpt.isEmpty()) {
            return ResponseEntity.status(404).body("Usuario no encontrado");
        }

        User requester = requesterOpt.get();
        java.util.Optional<Friendship> fOpt = friendshipRepository.findByRequesterAndReceiver(requester, receiver);
        if (fOpt.isPresent()) {
            Friendship f = fOpt.get();
            f.setStatus(Friendship.FriendshipStatus.ACCEPTED);
            friendshipRepository.save(f);
            return ResponseEntity.ok(Map.of("message", "Solicitud aceptada"));
        }
        return ResponseEntity.status(404).body("Solicitud no encontrada");
    }
}
