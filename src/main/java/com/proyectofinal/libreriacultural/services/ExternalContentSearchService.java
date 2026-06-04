package com.proyectofinal.libreriacultural.services;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.nio.charset.StandardCharsets;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;
import java.util.Optional;
import com.proyectofinal.libreriacultural.Repositories.UserContentRepository;
import com.proyectofinal.libreriacultural.domain.UserContent;

@Service
public class ExternalContentSearchService {

    private final RestClient restClient;
    private final UserContentRepository userContentRepository;
    
    private final String TMDB_API_KEY = "ecb446e90fa5f7ea3f7fed4aac7df0e4";
    private final String SPOTIFY_CLIENT_ID = "d52040e3f56a499d870c1d8bb13c4fe7";
    private final String SPOTIFY_CLIENT_SECRET = "2dc43684923e412e9040d73c4a97cdfb";

    @Value("${google.books.api.key:}")
    private String googleBooksApiKey;

    private volatile long googleBooksBackoffUntilMs = 0L;

    private String spotifyToken = null;
    private long tokenExpiry = 0;

    public ExternalContentSearchService(UserContentRepository userContentRepository) {
        System.out.println("[INFO] Inicializando ExternalContentSearchService...");
        this.restClient = RestClient.create();
        this.userContentRepository = userContentRepository;
    }

    private String getSpotifyToken() {
        if (spotifyToken != null && System.currentTimeMillis() < tokenExpiry) return spotifyToken;
        try {
            String auth = Base64.getEncoder().encodeToString((SPOTIFY_CLIENT_ID + ":" + SPOTIFY_CLIENT_SECRET).getBytes());
            ResponseEntity<Map> response = restClient.post().uri("https://accounts.spotify.com/api/token")
                .header("Authorization", "Basic " + auth).contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("grant_type=client_credentials").retrieve().toEntity(Map.class);
            if (response.getBody() != null) {
                spotifyToken = (String) response.getBody().get("access_token");
                tokenExpiry = System.currentTimeMillis() + (((Number)response.getBody().get("expires_in")).longValue() * 1000) - 60000;
                return spotifyToken;
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Spotify Auth: " + e.getMessage());
        }
        return null;
    }

    public List<ExternalContentItem> search(String query, String type) {
        if (query == null || query.isBlank()) return new ArrayList<>();
        System.out.println("[INFO] Buscando " + type + ": " + query);
        try {
            String t = type.toLowerCase();
            if (t.equals("pelicula") || t.equals("movie")) return searchTmdb(query, "movie");
            if (t.equals("serie") || t.equals("tv")) return searchTmdb(query, "tv");
            if (t.equals("musica") || t.equals("disco") || t.equals("album")) return searchSpotify(query);
            if (t.equals("libro") || t.equals("book")) return searchBooks(query);
        } catch (Exception e) {
            System.err.println("[ERROR] Search general: " + e.getMessage());
        }
        return new ArrayList<>();
    }

    private List<ExternalContentItem> searchTmdb(String query, String type) {
        return searchTmdb(query, type, 0);
    }

    private List<ExternalContentItem> searchTmdb(String query, String type, int iteration) {
        List<ExternalContentItem> items = new ArrayList<>();
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString("https://api.themoviedb.org/3/search/" + type)
                .queryParam("api_key", TMDB_API_KEY).queryParam("query", query);
            
            if (iteration == 0) {
                builder.queryParam("language", "es-ES");
            }
            
            String url = builder.build().toUriString();
            Map body = restClient.get().uri(url).retrieve().body(Map.class);
            if (body == null) return items;
            List<Map<String, Object>> results = (List<Map<String, Object>>) body.get("results");
            
            if (results != null && !results.isEmpty()) {
                for (Map<String, Object> res : results) items.add(parseTmdbItem(res, type));
            } else if (iteration == 0) {
                // Si no hay resultados en español, intentamos búsqueda global
                return searchTmdb(query, type, 1);
            }
        } catch (Exception e) {
            System.err.println("[ERROR] TMDb Search: " + e.getMessage());
        }
        return items;
    }

