package com.proyectofinal.libreriacultural.services;

import java.util.List;

public class ExternalAlbumDetails {
    private final ExternalContentItem album;
    private final List<ExternalTrack> tracks;

    public ExternalAlbumDetails(ExternalContentItem album, List<ExternalTrack> tracks) {
        this.album = album;
        this.tracks = tracks;
    }

    public ExternalContentItem getAlbum() { return album; }
    public List<ExternalTrack> getTracks() { return tracks; }
}
