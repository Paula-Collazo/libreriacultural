package com.proyectofinal.libreriacultural.controllers;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.proyectofinal.libreriacultural.Repositories.ContentRepository;
import com.proyectofinal.libreriacultural.Repositories.UserContentRepository;
import com.proyectofinal.libreriacultural.Repositories.UserRepository;
import com.proyectofinal.libreriacultural.Repositories.UserSeriesEpisodeProgressRepository;
import com.proyectofinal.libreriacultural.Repositories.UserSongProgressRepository;
import com.proyectofinal.libreriacultural.domain.Content;
import com.proyectofinal.libreriacultural.domain.User;
import com.proyectofinal.libreriacultural.domain.UserContent;
import com.proyectofinal.libreriacultural.domain.UserSeriesEpisodeProgress;
import com.proyectofinal.libreriacultural.domain.UserSongProgress;

@RestController
@RequestMapping("/library")
public class UserContentController {

    private static final Set<String> VALID_CONTENT_TYPES = Set.of("pelicula", "serie", "libro", "disco");
    private static final Set<String> VALID_GENERAL_STATUSES = Set.of("pendiente", "en_progreso", "abandonado");
    private static final Set<String> VALID_MOVIE_STATUSES = Set.of("visto", "no_visto");

    private final UserContentRepository userContentRepository;

    private final UserRepository userRepository;

    private final ContentRepository contentRepository;

    private final UserSeriesEpisodeProgressRepository userSeriesEpisodeProgressRepository;

    private final UserSongProgressRepository userSongProgressRepository;

    public UserContentController(UserContentRepository userContentRepository, UserRepository userRepository,
            ContentRepository contentRepository,
            UserSeriesEpisodeProgressRepository userSeriesEpisodeProgressRepository,
            UserSongProgressRepository userSongProgressRepository) {
        this.userContentRepository = userContentRepository;
        this.userRepository = userRepository;
        this.contentRepository = contentRepository;
        this.userSeriesEpisodeProgressRepository = userSeriesEpisodeProgressRepository;
        this.userSongProgressRepository = userSongProgressRepository;
    }

