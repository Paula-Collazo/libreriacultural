package com.proyectofinal.libreriacultural.services;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.http.HttpStatusCode;

@Service
public class ExternalContentSearchService {

    private static final Set<String> VALID_TYPES = Set.of("pelicula", "serie", "libro", "disco");

    private final RestClient restClient;
    private final String omdbApiKey;

    public ExternalContentSearchService(@Value("${omdb.api.key:thewdb}") String omdbApiKey) {
        this.restClient = RestClient.builder()
            .requestInterceptor((request, body, execution) -> {
                ClientHttpResponse response = execution.execute(request, body);
                // Si la respuesta es text/javascript (iTunes), la tratamos como JSON
                return new ClientHttpResponse() {
                    @Override
                    public HttpStatusCode getStatusCode() throws IOException { return response.getStatusCode(); }
                    @Override
                    public String getStatusText() throws IOException { return response.getStatusText(); }
                    @Override
                    public void close() { response.close(); }
                    @Override
                    public InputStream getBody() throws IOException { return response.getBody(); }
                    @Override
                    public HttpHeaders getHeaders() {
                        HttpHeaders headers = new HttpHeaders();
                        headers.putAll(response.getHeaders());
                        if (headers.getContentType() != null && headers.getContentType().toString().contains("javascript")) {
                            headers.setContentType(MediaType.APPLICATION_JSON);
                        }
                        return headers;
                    }
                };
            })
            .build();
        this.omdbApiKey = omdbApiKey == null ? "" : omdbApiKey.trim();
    }

    public List<ExternalContentItem> search(String rawQuery, String rawType) {
        String query = rawQuery == null ? "" : rawQuery.trim();
        String type = rawType == null ? "" : rawType.trim().toLowerCase();

        if (query.isBlank()) {
            return List.of();
        }

        if (!VALID_TYPES.contains(type)) {
            throw new IllegalArgumentException("Tipo no valido. Usa pelicula, serie, libro o disco");
        }

        return switch (type) {
            case "libro" -> searchBooks(query);
            case "serie" -> searchSeries(query);
            case "pelicula" -> searchMovies(query);
            case "disco" -> searchDiscs(query);
            default -> List.of();
        };
    }

    @SuppressWarnings("unchecked")
    private List<ExternalContentItem> searchBooks(String query) {
        String url = UriComponentsBuilder.fromUriString("https://openlibrary.org/search.json")
                .queryParam("q", query)
                .queryParam("limit", 8)
                .build()
                .toUriString();

        Map<String, Object> body = restClient.get().uri(url).retrieve().body(Map.class);
        if (body == null) {
            return List.of();
        }

        List<Map<String, Object>> docs = (List<Map<String, Object>>) body.getOrDefault("docs", List.of());
        List<ExternalContentItem> results = new ArrayList<>();

        for (Map<String, Object> doc : docs) {
            String title = asString(doc.get("title"));
            if (title.isBlank()) {
                continue;
            }

            String key = asString(doc.get("key"));
            Integer year = asInteger(doc.get("first_publish_year"));
            LocalDate releaseDate = year != null && year > 0 ? LocalDate.of(year, 1, 1) : null;

            List<Object> authors = (List<Object>) doc.getOrDefault("author_name", List.of());
            String description = authors.isEmpty() ? "Importado desde OpenLibrary"
                    : "Autor: " + asString(authors.get(0));
            Integer coverId = asInteger(doc.get("cover_i"));
            Integer pageCount = asInteger(doc.get("number_of_pages_median"));
            if (pageCount == null || pageCount <= 0) {
                pageCount = fetchBookPageCount(key);
            }
            String metricLabel = pageCount != null && pageCount > 0 ? pageCount + " paginas" : "Paginas sin dato";
            String coverUrl = coverId != null && coverId > 0
                    ? "https://covers.openlibrary.org/b/id/" + coverId + "-M.jpg"
                    : "";

            results.add(
                    new ExternalContentItem("OpenLibrary", key, title, "libro", description, releaseDate, coverUrl,
                            metricLabel, null, null, null, null));
        }

        return results;
    }

