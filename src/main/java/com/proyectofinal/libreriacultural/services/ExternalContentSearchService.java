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

@Service
public class ExternalContentSearchService {

    private final RestClient restClient;
    
    private final String TMDB_API_KEY = "ecb446e90fa5f7ea3f7fed4aac7df0e4";
    private final String SPOTIFY_CLIENT_ID = "d52040e3f56a499d870c1d8bb13c4fe7";
    private final String SPOTIFY_CLIENT_SECRET = "2dc43684923e412e9040d73c4a97cdfb";

    @Value("${google.books.api.key:}")
    private String googleBooksApiKey;

    private volatile long googleBooksBackoffUntilMs = 0L;

    private String spotifyToken = null;
    private long tokenExpiry = 0;

    public ExternalContentSearchService() {
        System.out.println("[INFO] Inicializando ExternalContentSearchService...");
        this.restClient = RestClient.create();
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
        List<ExternalContentItem> items = new ArrayList<>();
        try {
            String url = UriComponentsBuilder.fromUriString("https://api.themoviedb.org/3/search/" + type)
                .queryParam("api_key", TMDB_API_KEY).queryParam("query", query).queryParam("language", "es-ES").build().toUriString();
            Map body = restClient.get().uri(url).retrieve().body(Map.class);
            if (body == null) return items;
            List<Map<String, Object>> results = (List<Map<String, Object>>) body.get("results");
            if (results != null) for (Map<String, Object> res : results) items.add(parseTmdbItem(res, type));
        } catch (Exception e) {
            System.err.println("[ERROR] TMDb Search: " + e.getMessage());
        }
        return items;
    }

    public List<ExternalContentItem> getWeeklyTrendingMovies() {
        List<ExternalContentItem> items = new ArrayList<>();
        try {
            String url = UriComponentsBuilder.fromUriString("https://api.themoviedb.org/3/trending/movie/week")
                .queryParam("api_key", TMDB_API_KEY)
                .queryParam("language", "es-ES")
                .build().toUriString();
            Map body = restClient.get().uri(url).retrieve().body(Map.class);
            if (body == null) return items;
            List<Map<String, Object>> results = (List<Map<String, Object>>) body.get("results");
            if (results != null) {
                int count = 0;
                for (Map<String, Object> res : results) {
                    items.add(parseTmdbItem(res, "movie"));
                    count++;
                    if (count >= 8) break;
                }
            }
        } catch (Exception e) {
            System.err.println("[ERROR] TMDb Trending: " + e.getMessage());
        }
        return items;
    }

    public List<ExternalContentItem> getWeeklyTrendingSeries() {
        List<ExternalContentItem> items = new ArrayList<>();
        try {
            String url = UriComponentsBuilder.fromUriString("https://api.themoviedb.org/3/trending/tv/week")
                .queryParam("api_key", TMDB_API_KEY)
                .queryParam("language", "es-ES")
                .build().toUriString();
            Map body = restClient.get().uri(url).retrieve().body(Map.class);
            if (body == null) return items;
            List<Map<String, Object>> results = (List<Map<String, Object>>) body.get("results");
            if (results != null) {
                int count = 0;
                for (Map<String, Object> res : results) {
                    items.add(parseTmdbItem(res, "tv"));
                    count++;
                    if (count >= 8) break;
                }
            }
        } catch (Exception e) {
            System.err.println("[ERROR] TMDb Trending Series: " + e.getMessage());
        }
        return items;
    }

    private ExternalContentItem parseTmdbItem(Map<String, Object> res, String type) {
        String id = String.valueOf(res.get("id"));
        String title = asString(res.get(type.equals("movie") ? "title" : "name"));
        String poster = asString(res.get("poster_path"));
        String cover = poster.isEmpty() ? "" : "https://image.tmdb.org/t/p/w500" + poster;
        LocalDate date = parseDate(asString(res.get(type.equals("movie") ? "release_date" : "first_air_date")));
        return new ExternalContentItem("TMDb", id, title, type.equals("movie") ? "pelicula" : "serie", asString(res.get("overview")), date, cover, null, null, null, null, null);
    }

    public List<ExternalContentItem> searchSpotify(String query) {
        return searchSpotifyAlbums(query, 20);
    }

