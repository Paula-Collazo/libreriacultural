package com.proyectofinal.libreriacultural.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "user_series_episode_progress", uniqueConstraints = {
        @UniqueConstraint(name = "uk_series_episode_per_user_content", columnNames = { "user_content_id", "season_number",
                "episode_number" })
})
public class UserSeriesEpisodeProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_content_id", nullable = false)
    private UserContent userContent;

    @Column(name = "season_number", nullable = false)
    private Integer seasonNumber;

    @Column(name = "episode_number", nullable = false)
    private Integer episodeNumber;

    @Column(nullable = false)
    private Boolean watched;

    // Getters y Setters manuales
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public UserContent getUserContent() { return userContent; }
    public void setUserContent(UserContent userContent) { this.userContent = userContent; }
    public Integer getSeasonNumber() { return seasonNumber; }
    public void setSeasonNumber(Integer seasonNumber) { this.seasonNumber = seasonNumber; }
    public Integer getEpisodeNumber() { return episodeNumber; }
    public void setEpisodeNumber(Integer episodeNumber) { this.episodeNumber = episodeNumber; }
    public Boolean getWatched() { return watched; }
    public void setWatched(Boolean watched) { this.watched = watched; }
}
