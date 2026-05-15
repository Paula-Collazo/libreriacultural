package com.proyectofinal.libreriacultural.controllers;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
import com.proyectofinal.libreriacultural.services.ExternalAlbumDetails;
import com.proyectofinal.libreriacultural.services.ExternalContentItem;
import com.proyectofinal.libreriacultural.services.ExternalContentSearchService;
import com.proyectofinal.libreriacultural.services.UserAccountService;

import jakarta.servlet.http.HttpSession;

@Controller
public class SessionViewController {

    private static final String SESSION_USER_ID = "sessionUserId";
    private static final Set<String> VALID_CONTENT_TYPES = Set.of("pelicula", "serie", "libro", "disco");
    private static final Set<String> VALID_GENERAL_STATUSES = Set.of("pendiente", "en_progreso", "abandonado");
    private static final Set<String> VALID_MOVIE_STATUSES = Set.of("visto", "no_visto");

    private final UserContentRepository userContentRepository;
    private final UserRepository userRepository;
    private final ContentRepository contentRepository;
    private final UserSeriesEpisodeProgressRepository userSeriesEpisodeProgressRepository;
    private final UserSongProgressRepository userSongProgressRepository;
    private final ExternalContentSearchService externalContentSearchService;
    private final UserAccountService userAccountService;

    public SessionViewController(UserContentRepository userContentRepository, UserRepository userRepository,
            ContentRepository contentRepository,
            UserSeriesEpisodeProgressRepository userSeriesEpisodeProgressRepository,
            UserSongProgressRepository userSongProgressRepository,
            ExternalContentSearchService externalContentSearchService,
            UserAccountService userAccountService) {
        this.userContentRepository = userContentRepository;
        this.userRepository = userRepository;
        this.contentRepository = contentRepository;
        this.userSeriesEpisodeProgressRepository = userSeriesEpisodeProgressRepository;
        this.userSongProgressRepository = userSongProgressRepository;
        this.externalContentSearchService = externalContentSearchService;
        this.userAccountService = userAccountService;
    }

