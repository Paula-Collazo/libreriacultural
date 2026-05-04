package com.proyectofinal.libreriacultural.services;

import java.time.LocalDate;

public record ExternalContentItem(String source, String externalId, String title, String type, String description,
        LocalDate releaseDate, String coverUrl, String metricLabel, String artistName, String artistId,
        String directorName, String actors) {
}
