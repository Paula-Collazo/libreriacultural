package com.proyectofinal.libreriacultural.services;

public class ExternalTrack {
    private final Integer trackNumber;
    private final String title;
    private final String durationLabel;

    public ExternalTrack(Integer trackNumber, String title, String durationLabel) {
        this.trackNumber = trackNumber;
        this.title = title;
        this.durationLabel = durationLabel;
    }

    public Integer getTrackNumber() { return trackNumber; }
    public String getTitle() { return title; }
    public String getDurationLabel() { return durationLabel; }
}