    @GetMapping("/")
    public String home(Model model, HttpSession session) {
        User sessionUser = getSessionUser(session);
        if (sessionUser != null) {
            return "redirect:/profile";
        }

        List<Content> featured = contentRepository.findAll().stream().limit(8).toList();
        model.addAttribute("featured", featured);
        return "index";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password,
            RedirectAttributes redirectAttributes, HttpSession session) {
        // Delegamos la verificación en UserAccountService (usa BCrypt)
        User user = userAccountService.authenticate(username, password);

        if (user == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Usuario o contraseña incorrectos");
            return "redirect:/";
        }

        session.setAttribute(SESSION_USER_ID, user.getId());
        return "redirect:/profile";
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    @GetMapping("/explore")
    public String exploreRoot() {
        return "redirect:/explore/pelicula";
    }

    @GetMapping("/explore/{type}")
    public String exploreByType(@PathVariable String type,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String artistId,
            @RequestParam(required = false) String author,
            Model model,
            HttpSession session) {
        User sessionUser = getSessionUser(session);

        String normalizedType;
        try {
            normalizedType = normalizeAndValidateType(type);
        } catch (IllegalArgumentException ex) {
            return "redirect:/explore/pelicula";
        }

        String query = q == null ? "" : q.trim();
        String effectiveQuery = query;
        if ("libro".equals(normalizedType) && author != null && !author.isBlank()) {
            query = author.trim();
            effectiveQuery = "inauthor:" + query;
        }

        List<ExternalContentItem> results = List.of();
        try {
            if ("disco".equals(normalizedType) && artistId != null && !artistId.isBlank()) {
                results = externalContentSearchService.searchArtistAlbums(artistId.trim());
                model.addAttribute("artistFilter", resolveArtistName(results));
            } else if (query.isBlank() && "pelicula".equals(normalizedType)) {
                results = externalContentSearchService.getWeeklyTrendingMovies();
                if (results.isEmpty()) {
                    effectiveQuery = defaultExploreQuery(normalizedType);
                    results = externalContentSearchService.search(effectiveQuery, normalizedType);
                }
            } else if (query.isBlank() && "serie".equals(normalizedType)) {
                results = externalContentSearchService.getWeeklyTrendingSeries();
                if (results.isEmpty()) {
                    effectiveQuery = defaultExploreQuery(normalizedType);
                    results = externalContentSearchService.search(effectiveQuery, normalizedType);
                }
            } else if (query.isBlank() && "disco".equals(normalizedType)) {
                results = externalContentSearchService.getWeeklyTrendingDiscs();
            } else if (query.isBlank() && "libro".equals(normalizedType)) {
                results = externalContentSearchService.getWeeklyTrendingBooks();
                if (results.isEmpty()) {
                    effectiveQuery = defaultExploreQuery(normalizedType);
                    results = externalContentSearchService.search(effectiveQuery, normalizedType);
                }
            } else {
                if (effectiveQuery.isBlank()) {
                    effectiveQuery = defaultExploreQuery(normalizedType);
                }
                results = externalContentSearchService.search(effectiveQuery, normalizedType);
            }
        } catch (IllegalArgumentException ex) {
            model.addAttribute("apiErrorMessage", ex.getMessage());
        } catch (Exception ex) {
            model.addAttribute("apiErrorMessage", "No se pudo consultar la API externa en este momento");
        }

        model.addAttribute("user", sessionUser);
        model.addAttribute("activeType", normalizedType);
        model.addAttribute("typeLabel", displayTypeName(normalizedType));
        model.addAttribute("searchQuery", query);
        model.addAttribute("apiResults", results);
        return "explore";
    }

    @GetMapping("/explore/disco/{id}")
    public String exploreAlbumDetail(@PathVariable String id, Model model, HttpSession session) {
        User sessionUser = getSessionUser(session);

        try {
            Map<String, Object> details = externalContentSearchService.getDetails("Spotify", id, "musica");
            if (details == null || details.isEmpty()) return "redirect:/explore/disco";
            
            model.addAttribute("user", sessionUser);
            model.addAttribute("album", details);
            Object tracks = details.get("tracks");
            model.addAttribute("tracks", tracks != null ? tracks : List.of());
            
            String artist = (String) details.get("artist");
            if (artist != null && !artist.isBlank()) {
                model.addAttribute("relatedAlbums", externalContentSearchService.searchSpotify(artist).stream().limit(6).collect(Collectors.toList()));
            }
            
            model.addAttribute("activeType", "disco");
            return "album";
        } catch (Exception ex) {
            System.err.println("[ERROR] Detalle Disco: " + ex.getMessage());
            return "redirect:/explore/disco";
        }
    }

    @GetMapping("/explore/pelicula/{id}")
    public String exploreMovieDetail(@PathVariable String id, Model model, HttpSession session) {
        User sessionUser = getSessionUser(session);

        try {
            Map<String, Object> movie = externalContentSearchService.getDetails("TMDb", id, "pelicula");
            if (movie == null || movie.isEmpty()) return "redirect:/explore/pelicula";
            
            model.addAttribute("user", sessionUser);
            model.addAttribute("movie", movie);
            model.addAttribute("actors", movie.get("actors"));
            model.addAttribute("activeType", "pelicula");
            return "movie";
        } catch (Exception ex) {
            System.err.println("[ERROR] Detalle Pelicula: " + ex.getMessage());
            return "redirect:/explore/pelicula";
        }
    }

    @GetMapping("/explore/actor/{name}")
    public String exploreActorDetail(@PathVariable String name, Model model, HttpSession session) {
        User sessionUser = getSessionUser(session);
        try {
            Map<String, Object> actor = externalContentSearchService.getActorDetails(name);
            model.addAttribute("user", sessionUser);
            model.addAttribute("actor", actor);
            model.addAttribute("activeType", "pelicula");
            return "actor";
        } catch (Exception ex) {
            System.err.println("Error cargando perfil de actor: " + ex.getMessage());
            ex.printStackTrace();
            return "redirect:/explore/pelicula";
        }
    }

    @GetMapping("/explore/serie/{id}")
    public String exploreSerieDetail(@PathVariable String id, Model model, HttpSession session) {
        User sessionUser = getSessionUser(session);

        try {
            Map<String, Object> serie = externalContentSearchService.getDetails("TMDb", id, "serie");
            if (serie == null || serie.isEmpty()) return "redirect:/explore/serie";
            
            model.addAttribute("user", sessionUser);
            model.addAttribute("movie", serie); // La plantilla 'serie.html' usa 'movie' como objeto base
            model.addAttribute("actors", serie.get("actors"));
            model.addAttribute("activeType", "serie");
            return "serie";
        } catch (Exception ex) {
            System.err.println("[ERROR] Detalle Serie: " + ex.getMessage());
            return "redirect:/explore/serie";
        }
    }

    @GetMapping("/explore/libro/{id}")
        public String exploreBookDetail(@PathVariable String id,
            @RequestParam(required = false) String author,
            Model model, HttpSession session) {
        User sessionUser = getSessionUser(session);

        try {
            Map<String, Object> book = externalContentSearchService.getDetails("GoogleBooks", id, "libro");
            if (book == null || book.isEmpty()) return "redirect:/explore/libro";
            
            if ((book.get("author") == null || String.valueOf(book.get("author")).isBlank())
                    && author != null && !author.isBlank()) {
                book.put("author", author.trim());
            }

            model.addAttribute("user", sessionUser);
            model.addAttribute("movie", book); // Cambio a 'movie' para que coincida con la plantilla libro.html
            
            String authorName = (String) book.get("author");
            if (authorName != null && !authorName.isBlank()) {
                String authorQuery = "inauthor:" + authorName.trim();
                model.addAttribute("relatedBooks", externalContentSearchService.searchGoogleBooks(authorQuery).stream().limit(6).collect(Collectors.toList()));
            }
            
            model.addAttribute("activeType", "libro");
            return "libro";
        } catch (Exception ex) {
            System.err.println("[ERROR] Detalle Libro: " + ex.getMessage());
            return "redirect:/explore/libro";
        }
    }


    @GetMapping("/profile")
    public String profile(@RequestParam(required = false) String apiQuery,
            @RequestParam(required = false, defaultValue = "pelicula") String apiType,
            Model model, HttpSession session) {
        User sessionUser = getSessionUser(session);
        if (sessionUser == null) {
            return "redirect:/";
        }

        List<UserContent> entries = userContentRepository.findByUserId(sessionUser.getId());
        List<UserContent> movies = new ArrayList<>();
        List<UserContent> series = new ArrayList<>();
        List<UserContent> books = new ArrayList<>();
        List<UserContent> discs = new ArrayList<>();

        for (UserContent entry : entries) {
            String type = normalizeType(entry.getContent().getType());
            switch (type) {
                case "pelicula" -> movies.add(entry);
                case "serie" -> series.add(entry);
                case "libro" -> books.add(entry);
                case "disco" -> discs.add(entry);
                default -> {
                    // Ignore unsupported legacy content types in profile grouping.
                }
            }
        }

        List<Content> contents = contentRepository.findAll();
        Map<Long, List<UserSeriesEpisodeProgress>> episodesByEntry = loadEpisodesByEntry(series);
        Map<Long, List<UserSongProgress>> songsByEntry = loadSongsByEntry(discs);

        String normalizedApiType = normalizeType(apiType);
        if (!VALID_CONTENT_TYPES.contains(normalizedApiType)) {
            normalizedApiType = "pelicula";
        }

        List<ExternalContentItem> apiResults = List.of();
        if (apiQuery != null && !apiQuery.isBlank()) {
            try {
                apiResults = externalContentSearchService.search(apiQuery, normalizedApiType);
            } catch (IllegalArgumentException ex) {
                model.addAttribute("apiErrorMessage", ex.getMessage());
            } catch (Exception ex) {
                model.addAttribute("apiErrorMessage", "No se pudo consultar la API externa en este momento");
            }
        }

        model.addAttribute("user", sessionUser);
        model.addAttribute("entries", entries);
        model.addAttribute("movieEntries", movies);
        model.addAttribute("seriesEntries", series);
        model.addAttribute("bookEntries", books);
        model.addAttribute("discEntries", discs);
        model.addAttribute("contents", contents);
        model.addAttribute("movieStatusOptions", List.of("no_visto", "visto"));
        model.addAttribute("generalStatusOptions", List.of("pendiente", "en_progreso", "abandonado"));
        model.addAttribute("episodesByEntry", episodesByEntry);
        model.addAttribute("songsByEntry", songsByEntry);
        model.addAttribute("stats", loadStats(sessionUser.getId()));
        model.addAttribute("typeStats", loadTypeStats(sessionUser.getId()));
        model.addAttribute("totalCount", userContentRepository.countByUserId(sessionUser.getId()));
        model.addAttribute("apiTypeOptions", List.of("pelicula", "serie", "libro", "disco"));
        model.addAttribute("selectedApiType", normalizedApiType);
        model.addAttribute("apiQuery", apiQuery == null ? "" : apiQuery);
        model.addAttribute("apiResults", apiResults);
        model.addAttribute("activeType", "profile");

        return "profile";
    }

    @PostMapping("/profile/import-api")
    public String importFromApi(@RequestParam String externalTitle,
            @RequestParam String externalType,
            @RequestParam(required = false) String externalDescription,
            @RequestParam(required = false) String externalReleaseDate,
            RedirectAttributes redirectAttributes,
            @RequestParam(required = false) String externalMetric,
            @RequestParam(required = false) String returnTo,
            HttpSession session) {
        String redirectPath = resolveReturnPath(returnTo);
        User sessionUser = getSessionUser(session);
        if (sessionUser == null) {
            return "redirect:/";
        }

        String title = externalTitle == null ? "" : externalTitle.trim();
        String type;
        try {
            type = normalizeAndValidateType(externalType);
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:" + redirectPath;
        }
        if (title.isBlank()) {
            redirectAttributes.addFlashAttribute("errorMessage", "No se puede importar contenido sin titulo");
            return "redirect:" + redirectPath;
        }

        Content content = contentRepository.findFirstByTitleIgnoreCaseAndTypeIgnoreCase(title, type)
                .orElseGet(() -> {
                    Content created = new Content();
                    created.setTitle(title);
                    created.setType(type);
                    created.setDescription(buildImportedDescription(externalDescription, externalMetric));
                    if (externalReleaseDate != null && !externalReleaseDate.isBlank()) {
                        try {
                            created.setReleaseDate(LocalDate.parse(externalReleaseDate.trim()));
                        } catch (Exception ignored) {
                            created.setReleaseDate(null);
                        }
                    }
                    return contentRepository.save(created);
                });

        if (userContentRepository.existsByUserIdAndContentId(sessionUser.getId(), content.getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ese contenido ya esta en tu biblioteca");
            return "redirect:" + redirectPath;
        }

        UserContent entry = new UserContent();
        entry.setUser(sessionUser);
        entry.setContent(content);
        entry.setAddedDate(LocalDate.now());

        try {
            Integer importedPages = "libro".equals(type) ? parsePagesFromMetric(externalMetric) : null;
            applyTypeSpecificData(entry, type, null, null, importedPages);
            userContentRepository.save(entry);
            redirectAttributes.addFlashAttribute("successMessage", "Contenido importado y anadido a tu biblioteca");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "No se pudo importar el contenido seleccionado");
        }

        return "redirect:" + redirectPath;
    }

    @PostMapping("/profile/add")
    public String addToProfileLibrary(@RequestParam Long contentId,
            @RequestParam(required = false) String movieStatus,
            @RequestParam(required = false) Integer bookCurrentPage,
            @RequestParam(required = false) Integer bookTotalPages,
            RedirectAttributes redirectAttributes, HttpSession session) {
        User sessionUser = getSessionUser(session);
        if (sessionUser == null) {
            return "redirect:/";
        }

        Content content = contentRepository.findById(contentId).orElse(null);
        if (content == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Contenido no encontrado");
            return "redirect:/profile";
        }

        String contentType = normalizeAndValidateType(content.getType());

        if (userContentRepository.existsByUserIdAndContentId(sessionUser.getId(), contentId)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ese contenido ya esta en tu biblioteca");
            return "redirect:/profile";
        }

        UserContent entry = new UserContent();
        entry.setUser(sessionUser);
        entry.setContent(content);
        entry.setAddedDate(LocalDate.now());

        try {
            applyTypeSpecificData(entry, contentType, movieStatus, bookCurrentPage, bookTotalPages);
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/profile";
        }

        try {
            userContentRepository.save(entry);
            redirectAttributes.addFlashAttribute("successMessage", "Contenido anadido a tu perfil");
        } catch (DataIntegrityViolationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ese contenido ya esta en tu biblioteca");
        }

        return "redirect:/profile";
    }

    @PostMapping("/profile/{entryId}/status")
    public String updateProfileEntryStatus(@PathVariable Long entryId, @RequestParam String status,
            RedirectAttributes redirectAttributes, HttpSession session) {
        User sessionUser = getSessionUser(session);
        if (sessionUser == null) {
            return "redirect:/";
        }

        UserContent entry = userContentRepository.findById(entryId).orElse(null);
        if (entry == null || entry.getUser() == null || !sessionUser.getId().equals(entry.getUser().getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "No tienes permiso para editar ese registro");
            return "redirect:/profile";
        }

        String type = normalizeType(entry.getContent().getType());
        if ("pelicula".equals(type) || "libro".equals(type)) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Usa las acciones especificas de pelicula o libro para actualizar su progreso");
            return "redirect:/profile";
        }

        String normalizedStatus = normalizeGeneralStatus(status);
        if (!VALID_GENERAL_STATUSES.contains(normalizedStatus)) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Estado no valido. Usa: pendiente, en_progreso o abandonado");
            return "redirect:/profile";
        }