    @SuppressWarnings("unchecked")
    private List<ExternalContentItem> searchSeries(String query) {
        String url = UriComponentsBuilder.fromUriString("https://api.tvmaze.com/search/shows")
                .queryParam("q", query)
                .build()
                .toUriString();

        List<Map<String, Object>> body = restClient.get().uri(url).retrieve().body(List.class);
        if (body == null) {
            return List.of();
        }

        List<ExternalContentItem> results = new ArrayList<>();
        for (Map<String, Object> row : body.stream().limit(8).collect(Collectors.toList())) {
            Map<String, Object> show = (Map<String, Object>) row.get("show");
            if (show == null) {
                continue;
            }

            String title = asString(show.get("name"));
            if (title.isBlank()) {
                continue;
            }

            String summaryRaw = asString(show.get("summary"));
            String summary = summaryRaw.replaceAll("<[^>]*>", "").trim();
            String description = summary.isBlank() ? "Importado desde TVMaze" : summary;

            String premiered = asString(show.get("premiered"));
            LocalDate releaseDate = parseDateSafely(premiered);
            String id = asString(show.get("id"));
            Integer runtime = asInteger(show.get("runtime"));
            if (runtime == null || runtime <= 0) {
                runtime = asInteger(show.get("averageRuntime"));
            }
            String metricLabel = runtime != null && runtime > 0 ? runtime + " min por episodio" : "Duracion sin dato";

            @SuppressWarnings("unchecked")
            Map<String, Object> image = (Map<String, Object>) show.get("image");
            String coverUrl = image == null ? "" : asString(image.get("medium"));
            if (coverUrl.isBlank()) {
                coverUrl = image == null ? "" : asString(image.get("original"));
            }

            results.add(new ExternalContentItem("TVMaze", id, title, "serie", description, releaseDate, coverUrl,
                    metricLabel, null, null, null, null));
        }

        return results;
    }

    private List<ExternalContentItem> searchMovies(String query) {
        List<ExternalContentItem> results = searchMoviesFromOmdb(query);
        if (!results.isEmpty()) {
            return results;
        }
        return searchMoviesFromItunes(query);
    }

    @SuppressWarnings("unchecked")
    private List<ExternalContentItem> searchDiscs(String query) {
        try {
            String url = UriComponentsBuilder.fromUriString("https://itunes.apple.com/search")
                    .queryParam("term", query)
                    .queryParam("media", "music")
                    .queryParam("entity", "album")
                    .queryParam("country", "us")
                    .queryParam("limit", 8)
                    .build()
                    .toUriString();

            Map<String, Object> body = restClient.get().uri(url).retrieve().body(Map.class);
            if (body == null) {
                return List.of();
            }

            List<Map<String, Object>> rows = (List<Map<String, Object>>) body.getOrDefault("results", List.of());
            List<ExternalContentItem> results = new ArrayList<>();

            for (Map<String, Object> row : rows) {
                String title = asString(row.get("collectionName"));
                if (title.isBlank()) {
                    continue;
                }

                String artist = asString(row.get("artistName"));
                String artistId = asString(row.get("artistId"));
                String description = artist.isBlank() ? "" : "Artista: " + artist;
                LocalDate releaseDate = parseDateSafely(asString(row.get("releaseDate")));
                String id = asString(row.get("collectionId"));
                Integer trackCount = asInteger(row.get("trackCount"));
                String metricLabel = trackCount != null && trackCount > 0
                        ? trackCount + " canciones"
                        : "Canciones sin dato";
                String coverUrl = asString(row.get("artworkUrl100"));
                if (coverUrl != null) {
                    coverUrl = coverUrl.replace("100x100bb.jpg", "600x600bb.jpg");
                }

                results.add(new ExternalContentItem("iTunes", id, title, "disco", description, releaseDate, coverUrl,
                    metricLabel, artist, artistId, null, null));
            }
            return results.isEmpty() ? searchDiscsFromDeezer(query) : results;
        } catch (Exception ex) {
            return searchDiscsFromDeezer(query);
        }
    }

