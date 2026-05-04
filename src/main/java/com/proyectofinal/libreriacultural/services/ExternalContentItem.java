package com.proyectofinal.libreriacultural.services;

import java.time.LocalDate;

public class ExternalContentItem {
    private final String source;
    private final String externalId;
    private final String title;
    private final String type;
    private final String description;
    private final LocalDate releaseDate;
    private final String coverUrl;
    private final String metricLabel;
    private final String artistName;
    private final String artistId;
    private final String directorName;
    private final String actors;

    public ExternalContentItem(String source, String externalId, String title, String type, String description,
            LocalDate releaseDate, String coverUrl, String metricLabel, String artistName, String artistId,
            String directorName, String actors) {
        this.source = source;
        this.externalId = externalId;
        this.title = title;
        this.type = type;
        this.description = description;
        this.releaseDate = releaseDate;
        this.coverUrl = coverUrl;
        this.metricLabel = metricLabel;
        this.artistName = artistName;
        this.artistId = artistId;
        this.directorName = directorName;
        this.actors = actors;
    }

    public String getSource() { return source; }
    public String getExternalId() { return externalId; }
    public String getTitle() { return title; }
    public String getType() { return type; }
    public String getDescription() { return description; }
    public LocalDate getReleaseDate() { return releaseDate; }
    public String getCoverUrl() { return coverUrl; }
    public String getMetricLabel() { return metricLabel; }
    public String getArtistName() { return artistName; }
    public String getArtistId() { return artistId; }
    public String getDirectorName() { return directorName; }
    public String getActors() { return actors; }
}