        entry.setStatus(normalizedStatus);
        userContentRepository.save(entry);
        redirectAttributes.addFlashAttribute("successMessage", "Estado actualizado");
        return "redirect:/profile";
    }

    @PostMapping("/profile/{entryId}/movie-status")
    public String updateMovieStatus(@PathVariable Long entryId, @RequestParam String movieStatus,
            RedirectAttributes redirectAttributes, HttpSession session) {
        User sessionUser = getSessionUser(session);
        if (sessionUser == null) {
            return "redirect:/";
        }

        UserContent entry = userContentRepository.findById(entryId).orElse(null);
        if (entry == null || entry.getUser() == null || !sessionUser.getId().equals(entry.getUser().getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "No tienes permiso para editar ese registro");
            return "redirect:/profile";
        }

        if (!"pelicula".equals(normalizeType(entry.getContent().getType()))) {
            redirectAttributes.addFlashAttribute("errorMessage", "Este registro no es una pelicula");
            return "redirect:/profile";
        }

        String normalizedMovieStatus = normalizeMovieStatus(movieStatus);
        if (!VALID_MOVIE_STATUSES.contains(normalizedMovieStatus)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Estado de pelicula no valido: visto o no_visto");
            return "redirect:/profile";
        }

        entry.setMovieWatched("visto".equals(normalizedMovieStatus));
        entry.setStatus(normalizedMovieStatus);
        userContentRepository.save(entry);
        redirectAttributes.addFlashAttribute("successMessage", "Estado de pelicula actualizado");
        return "redirect:/profile";
    }

    @PostMapping("/profile/{entryId}/book-progress")
    public String updateBookProgress(@PathVariable Long entryId, @RequestParam Integer currentPage,
            @RequestParam(required = false) Integer totalPages,
            RedirectAttributes redirectAttributes, HttpSession session) {
        User sessionUser = getSessionUser(session);
        if (sessionUser == null) {
            return "redirect:/";
        }

        UserContent entry = userContentRepository.findById(entryId).orElse(null);
        if (entry == null || entry.getUser() == null || !sessionUser.getId().equals(entry.getUser().getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "No tienes permiso para editar ese registro");
            return "redirect:/profile";
        }

        if (!"libro".equals(normalizeType(entry.getContent().getType()))) {
            redirectAttributes.addFlashAttribute("errorMessage", "Este registro no es un libro");
            return "redirect:/profile";
        }

        int safeCurrent = currentPage == null ? 0 : currentPage;
        int safeTotal = totalPages == null ? defaultPageValue(entry.getBookTotalPages()) : totalPages;

        if (!isValidBookProgress(safeCurrent, safeTotal)) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Progreso invalido: revisa pagina actual y total de paginas");
            return "redirect:/profile";
        }

        entry.setBookCurrentPage(safeCurrent);
        entry.setBookTotalPages(safeTotal);
        entry.setStatus(resolveBookStatus(safeCurrent, safeTotal));
        userContentRepository.save(entry);

        redirectAttributes.addFlashAttribute("successMessage", "Progreso de lectura actualizado");
        return "redirect:/profile";
    }

    @PostMapping("/profile/{entryId}/episodes")
    public String saveEpisodeProgress(@PathVariable Long entryId, @RequestParam Integer seasonNumber,
            @RequestParam Integer episodeNumber, @RequestParam(required = false) Boolean watched,
            RedirectAttributes redirectAttributes, HttpSession session) {
        User sessionUser = getSessionUser(session);
        if (sessionUser == null) {
            return "redirect:/";
        }

        UserContent entry = userContentRepository.findById(entryId).orElse(null);
        if (entry == null || entry.getUser() == null || !sessionUser.getId().equals(entry.getUser().getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "No tienes permiso para editar ese registro");
            return "redirect:/profile";
        }

        if (!"serie".equals(normalizeType(entry.getContent().getType()))) {
            redirectAttributes.addFlashAttribute("errorMessage", "Este registro no es una serie");
            return "redirect:/profile";
        }

        if (seasonNumber == null || seasonNumber < 1 || episodeNumber == null || episodeNumber < 1) {
            redirectAttributes.addFlashAttribute("errorMessage", "Temporada y episodio deben ser >= 1");
            return "redirect:/profile";
        }

        boolean watchedFlag = watched != null && watched;
        UserSeriesEpisodeProgress progress = userSeriesEpisodeProgressRepository
                .findByUserContentIdAndSeasonNumberAndEpisodeNumber(entry.getId(), seasonNumber, episodeNumber)
                .orElseGet(UserSeriesEpisodeProgress::new);

        progress.setUserContent(entry);
        progress.setSeasonNumber(seasonNumber);
        progress.setEpisodeNumber(episodeNumber);
        progress.setWatched(watchedFlag);
        userSeriesEpisodeProgressRepository.save(progress);

        redirectAttributes.addFlashAttribute("successMessage", "Progreso de episodio guardado");
        return "redirect:/profile";
    }

    @PostMapping("/profile/{entryId}/songs")
    public String saveSongProgress(@PathVariable Long entryId, @RequestParam Integer trackNumber,
            @RequestParam String trackTitle, @RequestParam(required = false) Boolean listened,
            RedirectAttributes redirectAttributes, HttpSession session) {
        User sessionUser = getSessionUser(session);
        if (sessionUser == null) {
            return "redirect:/";
        }

        UserContent entry = userContentRepository.findById(entryId).orElse(null);
        if (entry == null || entry.getUser() == null || !sessionUser.getId().equals(entry.getUser().getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "No tienes permiso para editar ese registro");
            return "redirect:/profile";
        }

        if (!"disco".equals(normalizeType(entry.getContent().getType()))) {
            redirectAttributes.addFlashAttribute("errorMessage", "Este registro no es un disco");
            return "redirect:/profile";
        }

        if (trackNumber == null || trackNumber < 1 || trackTitle == null || trackTitle.isBlank()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Cancion invalida: revisa numero y titulo");
            return "redirect:/profile";
        }

        boolean listenedFlag = listened != null && listened;
        UserSongProgress progress = userSongProgressRepository.findByUserContentIdAndTrackNumber(entry.getId(), trackNumber)
                .orElseGet(UserSongProgress::new);

        progress.setUserContent(entry);
        progress.setTrackNumber(trackNumber);
        progress.setTrackTitle(trackTitle.trim());
        progress.setListened(listenedFlag);
        userSongProgressRepository.save(progress);

        redirectAttributes.addFlashAttribute("successMessage", "Progreso de cancion guardado");
        return "redirect:/profile";
    }

    @PostMapping("/profile/{entryId}/delete")
    public String deleteProfileEntry(@PathVariable Long entryId, RedirectAttributes redirectAttributes,
            HttpSession session) {
        User sessionUser = getSessionUser(session);
        if (sessionUser == null) {
            return "redirect:/";
        }

        UserContent entry = userContentRepository.findById(entryId).orElse(null);
        if (entry == null || entry.getUser() == null || !sessionUser.getId().equals(entry.getUser().getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "No tienes permiso para eliminar ese registro");
            return "redirect:/profile";
        }

        userContentRepository.deleteById(entryId);
        redirectAttributes.addFlashAttribute("successMessage", "Registro eliminado");
        return "redirect:/profile";
    }

    private User getSessionUser(HttpSession session) {
        Object sessionUserId = session.getAttribute(SESSION_USER_ID);
        if (!(sessionUserId instanceof Long)) {
            return null;
        }
        return userRepository.findById((Long) sessionUserId).orElse(null);
    }

    private Map<String, Long> loadStats(Long userId) {
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("visto", userContentRepository.countByUserIdAndStatus(userId, "visto"));
        stats.put("no_visto", userContentRepository.countByUserIdAndStatus(userId, "no_visto"));
        stats.put("leyendo", userContentRepository.countByUserIdAndStatus(userId, "leyendo"));
        stats.put("leido", userContentRepository.countByUserIdAndStatus(userId, "leido"));
        return stats;
    }

    private Map<String, Long> loadTypeStats(Long userId) {
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("peliculas", userContentRepository.countByUserIdAndContentType(userId, "pelicula"));
        stats.put("series", userContentRepository.countByUserIdAndContentType(userId, "serie"));
        stats.put("libros", userContentRepository.countByUserIdAndContentType(userId, "libro"));
        stats.put("discos", userContentRepository.countByUserIdAndContentType(userId, "disco"));
        return stats;
    }

    private Map<Long, List<UserSeriesEpisodeProgress>> loadEpisodesByEntry(List<UserContent> seriesEntries) {
        Map<Long, List<UserSeriesEpisodeProgress>> map = new HashMap<>();
        for (UserContent entry : seriesEntries) {
            map.put(entry.getId(),
                    userSeriesEpisodeProgressRepository.findByUserContentIdOrderBySeasonNumberAscEpisodeNumberAsc(
                            entry.getId()));
        }
        return map;
    }

    private Map<Long, List<UserSongProgress>> loadSongsByEntry(List<UserContent> discEntries) {
        Map<Long, List<UserSongProgress>> map = new HashMap<>();
        for (UserContent entry : discEntries) {
            map.put(entry.getId(), userSongProgressRepository.findByUserContentIdOrderByTrackNumberAsc(entry.getId()));
        }
        return map;
    }

    private void applyTypeSpecificData(UserContent entry, String contentType, String movieStatus,
            Integer bookCurrentPage, Integer bookTotalPages) {
        if ("pelicula".equals(contentType)) {
            String normalizedMovieStatus = normalizeMovieStatus(movieStatus);
            if (!VALID_MOVIE_STATUSES.contains(normalizedMovieStatus)) {
                throw new IllegalArgumentException("Estado de pelicula no valido: visto o no_visto");
            }
            entry.setMovieWatched("visto".equals(normalizedMovieStatus));
            entry.setStatus(normalizedMovieStatus);
            return;
        }

        if ("libro".equals(contentType)) {
            int safeCurrent = bookCurrentPage == null ? 0 : bookCurrentPage;
            int safeTotal = bookTotalPages == null ? 0 : bookTotalPages;

            if (!isValidBookProgress(safeCurrent, safeTotal)) {
                throw new IllegalArgumentException("Progreso de libro invalido: revisa paginas");
            }

            entry.setBookCurrentPage(safeCurrent);
            entry.setBookTotalPages(safeTotal);
            entry.setStatus(resolveBookStatus(safeCurrent, safeTotal));
            return;
        }

        if ("serie".equals(contentType)) {
            entry.setStatus("en_progreso");
            return;
        }

        entry.setStatus("en_progreso");
    }

    private boolean isValidBookProgress(int currentPage, int totalPages) {
        if (currentPage < 0 || totalPages < 0) {
            return false;
        }
        return totalPages <= 0 || currentPage <= totalPages;
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

    private int defaultPageValue(Integer value) {
        return value == null ? 0 : value;
    }

    private String normalizeAndValidateType(String rawType) {
        String normalized = normalizeType(rawType);
        if (!VALID_CONTENT_TYPES.contains(normalized)) {
            throw new IllegalArgumentException("Tipo no valido. Usa pelicula, serie, libro o disco");
        }
        return normalized;
    }

    private String normalizeType(String rawType) {
        return rawType == null ? "" : rawType.trim().toLowerCase();
    }

    private String normalizeMovieStatus(String movieStatus) {
        if (movieStatus == null || movieStatus.isBlank()) {
            return "no_visto";
        }
        return movieStatus.trim().toLowerCase();
    }

    private String normalizeGeneralStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return "pendiente";
        }
        return rawStatus.trim().toLowerCase();
    }

    private String defaultExploreQuery(String normalizedType) {
        return switch (normalizedType) {
            case "pelicula" -> "popular";
            case "serie" -> "top";
            case "libro" -> "bestseller";
            case "disco" -> "popular albums";
            default -> "popular";
        };
    }

    private String displayTypeName(String normalizedType) {
        return switch (normalizedType) {
            case "pelicula" -> "Peliculas";
            case "serie" -> "Series";
            case "libro" -> "Libros";
            case "disco" -> "Discos";
            default -> "Catalogo";
        };
    }

    private String resolveReturnPath(String returnTo) {
        if (returnTo == null || returnTo.isBlank()) {
            return "/profile";
        }

        String candidate = returnTo.trim();
        if (candidate.startsWith("/explore/") || candidate.startsWith("/profile")) {
            return candidate;
        }

        return "/profile";
    }

    private String buildImportedDescription(String externalDescription, String externalMetric) {
        String description = externalDescription == null ? "" : externalDescription.trim();
        String metric = externalMetric == null ? "" : externalMetric.trim();

        if (metric.isBlank()) {
            return description;
        }
        if (description.isBlank()) {
            return "Detalle: " + metric;
        }
        return description + " | " + metric;
    }

    private List<ExternalContentItem> filterRecentDiscs(List<ExternalContentItem> results, int daysBack) {
        if (results == null || results.isEmpty()) {
            return List.of();
        }
        LocalDate minDate = LocalDate.now().minusDays(daysBack);
        return results.stream()
                .filter(item -> item.getReleaseDate() != null && !item.getReleaseDate().isBefore(minDate))
                .filter(item -> item.getTitle() != null
                        && !item.getTitle().toLowerCase(Locale.ROOT).contains("popular"))
                .toList();
    }

    private Integer parsePagesFromMetric(String metric) {
        if (metric == null || metric.isBlank()) {
            return null;
        }

        String digits = metric.replaceAll("[^0-9]", "");
        if (digits.isBlank()) {
            return null;
        }

        try {
            return Integer.valueOf(digits);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String resolveArtistName(List<ExternalContentItem> results) {
        return results.stream()
                .map(ExternalContentItem::getArtistName)
                .filter(name -> name != null && !name.isBlank())
                .findFirst()
                .orElse(null);
    }

    private List<String> splitActors(String rawActors) {
        if (rawActors == null || rawActors.isBlank()) {
            return List.of();
        }

        String[] parts = rawActors.split(",");
        List<String> actors = new ArrayList<>();
        for (String part : parts) {
            String cleaned = part.trim();
            if (!cleaned.isBlank()) {
                actors.add(cleaned);
            }
        }
        return actors;
    }
}
