package com.proyectofinal.libreriacultural.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import com.proyectofinal.libreriacultural.services.ExternalContentItem;
import com.proyectofinal.libreriacultural.services.ExternalContentSearchService;

import com.proyectofinal.libreriacultural.Repositories.UserRepository;
import com.proyectofinal.libreriacultural.domain.User;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/external")
public class ExternalApiController {

    private final ExternalContentSearchService externalContentSearchService;
    private final UserRepository userRepository;

    public ExternalApiController(ExternalContentSearchService externalContentSearchService, UserRepository userRepository) {
        this.externalContentSearchService = externalContentSearchService;
        this.userRepository = userRepository;
    }

    @GetMapping("/search")
    public List<ExternalContentItem> search(@RequestParam String query, @RequestParam String type) {
        try {
            return externalContentSearchService.search(query, type);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Error consultando API externa. Intentalo de nuevo.");
        }
    }

    @GetMapping("/trending/movies")
    public List<ExternalContentItem> trendingMovies() {
        return externalContentSearchService.getWeeklyTrendingMovies();
    }

    @GetMapping("/trending/series")
    public List<ExternalContentItem> trendingSeries() {
        return externalContentSearchService.getWeeklyTrendingSeries();
    }

    @GetMapping("/trending/discs")
    public List<ExternalContentItem> trendingDiscs() {
        return externalContentSearchService.getWeeklyTrendingDiscs();
    }

    @GetMapping("/trending/books")
    public List<ExternalContentItem> trendingBooks() {
        return externalContentSearchService.getWeeklyTrendingBooks();
    }

    @GetMapping("/movies/genre")
    public List<ExternalContentItem> moviesByGenre(@RequestParam String genre) {
        return externalContentSearchService.getMoviesByGenre(genre);
    }

    @GetMapping("/series/genre")
    public List<ExternalContentItem> seriesByGenre(@RequestParam String genre) {
        return externalContentSearchService.getSeriesByGenre(genre);
    }

    @GetMapping("/discs/genre")
    public List<ExternalContentItem> discsByGenre(@RequestParam String genre) {
        return externalContentSearchService.getDiscsByGenre(genre);
    }

    @GetMapping("/books/genre")
    public List<ExternalContentItem> booksByGenre(@RequestParam String genre) {
        return externalContentSearchService.getBooksByGenre(genre);
    }

    @GetMapping("/details")
    public Map<String, Object> getDetails(@RequestParam String source, @RequestParam String id, @RequestParam String type, HttpSession session) {
        Long userId = (Long) session.getAttribute("sessionUserId");
        if (userId == null) {
            List<User> users = userRepository.findAll();
            if (!users.isEmpty()) userId = users.get(0).getId();
        }
        return externalContentSearchService.getDetails(source, id, type, userId);
    }

    @GetMapping("/actor")
    public Map<String, Object> getActor(@RequestParam String name) {
        return externalContentSearchService.getActorDetails(name);
    }
}