    public List<ExternalContentItem> getWeeklyTrendingMovies() {
        List<ExternalContentItem> items = new ArrayList<>();
        try {
            // Fetch first 2 pages to ensure at least 24 results
            for (int p = 1; p <= 2; p++) {
                String url = UriComponentsBuilder.fromUriString("https://api.themoviedb.org/3/trending/movie/week")
                    .queryParam("api_key", TMDB_API_KEY)
                    .queryParam("language", "es-ES")
                    .queryParam("page", p)
                    .build().toUriString();
                Map body = restClient.get().uri(url).retrieve().body(Map.class);
                if (body == null) break;
                List<Map<String, Object>> results = (List<Map<String, Object>>) body.get("results");
                if (results != null) {
                    for (Map<String, Object> res : results) {
                        items.add(parseTmdbItem(res, "movie"));
                        if (items.size() >= 24) return items;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[ERROR] TMDb Trending Movies: " + e.getMessage());
        }

        // Padding si no llegamos a 24
        if (items.size() < 24) {
            List<ExternalContentItem> fb = getFallBackMovies();
            for (ExternalContentItem f : fb) {
                if (items.size() >= 24) break;
                if (items.stream().noneMatch(i -> i.getExternalId().equals(f.getExternalId()))) {
                    items.add(f);
                }
            }
        }
        return items;
    }

    public List<ExternalContentItem> getWeeklyTrendingSeries() {
        List<ExternalContentItem> items = new ArrayList<>();
        try {
            // Fetch first 2 pages to ensure at least 24 results
            for (int p = 1; p <= 2; p++) {
                String url = UriComponentsBuilder.fromUriString("https://api.themoviedb.org/3/trending/tv/week")
                    .queryParam("api_key", TMDB_API_KEY)
                    .queryParam("language", "es-ES")
                    .queryParam("page", p)
                    .build().toUriString();
                Map body = restClient.get().uri(url).retrieve().body(Map.class);
                if (body == null) break;
                List<Map<String, Object>> results = (List<Map<String, Object>>) body.get("results");
                if (results != null) {
                    for (Map<String, Object> res : results) {
                        items.add(parseTmdbItem(res, "serie"));
                        if (items.size() >= 24) return items;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[ERROR] TMDb Trending Series: " + e.getMessage());
        }

        // Padding si no llegamos a 24
        if (items.size() < 24) {
            List<ExternalContentItem> fb = getFallBackSeries();
            for (ExternalContentItem f : fb) {
                if (items.size() >= 24) break;
                if (items.stream().noneMatch(i -> i.getExternalId().equals(f.getExternalId()))) {
                    items.add(f);
                }
            }
        }
        return items;
    }

    public List<ExternalContentItem> getDiscoveryByGenre(String type, String genreId) {
        List<ExternalContentItem> items = new ArrayList<>();
        try {
            String tmdbType = type.equals("PELICULA") ? "movie" : "tv";
            String url = UriComponentsBuilder.fromUriString("https://api.themoviedb.org/3/discover/" + tmdbType)
                .queryParam("api_key", TMDB_API_KEY)
                .queryParam("with_genres", genreId)
                .queryParam("sort_by", "popularity.desc")
                .queryParam("language", "es-ES")
                .build().toUriString();
            Map body = restClient.get().uri(url).retrieve().body(Map.class);
            if (body == null) return items;
            List<Map<String, Object>> results = (List<Map<String, Object>>) body.get("results");
            if (results != null) {
                for (Map<String, Object> res : results) {
                    items.add(parseTmdbItem(res, tmdbType));
                    if (items.size() >= 24) break;
                }
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Discovery Genre: " + e.getMessage());
        }
        return items;
    }

    private ExternalContentItem parseTmdbItem(Map<String, Object> res, String type) {
        String id = String.valueOf(res.get("id"));
        String title = asString(res.get(type.equals("movie") ? "title" : "name"));
        String poster = asString(res.get("poster_path"));
        String cover = poster.isEmpty() ? "" : "https://image.tmdb.org/t/p/w500" + poster;
        LocalDate date = parseDate(asString(res.get(type.equals("movie") ? "release_date" : "first_air_date")));
        return new ExternalContentItem("TMDb", id, title, type.equals("movie") ? "PELICULA" : "SERIE", asString(res.get("overview")), date, cover, null, null, null, null, null);
    }

    public List<ExternalContentItem> searchSpotify(String query) {
        return searchSpotifyAlbums(query, 24);
    }

    public List<ExternalContentItem> getWeeklyTrendingDiscs() {
        try {
            List<ExternalContentItem> candidates = new ArrayList<>();
            Set<String> candidateKeys = new HashSet<>();
            int currentYear = LocalDate.now().getYear();

            // Reducimos llamadas para evitar 429
            appendDiscSearchCandidates(candidates, candidateKeys, "tag:new year:" + currentYear, 24);
            if (candidates.size() < 10) {
                appendDiscSearchCandidates(candidates, candidateKeys, "year:" + currentYear, 24);
            }

            if (candidates.isEmpty()) {
                System.out.println("[INFO] Candidates vacíos, usando fallback para Discos");
                return getFallBackDiscs();
            }

            int maxCount = 24;
            List<ExternalContentItem> results = new ArrayList<>();
            Set<String> seen = new HashSet<>();

            appendDiscCandidates(results, seen, candidates, LocalDate.now().minusDays(7), false, maxCount);
            if (results.size() < maxCount) {
                appendDiscCandidates(results, seen, candidates, LocalDate.now().minusDays(30), false, maxCount);
            }
            if (results.size() < maxCount) {
                appendDiscCandidates(results, seen, candidates, null, true, maxCount);
            }

            if (results.isEmpty()) {
                System.out.println("[INFO] Resultados filtrados vacíos o error, usando fallback para Discos");
                return getFallBackDiscs();
            }

            // Padding si no llegamos a 24 para garantizar simetría
            if (results.size() < 24) {
                List<ExternalContentItem> fb = getFallBackDiscs();
                for (ExternalContentItem f : fb) {
                    if (results.size() >= 24) break;
                    if (results.stream().noneMatch(i -> i.getExternalId().equals(f.getExternalId()))) {
                        results.add(f);
                    }
                }
            }

            System.out.println("[INFO] Retornando " + results.size() + " discos encontrados");
            return results;
        } catch (Exception e) {
            System.err.println("[ERROR] getWeeklyTrendingDiscs: " + e.getMessage());
            return getFallBackDiscs();
        }
    }

    private List<ExternalContentItem> getFallBackMovies() {
        List<ExternalContentItem> fallback = new ArrayList<>();
        // El Padrino
        fallback.add(new ExternalContentItem("TMDb", "238", "El Padrino", "PELICULA", "El patriarca envejecido de una dinastía del crimen organizado...", LocalDate.of(1972, 3, 14), "https://image.tmdb.org/t/p/w500/3bhkrjOiERvSTqX6DHAs679Pzmb.jpg", null, null, null, null, null));
        // Pulp Fiction
        fallback.add(new ExternalContentItem("TMDb", "680", "Pulp Fiction", "PELICULA", "Las vidas de dos gánsteres, un boxeador y la esposa de un gánster...", LocalDate.of(1994, 9, 10), "https://image.tmdb.org/t/p/w500/d5iIlSXY9CnpU7Cu6v3SzzuLUZ.jpg", null, null, null, null, null));
        // Cadena Perpetua
        fallback.add(new ExternalContentItem("TMDb", "278", "Cadena Perpetua", "PELICULA", "Dos hombres encarcelados entablan una amistad a lo largo de décadas...", LocalDate.of(1994, 9, 22), "https://image.tmdb.org/t/p/w500/9O7mUunSjz0giG7YBRm36PLQEWD.jpg", null, null, null, null, null));
        // El Caballero Oscuro
        fallback.add(new ExternalContentItem("TMDb", "155", "El Caballero Oscuro", "PELICULA", "Batman se propone destruir el crimen organizado en Gotham City...", LocalDate.of(2008, 7, 16), "https://image.tmdb.org/t/p/w500/qJ2tW6WMUDp9EXjBY0Mih3UAn5P.jpg", null, null, null, null, null));
        // Origen
        fallback.add(new ExternalContentItem("TMDb", "27205", "Origen", "PELICULA", "A un ladrón que roba secretos corporativos mediante el uso de la tecnología...", LocalDate.of(2010, 7, 15), "https://image.tmdb.org/t/p/w500/edv5bs1pUQCWo8Y5fYYjL4pZ4ef.jpg", null, null, null, null, null));
        // Matrix
        fallback.add(new ExternalContentItem("TMDb", "603", "Matrix", "PELICULA", "Un programador de computación descubre que su mundo es una simulación...", LocalDate.of(1999, 3, 30), "https://image.tmdb.org/t/p/w500/f89U3Y9L7dbptvTMRccpUvSTpYy.jpg", null, null, null, null, null));
        return fallback;
    }

    private List<ExternalContentItem> getFallBackSeries() {
        List<ExternalContentItem> fallback = new ArrayList<>();
        // Breaking Bad
        fallback.add(new ExternalContentItem("TMDb", "1396", "Breaking Bad", "SERIE", "Un profesor de química con cáncer terminal comienza a cocinar metanfetamina...", LocalDate.of(2008, 1, 20), "https://image.tmdb.org/t/p/w500/ztkUQvHnd79fv6rnB69xz9vU046.jpg", null, null, null, null, null));
        // Juego de Tronos
        fallback.add(new ExternalContentItem("TMDb", "1399", "Juego de Tronos", "SERIE", "Siete familias nobles luchan por el control de la mítica tierra de Poniente...", LocalDate.of(2011, 4, 17), "https://image.tmdb.org/t/p/w500/u3bZgnSFTTV40GPA995I6UvPBv2.jpg", null, null, null, null, null));
        // Los Soprano
        fallback.add(new ExternalContentItem("TMDb", "1390", "Los Soprano", "SERIE", "La vida de un jefe de la mafia de Nueva Jersey que acude a una psiquiatra...", LocalDate.of(1999, 1, 10), "https://image.tmdb.org/t/p/w500/69Uqt7vSbebs1uL6pBCH3H6mvwO.jpg", null, null, null, null, null));
        // The Wire
        fallback.add(new ExternalContentItem("TMDb", "1361", "The Wire (Bajo escucha)", "SERIE", "Una visión realista del mundo del narcotráfico y la corrupción policial en Baltimore...", LocalDate.of(2002, 6, 2), "https://image.tmdb.org/t/p/w500/9eS9f7nS3t4vO9KIDy7yS6Wv3U0.jpg", null, null, null, null, null));
        // Stranger Things
        fallback.add(new ExternalContentItem("TMDb", "66732", "Stranger Things", "SERIE", "Cuando un niño desaparece, sus amigos, su familia y la policía se ven envueltos en un misterio...", LocalDate.of(2016, 7, 15), "https://image.tmdb.org/t/p/w500/49WfTJs0z4Hy6JNNuOV0ls93DPT.jpg", null, null, null, null, null));
        // Friends
        fallback.add(new ExternalContentItem("TMDb", "1668", "Friends", "SERIE", "Las aventuras de seis amigos de Manhattan que se enfrentan a la vida y el amor...", LocalDate.of(1994, 9, 22), "https://image.tmdb.org/t/p/w500/f496p9S7S97v26eY69S9vY39p9v.jpg", null, null, null, null, null));
        return fallback;
    }

    private List<ExternalContentItem> getFallBackDiscs() {
        List<ExternalContentItem> fallback = new ArrayList<>();
        // Radiohead - OK Computer
        fallback.add(new ExternalContentItem("Spotify", "63O49Zp9RJHExmYv6yY8YF", "Radiohead - OK Computer", "DISCO", "Álbum clásico", LocalDate.of(1997, 6, 16), "https://i.scdn.co/image/ab67616d0000b273934d4007d4b245084a441113", null, "Radiohead", "4Z8W4f9KiYMW9d6tFSRS6z", null, null));
        // David Bowie - Ziggy Stardust
        fallback.add(new ExternalContentItem("Spotify", "48DnkS399vY39p9vY39p9v", "David Bowie - Ziggy Stardust", "DISCO", "Rock clásico", LocalDate.of(1972, 6, 16), "https://i.scdn.co/image/ab67616d0000b273b508937000300a74bca0d32", null, "David Bowie", "0oSGxfWSnnOXX64STFv9qI", null, null));
        // Arctic Monkeys - AM
        fallback.add(new ExternalContentItem("Spotify", "78bpImsn2u69v9R4p9vY39", "Arctic Monkeys - AM", "DISCO", "Indie Rock", LocalDate.of(2013, 9, 9), "https://i.scdn.co/image/ab67616d0000b2737bc0f57dfc201243a860773c", null, "Arctic Monkeys", "7Ln80S36vabn0YelfuA3Gq", null, null));
        // Rosalía - MOTOMAMI
        fallback.add(new ExternalContentItem("Spotify", "66S9vY39p9vY39p9vY39p9", "Rosalía - MOTOMAMI", "DISCO", "Pop Experimental", LocalDate.of(2022, 3, 18), "https://i.scdn.co/image/ab67616d0000b273fd68853b00681db6fd122c4f", null, "Rosalía", "7ltDVBr6mKbvMDzq3t0cN8", null, null));
        // Pink Floyd - The Dark Side of the Moon
        fallback.add(new ExternalContentItem("Spotify", "4LH4U3Yv8nuvovCqXpnb6g", "Pink Floyd - The Dark Side of the Moon", "DISCO", "Rock progresivo", LocalDate.of(1973, 3, 1), "https://i.scdn.co/image/ab67616d0000b273ea7caaff71726f1a0da49dbd", null, "Pink Floyd", "0k17h9DSHSRu9vY9vY9vY9", null, null));
        // The Beatles - Abbey Road
        fallback.add(new ExternalContentItem("Spotify", "0ETFjACtuP26mS6RPLUH9r", "The Beatles - Abbey Road", "DISCO", "Rock clásico", LocalDate.of(1969, 9, 26), "https://i.scdn.co/image/ab67616d0000b273dc30583ba717007b00cceb25", null, "The Beatles", "3WrYpYvS6YnH8z6YvXvH5p", null, null));
        // Nirvana - Nevermind
        fallback.add(new ExternalContentItem("Spotify", "2guqiSPr4Y0YQ3Aunfy8Un", "Nirvana - Nevermind", "DISCO", "Grunge", LocalDate.of(1991, 9, 24), "https://i.scdn.co/image/ab67616d0000b273a70f80ced50b5ca37637d77b", null, "Nirvana", "6vWf0pBKMUnwiC0pX70azu", null, null));
        // Daft Punk - discovery
        fallback.add(new ExternalContentItem("Spotify", "2noRn2iesS0ArMDMDm9fcS", "Daft Punk - Discovery", "DISCO", "Electrónica", LocalDate.of(2001, 3, 12), "https://i.scdn.co/image/ab67616d0000b2732cd42526ed5c3539be276ceb", null, "Daft Punk", "4tZwfgrHOu2pZfXpZfXpZf", null, null));
        // Fleetwood Mac - Rumours
        fallback.add(new ExternalContentItem("Spotify", "1bt6q2SruMs7u0ZpPs9h7R", "Fleetwood Mac - Rumours", "DISCO", "Soft Rock", LocalDate.of(1977, 2, 4), "https://i.scdn.co/image/ab67616d0000b273eb64a938c35928d32095ccfe", null, "Fleetwood Mac", "08td7MxvCkrn6pS3vU9PjC", null, null));
        // Michael Jackson - Thriller
        fallback.add(new ExternalContentItem("Spotify", "2ANVgaSgkIlmYv378unS3u", "Michael Jackson - Thriller", "DISCO", "Pop", LocalDate.of(1982, 11, 30), "https://i.scdn.co/image/ab67616d0000b2734121a36452292f75a6c174f8", null, "Michael Jackson", "3fMbd9B4jeas0p6uYq", null, null));
        // Amy Winehouse - Back To Black
        fallback.add(new ExternalContentItem("Spotify", "09777_backtoblack", "Amy Winehouse - Back To Black", "DISCO", "Soul", LocalDate.of(2006, 10, 27), "https://i.scdn.co/image/ab67616d0000b27364653_amy", null, "Amy Winehouse", "6eUov_amy", null, null));
        // Queen - A Night At The Opera
        fallback.add(new ExternalContentItem("Spotify", " Queen_NightOpera", "Queen - A Night At The Opera", "DISCO", "Rock", LocalDate.of(1975, 11, 21), "https://i.scdn.co/image/ab67616d0000b2731c36005086d7950949102431", null, "Queen", "1vC_Queen", null, null));
        // Kendrick Lamar - DAMN.
        fallback.add(new ExternalContentItem("Spotify", "Kendrick_DAMN", "Kendrick Lamar - DAMN.", "DISCO", "Hip Hop", LocalDate.of(2017, 4, 14), "https://i.scdn.co/image/ab67616d0000b273d28daa1e9ba0d3482928a9b2", null, "Kendrick Lamar", "2YZ_Kendrick", null, null));
        // Led Zeppelin - IV
        fallback.add(new ExternalContentItem("Spotify", "LedZeppelin_IV", "Led Zeppelin - IV", "DISCO", "Hard Rock", LocalDate.of(1971, 11, 8), "https://i.scdn.co/image/ab67616d0000b273c3d5175949102431bba0", null, "Led Zeppelin", "36Q_Zeppelin", null, null));
        // Miles Davis - Kind of Blue
        fallback.add(new ExternalContentItem("Spotify", "MilesDavis_KindBlue", "Miles Davis - Kind of Blue", "DISCO", "Jazz", LocalDate.of(1959, 8, 17), "https://i.scdn.co/image/ab67616d0000b27394c8b871c508a8cc2058", null, "Miles Davis", "0kb_Miles", null, null));
        // Tame Impala - Currents
        fallback.add(new ExternalContentItem("Spotify", "TameImpala_Currents", "Tame Impala - Currents", "DISCO", "Psych Pop", LocalDate.of(2015, 7, 17), "https://i.scdn.co/image/ab67616d0000b2739b15239ba0d3482928a9b2", null, "Tame Impala", "5q_Tame", null, null));
        return fallback;
    }

    private void appendDiscCandidates(List<ExternalContentItem> results, Set<String> seen,
            List<ExternalContentItem> candidates, LocalDate minDate, boolean allowMissingDate, int maxCount) {
        for (ExternalContentItem item : candidates) {
            String title = item.getTitle();
            if (containsPopularTitle(title)) {
                continue;
            }
            LocalDate releaseDate = item.getReleaseDate();
            if (releaseDate == null && !allowMissingDate) {
                continue;
            }
            if (minDate != null && releaseDate != null && releaseDate.isBefore(minDate)) {
                continue;
            }
            String key = item.getExternalId();
            if (key == null || key.isBlank()) {
                key = title;
            }
            if (key == null || key.isBlank() || seen.contains(key)) {
                continue;
            }
            seen.add(key);
            results.add(item);
            if (results.size() >= maxCount) {
                return;
            }
        }
    }

    private long lastSpotify429Time = 0;

    private List<ExternalContentItem> searchSpotifyAlbums(String query, int limit) {
        // Si tuvimos un 429 hace menos de 10 minutos, no intentamos de nuevo para evitar saturar
        if (System.currentTimeMillis() - lastSpotify429Time < 10 * 60 * 1000) {
            return new ArrayList<>();
        }

        List<ExternalContentItem> items = new ArrayList<>();
        String token = getSpotifyToken();
        if (token == null) return items;
        try {
            int maxResults = Math.max(1, limit);
            int offset = 0;
            int pageSize = 10;

            while (items.size() < maxResults) {
                java.net.URI url = UriComponentsBuilder.fromHttpUrl("https://api.spotify.com/v1/search")
                    .queryParam("q", query)
                    .queryParam("type", "album")
                    .queryParam("limit", pageSize)
                    .queryParam("offset", offset)
                    .build()
                    .encode()
                    .toUri();
                Map body = restClient.get().uri(url).header("Authorization", "Bearer " + token).retrieve().body(Map.class);
                if (body == null || body.get("albums") == null) break;
                List<Map<String, Object>> results = (List<Map<String, Object>>) ((Map)body.get("albums")).get("items");
                if (results == null || results.isEmpty()) break;

                for (Map<String, Object> res : results) {
                    items.add(parseSpotifyItem(res));
                    if (items.size() >= maxResults) {
                        break;
                    }
                }

                if (results.size() < pageSize) {
                    break;
                }
                offset += pageSize;
                if (offset >= 200) {
                    break;
                }
            }
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("429")) {
                lastSpotify429Time = System.currentTimeMillis();
                System.err.println("[WARN] Spotify Rate Limited (429). Activando cooldown.");
            }
            System.err.println("[ERROR] Spotify Search: " + e.getMessage());
        }
        return items;
    }

    private void appendDiscSearchCandidates(List<ExternalContentItem> candidates, Set<String> keys,
            String query, int limit) {
        if (query == null || query.isBlank()) {
            return;
        }
        List<ExternalContentItem> batch = searchSpotifyAlbums(query, limit);
        for (ExternalContentItem item : batch) {
            String key = item.getExternalId();
            if (key == null || key.isBlank()) {
                key = item.getTitle();
            }
            if (key == null || key.isBlank() || keys.contains(key)) {
                continue;
            }
            keys.add(key);
            candidates.add(item);
        }
    }

    private List<Map<String, Object>> fetchSpotifyNewReleases(String token, String country, int limit) {
        List<Map<String, Object>> albums = new ArrayList<>();
        try {
            UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl("https://api.spotify.com/v1/browse/new-releases")
                .queryParam("limit", limit);
            if (country != null && !country.isBlank()) {
                builder.queryParam("country", country);
            }
            java.net.URI url = builder.build().toUri();
            Map body = restClient.get().uri(url).header("Authorization", "Bearer " + token).retrieve().body(Map.class);
            if (body == null || body.get("albums") == null) return albums;
            List<Map<String, Object>> results = (List<Map<String, Object>>) ((Map) body.get("albums")).get("items");
            if (results != null) {
                albums.addAll(results);
            }
        } catch (Exception e) {
            return albums;
        }
        return albums;
    }

    private void addTrendingAlbums(List<ExternalContentItem> items, List<Map<String, Object>> albumRows,
            Set<String> seenAlbums, LocalDate minDate) {
        for (Map<String, Object> album : albumRows) {
            String albumId = asString(album.get("id"));
            if (albumId.isBlank() || seenAlbums.contains(albumId)) continue;
            String title = asString(album.get("name"));
            if (title.isBlank() || containsPopularTitle(title)) continue;
            LocalDate releaseDate = parseSpotifyReleaseDate(album);
            if (releaseDate == null) continue;
            if (releaseDate.isBefore(minDate)) continue;
            seenAlbums.add(albumId);
            items.add(parseSpotifyItem(album));
            if (items.size() >= 8) break;
        }
    }

    private String findToplistPlaylistId(String token, String country) {
        try {
            java.net.URI url = UriComponentsBuilder
                .fromHttpUrl("https://api.spotify.com/v1/browse/categories/toplists/playlists")
                .queryParam("country", country)
                .queryParam("limit", 20)
                .build().toUri();
            Map body = restClient.get().uri(url).header("Authorization", "Bearer " + token).retrieve().body(Map.class);
            if (body == null || body.get("playlists") == null) return "";
            List<Map<String, Object>> results = (List<Map<String, Object>>) ((Map) body.get("playlists")).get("items");
            if (results == null) return "";
            String id = findPlaylistByName(results, "Viral 50");
            if (!id.isBlank()) return id;
            return findPlaylistByName(results, "Top 50");
        } catch (Exception e) {
            return "";
        }
    }

    private String findPlaylistByName(List<Map<String, Object>> playlists, String token) {
        for (Map<String, Object> playlist : playlists) {
            String name = asString(playlist.get("name")).toLowerCase();
            if (name.contains(token.toLowerCase())) {
                String id = asString(playlist.get("id"));
                if (!id.isBlank()) return id;
            }
        }
        return "";
    }

    private String findPlaylistId(String token, String query, String market) {
        try {
            java.net.URI url = UriComponentsBuilder.fromHttpUrl("https://api.spotify.com/v1/search")
                .queryParam("q", query)
                .queryParam("type", "playlist")
                .queryParam("limit", 5)
                .queryParam("market", market)
                .build().toUri();
            Map body = restClient.get().uri(url).header("Authorization", "Bearer " + token).retrieve().body(Map.class);
            if (body == null || body.get("playlists") == null) return "";
            List<Map<String, Object>> results = (List<Map<String, Object>>) ((Map) body.get("playlists")).get("items");
            if (results == null) return "";
            for (Map<String, Object> playlist : results) {
                String id = asString(playlist.get("id"));
                if (!id.isBlank()) return id;
            }
        } catch (Exception e) {
            return "";
        }
        return "";
    }

    private ExternalContentItem parseSpotifyItem(Map<String, Object> res) {
        List<Map<String, Object>> artists = (List<Map<String, Object>>) res.get("artists");
        String artist = (artists != null && !artists.isEmpty()) ? asString(artists.get(0).get("name")) : "";
        String artistId = (artists != null && !artists.isEmpty()) ? asString(artists.get(0).get("id")) : "";
        List<Map<String, Object>> images = (List<Map<String, Object>>) res.get("images");
        String cover = (images != null && !images.isEmpty()) ? asString(images.get(0).get("url")) : "";
        LocalDate releaseDate = parseSpotifyReleaseDate(res);
        return new ExternalContentItem("Spotify", asString(res.get("id")), asString(res.get("name")), "DISCO", "Álbum de " + artist, releaseDate, cover, null, artist, artistId, null, null);
    }

    public List<ExternalContentItem> searchBooks(String query) {
        String normalizedQuery = query == null ? "" : query.trim();
        boolean authorQuery = normalizedQuery.toLowerCase(Locale.ROOT).startsWith("inauthor:");
        String authorValue = authorQuery ? normalizedQuery.substring("inauthor:".length()).trim() : "";
        return authorQuery ? searchBooksByAuthor(authorValue) : searchOpenLibrary(normalizedQuery);
    }

    public List<ExternalContentItem> getWeeklyTrendingBooks() {
        List<ExternalContentItem> items = new ArrayList<>();
        try {
            // Fetch from multiple popular subjects to ensure 24 items
            String[] subjects = {"fiction", "fantasy", "mystery", "history", "classic"};
            Set<String> seen = new HashSet<>();
            
            for (String subject : subjects) {
                List<ExternalContentItem> batch = searchOpenLibrary("subject:" + subject, 15);
                for (ExternalContentItem item : batch) {
                    if (!seen.contains(item.getExternalId())) {
                        seen.add(item.getExternalId());
                        items.add(item);
                        if (items.size() >= 24) return items;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[ERROR] getWeeklyTrendingBooks: " + e.getMessage());
        }
        
        if (items.isEmpty()) return getFallBackBooks();

        // Padding si no llegamos a 24
        if (items.size() < 24) {
            List<ExternalContentItem> fb = getFallBackBooks();
            for (ExternalContentItem f : fb) {
                if (items.size() >= 24) break;
                if (items.stream().noneMatch(i -> i.getExternalId().equals(f.getExternalId()))) {
                    items.add(f);
                }
            }
        }
        return items;
    }

    private List<ExternalContentItem> getFallBackBooks() {
        List<ExternalContentItem> fallback = new ArrayList<>();
        // El Quijote
        fallback.add(new ExternalContentItem("OpenLibrary", "/works/OL103123W", "Don Quijote de la Mancha", "LIBRO", "Clásico de Cervantes", LocalDate.of(1605, 1, 1), "https://covers.openlibrary.org/b/id/12818862-M.jpg", null, "Miguel de Cervantes", null, null, null));
        // 1984
        fallback.add(new ExternalContentItem("OpenLibrary", "/works/OL1168083W", "1984", "LIBRO", "Distopía de Orwell", LocalDate.of(1949, 6, 8), "https://covers.openlibrary.org/b/id/12640232-M.jpg", null, "George Orwell", null, null, null));
        // El Principito
        fallback.add(new ExternalContentItem("OpenLibrary", "/works/OL15181231W", "El Principito", "LIBRO", "Fábula", LocalDate.of(1943, 4, 6), "https://covers.openlibrary.org/b/id/10523456-M.jpg", null, "Antoine de Saint-Exupéry", null, null, null));
        // Cien años de soledad
        fallback.add(new ExternalContentItem("OpenLibrary", "/works/OL456123W", "Cien años de soledad", "LIBRO", "Realismo mágico", LocalDate.of(1967, 5, 30), "https://covers.openlibrary.org/b/id/12712345-M.jpg", null, "Gabriel García Márquez", null, null, null));
        // Harry Potter 1
        fallback.add(new ExternalContentItem("OpenLibrary", "/works/OL82563W", "Harry Potter y la piedra filosofal", "LIBRO", "Fantasía", LocalDate.of(1997, 6, 26), "https://covers.openlibrary.org/b/id/10522132-M.jpg", null, "J.K. Rowling", null, null, null));
        // El Hobbit
        fallback.add(new ExternalContentItem("OpenLibrary", "/works/OL27448W", "El Hobbit", "LIBRO", "Fantasía épica", LocalDate.of(1937, 9, 21), "https://covers.openlibrary.org/b/id/10123456-M.jpg", null, "J.R.R. Tolkien", null, null, null));
        // Rayuela
        fallback.add(new ExternalContentItem("OpenLibrary", "/works/OL102123W", "Rayuela", "LIBRO", "Boom Latinoamericano", LocalDate.of(1963, 6, 28), "https://covers.openlibrary.org/b/id/11122334-M.jpg", null, "Julio Cortázar", null, null, null));
        // La sombra del viento
        fallback.add(new ExternalContentItem("OpenLibrary", "/works/OL55123W", "La sombra del viento", "LIBRO", "Misterio", LocalDate.of(2001, 1, 1), "https://covers.openlibrary.org/b/id/12345678-M.jpg", null, "Carlos Ruiz Zafón", null, null, null));
        // Orgullo y Prejuicio
        fallback.add(new ExternalContentItem("OpenLibrary", "/works/OL66123W", "Orgullo y Prejuicio", "LIBRO", "Romance clásico", LocalDate.of(1813, 1, 28), "https://covers.openlibrary.org/b/id/10512345-M.jpg", null, "Jane Austen", null, null, null));
        // Crónica de una muerte anunciada
        fallback.add(new ExternalContentItem("OpenLibrary", "/works/OL77123W", "Crónica de una muerte anunciada", "LIBRO", "Realismo", LocalDate.of(1981, 1, 1), "https://covers.openlibrary.org/b/id/10612345-M.jpg", null, "Gabriel García Márquez", null, null, null));
        // El Alquimista
        fallback.add(new ExternalContentItem("OpenLibrary", "/works/OL88123W", "El Alquimista", "LIBRO", "Fábula espiritual", LocalDate.of(1988, 1, 1), "https://covers.openlibrary.org/b/id/10712345-M.jpg", null, "Paulo Coelho", null, null, null));
        // Ensayo sobre la ceguera
        fallback.add(new ExternalContentItem("OpenLibrary", "/works/OL99123W", "Ensayo sobre la ceguera", "LIBRO", "Ficción filosófica", LocalDate.of(1995, 1, 1), "https://covers.openlibrary.org/b/id/10812345-M.jpg", null, "José Saramago", null, null, null));
        // La metamorfosis
        fallback.add(new ExternalContentItem("OpenLibrary", "/works/OL111111W", "La metamorfosis", "LIBRO", "Existencialismo", LocalDate.of(1915, 1, 1), "https://covers.openlibrary.org/b/id/10912345-M.jpg", null, "Franz Kafka", null, null, null));
        // Pedro Páramo
        fallback.add(new ExternalContentItem("OpenLibrary", "/works/OL222222W", "Pedro Páramo", "LIBRO", "Realismo mágico", LocalDate.of(1955, 1, 1), "https://covers.openlibrary.org/b/id/11012345-M.jpg", null, "Juan Rulfo", null, null, null));
        // Los pilares de la Tierra
        fallback.add(new ExternalContentItem("OpenLibrary", "/works/OL333333W", "Los pilares de la Tierra", "LIBRO", "Ficción histórica", LocalDate.of(1989, 1, 1), "https://covers.openlibrary.org/b/id/11112345-M.jpg", null, "Ken Follett", null, null, null));
        // El nombre de la rosa
        fallback.add(new ExternalContentItem("OpenLibrary", "/works/OL444444W", "El nombre de la rosa", "LIBRO", "Misterio histórico", LocalDate.of(1980, 1, 1), "https://covers.openlibrary.org/b/id/11212345-M.jpg", null, "Umberto Eco", null, null, null));
        // Anna Karenina
        fallback.add(new ExternalContentItem("OpenLibrary", "/works/OL555555W", "Anna Karenina", "LIBRO", "Realismo ruso", LocalDate.of(1877, 1, 1), "https://covers.openlibrary.org/b/id/11312345-M.jpg", null, "Leo Tolstoy", null, null, null));
        // El retrato de Dorian Gray
        fallback.add(new ExternalContentItem("OpenLibrary", "/works/OL666666W", "El retrato de Dorian Gray", "LIBRO", "Ficción gótica", LocalDate.of(1890, 1, 1), "https://covers.openlibrary.org/b/id/11412345-M.jpg", null, "Oscar Wilde", null, null, null));
        return fallback;
    }

    public List<ExternalContentItem> searchArtistAlbums(String artistId) {
        List<ExternalContentItem> items = new ArrayList<>();
        String token = getSpotifyToken();
        if (token == null) return items;
        try {
            String url = "https://api.spotify.com/v1/artists/" + artistId + "/albums?limit=24";
            Map body = restClient.get().uri(url).header("Authorization", "Bearer " + token).retrieve().body(Map.class);
            List<Map<String, Object>> results = (List<Map<String, Object>>) body.get("items");
            if (results != null) for (Map<String, Object> res : results) items.add(parseSpotifyItem(res));
        } catch (Exception e) {}
        return items;
    }

    public Map<String, Object> getDetails(String source, String externalId, String type) {
        Map<String, Object> details = new HashMap<>();
        try {
            if ("Spotify".equalsIgnoreCase(source)) {
                details = getSpotifyDetails(externalId);
            } else if ("TMDb".equalsIgnoreCase(source)) {
                details = getTmdbDetails(externalId, type);
            } else if ("GoogleBooks".equalsIgnoreCase(source) || "OpenLibrary".equalsIgnoreCase(source)) {
                if (externalId != null && (externalId.toLowerCase().contains("works") || externalId.toUpperCase().contains("OL"))) {
                    details = getOpenLibraryDetails(externalId);
                } else {
                    details = getGoogleBooksDetails(externalId);
                }
            }
            
            // Enriquecer con info de la biblioteca local
            enrichWithLibraryInfo(details, externalId);
            
        } catch (Exception e) {
            System.err.println("[ERROR] Details general: " + e.getMessage());
        }
        return details;
    }

    private void enrichWithLibraryInfo(Map<String, Object> details, String externalId) {
        if (externalId == null || externalId.isBlank()) return;
        
        // Buscamos si existe en la biblioteca de 'paula' (id=1, según DataLoader)
        Optional<UserContent> uc = userContentRepository.findByUserIdAndContentExternalId(1L, externalId);
        if (uc.isPresent()) {
            details.put("inLibrary", true);
            details.put("libraryId", uc.get().getId());
            details.put("favorite", uc.get().getFavorite());
            details.put("localStatus", uc.get().getStatus());
        } else {
            details.put("inLibrary", false);
            details.put("favorite", false);
        }
    }

    private Map<String, Object> getTmdbDetails(String id, String type) {
        Map<String, Object> result = new HashMap<>();
        String tmdbType = type.equalsIgnoreCase("serie") ? "tv" : "movie";
        try {
            String url = UriComponentsBuilder.fromUriString("https://api.themoviedb.org/3/" + tmdbType + "/" + id)
                .queryParam("api_key", TMDB_API_KEY).queryParam("language", "es-ES").queryParam("append_to_response", "credits").build().toUriString();
            Map body = restClient.get().uri(url).retrieve().body(Map.class);
            result.put("title", asString(body.get(tmdbType.equals("movie") ? "title" : "name")));
            result.put("description", asString(body.get("overview")));
            result.put("tagline", asString(body.get("tagline")));
            
            List<Map<String, Object>> genresList = (List<Map<String, Object>>) body.get("genres");
            if (genresList != null && !genresList.isEmpty()) {
                result.put("genre", genresList.get(0).get("name"));
            }

            String poster = asString(body.get("poster_path"));
            result.put("coverUrl", poster.isBlank() ? null : "https://image.tmdb.org/t/p/w500" + poster);
            result.put("releaseDate", parseDate(asString(body.get(tmdbType.equals("movie") ? "release_date" : "first_air_date"))));
            result.put("externalId", id);
            result.put("type", type);
            if ("tv".equals(tmdbType)) {
                Object seasonsObj = body.get("number_of_seasons");
                Object episodes = body.get("number_of_episodes");
                Integer sCount = seasonsObj instanceof Number ? ((Number)seasonsObj).intValue() : 0;
                Integer eCount = episodes instanceof Number ? ((Number)episodes).intValue() : 0;
                result.put("totalSeasons", sCount);
                result.put("totalEpisodes", eCount);
                result.put("seriesMetadata", sCount + " temporadas, " + eCount + " episodios");

                // Capturamos el detalle de episodios por temporada
                List<Map<String, Object>> seasons = (List<Map<String, Object>>) body.get("seasons");
                if (seasons != null) {
                    String seasonData = seasons.stream()
                        .filter(s -> ((Number)s.get("season_number")).intValue() > 0) // Ignoramos temporada 0 (especiales) si quieres
                        .map(s -> asString(s.get("episode_count")))
                        .collect(Collectors.joining("|"));
                    result.put("seasonData", seasonData);
                }
            }
            Map<String, Object> credits = (Map<String, Object>) body.get("credits");
            if (credits != null) {
                List<Map<String, Object>> cast = (List<Map<String, Object>>) credits.get("cast");
                if (cast != null) {
                    result.put("actors", cast.stream()
                        .filter(c -> c.get("name") != null && !asString(c.get("name")).isBlank())
                        .limit(10).map(c -> {
                            Map<String, String> a = new HashMap<>();
                            a.put("name", asString(c.get("name")));
                            String path = asString(c.get("profile_path"));
                            a.put("photoUrl", path.isEmpty() ? "" : "https://image.tmdb.org/t/p/w200" + path);
                            return a;
                        }).collect(Collectors.toList()));
                }
            }
            // 3. Obtener Recomendaciones / Similares
            String recUrl = UriComponentsBuilder.fromUriString("https://api.themoviedb.org/3/" + tmdbType + "/" + id + "/recommendations")
                .queryParam("api_key", TMDB_API_KEY).queryParam("language", "es-ES").build().toUriString();
            Map recBody = restClient.get().uri(recUrl).retrieve().body(Map.class);
            List<Map<String, Object>> recResults = (List<Map<String, Object>>) recBody.get("results");
            if (recResults != null) {
                result.put("recommendations", recResults.stream().limit(6).map(r -> {
                    Map<String, String> item = new HashMap<>();
                    item.put("externalId", String.valueOf(r.get("id")));
                    item.put("type", type);
                    item.put("title", asString(r.get(tmdbType.equals("movie") ? "title" : "name")));
                    String p = asString(r.get("poster_path"));
                    item.put("coverUrl", p.isEmpty() ? "" : "https://image.tmdb.org/t/p/w300" + p);
                    return item;
                }).collect(Collectors.toList()));
            }
        } catch (Exception e) {
            System.err.println("[ERROR] TMDb Details: " + e.getMessage());
        }
        return result;
    }

    private Map<String, Object> getSpotifyDetails(String id) {
        Map<String, Object> result = new HashMap<>();
        String token = getSpotifyToken();
        if (token == null) return result;
        try {
            Map body = restClient.get().uri("https://api.spotify.com/v1/albums/" + id)
                .header("Authorization", "Bearer " + token).retrieve().body(Map.class);
            result.put("title", asString(body.get("name")));
            List<Map<String, Object>> images = (List<Map<String, Object>>) body.get("images");
            result.put("coverUrl", (images != null && !images.isEmpty()) ? asString(images.get(0).get("url")) : null);
            List<Map<String, Object>> artists = (List<Map<String, Object>>) body.get("artists");
            result.put("artist", (artists != null && !artists.isEmpty()) ? asString(artists.get(0).get("name")) : "");
            result.put("externalId", id);
            result.put("type", "DISCO");
            result.put("releaseDate", parseDate(asString(body.get("release_date"))));
            Object totalTracks = body.get("total_tracks");
            result.put("totalTracks", totalTracks instanceof Number ? ((Number)totalTracks).intValue() : 0);
            Map<String, Object> tracksObj = (Map<String, Object>) body.get("tracks");
            if (tracksObj != null) {
                List<Map<String, Object>> items = (List<Map<String, Object>>) tracksObj.get("items");
                if (items != null) {
                    List<ExternalTrack> trackList = items.stream().map(t -> {
                        int num = t.get("track_number") != null ? ((Number)t.get("track_number")).intValue() : 0;
                        String name = asString(t.get("name"));
                        long durMs = t.get("duration_ms") != null ? ((Number)t.get("duration_ms")).longValue() : 0;
                        return new ExternalTrack(num, name, formatTrackDuration(durMs));
                    }).collect(Collectors.toList());
                    result.put("tracks", trackList);
                    // Pre-formateamos la lista para el campo oculto
                    String joined = trackList.stream()
                        .map(t -> t.getNumber() + ":" + t.getTitle())
                        .collect(Collectors.joining("|"));
                    result.put("trackListString", joined);
                }
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Spotify Details: " + e.getMessage());
        }
        return result;
    }

    private Map<String, Object> getGoogleBooksDetails(String id) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (!isGoogleBooksAvailable()) {
                return getOpenLibraryDetails(id);
            }
            UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString("https://www.googleapis.com/books/v1/volumes/" + id);
            String url = applyGoogleBooksKey(builder).build().toUriString();
            Map body = restClient.get().uri(url).retrieve().body(Map.class);
            if (body == null) return getOpenLibraryDetails(id);
            Map<String, Object> info = (Map<String, Object>) body.get("volumeInfo");
            result.put("title", asString(info.get("title")));
            result.put("description", asString(info.get("description")));
            List<String> authors = (List<String>) info.get("authors");
            result.put("author", (authors != null && !authors.isEmpty()) ? authors.get(0) : "");
            
            List<String> categories = (List<String>) info.get("categories");
            if (categories != null && !categories.isEmpty()) {
                result.put("genre", categories.get(0));
            }
            
            Map<String, Object> imgs = (Map<String, Object>) info.get("imageLinks");
            result.put("coverUrl", (imgs != null) ? asString(imgs.get("thumbnail")).replace("http://", "https://") : "");
            result.put("releaseDate", parseDate(asString(info.get("publishedDate"))));
            result.put("externalId", id);
            result.put("type", "LIBRO");
        } catch (Exception e) {
            markGoogleBooksRateLimited(e);
            System.err.println("[ERROR] GoogleBooks Details: " + e.getMessage());
        }
        return result;
    }

    private List<ExternalContentItem> searchOpenLibrary(String query) {
        return searchOpenLibrary(query, 8);
    }

    private List<ExternalContentItem> searchOpenLibrary(String query, int limit) {
        List<ExternalContentItem> items = new ArrayList<>();
        try {
            // Buscamos mÃ¡s para filtrar y quedarnos con los que tienen portada si es posible
            java.net.URI url = buildOpenLibrarySearchUrl("q", query, limit * 2);
            Map body = restClient.get().uri(url).retrieve().body(Map.class);
            if (body == null) return items;
            List<Map<String, Object>> docs = (List<Map<String, Object>>) body.get("docs");
            if (docs == null) return items;
            
            // Primero intentamos añadir los que tienen portada
            for (Map<String, Object> doc : docs) {
                if (doc.get("cover_i") != null) {
                    items.add(parseOpenLibraryDoc(doc));
                    if (items.size() >= limit) return items;
                }
            }
            
            // Si faltan, añadimos los que no tienen
            for (Map<String, Object> doc : docs) {
                if (doc.get("cover_i") == null) {
                    items.add(parseOpenLibraryDoc(doc));
                    if (items.size() >= limit) return items;
                }
            }
        } catch (Exception e) {
            System.err.println("[ERROR] OpenLibrary Search: " + e.getMessage());
        }
        return items;
    }

    private ExternalContentItem parseOpenLibraryDoc(Map<String, Object> doc) {
        String title = asString(doc.get("title"));
        String key = asString(doc.get("key"));
        Integer year = doc.get("first_publish_year") instanceof Number
            ? ((Number) doc.get("first_publish_year")).intValue()
            : null;
        LocalDate releaseDate = year != null && year > 0 ? LocalDate.of(year, 1, 1) : null;
        List<String> authors = (List<String>) doc.get("author_name");
        String author = (authors != null && !authors.isEmpty()) ? authors.get(0) : "";
        String description = extractOpenLibraryDescription(doc);
        Integer coverId = doc.get("cover_i") instanceof Number
            ? ((Number) doc.get("cover_i")).intValue()
            : null;
        String cover = coverId != null ? "https://covers.openlibrary.org/b/id/" + coverId + "-M.jpg" : "";
        return new ExternalContentItem("OpenLibrary", key, title, "LIBRO", description, releaseDate, cover,
            null, author, null, null, null);
    }

    public List<ExternalContentItem> searchBooksByAuthor(String author) {
        if (author == null || author.isBlank()) {
            return List.of();
        }
        List<ExternalContentItem> items = new ArrayList<>();
        try {
            java.net.URI url = buildOpenLibrarySearchUrl("author", author);
            Map body = restClient.get().uri(url).retrieve().body(Map.class);
            if (body == null) return items;
            List<Map<String, Object>> docs = (List<Map<String, Object>>) body.get("docs");
            if (docs == null) return items;
            for (Map<String, Object> doc : docs) {
                String title = asString(doc.get("title"));
                if (title.isBlank()) continue;
                String key = asString(doc.get("key"));
                Integer year = doc.get("first_publish_year") instanceof Number
                    ? ((Number) doc.get("first_publish_year")).intValue()
                    : null;
                LocalDate releaseDate = year != null && year > 0 ? LocalDate.of(year, 1, 1) : null;
                List<String> authors = (List<String>) doc.get("author_name");
                String authorName = (authors != null && !authors.isEmpty()) ? authors.get(0) : "";
                String description = extractOpenLibraryDescription(doc);
                Integer coverId = doc.get("cover_i") instanceof Number
                    ? ((Number) doc.get("cover_i")).intValue()
                    : null;
                String cover = coverId != null ? "https://covers.openlibrary.org/b/id/" + coverId + "-M.jpg" : "";
                items.add(new ExternalContentItem("OpenLibrary", key, title, "LIBRO", description, releaseDate, cover,
                    null, authorName, null, null, null));
            }
        } catch (Exception e) {
            System.err.println("[ERROR] OpenLibrary Author Search: " + e.getMessage());
        }
        if (items.isEmpty()) {
            return searchOpenLibrary("author:" + author);
        }
        return items;
    }

    private Map<String, Object> getOpenLibraryDetails(String id) {
        Map<String, Object> result = new HashMap<>();
        try {
            String workId = normalizeOpenLibraryWorkId(id);
            if (workId.isBlank()) return result;
            String url = UriComponentsBuilder.fromUriString("https://openlibrary.org" + workId + ".json")
                .build(true)
                .toUriString();
            Map body = restClient.get().uri(url).retrieve().body(Map.class);
            if (body == null) {
                System.out.println("[WARN] OpenLibrary Details: No se recibió body para " + workId);
                return result;
            }
            String title = asString(body.get("title"));
            Object descriptionObj = body.get("description");
            String description = "";
            if (descriptionObj instanceof Map) {
                description = asString(((Map) descriptionObj).get("value"));
            } else if (descriptionObj != null) {
                description = asString(descriptionObj);
            }

            // Si no hay descripción en el Work, intentamos buscarla en los resultados de búsqueda previos o en el primer extracto
            if (description.isBlank()) {
                Object notes = body.get("notes");
                if (notes instanceof Map) description = asString(((Map) notes).get("value"));
                else if (notes != null) description = asString(notes);
            }
            
            if (description.isBlank()) {
                Object excerpts = body.get("excerpts");
                if (excerpts instanceof List && !((List)excerpts).isEmpty()) {
                    Object firstExcerpt = ((List)excerpts).get(0);
                    if (firstExcerpt instanceof Map) description = asString(((Map) firstExcerpt).get("excerpt"));
                }
            }

            List<Object> covers = (List<Object>) body.get("covers");
            String coverUrl = "";
            if (covers != null && !covers.isEmpty()) {
                Object coverId = covers.get(0);
                coverUrl = "https://covers.openlibrary.org/b/id/" + asString(coverId) + "-L.jpg";
            }
            String authorName = resolveOpenLibraryAuthor(body.get("authors"));
            result.put("title", title);
            result.put("description", description);
            result.put("author", authorName);
            result.put("coverUrl", coverUrl);
            result.put("releaseDate", null);
            result.put("externalId", id);
            result.put("type", "LIBRO");

            try {
                String editionsUrl = UriComponentsBuilder.fromUriString("https://openlibrary.org" + workId + "/editions.json")
                    .queryParam("limit", 10).build(true).toUriString();
                Map editionsBody = restClient.get().uri(editionsUrl).retrieve().body(Map.class);
                if (editionsBody != null) {
                    List<Map<String, Object>> entries = (List<Map<String, Object>>) editionsBody.get("entries");
                    if (entries != null) {
                        for (Map<String, Object> entry : entries) {
                            Object pagesObj = entry.get("number_of_pages");
                            if (pagesObj instanceof Number) {
                                result.put("totalPages", ((Number) pagesObj).intValue());
                                break;
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
        } catch (Exception e) {
            System.err.println("[ERROR] OpenLibrary Details: " + e.getMessage());
        }
        return result;
    }

    public Map<String, Object> getActorDetails(String name) {
        Map<String, Object> result = new HashMap<>();
        result.put("name", name);
        try {
            // 1. Buscar ID de la persona en TMDb
            String sUrl = UriComponentsBuilder.fromUriString("https://api.themoviedb.org/3/search/person")
                .queryParam("api_key", TMDB_API_KEY).queryParam("query", name).queryParam("language", "es-ES").build().toUriString();
            Map tmdbBody = restClient.get().uri(sUrl).retrieve().body(Map.class);
            List<Map<String, Object>> tmdbRes = (List<Map<String, Object>>) tmdbBody.get("results");
            
            if (tmdbRes != null && !tmdbRes.isEmpty()) {
                Map<String, Object> personSearch = tmdbRes.get(0);
                String pId = String.valueOf(personSearch.get("id"));
                
                // 2. Obtener detalles reales (Biografia y Foto HQ)
                String dUrl = UriComponentsBuilder.fromUriString("https://api.themoviedb.org/3/person/" + pId)
                    .queryParam("api_key", TMDB_API_KEY).queryParam("language", "es-ES").build().toUriString();
                Map pDetail = restClient.get().uri(dUrl).retrieve().body(Map.class);
                
                String bio = asString(pDetail.get("biography"));
                
                // Si la biografía está vacía en español, intentamos en inglés
                if (bio.isEmpty()) {
                    String enUrl = UriComponentsBuilder.fromUriString("https://api.themoviedb.org/3/person/" + pId)
                        .queryParam("api_key", TMDB_API_KEY).queryParam("language", "en-US").build().toUriString();
                    Map pEnDetail = restClient.get().uri(enUrl).retrieve().body(Map.class);
                    bio = asString(pEnDetail.get("biography"));
                }
                
                result.put("bio", bio.isEmpty() ? "Biografía no disponible por el momento." : bio);
                String path = asString(pDetail.get("profile_path"));
                result.put("photoUrl", path.isEmpty() ? "" : "https://image.tmdb.org/t/p/w600_and_h900_bestv2" + path);
                
                // 3. Obtener Filmografia completa
                String cUrl = UriComponentsBuilder.fromUriString("https://api.themoviedb.org/3/person/" + pId + "/combined_credits")
                    .queryParam("api_key", TMDB_API_KEY).queryParam("language", "es-ES").build().toUriString();
                Map cBody = restClient.get().uri(cUrl).retrieve().body(Map.class);
                List<Map<String, Object>> cast = (List<Map<String, Object>>) cBody.get("cast");
                
                if (cast != null) {
                    result.put("movies", cast.stream()
                        .filter(m -> m.get("poster_path") != null)
                        .sorted((m1, m2) -> {
                            double p2 = m2.get("popularity") != null ? ((Number)m2.get("popularity")).doubleValue() : 0;
                            double p1 = m1.get("popularity") != null ? ((Number)m1.get("popularity")).doubleValue() : 0;
                            return Double.compare(p2, p1);
                        })
                        .limit(48)
                        .map(m -> {
                            Map<String, String> item = new HashMap<>();
                            String mId = String.valueOf(m.get("id"));
                            String mType = "movie".equals(m.get("media_type")) ? "pelicula" : "serie";
                            item.put("externalId", mId);
                            item.put("type", mType);
                            item.put("title", asString(m.get("title") != null ? m.get("title") : m.get("name")));
                            item.put("coverUrl", "https://image.tmdb.org/t/p/w300" + asString(m.get("poster_path")));
                            return item;
                        }).collect(Collectors.toList()));
                }
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Actor Deep Search: " + e.getMessage());
            result.put("bio", "No se pudo cargar la información detallada.");
        }
        return result;
    }

    private String asString(Object v) { return v == null ? "" : String.valueOf(v).trim(); }
    private boolean isGoogleBooksAvailable() {
        if (googleBooksApiKey == null || googleBooksApiKey.isBlank()) {
            return false;
        }
        return System.currentTimeMillis() >= googleBooksBackoffUntilMs;
    }
    private LocalDate parseSpotifyReleaseDate(Map<String, Object> album) {
        if (album == null) {
            return null;
        }
        String raw = asString(album.get("release_date"));
        if (raw.isBlank()) {
            return null;
        }
        String precision = asString(album.get("release_date_precision")).toLowerCase(Locale.ROOT);
        try {
            if ("day".equals(precision)) {
                return LocalDate.parse(raw);
            }
            if ("month".equals(precision)) {
                YearMonth month = YearMonth.parse(raw);
                return month.atEndOfMonth();
            }
            if ("year".equals(precision)) {
                int year = Integer.parseInt(raw.substring(0, 4));
                return LocalDate.of(year, 12, 31);
            }
        } catch (Exception ignored) {
            return parseDate(raw);
        }
        return parseDate(raw);
    }
    private void markGoogleBooksRateLimited(Exception e) {
        if (e == null) {
            return;
        }
        String message = e.getMessage();
        if (message == null) {
            return;
        }
        String normalized = message.toLowerCase(Locale.ROOT);
        if (normalized.contains("429") || normalized.contains("rate_limit") || normalized.contains("resource_exhausted")) {
            googleBooksBackoffUntilMs = System.currentTimeMillis() + (6L * 60L * 60L * 1000L);
        }
    }
    private String extractOpenLibraryDescription(Map<String, Object> doc) {
        if (doc == null) {
            return "";
        }
        Object firstSentence = doc.get("first_sentence");
        if (firstSentence instanceof List) {
            List<?> list = (List<?>) firstSentence;
            if (!list.isEmpty()) {
                return asString(list.get(0));
            }
        }
        return asString(firstSentence);
    }
    private java.net.URI buildOpenLibrarySearchUrl(String param, String value) {
        return buildOpenLibrarySearchUrl(param, value, 8);
    }
    private java.net.URI buildOpenLibrarySearchUrl(String param, String value, int limit) {
        return UriComponentsBuilder.fromUriString("https://openlibrary.org/search.json")
            .queryParam(param, value)
            .queryParam("limit", limit)
            .build()
            .encode()
            .toUri();
    }
    private String resolveOpenLibraryAuthor(Object authorsObj) {
        if (!(authorsObj instanceof List)) {
            return "";
        }
        List<?> authors = (List<?>) authorsObj;
        if (authors.isEmpty()) {
            return "";
        }
        Object first = authors.get(0);
        if (!(first instanceof Map)) {
            return "";
        }
        Object authorObj = ((Map<?, ?>) first).get("author");
        if (!(authorObj instanceof Map)) {
            return "";
        }
        String key = asString(((Map<?, ?>) authorObj).get("key"));
        if (key.isBlank()) {
            return "";
        }
        try {
            String url = UriComponentsBuilder.fromUriString("https://openlibrary.org" + key + ".json")
                .build(true)
                .toUriString();
            Map body = restClient.get().uri(url).retrieve().body(Map.class);
            return body == null ? "" : asString(body.get("name"));
        } catch (Exception e) {
            return "";
        }
    }
    private String normalizeOpenLibraryWorkId(String id) {
        if (id == null) return "";
        String trimmed = id.trim();
        if (trimmed.isBlank()) return "";
        if (trimmed.startsWith("/works/")) return trimmed;
        if (trimmed.startsWith("works/")) return "/" + trimmed;
        if (trimmed.startsWith("OL")) return "/works/" + trimmed;
        return trimmed.contains("/") ? trimmed : "/works/" + trimmed;
    }
    private boolean containsPopularTitle(String title) {
        return title != null && title.toLowerCase(Locale.ROOT).contains("popular");
    }
    private UriComponentsBuilder applyGoogleBooksKey(UriComponentsBuilder builder) {
        if (googleBooksApiKey != null && !googleBooksApiKey.isBlank()) {
            builder.queryParam("key", googleBooksApiKey.trim());
        }
        return builder;
    }
    private String formatTrackDuration(long ms) { return String.format("%d:%02d", (ms/1000)/60, (ms/1000)%60); }
    private LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try { return LocalDate.parse(s.substring(0, 10)); } catch (Exception e) {
            try { if (s.length() >= 4) return LocalDate.of(Integer.parseInt(s.substring(0, 4)), 1, 1); } catch (Exception e2) {}
            return null;
        }
    }

    public List<ExternalContentItem> getMoviesByGenre(String genre) {
        String genreId = switch (genre.toLowerCase()) {
            case "accion" -> "28";
            case "comedia" -> "35";
            case "drama" -> "18";
            case "terror" -> "27";
            case "scifi" -> "878";
            default -> "";
        };
        if (genreId.isEmpty()) return new ArrayList<>();
        List<ExternalContentItem> items = new ArrayList<>();
        try {
            String url = UriComponentsBuilder.fromUriString("https://api.themoviedb.org/3/discover/movie")
                .queryParam("api_key", TMDB_API_KEY)
                .queryParam("language", "es-ES")
                .queryParam("with_genres", genreId)
                .queryParam("sort_by", "popularity.desc")
                .build().toUriString();
            Map body = restClient.get().uri(url).retrieve().body(Map.class);
            if (body == null) return items;
            List<Map<String, Object>> results = (List<Map<String, Object>>) body.get("results");
            if (results != null) {
                int count = 0;
                for (Map<String, Object> res : results) {
                    items.add(parseTmdbItem(res, "movie"));
                    count++;
                    if (count >= 24) break;
                }
            }
        } catch (Exception e) {
            System.err.println("[ERROR] TMDb discover movie genre: " + e.getMessage());
        }
        return items;
    }

    public List<ExternalContentItem> getSeriesByGenre(String genre) {
        String genreId = switch (genre.toLowerCase()) {
            case "drama" -> "18";
            case "comedia" -> "35";
            case "misterio" -> "9648";
            case "scifi" -> "10765";
            default -> "";
        };
        if (genreId.isEmpty()) return new ArrayList<>();
        List<ExternalContentItem> items = new ArrayList<>();
        try {
            String url = UriComponentsBuilder.fromUriString("https://api.themoviedb.org/3/discover/tv")
                .queryParam("api_key", TMDB_API_KEY)
                .queryParam("language", "es-ES")
                .queryParam("with_genres", genreId)
                .queryParam("sort_by", "popularity.desc")
                .build().toUriString();
            Map body = restClient.get().uri(url).retrieve().body(Map.class);
            if (body == null) return items;
            List<Map<String, Object>> results = (List<Map<String, Object>>) body.get("results");
            if (results != null) {
                int count = 0;
                for (Map<String, Object> res : results) {
                    items.add(parseTmdbItem(res, "tv"));
                    count++;
                    if (count >= 24) break;
                }
            }
        } catch (Exception e) {
            System.err.println("[ERROR] TMDb discover tv genre: " + e.getMessage());
        }
        return items;
    }

    public List<ExternalContentItem> getDiscsByGenre(String genre) {
        String query = switch (genre.toLowerCase()) {
            case "pop" -> "top pop albums";
            case "rock" -> "classic rock albums";
            case "electronic" -> "electronic dance music albums";
            case "indie" -> "indie alternative albums";
            default -> genre;
        };
        List<ExternalContentItem> results = searchSpotifyAlbums(query, 24);
        if (results == null || results.isEmpty()) {
            return getFallBackDiscs();
        }
        return results;
    }

    public List<ExternalContentItem> getBooksByGenre(String genre) {
        String subject = switch (genre.toLowerCase()) {
            case "fiction" -> "fiction";
            case "fantasy" -> "fantasy";
            case "science_fiction" -> "science_fiction";
            case "history" -> "history";
            case "thriller" -> "thriller";
            default -> genre;
        };
        List<ExternalContentItem> items = new ArrayList<>();
        try {
            java.net.URI url = buildOpenLibrarySearchUrl("subject", subject, 24);
            Map body = restClient.get().uri(url).retrieve().body(Map.class);
            if (body == null) return items;
            List<Map<String, Object>> docs = (List<Map<String, Object>>) body.get("docs");
            if (docs == null) return items;
            for (Map<String, Object> doc : docs) {
                String title = asString(doc.get("title"));
                if (title.isBlank()) continue;
                String key = asString(doc.get("key"));
                Integer year = doc.get("first_publish_year") instanceof Number
                    ? ((Number) doc.get("first_publish_year")).intValue()
                    : null;
                LocalDate releaseDate = year != null && year > 0 ? LocalDate.of(year, 1, 1) : null;
                List<String> authors = (List<String>) doc.get("author_name");
                String author = (authors != null && !authors.isEmpty()) ? authors.get(0) : "";
                String description = extractOpenLibraryDescription(doc);
                Integer coverId = doc.get("cover_i") instanceof Number
                    ? ((Number) doc.get("cover_i")).intValue()
                    : null;
                String cover = coverId != null ? "https://covers.openlibrary.org/b/id/" + coverId + "-M.jpg" : "";
                items.add(new ExternalContentItem("OpenLibrary", key, title, "LIBRO", description, releaseDate, cover,
                    null, author, null, null, null));
                if (items.size() >= 24) break;
            }
        } catch (Exception e) {
            System.err.println("[ERROR] OpenLibrary Genre Search: " + e.getMessage());
        }
        return items;
    }
}