    @GetMapping("/user/{userId}")
    public List<UserContent> getByUser(@PathVariable Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado");
        }
        return userContentRepository.findByUserId(userId);
    }

    @GetMapping("/user/{userId}/type/{type}")
    public List<UserContent> getByUserAndType(@PathVariable Long userId, @PathVariable String type) {
        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado");
        }

        String normalizedType = normalizeAndValidateType(type);
        return userContentRepository.findByUserIdAndContentType(userId, normalizedType);
    }

    @GetMapping("/user/{userId}/status/{status}")
    public List<UserContent> getByUserAndStatus(@PathVariable Long userId, @PathVariable String status) {
        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado");
        }

        String normalizedStatus = normalizeAndValidateGeneralStatus(status);
        return userContentRepository.findByUserIdAndStatus(userId, normalizedStatus);
    }

    @GetMapping("/user/{userId}/stats")
    public LibraryStatsResponse getUserStats(@PathVariable Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado");
        }

        return new LibraryStatsResponse(
                userContentRepository.countByUserId(userId),
                Map.of(
                "pelicula", userContentRepository.countByUserIdAndContentType(userId, "pelicula"),
                "serie", userContentRepository.countByUserIdAndContentType(userId, "serie"),
                "libro", userContentRepository.countByUserIdAndContentType(userId, "libro"),
                "disco", userContentRepository.countByUserIdAndContentType(userId, "disco")),
            Map.of(
                        "visto", userContentRepository.countByUserIdAndStatus(userId, "visto"),
                "no_visto", userContentRepository.countByUserIdAndStatus(userId, "no_visto")));
    }

    @PostMapping
    public UserContent addToLibrary(@RequestBody AddToLibraryRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        Content content = contentRepository.findById(request.contentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contenido no encontrado"));

        String contentType = normalizeAndValidateType(content.getType());

        if (userContentRepository.existsByUserIdAndContentId(user.getId(), content.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El contenido ya esta en la biblioteca de este usuario");
        }

        UserContent userContent = new UserContent();
        userContent.setUser(user);
        userContent.setContent(content);
        userContent.setAddedDate(LocalDate.now());

        applyTypeSpecificData(userContent, contentType, request);

        try {
            return userContentRepository.save(userContent);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El contenido ya esta en la biblioteca de este usuario");
        }
    }

    @PutMapping("/{userContentId}/status")
    public UserContent updateStatus(@PathVariable Long userContentId, @RequestBody UpdateStatusRequest request) {
        UserContent userContent = userContentRepository.findById(userContentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registro de biblioteca no encontrado"));

        String type = normalizeAndValidateType(userContent.getContent().getType());

        if ("pelicula".equals(type)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Para peliculas usa /library/{id}/movie-status con visto/no_visto");
        }

        if ("libro".equals(type)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Para libros usa /library/{id}/book-progress con paginas");
        }

        userContent.setStatus(normalizeAndValidateGeneralStatus(request.status()));
        
        // Si el estado es visto/leído/completado, ponemos la fecha de hoy si no existe
        if (isCompletedStatus(userContent.getStatus()) && userContent.getCompletionDate() == null) {
            userContent.setCompletionDate(LocalDate.now());
        }
        
        return userContentRepository.save(userContent);
    }

    private boolean isCompletedStatus(String status) {
        return "visto".equals(status) || "leido".equals(status) || "completado".equals(status);
    }

    @PutMapping("/{userContentId}/movie-status")
    public UserContent updateMovieStatus(@PathVariable Long userContentId, @RequestBody UpdateMovieStatusRequest request) {
        UserContent userContent = getUserContentById(userContentId);
        ensureType(userContent, "pelicula");

        String normalizedMovieStatus = normalizeAndValidateMovieStatus(request.status());
        userContent.setMovieWatched("visto".equals(normalizedMovieStatus));
        userContent.setStatus(normalizedMovieStatus);
        
        if ("visto".equals(normalizedMovieStatus) && userContent.getCompletionDate() == null) {
            userContent.setCompletionDate(LocalDate.now());
        }

        return userContentRepository.save(userContent);
    }

    @PutMapping("/{userContentId}/book-progress")
    public UserContent updateBookProgress(@PathVariable Long userContentId, @RequestBody UpdateBookProgressRequest request) {
        UserContent userContent = getUserContentById(userContentId);
        ensureType(userContent, "libro");

        int currentPage = request.currentPage() == null ? defaultPageValue(userContent.getBookCurrentPage())
            : defaultPageValue(request.currentPage());
        int totalPages = request.totalPages() == null ? defaultPageValue(userContent.getBookTotalPages())
            : defaultPageValue(request.totalPages());

        validateBookPages(currentPage, totalPages);

        userContent.setBookCurrentPage(currentPage);
        userContent.setBookTotalPages(totalPages);
        String status = resolveBookStatus(currentPage, totalPages);
        userContent.setStatus(status);

        if ("leido".equals(status) && userContent.getCompletionDate() == null) {
            userContent.setCompletionDate(LocalDate.now());
        }

        return userContentRepository.save(userContent);
    }

    @PutMapping("/{userContentId}/completion-date")
    public UserContent updateCompletionDate(@PathVariable Long userContentId, @RequestBody Map<String, String> body) {
        UserContent userContent = getUserContentById(userContentId);
        String dateStr = body.get("completionDate");
        if (dateStr == null || dateStr.isBlank()) {
            userContent.setCompletionDate(null);
        } else {
            userContent.setCompletionDate(LocalDate.parse(dateStr));
        }
        return userContentRepository.save(userContent);
    }

    @PutMapping("/{userContentId}/top-rank")
    public UserContent updateTopRank(@PathVariable Long userContentId, @RequestBody Map<String, Integer> body) {
        UserContent userContent = getUserContentById(userContentId);
        Integer rank = body.get("topRank");
        
        if (rank != null && (rank < 1 || rank > 4)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El rank debe estar entre 1 y 4");
        }
        
        if (rank != null) {
            String type = userContent.getContent().getType();
            List<UserContent> others = userContentRepository.findByUserId(userContent.getUser().getId())
                .stream()
                .filter(uc -> type.equalsIgnoreCase(uc.getContent().getType()) && rank.equals(uc.getTopRank()))
                .toList();
            for (UserContent other : others) {
                other.setTopRank(null);
                userContentRepository.save(other);
            }
        }
        
        userContent.setTopRank(rank);
        return userContentRepository.save(userContent);
    }

    @PutMapping("/{userContentId}/rating")
    public UserContent updateRating(@PathVariable Long userContentId, @RequestBody Map<String, Object> body) {
        UserContent userContent = getUserContentById(userContentId);
        Object ratingObj = body.get("rating");
        Double rating = null;
        if (ratingObj instanceof Number) {
            rating = ((Number) ratingObj).doubleValue();
        }
        
        if (rating != null && (rating < 0.0 || rating > 5.0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La puntuación debe estar entre 0 y 5");
        }
        
        userContent.setRating(rating);
        return userContentRepository.save(userContent);
    }

    @GetMapping("/user/{userId}/time-stats")
    public Map<String, Object> getTimeStats(@PathVariable Long userId) {
        List<UserContent> all = userContentRepository.findByUserId(userId);
        LocalDate now = LocalDate.now();
        LocalDate weekStart = now.minusDays(7);
        LocalDate monthStart = now.minusMonths(1);
        
        long thisWeek = all.stream()
            .filter(uc -> uc.getCompletionDate() != null && !uc.getCompletionDate().isBefore(weekStart))
            .count();
            
        long thisMonth = all.stream()
            .filter(uc -> uc.getCompletionDate() != null && !uc.getCompletionDate().isBefore(monthStart))
            .count();

        Map<String, Long> history = all.stream()
            .filter(uc -> uc.getCompletionDate() != null)
            .collect(java.util.stream.Collectors.groupingBy(
                uc -> uc.getCompletionDate().toString(),
                java.util.stream.Collectors.counting()
            ));

        return Map.of(
            "thisWeek", thisWeek,
            "thisMonth", thisMonth,
            "history", history.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(java.util.stream.Collectors.toMap(
                    Map.Entry::getKey, 
                    Map.Entry::getValue,
                    (e1, e2) -> e1, 
                    java.util.LinkedHashMap::new
                ))
        );
    }

    @GetMapping("/{userContentId}/episodes")
    public List<UserSeriesEpisodeProgress> getSeriesEpisodes(@PathVariable Long userContentId) {
        UserContent userContent = getUserContentById(userContentId);
        ensureType(userContent, "serie");
        return userSeriesEpisodeProgressRepository.findByUserContentIdOrderBySeasonNumberAscEpisodeNumberAsc(userContentId);
    }

    @PostMapping("/{userContentId}/episodes")
    public UserSeriesEpisodeProgress saveSeriesEpisode(@PathVariable Long userContentId,
            @RequestBody SaveEpisodeProgressRequest request) {
        UserContent userContent = getUserContentById(userContentId);
        ensureType(userContent, "serie");

        if (request.seasonNumber() == null || request.seasonNumber() < 1 || request.episodeNumber() == null
                || request.episodeNumber() < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Temporada y episodio deben ser >= 1");
        }

        boolean watched = request.watched() != null && request.watched();
        UserSeriesEpisodeProgress progress = userSeriesEpisodeProgressRepository
                .findByUserContentIdAndSeasonNumberAndEpisodeNumber(userContentId, request.seasonNumber(),
                        request.episodeNumber())
                .orElseGet(UserSeriesEpisodeProgress::new);

        progress.setUserContent(userContent);
        progress.setSeasonNumber(request.seasonNumber());
        progress.setEpisodeNumber(request.episodeNumber());
        progress.setWatched(watched);

        return userSeriesEpisodeProgressRepository.save(progress);
    }

    @GetMapping("/{userContentId}/songs")
    public List<UserSongProgress> getAlbumSongs(@PathVariable Long userContentId) {
        UserContent userContent = getUserContentById(userContentId);
        ensureType(userContent, "disco");
        return userSongProgressRepository.findByUserContentIdOrderByTrackNumberAsc(userContentId);
    }

    @PostMapping("/{userContentId}/songs")
    public UserSongProgress saveAlbumSong(@PathVariable Long userContentId, @RequestBody SaveSongProgressRequest request) {
        UserContent userContent = getUserContentById(userContentId);
        ensureType(userContent, "disco");

        if (request.trackNumber() == null || request.trackNumber() < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "trackNumber debe ser >= 1");
        }

        if (request.trackTitle() == null || request.trackTitle().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "trackTitle es obligatorio");
        }

        boolean listened = request.listened() != null && request.listened();
        UserSongProgress progress = userSongProgressRepository.findByUserContentIdAndTrackNumber(userContentId,
                request.trackNumber()).orElseGet(UserSongProgress::new);

        progress.setUserContent(userContent);
        progress.setTrackNumber(request.trackNumber());
        progress.setTrackTitle(request.trackTitle().trim());
        progress.setListened(listened);

        return userSongProgressRepository.save(progress);
    }

    @PutMapping("/songs/{songId}/listened")
    public UserSongProgress toggleSongListened(@PathVariable Long songId) {
        UserSongProgress song = userSongProgressRepository.findById(songId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Canción no encontrada"));
        song.setListened(song.getListened() == null ? true : !song.getListened());
        return userSongProgressRepository.save(song);
    }

    @DeleteMapping("/{userContentId}")
    public void deleteFromLibrary(@PathVariable Long userContentId) {
        if (!userContentRepository.existsById(userContentId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Registro de biblioteca no encontrado");
        }
        userContentRepository.deleteById(userContentId);
    }

    private void applyTypeSpecificData(UserContent userContent, String type, AddToLibraryRequest request) {
        if ("pelicula".equals(type)) {
            String movieStatus = normalizeAndValidateMovieStatus(request.movieStatus());
            userContent.setMovieWatched("visto".equals(movieStatus));
            userContent.setStatus(movieStatus);
            userContent.setBookCurrentPage(null);
            userContent.setBookTotalPages(null);
            return;
        }

        if ("libro".equals(type)) {
            int currentPage = request.bookCurrentPage() == null ? 0 : defaultPageValue(request.bookCurrentPage());
            int totalPages = request.bookTotalPages() == null ? 0 : defaultPageValue(request.bookTotalPages());
            validateBookPages(currentPage, totalPages);

            userContent.setBookCurrentPage(currentPage);
            userContent.setBookTotalPages(totalPages);
            userContent.setStatus(resolveBookStatus(currentPage, totalPages));
            userContent.setMovieWatched(null);
            return;
        }

        if ("serie".equals(type)) {
            userContent.setStatus("seguimiento_episodios");
            userContent.setMovieWatched(null);
            userContent.setBookCurrentPage(null);
            userContent.setBookTotalPages(null);
            return;
        }

        userContent.setStatus("seguimiento_canciones");
        userContent.setMovieWatched(null);
        userContent.setBookCurrentPage(null);
        userContent.setBookTotalPages(null);
    }

    private void validateBookPages(int currentPage, int totalPages) {
        if (currentPage < 0 || totalPages < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Las paginas no pueden ser negativas");
        }

        if (totalPages > 0 && currentPage > totalPages) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La pagina actual no puede superar el total de paginas");
        }
    }

    private int defaultPageValue(Integer value) {
        return value == null ? 0 : value;
    }

    private String resolveBookStatus(int currentPage, int totalPages) {
        if (currentPage <= 0) {
            return "no_iniciado";
        }

        if (totalPages > 0 && currentPage >= totalPages) {
            return "leido";
        }

        return "leyendo";
    }

    private UserContent getUserContentById(Long userContentId) {
        return userContentRepository.findById(userContentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registro de biblioteca no encontrado"));
    }

    private void ensureType(UserContent userContent, String expectedType) {
        String currentType = normalizeAndValidateType(userContent.getContent().getType());
        if (!expectedType.equals(currentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Este endpoint solo aplica a contenidos de tipo " + expectedType);
        }
    }

    private String normalizeAndValidateType(String rawType) {
        if (rawType == null || rawType.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El contenido debe tener tipo: pelicula, serie, libro o disco");
        }

        String normalized = rawType.trim().toLowerCase();

        if (!VALID_CONTENT_TYPES.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Tipo no valido. Usa: pelicula, serie, libro o disco");
        }

        return normalized;
    }

    private String normalizeAndValidateMovieStatus(String rawStatus) {
        String normalized = (rawStatus == null || rawStatus.isBlank()) ? "no_visto" : rawStatus.trim().toLowerCase();

        if (!VALID_MOVIE_STATUSES.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Estado de pelicula no valido. Usa: visto o no_visto");
        }

        return normalized;
    }

    private String normalizeAndValidateGeneralStatus(String rawStatus) {
        String normalized = (rawStatus == null || rawStatus.isBlank()) ? "pendiente" : rawStatus.trim().toLowerCase();

        if (!VALID_GENERAL_STATUSES.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Estado no valido para este tipo. Usa: pendiente, en_progreso o abandonado");
        }

        return normalized;
    }

    @PostMapping("/{id}/favorite")
    public UserContent toggleFavorite(@PathVariable Long id) {
        UserContent uc = userContentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registro no encontrado"));
        uc.setFavorite(uc.getFavorite() == null ? true : !uc.getFavorite());
        return userContentRepository.save(uc);
    }

    @PostMapping("/{id}/progress")
    public UserContent updateProgress(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        UserContent uc = userContentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registro no encontrado"));
        
        if (body.containsKey("currentPage")) uc.setBookCurrentPage(body.get("currentPage"));
        if (body.containsKey("totalPages")) uc.setBookTotalPages(body.get("totalPages"));
        
        return userContentRepository.save(uc);
    }

    public record AddToLibraryRequest(Long userId, Long contentId, String movieStatus, Integer bookCurrentPage,
            Integer bookTotalPages) {
    }

    public record UpdateStatusRequest(String status) {
    }

    public record UpdateMovieStatusRequest(String status) {
    }

    public record UpdateBookProgressRequest(Integer currentPage, Integer totalPages) {
    }

    public record SaveEpisodeProgressRequest(Integer seasonNumber, Integer episodeNumber, Boolean watched) {
    }

    public record SaveSongProgressRequest(Integer trackNumber, String trackTitle, Boolean listened) {
    }

    public record LibraryStatsResponse(Long total, Map<String, Long> byType, Map<String, Long> byMovieStatus) {
    }
}