    public List<ExternalContentItem> getWeeklyTrendingDiscs() {
        List<ExternalContentItem> candidates = new ArrayList<>();
        Set<String> candidateKeys = new HashSet<>();
        int currentYear = LocalDate.now().getYear();

        appendDiscSearchCandidates(candidates, candidateKeys, "tag:new year:" + currentYear, 60);
        appendDiscSearchCandidates(candidates, candidateKeys, "tag:new year:" + (currentYear - 1), 40);
        appendDiscSearchCandidates(candidates, candidateKeys, "tag:new", 40);
        appendDiscSearchCandidates(candidates, candidateKeys, "year:" + currentYear, 40);

        if (candidates.isEmpty()) {
            return List.of();
        }

        int maxCount = 16;
        List<ExternalContentItem> results = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        appendDiscCandidates(results, seen, candidates, LocalDate.now().minusDays(7), false, maxCount);
        if (results.size() < maxCount) {
            appendDiscCandidates(results, seen, candidates, LocalDate.now().minusDays(30), false, maxCount);
        }
        if (results.size() < maxCount) {
            appendDiscCandidates(results, seen, candidates, null, true, maxCount);
        }

        return results;
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

    private List<ExternalContentItem> searchSpotifyAlbums(String query, int limit) {
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
        return new ExternalContentItem("Spotify", asString(res.get("id")), asString(res.get("name")), "disco", "Álbum de " + artist, releaseDate, cover, null, artist, artistId, null, null);
    }

    public List<ExternalContentItem> searchBooks(String query) {
        String normalizedQuery = query == null ? "" : query.trim();
        boolean authorQuery = normalizedQuery.toLowerCase(Locale.ROOT).startsWith("inauthor:");
        String authorValue = authorQuery ? normalizedQuery.substring("inauthor:".length()).trim() : "";
        return authorQuery ? searchBooksByAuthor(authorValue) : searchOpenLibrary(normalizedQuery);
    }

    public List<ExternalContentItem> getWeeklyTrendingBooks() {
        return searchOpenLibrary("bestseller");
    }

    public List<ExternalContentItem> searchArtistAlbums(String artistId) {
        List<ExternalContentItem> items = new ArrayList<>();
        String token = getSpotifyToken();
        if (token == null) return items;
        try {
            String url = "https://api.spotify.com/v1/artists/" + artistId + "/albums?limit=20";
            Map body = restClient.get().uri(url).header("Authorization", "Bearer " + token).retrieve().body(Map.class);
            List<Map<String, Object>> results = (List<Map<String, Object>>) body.get("items");
            if (results != null) for (Map<String, Object> res : results) items.add(parseSpotifyItem(res));
        } catch (Exception e) {}
        return items;
    }

    public Map<String, Object> getDetails(String source, String externalId, String type) {
        try {
            if ("Spotify".equalsIgnoreCase(source)) return getSpotifyDetails(externalId);
            if ("TMDb".equalsIgnoreCase(source)) return getTmdbDetails(externalId, type);
            if ("GoogleBooks".equalsIgnoreCase(source) || "OpenLibrary".equalsIgnoreCase(source)) {
                if (externalId != null && externalId.startsWith("OL")) {
                    return getOpenLibraryDetails(externalId);
                }
                return getGoogleBooksDetails(externalId);
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Details general: " + e.getMessage());
        }
        return new HashMap<>();
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
            String poster = asString(body.get("poster_path"));
            result.put("coverUrl", poster.isEmpty() ? "" : "https://image.tmdb.org/t/p/w500" + poster);
            result.put("releaseDate", parseDate(asString(body.get(tmdbType.equals("movie") ? "release_date" : "first_air_date"))));
            result.put("externalId", id);
            result.put("type", type);
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
            result.put("coverUrl", (images != null && !images.isEmpty()) ? asString(images.get(0).get("url")) : "");
            List<Map<String, Object>> artists = (List<Map<String, Object>>) body.get("artists");
            result.put("artist", (artists != null && !artists.isEmpty()) ? asString(artists.get(0).get("name")) : "");
            result.put("externalId", id);
            result.put("type", "disco");
            result.put("releaseDate", parseDate(asString(body.get("release_date"))));
            Map<String, Object> tracksObj = (Map<String, Object>) body.get("tracks");
            if (tracksObj != null) {
                List<Map<String, Object>> items = (List<Map<String, Object>>) tracksObj.get("items");
                if (items != null) {
                    result.put("tracks", items.stream().map(t -> {
                        int num = t.get("track_number") != null ? ((Number)t.get("track_number")).intValue() : 0;
                        String name = asString(t.get("name"));
                        long durMs = t.get("duration_ms") != null ? ((Number)t.get("duration_ms")).longValue() : 0;
                        return new ExternalTrack(num, name, formatTrackDuration(durMs));
                    }).collect(Collectors.toList()));
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
            Map<String, Object> imgs = (Map<String, Object>) info.get("imageLinks");
            result.put("coverUrl", (imgs != null) ? asString(imgs.get("thumbnail")).replace("http://", "https://") : "");
            result.put("releaseDate", parseDate(asString(info.get("publishedDate"))));
            result.put("externalId", id);
            result.put("type", "libro");
        } catch (Exception e) {
            markGoogleBooksRateLimited(e);
            System.err.println("[ERROR] GoogleBooks Details: " + e.getMessage());
        }
        return result;
    }

    private List<ExternalContentItem> searchOpenLibrary(String query) {
        List<ExternalContentItem> items = new ArrayList<>();
        try {
            java.net.URI url = buildOpenLibrarySearchUrl("q", query);
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
                items.add(new ExternalContentItem("OpenLibrary", key, title, "libro", description, releaseDate, cover,
                    null, author, null, null, null));
            }
        } catch (Exception e) {
            System.err.println("[ERROR] OpenLibrary Search: " + e.getMessage());
        }
        return items;
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
                items.add(new ExternalContentItem("OpenLibrary", key, title, "libro", description, releaseDate, cover,
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
            if (body == null) return result;
            String title = asString(body.get("title"));
            Object descriptionObj = body.get("description");
            String description = "";
            if (descriptionObj instanceof Map) {
                description = asString(((Map) descriptionObj).get("value"));
            } else {
                description = asString(descriptionObj);
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
            result.put("type", "libro");
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
                result.put("bio", bio.isEmpty() ? "Biografía no disponible." : bio);
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
                        .filter(m -> {
                            String character = asString(m.get("character")).toLowerCase();
                            return !character.contains("self") && !character.contains("himself") && !character.contains("herself");
                        })
                        .sorted((m1, m2) -> {
                            double p2 = m2.get("popularity") != null ? ((Number)m2.get("popularity")).doubleValue() : 0;
                            double p1 = m1.get("popularity") != null ? ((Number)m1.get("popularity")).doubleValue() : 0;
                            return Double.compare(p2, p1);
                        })
                        .limit(24)
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
        return UriComponentsBuilder.fromUriString("https://openlibrary.org/search.json")
            .queryParam(param, value)
            .queryParam("limit", 8)
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
        if (trimmed.startsWith("OL")) return "/works/" + trimmed;
        return "";
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
}
