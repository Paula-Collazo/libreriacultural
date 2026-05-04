package com.proyectofinal.libreriacultural.services;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
            if (t.equals("libro") || t.equals("book")) return searchGoogleBooks(query);
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

    private ExternalContentItem parseTmdbItem(Map<String, Object> res, String type) {
        String id = String.valueOf(res.get("id"));
        String title = asString(res.get(type.equals("movie") ? "title" : "name"));
        String poster = asString(res.get("poster_path"));
        String cover = poster.isEmpty() ? "" : "https://image.tmdb.org/t/p/w500" + poster;
        LocalDate date = parseDate(asString(res.get(type.equals("movie") ? "release_date" : "first_air_date")));
        return new ExternalContentItem("TMDb", id, title, type.equals("movie") ? "pelicula" : "serie", asString(res.get("overview")), date, cover, null, null, null, null, null);
    }

    public List<ExternalContentItem> searchSpotify(String query) {
        List<ExternalContentItem> items = new ArrayList<>();
        String token = getSpotifyToken();
        if (token == null) return items;
        try {
            java.net.URI url = UriComponentsBuilder.fromHttpUrl("https://api.spotify.com/v1/search")
                .queryParam("q", query)
                .queryParam("type", "album")
                .queryParam("limit", 10)
                .build().toUri();
            Map body = restClient.get().uri(url).header("Authorization", "Bearer " + token).retrieve().body(Map.class);
            if (body == null || body.get("albums") == null) return items;
            List<Map<String, Object>> results = (List<Map<String, Object>>) ((Map)body.get("albums")).get("items");
            if (results != null) for (Map<String, Object> res : results) items.add(parseSpotifyItem(res));
        } catch (Exception e) {
            System.err.println("[ERROR] Spotify Search: " + e.getMessage());
        }
        return items;
    }

    private ExternalContentItem parseSpotifyItem(Map<String, Object> res) {
        List<Map<String, Object>> artists = (List<Map<String, Object>>) res.get("artists");
        String artist = (artists != null && !artists.isEmpty()) ? asString(artists.get(0).get("name")) : "";
        String artistId = (artists != null && !artists.isEmpty()) ? asString(artists.get(0).get("id")) : "";
        List<Map<String, Object>> images = (List<Map<String, Object>>) res.get("images");
        String cover = (images != null && !images.isEmpty()) ? asString(images.get(0).get("url")) : "";
        return new ExternalContentItem("Spotify", asString(res.get("id")), asString(res.get("name")), "disco", "Álbum de " + artist, parseDate(asString(res.get("release_date"))), cover, null, artist, artistId, null, null);
    }

    private List<ExternalContentItem> searchGoogleBooks(String query) {
        List<ExternalContentItem> items = new ArrayList<>();
        try {
            String url = UriComponentsBuilder.fromUriString("https://www.googleapis.com/books/v1/volumes")
                .queryParam("q", query).queryParam("maxResults", 20).build().toUriString();
            Map body = restClient.get().uri(url).retrieve().body(Map.class);
            if (body == null) return items;
            List<Map<String, Object>> results = (List<Map<String, Object>>) body.get("items");
            if (results != null) {
                for (Map<String, Object> res : results) {
                    Map<String, Object> info = (Map<String, Object>) res.get("volumeInfo");
                    List<String> authors = (List<String>) info.get("authors");
                    Map<String, Object> imgs = (Map<String, Object>) info.get("imageLinks");
                    String cover = (imgs != null) ? asString(imgs.get("thumbnail")).replace("http://", "https://") : "";
                    items.add(new ExternalContentItem("GoogleBooks", asString(res.get("id")), asString(info.get("title")), "libro", asString(info.get("description")), parseDate(asString(info.get("publishedDate"))), cover, null, (authors != null && !authors.isEmpty()) ? authors.get(0) : "", null, null, null));
                }
            }
        } catch (Exception e) {
            System.err.println("[ERROR] GoogleBooks Search: " + e.getMessage());
        }
        return items;
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
            if ("GoogleBooks".equalsIgnoreCase(source)) return getGoogleBooksDetails(externalId);
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
            String poster = asString(body.get("poster_path"));
            result.put("coverUrl", poster.isEmpty() ? "" : "https://image.tmdb.org/t/p/w500" + poster);
            result.put("releaseDate", parseDate(asString(body.get(tmdbType.equals("movie") ? "release_date" : "first_air_date"))));
            result.put("externalId", id);
            result.put("type", type);
            Map<String, Object> credits = (Map<String, Object>) body.get("credits");
            if (credits != null) {
                List<Map<String, Object>> cast = (List<Map<String, Object>>) credits.get("cast");
                if (cast != null) {
                    result.put("actors", cast.stream().limit(10).map(c -> {
                        Map<String, String> a = new HashMap<>();
                        a.put("name", asString(c.get("name")));
                        String path = asString(c.get("profile_path"));
                        a.put("photoUrl", path.isEmpty() ? "" : "https://image.tmdb.org/t/p/w200" + path);
                        return a;
                    }).collect(Collectors.toList()));
                }
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
            Map body = restClient.get().uri("https://www.googleapis.com/books/v1/volumes/" + id).retrieve().body(Map.class);
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
            System.err.println("[ERROR] GoogleBooks Details: " + e.getMessage());
        }
        return result;
    }

    public Map<String, Object> getActorDetails(String name) {
        Map<String, Object> result = new HashMap<>();
        result.put("name", name);
        try {
            String wikiUrl = UriComponentsBuilder.fromUriString("https://es.wikipedia.org/w/api.php")
                .queryParam("action", "query").queryParam("prop", "extracts|pageimages").queryParam("exintro", "").queryParam("explaintext", "").queryParam("piprop", "original").queryParam("titles", name).queryParam("format", "json").queryParam("origin", "*").build().toUriString();
            Map body = restClient.get().uri(wikiUrl).retrieve().body(Map.class);
            Map<String, Object> page = (Map<String, Object>) ((Map)((Map)body.get("query")).get("pages")).values().iterator().next();
            result.put("bio", asString(page.get("extract")));
            if (page.get("original") != null) result.put("photoUrl", asString(((Map)page.get("original")).get("source")));

            String sUrl = UriComponentsBuilder.fromUriString("https://api.themoviedb.org/3/search/person").queryParam("api_key", TMDB_API_KEY).queryParam("query", name).build().toUriString();
            Map tmdbBody = restClient.get().uri(sUrl).retrieve().body(Map.class);
            List<Map<String, Object>> tmdbRes = (List<Map<String, Object>>) tmdbBody.get("results");
            if (tmdbRes != null && !tmdbRes.isEmpty()) {
                String pId = String.valueOf(tmdbRes.get(0).get("id"));
                String cUrl = UriComponentsBuilder.fromUriString("https://api.themoviedb.org/3/person/" + pId + "/movie_credits").queryParam("api_key", TMDB_API_KEY).queryParam("language", "es-ES").build().toUriString();
                Map cBody = restClient.get().uri(cUrl).retrieve().body(Map.class);
                List<Map<String, Object>> cast = (List<Map<String, Object>>) cBody.get("cast");
                if (cast != null) result.put("movies", cast.stream().limit(20).map(m -> parseTmdbItem(m, "movie")).collect(Collectors.toList()));
            }
        } catch (Exception e) {}
        return result;
    }

    private String asString(Object v) { return v == null ? "" : String.valueOf(v).trim(); }
    private String formatTrackDuration(long ms) { return String.format("%d:%02d", (ms/1000)/60, (ms/1000)%60); }
    private LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try { return LocalDate.parse(s.substring(0, 10)); } catch (Exception e) {
            try { if (s.length() >= 4) return LocalDate.of(Integer.parseInt(s.substring(0, 4)), 1, 1); } catch (Exception e2) {}
            return null;
        }
    }
}