    @SuppressWarnings("unchecked")
    private List<ExternalContentItem> searchDiscsFromDeezer(String query) {
        try {
                String url = UriComponentsBuilder.fromUriString("http://api.deezer.com/search/album")
                    .queryParam("q", query)
                    .build()
                    .toUriString();

            Map<String, Object> body = restClient.get().uri(url).retrieve().body(Map.class);
            if (body == null) {
                return List.of();
            }

            List<Map<String, Object>> rows = (List<Map<String, Object>>) body.getOrDefault("data", List.of());
            List<ExternalContentItem> results = new ArrayList<>();

            for (Map<String, Object> row : rows.stream().limit(8).collect(Collectors.toList())) {
                String title = asString(row.get("title"));
                if (title.isBlank()) {
                    continue;
                }

                @SuppressWarnings("unchecked")
                Map<String, Object> artist = (Map<String, Object>) row.get("artist");
                String artistName = artist == null ? "" : asString(artist.get("name"));
                String artistId = artist == null ? "" : asString(artist.get("id"));
                String description = artistName.isBlank() ? "Importado desde Deezer" : "Artista: " + artistName;
                LocalDate releaseDate = parseDateSafely(asString(row.get("release_date")));
                String id = asString(row.get("id"));
                Integer trackCount = asInteger(row.get("nb_tracks"));
                String metricLabel = trackCount != null && trackCount > 0
                        ? trackCount + " canciones"
                        : "Canciones sin dato";

                String coverUrl = asString(row.get("cover_big"));
                if (coverUrl == null || coverUrl.isBlank()) {
                    coverUrl = asString(row.get("cover_medium"));
                }
                if (coverUrl.isBlank()) {
                    coverUrl = asString(row.get("cover"));
                }

                results.add(new ExternalContentItem("Deezer", id, title, "disco", description, releaseDate,
                        coverUrl, metricLabel, artistName, artistId, null, null));
            }

            return results;
        } catch (Exception ex) {
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private List<ExternalContentItem> searchMoviesFromOmdb(String query) {
        try {
            if (omdbApiKey.isBlank()) {
                return List.of();
            }

            String url = UriComponentsBuilder.fromUriString("https://www.omdbapi.com/")
                    .queryParam("apikey", omdbApiKey)
                    .queryParam("s", query)
                    .queryParam("type", "movie")
                    .build()
                    .toUriString();

            Map<String, Object> body = restClient.get().uri(url).retrieve().body(Map.class);
            if (body == null) {
                return List.of();
            }

            List<Map<String, Object>> rows = (List<Map<String, Object>>) body.getOrDefault("Search", List.of());
            List<ExternalContentItem> results = new ArrayList<>();

            for (Map<String, Object> row : rows.stream().limit(8).collect(Collectors.toList())) {
                String title = asString(row.get("Title"));
                if (title.isBlank()) {
                    continue;
                }

                String id = asString(row.get("imdbID"));
                OmdbMovieDetails details = fetchOmdbMovieDetails(id);
                String description = details.plot();
                String metricLabel = normalizeRuntime(details.runtime());
                LocalDate releaseDate = parseOmdbReleaseDate(details.released());
                if (releaseDate == null) {
                    String year = asString(row.get("Year"));
                    if (!year.isBlank() && year.length() >= 4) {
                        Integer yearValue = asInteger(year.substring(0, 4));
                        if (yearValue != null && yearValue > 0) {
                            releaseDate = LocalDate.of(yearValue, 1, 1);
                        }
                    }
                }

                String coverUrl = normalizeOmdbValue(asString(row.get("Poster")));
                if (coverUrl.isBlank()) {
                    coverUrl = details.poster();
                }
                if (!coverUrl.isBlank()) {
                    coverUrl = coverUrl.replace("SX300", "SX1000");
                }

                results.add(new ExternalContentItem("OMDb", id, title, "pelicula", description,
                    releaseDate, coverUrl, metricLabel, null, null, details.director(), details.actors()));
            }

            return results;
        } catch (Exception ex) {
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private List<ExternalContentItem> searchMoviesFromItunes(String query) {
        try {
            String url = UriComponentsBuilder.fromUriString("https://itunes.apple.com/search")
                    .queryParam("term", query)
                    .queryParam("media", "movie")
                    .queryParam("country", "us")
                    .queryParam("limit", 8)
                    .build()
                    .toUriString();

            Map<String, Object> body = restClient.get().uri(url).retrieve().body(Map.class);
            if (body == null) {
                return List.of();
            }

            List<Map<String, Object>> rows = (List<Map<String, Object>>) body.getOrDefault("results", List.of());
            List<ExternalContentItem> results = new ArrayList<>();

            for (Map<String, Object> row : rows) {
                String title = asString(row.get("trackName"));
                if (title.isBlank()) {
                    continue;
                }

                String description = asString(row.get("longDescription"));
                if (description.isBlank()) {
                    description = asString(row.get("shortDescription"));
                }
                if (description.isBlank()) {
                    description = "Importado desde iTunes";
                }

                LocalDate releaseDate = parseDateSafely(asString(row.get("releaseDate")));
                String id = asString(row.get("trackId"));
                Long trackTimeMillis = asLong(row.get("trackTimeMillis"));
                String metricLabel = trackTimeMillis != null && trackTimeMillis > 0
                        ? Math.max(1, trackTimeMillis / 60000L) + " min"
                        : "Duracion sin dato";
                String coverUrl = asString(row.get("artworkUrl100"));
                if (coverUrl != null) {
                    coverUrl = coverUrl.replace("100x100bb.jpg", "600x600bb.jpg");
                }

                results.add(new ExternalContentItem("iTunes", id, title, "pelicula", description, releaseDate,
                        coverUrl, metricLabel, null, null, null, null));
            }

            return results;
        } catch (Exception ex) {
            return List.of();
        }
    }

    public ExternalAlbumDetails getAlbumDetails(String albumId) {
        String safeId = albumId == null ? "" : albumId.trim();
        if (safeId.isBlank()) {
            throw new IllegalArgumentException("albumId invalido");
        }

        try {
            String url = UriComponentsBuilder.fromUriString("https://itunes.apple.com/lookup")
                    .queryParam("id", safeId)
                    .queryParam("entity", "song")
                    .queryParam("country", "us")
                    .build()
                    .toUriString();

            Map<String, Object> body = restClient.get().uri(url).retrieve().body(Map.class);
            if (body != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> rows = (List<Map<String, Object>>) body.getOrDefault("results", List.of());
                if (!rows.isEmpty()) {
                    Map<String, Object> albumRow = rows.stream()
                            .filter(row -> "collection".equalsIgnoreCase(asString(row.get("wrapperType"))))
                            .findFirst()
                            .orElse(rows.get(0));

                    ExternalContentItem album = toAlbumItem(albumRow);
                    List<ExternalTrack> tracks = new ArrayList<>();
                    for (Map<String, Object> row : rows) {
                        if (!"track".equalsIgnoreCase(asString(row.get("wrapperType")))) {
                            continue;
                        }
                        if (!"song".equalsIgnoreCase(asString(row.get("kind")))) {
                            continue;
                        }

                        String trackName = asString(row.get("trackName"));
                        if (trackName.isBlank()) {
                            continue;
                        }
                        Integer trackNumber = asInteger(row.get("trackNumber"));
                        Long durationMillis = asLong(row.get("trackTimeMillis"));
                        String durationLabel = formatTrackDuration(durationMillis);
                        tracks.add(new ExternalTrack(trackNumber, trackName, durationLabel));
                    }
                    return new ExternalAlbumDetails(album, tracks);
                }
            }
        } catch (Throwable ex) {
            // Ignorar para intentar con Deezer
        }

        // Fallback to Deezer
        try {
            String url = UriComponentsBuilder.fromUriString("https://api.deezer.com/album/" + safeId).toUriString();
            Map<String, Object> body = restClient.get().uri(url).retrieve().body(Map.class);
            if (body == null || body.containsKey("error")) {
                throw new IllegalStateException("Album sin resultados en Deezer tampoco");
            }

            String title = asString(body.get("title"));
            @SuppressWarnings("unchecked")
            Map<String, Object> artistMap = (Map<String, Object>) body.get("artist");
            String artistName = artistMap == null ? "" : asString(artistMap.get("name"));
            String artistId = artistMap == null ? "" : asString(artistMap.get("id"));
            String description = artistName.isBlank() ? "Importado desde Deezer" : "Artista: " + artistName;
            LocalDate releaseDate = parseDateSafely(asString(body.get("release_date")));
            Integer trackCount = asInteger(body.get("nb_tracks"));
            String metricLabel = trackCount != null && trackCount > 0 ? trackCount + " canciones" : "Canciones sin dato";
            String coverUrl = asString(body.get("cover_medium"));

            ExternalContentItem album = new ExternalContentItem("Deezer", safeId, title, "disco", description, releaseDate, coverUrl, metricLabel, artistName, artistId, null, null);

            List<ExternalTrack> tracks = new ArrayList<>();
            @SuppressWarnings("unchecked")
            Map<String, Object> tracksMap = (Map<String, Object>) body.get("tracks");
            if (tracksMap != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> data = (List<Map<String, Object>>) tracksMap.getOrDefault("data", List.of());
                int i = 1;
                for (Map<String, Object> trackRow : data) {
                    String trackName = asString(trackRow.get("title"));
                    Integer durationSec = asInteger(trackRow.get("duration"));
                    String durationLabel = durationSec != null ? formatTrackDuration(durationSec * 1000L) : "";
                    tracks.add(new ExternalTrack(i++, trackName, durationLabel));
                }
            }

            return new ExternalAlbumDetails(album, tracks);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo consultar el album en Deezer", ex);
        }
    }

    public List<ExternalContentItem> searchArtistAlbums(String artistId) {
        String safeId = artistId == null ? "" : artistId.trim();
        if (safeId.isBlank()) {
            return List.of();
        }

        String url = UriComponentsBuilder.fromUriString("https://itunes.apple.com/lookup")
                .queryParam("id", safeId)
                .queryParam("entity", "album")
                .queryParam("country", "us")
                .build()
                .toUriString();

        Map<String, Object> body = restClient.get().uri(url).retrieve().body(Map.class);
        if (body == null) {
            return List.of();
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) body.getOrDefault("results", List.of());
        List<ExternalContentItem> results = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            if (!"collection".equalsIgnoreCase(asString(row.get("wrapperType")))) {
                continue;
            }
            if (!"Album".equalsIgnoreCase(asString(row.get("collectionType")))) {
                continue;
            }
            ExternalContentItem album = toAlbumItem(row);
            results.add(album);
        }

        return results.stream().limit(8).collect(Collectors.toList());
    }

    public Map<String, Object> getActorDetails(String name) {
        String safeName = name == null ? "" : name.trim();
        Map<String, Object> result = new HashMap<>();
        result.put("name", safeName);

        // Bio y Foto de Wikipedia
        try {
            String wikiUrl = "https://es.wikipedia.org/api/rest_v1/page/summary/" + safeName.replace(" ", "_");
            ResponseEntity<Map> response = restClient.get().uri(wikiUrl)
                .retrieve()
                .onStatus(status -> status.isError(), (req, res) -> { /* ignore */ })
                .toEntity(Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> wiki = response.getBody();
                result.put("bio", wiki.get("extract"));
                Object thumbObj = wiki.get("thumbnail");
                if (thumbObj instanceof Map) {
                    Map<?, ?> thumb = (Map<?, ?>) thumbObj;
                    result.put("photoUrl", thumb.get("source"));
                }
            } else {
                result.put("bio", "No se encontro biografia en Wikipedia.");
            }
        } catch (Exception ex) {
            result.put("bio", "Biografia no disponible temporalmente.");
        }

        // Peliculas de iTunes
        try {
            String itunesUrl = UriComponentsBuilder.fromUriString("https://itunes.apple.com/search")
                .queryParam("term", safeName)
                .queryParam("media", "movie")
                .queryParam("entity", "movie")
                .queryParam("limit", 15)
                .build().toUriString();
            
            Map<String, Object> body = restClient.get().uri(itunesUrl).retrieve().body(Map.class);
            List<ExternalContentItem> movies = new ArrayList<>();
            if (body != null && body.get("results") instanceof List) {
                List<?> rows = (List<?>) body.get("results");
                for (Object obj : rows) {
                    if (obj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> row = (Map<String, Object>) obj;
                        String title = asString(row.get("trackName"));
                        String id = asString(row.get("trackId"));
                        String coverUrl = asString(row.get("artworkUrl100"));
                        if (coverUrl != null) coverUrl = coverUrl.replace("100x100bb.jpg", "600x600bb.jpg");
                        movies.add(new ExternalContentItem("iTunes", id, title, "pelicula", "", null, coverUrl, "", null, null, null, null));
                    }
                }
            }
            result.put("movies", movies);
        } catch (Exception ex) {
            result.put("movies", new ArrayList<ExternalContentItem>());
        }

        return result;
    }

    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private Integer asInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            String text = (String) value;
            if (text.isBlank()) {
                return null;
            }
            try {
                return Integer.valueOf(text);
            } catch (NumberFormatException ex) {
                return null;
            }
        }

        return null;
    }

    private Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            String text = (String) value;
            if (text.isBlank()) {
                return null;
            }
            try {
                return Long.valueOf(text);
            } catch (NumberFormatException ex) {
                return null;
            }
        }

