package com.proyectofinal.libreriacultural.services;

public class ExternalTrack {
    private int number;
    private String title;
    private String duration;

    public ExternalTrack(int number, String title, String duration) {
        this.number = number;
        this.title = title;
        this.duration = duration;
    }

    // Getters
    public int getNumber() { return number; }
    public String getTitle() { return title; }
    public String getDuration() { return duration; }
}
