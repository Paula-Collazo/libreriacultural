package com.proyectofinal.libreriacultural.services;

import java.util.List;

public record ExternalAlbumDetails(ExternalContentItem album, List<ExternalTrack> tracks) {
}
