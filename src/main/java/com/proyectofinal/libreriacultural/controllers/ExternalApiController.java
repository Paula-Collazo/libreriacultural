package com.proyectofinal.libreriacultural.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.proyectofinal.libreriacultural.services.ExternalContentItem;
import com.proyectofinal.libreriacultural.services.ExternalContentSearchService;

@RestController
@RequestMapping("/api/external")
public class ExternalApiController {

    private final ExternalContentSearchService externalContentSearchService;

    public ExternalApiController(ExternalContentSearchService externalContentSearchService) {
        this.externalContentSearchService = externalContentSearchService;
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
}