        return null;
    }

    private ExternalContentItem toAlbumItem(Map<String, Object> row) {
        String title = asString(row.get("collectionName"));
        String id = asString(row.get("collectionId"));
        String artist = asString(row.get("artistName"));
        String artistId = asString(row.get("artistId"));
        LocalDate releaseDate = parseDateSafely(asString(row.get("releaseDate")));
        Integer trackCount = asInteger(row.get("trackCount"));
        String metricLabel = trackCount != null && trackCount > 0
                ? trackCount + " canciones"
                : "Canciones sin dato";
        String coverUrl = asString(row.get("artworkUrl100"));
        String description = artist.isBlank() ? "" : "Artista: " + artist;

        return new ExternalContentItem("iTunes", id, title, "disco", description, releaseDate, coverUrl,
            metricLabel, artist, artistId, null, null);
    }

    public ExternalContentItem getSerieDetails(String tvmazeId) {
        String url = UriComponentsBuilder.fromUriString("https://api.tvmaze.com/shows/" + tvmazeId).toUriString();
        try {
            Map<String, Object> show = restClient.get().uri(url).retrieve().body(Map.class);
            if (show == null) throw new IllegalStateException("Serie no encontrada");
            String title = asString(show.get("name"));
            String idStr = asString(show.get("id"));
            String desc = asString(show.get("summary"));
            if (desc != null) desc = desc.replaceAll("<[^>]*>", "");
            String premiered = asString(show.get("premiered"));
            LocalDate releaseDate = parseDateSafely(premiered);
            @SuppressWarnings("unchecked")
            Map<String, Object> image = (Map<String, Object>) show.get("image");
            String coverUrl = image == null ? "" : asString(image.get("medium"));
            if (coverUrl.isBlank()) {
                coverUrl = image == null ? "" : asString(image.get("original"));
            }
            Integer runtime = asInteger(show.get("runtime"));
            if (runtime == null || runtime <= 0) {
                runtime = asInteger(show.get("averageRuntime"));
            }
            String metricLabel = runtime != null && runtime > 0 ? runtime + " min por episodio" : "Duracion sin dato";
            
            return new ExternalContentItem("TVMaze", idStr, title, "serie", desc, releaseDate, coverUrl, metricLabel, null, null, null, null);
        } catch (Exception ex) {
            throw new IllegalStateException("Error consultando serie", ex);
        }
    }

    @SuppressWarnings("unchecked")
    public ExternalContentItem getBookDetails(String olid) {
        String safeId = olid.startsWith("/works/") ? olid : "/works/" + olid;
        String url = UriComponentsBuilder.fromUriString("https://openlibrary.org" + safeId + ".json").toUriString();
        try {
            Map<String, Object> doc = restClient.get().uri(url).retrieve().body(Map.class);
            if (doc == null) throw new IllegalStateException("Libro no encontrado");
            String title = asString(doc.get("title"));
            String desc = "";
            Object descObj = doc.get("description");
            if (descObj instanceof Map) {
                Map<?, ?> m = (Map<?, ?>) descObj;
                desc = asString(m.get("value"));
            } else if (descObj instanceof String) {
                desc = (String) descObj;
            }
            LocalDate releaseDate = null;
            
            List<Integer> covers = (List<Integer>) doc.get("covers");
            String coverUrl = (covers != null && !covers.isEmpty()) ? "https://covers.openlibrary.org/b/id/" + covers.get(0) + "-M.jpg" : "";
            
            return new ExternalContentItem("OpenLibrary", olid, title, "libro", desc, releaseDate, coverUrl, "", null, null, null, null);
        } catch (Exception ex) {
            throw new IllegalStateException("Error consultando libro", ex);
        }
    }

    public ExternalContentItem getMovieDetails(String imdbId) {
        String safeId = imdbId == null ? "" : imdbId.trim();
        if (safeId.isBlank()) {
            throw new IllegalArgumentException("imdbId invalido");
        }

        OmdbMovieDetails details = fetchOmdbMovieDetails(safeId);
        String title = details.title().isBlank() ? "Pelicula" : details.title();
        String coverUrl = normalizeOmdbValue(details.poster());
        if (!coverUrl.isBlank()) {
            coverUrl = coverUrl.replace("SX300", "SX1000");
        }
        LocalDate releaseDate = parseOmdbReleaseDate(details.released());
        String description = details.plot();
        String metricLabel = normalizeRuntime(details.runtime());

        return new ExternalContentItem("OMDb", safeId, title, "pelicula", description,
                releaseDate, coverUrl, metricLabel, null, null, details.director(), details.actors());
    }

    private String normalizeRuntime(String runtimeRaw) {
        String normalized = runtimeRaw == null ? "" : runtimeRaw.trim();
        if (normalized.isBlank() || "N/A".equalsIgnoreCase(normalized)) {
            return "Duracion sin dato";
        }
        return normalized;
    }

    private String formatTrackDuration(Long millis) {
        if (millis == null || millis <= 0) {
            return "";
        }
        long totalSeconds = millis / 1000L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format("%d:%02d", minutes, seconds);
    }

    private OmdbMovieDetails fetchOmdbMovieDetails(String imdbId) {
        String safeId = imdbId == null ? "" : imdbId.trim();
        if (safeId.isBlank()) {
            return new OmdbMovieDetails("", "", "", "", "", "", "");
        }

        if (omdbApiKey.isBlank()) {
            return new OmdbMovieDetails("", "", "", "", "", "", "");
        }

        String url = UriComponentsBuilder.fromUriString("https://www.omdbapi.com/")
                .queryParam("apikey", omdbApiKey)
                .queryParam("i", safeId)
                .build()
                .toUriString();

        Map<String, Object> body = restClient.get().uri(url).retrieve().body(Map.class);
        if (body == null) {
            return new OmdbMovieDetails("", "", "", "", "", "", "");
        }

        String plot = normalizeOmdbValue(asString(body.get("Plot")));
        String runtime = normalizeOmdbValue(asString(body.get("Runtime")));
        String director = normalizeOmdbValue(asString(body.get("Director")));
        String actors = normalizeOmdbValue(asString(body.get("Actors")));
        String released = normalizeOmdbValue(asString(body.get("Released")));
        String poster = normalizeOmdbValue(asString(body.get("Poster")));
        String title = normalizeOmdbValue(asString(body.get("Title")));

        return new OmdbMovieDetails(plot, runtime, director, actors, released, poster, title);
    }

    private String normalizeOmdbValue(String value) {
        if (value == null || value.isBlank() || "N/A".equalsIgnoreCase(value)) {
            return "";
        }
        return value.trim();
    }

    private LocalDate parseOmdbReleaseDate(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            return null;
        }

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.US);
            return LocalDate.parse(rawDate.trim(), formatter);
        } catch (Exception ex) {
            return null;
        }
    }

    private static class OmdbMovieDetails {
        private final String plot;
        private final String runtime;
        private final String director;
        private final String actors;
        private final String released;
        private final String poster;
        private final String title;

        public OmdbMovieDetails(String plot, String runtime, String director, String actors, String released,
                String poster, String title) {
            this.plot = plot;
            this.runtime = runtime;
            this.director = director;
            this.actors = actors;
            this.released = released;
            this.poster = poster;
            this.title = title;
        }

        public String plot() { return plot; }
        public String runtime() { return runtime; }
        public String director() { return director; }
        public String actors() { return actors; }
        public String released() { return released; }
        public String poster() { return poster; }
        public String title() { return title; }
    }

    private Integer fetchBookPageCount(String workKey) {
        String safeKey = workKey == null ? "" : workKey.trim();
        if (safeKey.isBlank() || !safeKey.startsWith("/works/")) {
            return null;
        }

        try {
            String url = UriComponentsBuilder.fromUriString("https://openlibrary.org")
                    .path(safeKey)
                    .path("/editions.json")
                    .queryParam("limit", 5)
                    .build()
                    .toUriString();

            Map<String, Object> body = restClient.get().uri(url).retrieve().body(Map.class);
            if (body == null) {
                return null;
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> entries = (List<Map<String, Object>>) body.getOrDefault("entries", List.of());
            for (Map<String, Object> entry : entries) {
                Integer pages = asInteger(entry.get("number_of_pages"));
                if (pages != null && pages > 0) {
                    return pages;
                }
                Integer parsed = parsePageCountFromPagination(asString(entry.get("pagination")));
                if (parsed != null && parsed > 0) {
                    return parsed;
                }
            }
        } catch (Exception ex) {
            return null;
        }

        return null;
    }

    private Integer parsePageCountFromPagination(String pagination) {
        if (pagination == null || pagination.isBlank()) {
            return null;
        }

        String normalized = pagination.replaceAll("[^0-9]+", " ").trim();
        if (normalized.isBlank()) {
            return null;
        }

        String[] parts = normalized.split("\\s+");
        Integer candidate = null;
        for (String part : parts) {
            Integer value = asInteger(part);
            if (value != null && value > 0) {
                candidate = value;
            }
        }

        return candidate;
    }

    private LocalDate parseDateSafely(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            return null;
        }

        String candidate = rawDate.trim();
        if (candidate.length() > 10) {
            candidate = candidate.substring(0, 10);
        }

        try {
            return LocalDate.parse(candidate);
        } catch (Exception ex) {
            return null;
        }
    }
}
